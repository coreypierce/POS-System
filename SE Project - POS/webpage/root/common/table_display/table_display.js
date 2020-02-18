var TableDisplay = {};
(function(disp, canvas_id) {
	if(!$) throw "Missing jQuery! Table-Display requires page to include jQuery";
	
	// query canvas, and get HTML-tag
	var canvas = $("#" + canvas_id)[0];
	if(!canvas) throw "Cannot find canvas '" + canvas_id + "'";
	
	// extract useful values
	var w = canvas.width, h = canvas.height;
	var g = canvas.getContext("2d");

	var grid_width = 10;
	// must be a whole number multiple of "grid_width"
	var grid_major = grid_width * 10;
	
	var grid_colour_major = "#d9d9d9"; 
	var grid_colour_minor = "#ffffff"; 
	
	g.drawLine = function(x0, y0, x1, y1) {
		var ctx = this;
		
		ctx.beginPath();
		ctx.moveTo(x0, y0);
		ctx.lineTo(x1, y1);
		ctx.stroke();
	} 
	
	function transformBounds(w, h) {
		// TODO: Calculate dimentions and offset
		//			Perform pre-draw transform
	}
	
	function clearScreen(g) {
//		g.fillStyle = "#f2f2f2";
//		g.fillRect(0, 0, w, h);
		
		g.clearRect(0, 0, w, h);
	}
	
	function drawGrid(g) {
		for(var x = 0; x < w; x += grid_width) {
			g.strokeStyle = x % grid_major == 0 ? grid_colour_major : grid_colour_minor;
			g.drawLine(x, 0, x, h);
		}
		
		for(var y = 0; y < h; y += grid_width) {
			g.strokeStyle = y % grid_major == 0 ? grid_colour_major : grid_colour_minor;
			// if it's a major-grid line, use new line, else old line
			g.globalCompositeOperation = y % grid_major == 0 ? "source-over" : "destination-over";
			g.drawLine(0, y, w, y);
		}
	}
	
	$(window).resize(function() {
		disp.resize();
	})
	
	$(function() {
		disp.resize();
	});
	
	disp.resize = function() {
		w = canvas.width = canvas.clientWidth;
		h = canvas.height = canvas.clientHeight;
		
		disp.redraw();
	}
	
	disp.redraw = function() {
		clearScreen(g);
		drawGrid(g);
	}
	
	disp.redraw();

})(TableDisplay, "{!canvas_id}")