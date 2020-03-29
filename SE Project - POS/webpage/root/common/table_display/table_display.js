var TableDisplay = TableDisplay || { mode: "view" };
(function(TableDisplay) {
	if(!$) throw "Missing jQuery! Table-Display requires page to include jQuery";
	if(!TableDisplay.mode) TableDisplay.mode = "view";
	
	// query canvas, and get HTML-tag
	var canvas = $("#table_canvas")[0];
	if(!canvas) throw "Cannot find canvas '#table_canvas'";
	
	// query table-wrapper
	var wrapper = $("#tables_wrapper");
	if(wrapper.length <= 0) throw "Cannot find table-wrapper '#tables_wrapper'";
	
// ============================================ ============== ============================================ \\
// ============================================ Graphics Setup ============================================ \\
	
	// extract useful values
	var w = canvas.width, h = canvas.height;
	var g = canvas.getContext("2d");

	// major grid spacing
	var grid_major = 5;
	
	// grid coloring
	var grid_color_major = "#d9d9d9"; 
	var grid_color_minor = "#ffffff"; 
	
	var wall_color = "#3f3f3f"; 
	
// ============================================ ============== ============================================ \\
// ============================================ Element Lookup ============================================ \\
	
	var id_map = {};
	
	TableDisplay.findItemByElement = function(ele) {
		return id_map[ele.attr("id")];
	}
	
	TableDisplay.forEachTable = function(func) {
		for(var id in id_map) {
			// if the item is a table
			if(id_map[id] instanceof Table) {
				func && func(id_map[id]);
			}
		}
	}

// ============================================ =========== ============================================ \\
// ============================================ Layout Size ============================================ \\
	
	TableDisplay.layout_width = 1;
	TableDisplay.layout_height = 1;
	
	var layoutSizeListeners = [];
	
	TableDisplay.addLayoutSizeListener = function(listener) {
		layoutSizeListeners.push(listener);
	};
	
// ============================================ =============== ============================================ \\
// ============================================ Setup Functions ============================================ \\

	var items = [];
	
	function parseItems(items_data) {
		for(var item of items_data) {
			// assign 'class' to Object, based on if item contains table-data 
			Object.setPrototypeOf(item, item.table && Table.prototype || Wall.prototype);
			setupElement(item);
		}
		
		// assign items to field
		items = items_data;
	}
	
	function setupElement(item) {
		// add mappings for all item
		item.makeElement();
		id_map[item.element.attr("id")] = item;
		
		// if edit-mode
		TableDisplay.setup && TableDisplay.setup(item.element);
	}
	
	TableDisplay.addItem = function(item) {
		// add item to the list
		items.push(item);
		// setup any additional properties
		setupElement(item);
	}
	
	TableDisplay.removeItem = function(item) {
		for(var i = 0; i < items.length; i ++) {
			if(items[i] == item) {
				item.element.remove();
				id_map[item.element.attr("id")] = undefined;
				items.splice(i, 1);
				return;
			}
		} 
	} 
	
// ============================================ ================== ============================================ \\
// ============================================ Movement Functions ============================================ \\
	
	function doesIntersect(p0, b0, p1, b1) {
		if(p0.x + b0.width - 1 < p1.x) return false;
		if(p0.y + b0.height - 1 < p1.y) return false;
		
		if(p1.x + b1.width - 1 < p0.x) return false;
		if(p1.y + b1.height - 1 < p0.y) return false;
		
		return true;
	}
	
	function moveItem(item, new_pos, new_bounds, new_rotation, override) {
		if(new_pos.x < 0 || new_pos.y < 0) return false;
		if(new_pos.x + new_bounds.width > TableDisplay.layout_width) return false;
		if(new_pos.y + new_bounds.height > TableDisplay.layout_height) return false;
		
		if(new_bounds.width <= 0 || new_bounds.height <= 0) return false;

		// auto-assign rotation, if none was provided
		typeof(new_rotation) == 'undefined' && (new_rotation = item.rotation);
		
		if(item instanceof Wall && !override) {
			for(var check of items) {
				// walls are allowed to intersect other walls
				if(check instanceof Wall) continue;
				if(doesIntersect(new_pos, new_bounds, check.position, check.bounds)) {
					return false;
				}
			}
			
		} else {
			// tables are checked against everything
			for(var check of items) {
				// do not check against self
				if(check == item) continue;
				if(doesIntersect(new_pos, new_bounds, check.position, check.bounds)) {
					return false;
				}
			}
		}
		
		item.position = new_pos;
		item.bounds = new_bounds;
		item.rotation = new_rotation;
		return true;
	}
	
// ============================================ ============ ============================================ \\
// ============================================ Request Data ============================================ \\
	
	function requestLayout() {
		$.ajax({
			url: "/api/layout/get_layout",
			method: "GET",
		})
		.done(function(data, status, xhr) {
			// if request was successful
			if(status == "success") {
				// extract useful data
				TableDisplay.layout_width = data.width; 
				TableDisplay.layout_height = data.height;
				
				parseItems(data.items);
				TableDisplay.resize();
				
				// call the size listeners when the size changes due to request
				layoutSizeListeners.forEach(listener => 
					listener(TableDisplay.layout_width, TableDisplay.layout_height));

				Sections && Sections.setup();
			}
		})
		.fail(function(xhr, status, error) {
			// TODO: Replace with proper message
			alert("An Error has occured! " + status + " --- " + error);
		});
	}
	
// ============================================ =========== ============================================ \\
// ============================================ Item Class ============================================ \\
	
	class Item {
		element;
		
		rotation;
		position;
		bounds;
		
		constructor(x, y, width, height) {
			this.rotation = 0;
			this.position = { 'x': x, 'y': y };
			this.bounds = { 'width': width, 'height': height };
		}
		
		makeElement() { }
		updateElement() { }
		
		resize() {
			this.makeElement();
			
			this.element && this.element.css({
				"top": 	this.position.y * TableDisplay.scaleY,
				"left": this.position.x * TableDisplay.scaleX,
				
				"width": 	this.bounds.width  * TableDisplay.scaleX + "px",
				"height": 	this.bounds.height * TableDisplay.scaleY + "px",
			});
		}
		
		draw(g) {}
		
		move(position, bounds, rotation, override) {
			return moveItem(this, position, bounds, rotation, override);
		}
	}
	
// ============================================ =========== ============================================ \\
// ============================================ Table Class ============================================ \\

	var nextTableID = 0;
	class Table extends Item {
		makeElement() {
			// if we don't have an element
			if(!this.element) {
				// main div-element
				this.element = $("<div />")
					.attr("id", "table_" + nextTableID ++)
					.addClass("table-icon");
				
				// table-label
				var label = $("<span />")
					.text(this.table.name);
				
				// svg-icon
				var image = $(document.createElementNS('http://www.w3.org/2000/svg', "svg"))
					.attr("width", "100%")
					.attr("height", "100%")
					.append($(document.createElementNS('http://www.w3.org/2000/svg', "use"))
						.attr("href", "#" + this.table.icon)
					);
				
					 if(this.table.status == "Open") this.element.addClass("table-status_open");
				else if(this.table.status == "Seated") this.element.addClass("table-status_seated");
				else if(this.table.status == "Order_Placed") this.element.addClass("table-status_ordered");
				else if(this.table.status == "Check_Printed") this.element.addClass("table-status_checkout");
				else this.element.addClass("table-status_unknown");
				
				this.element.append(label);
				this.element.append(image);

				// add class-flags for editing the item
				this.element
					.addClass("table-selectable").addClass("table-draggable")
					.addClass("table-edit_rotatable");
				
				this.element.on("click", e => TableDisplay.mode != 'edit' && openTableActions(e, this));
				
				// add to tables-wrapper
				wrapper.append(this.element);
			}
		}
		
		resize() {
			super.resize();
			
			var w = this.element.css("width");
			w = w.substr(0, w.length - 2);

			var h = this.element.css("height");
			h = h.substr(0, h.length - 2);
			
			var max = Math.max(w, h) + "px";
			
			this.element.find("svg")
				.attr("width", max)
				.attr("height", max)
				.css("transform", "translate(-50%, -50%) rotateZ(" + this.rotation + "deg)");
		}

		updateElement() {
			this.element
				.removeClass("table-status_open")
				.removeClass("table-status_seated")
				.removeClass("table-status_ordered")
				.removeClass("table-status_checkout")
				.removeClass("table-status_unknown");
			
				if(this.table.status == "Open") this.element.addClass("table-status_open");
			else if(this.table.status == "Seated") this.element.addClass("table-status_seated");
			else if(this.table.status == "Order_Placed") this.element.addClass("table-status_ordered");
			else if(this.table.status == "Check_Printed") this.element.addClass("table-status_checkout");
			else this.element.addClass("table-status_unknown");
				
			this.element.find("span").text(this.table.name);
		}
	}
	
	function openTableActions(event, item) {
		var tableID = item && item.table && item.table.id;
		if(!tableID) return;
		
		var menu = $("#table_context_menu");

		var bounds = $(".table-display_wrapper")[0].getBoundingClientRect();
		var ele_bounds = item.element[0].getBoundingClientRect();
		var menu_bounds = menu[0].getBoundingClientRect();
		
		// calculate position relative to parent
		var top = (ele_bounds.bottom + ele_bounds.top) / 2 - bounds.top; // event.pageY
		var left = (ele_bounds.right + ele_bounds.left) / 2 - bounds.left; // event.pageX
		
		// make sure it doesn't roll of the edge of the page
		if(bounds.top + top + menu_bounds.height > window.innerHeight) {
			top -= menu_bounds.height;
		}

		// make sure it doesn't roll of the edge of the page
		if(bounds.left + left + menu_bounds.width > window.innerWidth) {
			left -= menu_bounds.width;
		}
		
		// set position and visible
		menu.addClass("table-menu_open")
			.removeClass("table-menu-status_Open")
			.removeClass("table-menu-status_Seated")
			.removeClass("table-menu-status_Order_Placed")
			.removeClass("table-menu-status_Check_Printed")
			.addClass("table-menu-status_" + item.table.status)
			.css("top", top).css("left", left);
		
		TableDisplay.menu_item = item;
	
		// prevent window from being called
		event.stopPropagation();
	}
	
// ============================================ ========== ============================================ \\
// ============================================ Wall Class ============================================ \\
	
	var nextWallID = 0;
	class Wall extends Item {
		makeElement() {
			// if we don't have an element
			if(!this.element) {
				// main div-element
				this.element = $("<div />")
					.attr("id", "wall_" + nextWallID ++)
					.addClass("table-wall");
				
				// add class-flags for editing the item
				this.element
					.addClass("table-selectable").addClass("table-draggable")
					.addClass("table-edit_scalable");
				
				// add to tables-wrapper
				wrapper.append(this.element);
			}
		}
		
		draw(g) {
			g.fillStyle = wall_color;
			g.fillRect(
				this.position.x + .25, this.position.y + .25, 
				this.bounds.width - .5, this.bounds.height - .5);
		}
	}
	
	TableDisplay.Wall = Wall;
	TableDisplay.Table = Table;

// ============================================ ========= ============================================ \\
// ============================================ Grid Draw ============================================ \\
	
	function clearScreen(g) {
//		g.fillStyle = "#f2f2f2";
//		g.fillRect(0, 0, w, h);
		
		g.setTransform(1, 0, 0, 1, 0, 0);
		g.clearRect(0, 0, w, h);
	}

	// define extra graphics functions
	g.drawLine = function(x0, y0, x1, y1) {
		var ctx = this;
		
		ctx.beginPath();
		ctx.moveTo(x0, y0);
		ctx.lineTo(x1, y1);
		ctx.stroke();
	} 
	
	function drawGrid(g) {
		for(var x = 0, index = 0; x < w; x += TableDisplay.scaleX, index ++) {
			g.strokeStyle = index % grid_major == 0 ? grid_color_major : grid_color_minor;
			g.drawLine(x, 0, x, h);
		}
		
		for(var y = 0, index = 0; y < h; y += TableDisplay.scaleY, index ++) {
			g.strokeStyle = index % grid_major == 0 ? grid_color_major : grid_color_minor;
			// if it's a major-grid line, use new line, else old line
			g.globalCompositeOperation = index % grid_major == 0 ? "source-over" : "destination-over";
			g.drawLine(0, y, w, y);
		}

		// reset to default
		g.globalCompositeOperation = "source-over";
	}
	
	function drawLayoutBounds(g) {
		g.fillStyle = "#0003";
		g.fillRect(0, TableDisplay.layout_height, TableDisplay.layout_width, h / TableDisplay.scaleY);
		g.fillRect(TableDisplay.layout_width, 0, w / TableDisplay.scaleX, TableDisplay.layout_height);
	}

// ============================================ ================== ============================================ \\
// ============================================ External Functions ============================================ \\
	
	TableDisplay.resize = function() {
		w = canvas.width = canvas.clientWidth;
		h = canvas.height = canvas.clientHeight;
		
		// calculate the max-square scale factor
		TableDisplay.scaleX = w / TableDisplay.layout_width;
		TableDisplay.scaleY = h / TableDisplay.layout_height;
		
		if(TableDisplay.scaleX < TableDisplay.scaleY)
			TableDisplay.scaleY = TableDisplay.scaleX;
		else
			TableDisplay.scaleX = TableDisplay.scaleY;
		
		// resize all elements
		for(var item of items) {
			item.resize();
		}
		
		// redraw
		TableDisplay.redraw();
		
		Sections && (Sections.regrow(), Sections.draw());
	}
	
	TableDisplay.redraw = function() {
		clearScreen(g);
		drawGrid(g);
		
		// scale here so items can ignore scale when drawing
		g.scale(TableDisplay.scaleX, TableDisplay.scaleY);
		
		// draw the limits of the restaurant
		drawLayoutBounds(g);
		
		// draw all items
		for(var item of items) {
			item.draw(g);
		}
	}
	
	TableDisplay.closeMenu = function() {
		$("#table_context_menu").removeClass("table-menu_open");
		TableDisplay.menu_item = null;
	}

// ============================================ ============== ============================================ \\
// ============================================ Event Handlers ============================================ \\
	
	$(window).on("click", TableDisplay.closeMenu);
	
	// on window-resize
	$(window).resize(function() {
		TableDisplay.resize();
	})
	
	// on document-load
	$(function() {
		TableDisplay.resize();
	});
	
	// request/load layout data
	requestLayout();

})(TableDisplay)