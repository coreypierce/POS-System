var TableDisplay = TableDisplay || { mode: "view" };
(function(disp) {
	if(!$) throw "Missing jQuery! Table-Display requires page to include jQuery";
	
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
	
	disp.findItemByElement = function(ele) {
		return id_map[ele.attr("id")];
	}
	
// ============================================ =============== ============================================ \\
// ============================================ Setup Functions ============================================ \\

	var items = [];
	var layout_width = 1, layout_height = 1;
	
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
		disp.setup && disp.setup(item.element);
	}
	
	function addItem(item) {
		// add item to the list
		items.push(item);
		// setup any additional properties
		setupElement(item);
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
	
	function moveItem(item, new_pos, new_bounds) {
		if(new_pos.x < 0 || new_pos.y < 0) return false;
		if(new_pos.x + new_bounds.width > layout_width) return false;
		if(new_pos.y + new_bounds.height > layout_height) return false;
		
		if(new_bounds.width <= 0 || new_bounds.height <= 0) return false;
		
		if(item instanceof Wall) {
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
				layout_width = data.width; 
				layout_height = data.height;
				
				parseItems(data.items);
				disp.resize();
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
			rotation = 0;
			position = { 'x': x, 'y': y };
			bounds = { 'width': width, 'height': height };
		}
		
		makeElement() { }
		
		resize() {
			this.makeElement();
			
			this.element && this.element.css({
				"top": 	this.position.y * disp.scaleY,
				"left": this.position.x * disp.scaleX,
				"width": 	this.bounds.width  * disp.scaleX + "px",
				"height": 	this.bounds.height * disp.scaleY + "px",
			});
		}
		
		draw(g) {}
		
		move(position, bounds) {
			moveItem(this, position, bounds);
		}
	}
	
// ============================================ =========== ============================================ \\
// ============================================ Table Class ============================================ \\
	
	class Table extends Item {
		makeElement() {
			// if we don't have an element
			if(!this.element) {
				// main div-element
				this.element = $("<div />")
					.attr("id", "table_" + this.table.id)
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
				
				// add to tables-wrapper
				wrapper.append(this.element);
			}
		}
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
		for(var x = 0, index = 0; x < w; x += disp.scaleX, index ++) {
			g.strokeStyle = index % grid_major == 0 ? grid_color_major : grid_color_minor;
			g.drawLine(x, 0, x, h);
		}
		
		for(var y = 0, index = 0; y < h; y += disp.scaleY, index ++) {
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
		g.fillRect(0, layout_height, layout_width, h / disp.scaleY);
		g.fillRect(layout_width, 0, w / disp.scaleX, layout_height);
	}

// ============================================ ================== ============================================ \\
// ============================================ External Functions ============================================ \\
	
	disp.resize = function() {
		w = canvas.width = canvas.clientWidth;
		h = canvas.height = canvas.clientHeight;
		
		// calculate the max-square scale factor
		disp.scaleX = w / layout_width;
		disp.scaleY = h / layout_height;
		
		if(disp.scaleX < disp.scaleY)
			disp.scaleY = disp.scaleX;
		else
			disp.scaleX = disp.scaleY;
		
		// resize all elements
		for(var item of items) {
			item.resize();
		}
		
		// redraw
		disp.redraw();
	}
	
	disp.redraw = function() {
		clearScreen(g);
		drawGrid(g);
		
		// scale here so items can ignore scale when drawing
		g.scale(disp.scaleX, disp.scaleY);
		
		// draw the limits of the restaurant
		drawLayoutBounds(g);
		
		// draw all items
		for(var item of items) {
			item.draw(g);
		}
	}

// ============================================ ============== ============================================ \\
// ============================================ Event Handlers ============================================ \\
	
	// on window-resize
	$(window).resize(function() {
		disp.resize();
	})
	
	// on document-load
	$(function() {
		disp.resize();
	});
	
	// request/load layout data
	requestLayout();

})(TableDisplay)