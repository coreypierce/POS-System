var TableDisplay = {};
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
	
	// draw scale factor
	var scaleX, scaleY;
	
// ============================================ ============ ============================================ \\
// ============================================ __ Functions ============================================ \\

	var items = [];
	var layout_width = 1, layout_height = 1;
	
	function parseItems(items_data) {
		for(var item of items_data) {
			// assign 'class' to Object, based on if item contains table-data 
			Object.setPrototypeOf(item, item.table && Table.prototype || Wall.prototype)
		}
		
		// assign items to field
		items = items_data;
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
// ============================================ Table Class ============================================ \\
	
	class Table {
		element;
		table;
		
		position;
		bounds;
		
		constructor(x, y, width, height) {
			position = { 'x': x, 'y': y };
			bounds = { 'width': width, 'height': height };
		}
		
		makeElement() {
			// if we don't have an element
			if(!this.element) {
				// main div-element
				this.element = $("<div />")
					.attr("id", "table_" + this.table.id)
					.addClass("table-icon")
				
				// table-label
				var label = $("<span />")
					.text(this.table.name);
				
				// svg-icon
				var image = $("<svg />")
					.attr("width", "100%")
					.attr("height", "100%")
					.append($("<use />")
						.attr("href", "#" + this.table.icon)
					);
				
				this.element.append(label);
				this.element.append(image);
				
				// add to tables-wrapper
				wrapper.append(this.element);
			}
		}
		
		resize() {
			this.makeElement();
			
			this.element.css({
				"top": 	this.position.y * scaleY,
				"left": this.position.x * scaleX,
				"width": 	this.bounds.width  * scaleX + "px",
				"height": 	this.bounds.height * scaleY + "px",
			});
			
//			this.element.find("svg")
//				.attr("width", this.bounds.width * scaleX)
//				.attr("height", this.bounds.height * scaleY);
		}
		
		draw(g) {}
	}
	
// ============================================ =========== ============================================ \\
// ============================================ Wall Class ============================================ \\
	
	class Wall {
		constructor(x, y, width, height) {
			position = { 'x': x, 'y': y };
			bounds = { 'width': width, 'height': height };
		}
		
		resize() {}
		
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
		for(var x = 0, index = 0; x < w; x += scaleX, index ++) {
			g.strokeStyle = index % grid_major == 0 ? grid_color_major : grid_color_minor;
			g.drawLine(x, 0, x, h);
		}
		
		for(var y = 0, index = 0; y < h; y += scaleY, index ++) {
			g.strokeStyle = index % grid_major == 0 ? grid_color_major : grid_color_minor;
			// if it's a major-grid line, use new line, else old line
			g.globalCompositeOperation = index % grid_major == 0 ? "source-over" : "destination-over";
			g.drawLine(0, y, w, y);
		}

		// reset to default
		g.globalCompositeOperation = "source-over";
	}

// ============================================ ================== ============================================ \\
// ============================================ External Functions ============================================ \\
	
	disp.resize = function() {
		w = canvas.width = canvas.clientWidth;
		h = canvas.height = canvas.clientHeight;
		
		// calculate the max-square scale factor
		scaleX = w / layout_width;
		scaleY = h / layout_height;
		
		if(scaleX < scaleY)
			scaleY = scaleX;
		else
			scaleX = scaleY;
		
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
		g.scale(scaleX, scaleY);
		
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