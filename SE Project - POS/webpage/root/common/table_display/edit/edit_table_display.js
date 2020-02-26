var TableDisplay = TableDisplay || { mode: "edit" };
(function(edit) {
	edit.mode = 'edit';

	var edit_disp = $(".table-display_wrapper.tables-edit_mode");
	if(!edit_disp) { // if the element isn't loaded yet
		$(function() {
			// wait for document to load, then look again
			edit_disp = $(".table-display_wrapper.tables-edit_mode");
			
			// if still missing
			if(!edit_disp) throw "cannot find editable table-display";
		})
	}
	

// ============================================ ======== ============================================ \\
// ============================================ Drggable ============================================ \\

	class Draggable {
		constructor(ele, event) {
			this.element = ele;

			this.start_x = event.screenX;
			this.start_y = event.screenY;
			
			this.init();
		} 
		
		init() {}
		
		drag(e) {}
		drop(e) {}
		
		cancel(e) {}
	}

// ============================================ ========== ============================================ \\
// ============================================ Drag-Scale ============================================ \\
	
	class ScaleHandle extends Draggable {
		init() {
			this.item = TableDisplay.findItemByElement(selected_target);
			if(!this.item) {
				console.warn("could not find a table-item for selected-element");
				return;
			}
			
			// record starting size and location, of item
			this.base_size = Object.assign({}, this.item.bounds);
			this.base_position = Object.assign({}, this.item.position);
			
			var movement = this.element.data("axis");
			this.axis = movement[0];
			this.direction = movement[1] == '-' && -1 || 1;
		}
		
		drag(e) {
			if(!this.item) return;
			
			var x = event.screenX, y = event.screenY;
			var dx = x - this.start_x, dy = y - this.start_y;
			dx = Math.round(dx / edit.scaleX); dy = Math.round(dy / edit.scaleY);
			
			var pos = Object.assign({}, this.item.position);
			var bounds = Object.assign({}, this.item.bounds);
			
			// if this is for the horizontal-axis, only change width
			if(this.axis == 'h')
				bounds.width = this.base_size.width + dx * this.direction;
			else
				bounds.height = this.base_size.height + dy * this.direction;
			
			// if moving "backwards" position also needs to be adjusted 
			if(this.direction < 0) {
				// if this is for the horizontal-axis, only change x
				if(this.axis == 'h')
					pos.x = this.base_position.x + dx;
				else
					pos.y = this.base_position.y + dy;
			}
			
			this.item.move(pos, bounds);
			
			// update element position
			this.item.resize();
			// update graphic
			TableDisplay.redraw();
			// update edit-box's position
			positionSelectBox();
		}
		
		cancel(e) {
			if(!this.item) return;
			
			this.item.position = this.base_position;
			this.item.bounds = this.base_size;
			
			// update element position
			this.item.resize();
			// update graphic
			TableDisplay.redraw();
			// update edit-box's position
			positionSelectBox();
		}
	}
	
// ============================================ =========== ============================================ \\
// ============================================ Drag-Rotate ============================================ \\
	
	function calcBounds(rot, position, bounds) {
		// get corners
		var x0 = position.x, y0 = position.y;
		var x1 = x0 + bounds.width, y1 = y0 + bounds.height;
		
		// find center point
		var cx = (x1 + x0) / 2, cy = (y1 + y0) / 2;
		
		// translate to origin
		x0 -= cx; x1 -= cx;
		y0 -= cy; y1 -= cy;
		
		var angle = rot / 180 * Math.PI;

		// rotate points
		var rx0 = x0 * Math.cos(angle) - y0 * Math.sin(angle);
		var ry0 = x0 * Math.sin(angle) + y0 * Math.cos(angle);

		var rx1 = x1 * Math.cos(angle) - y1 * Math.sin(angle);
		var ry1 = x1 * Math.sin(angle) + y1 * Math.cos(angle);
		
		// translate back
		rx0 += cx; rx1 += cx;
		ry0 += cy; ry1 += cy;
		
		// find corners
		var right = Math.max(rx0, rx1);
		var left = Math.min(rx0, rx1);
		var top = Math.max(ry0, ry1);
		var bottom = Math.min(ry0, ry1);
		
		// calculate bounds
		bounds.width = Math.round(right - left);
		bounds.height = Math.round(top - bottom);
		
		// set position
		position.x = Math.round(left);
		position.y = Math.round(bottom);
	}
	
	class RotationHandle extends Draggable {
		init() {
			this.item = TableDisplay.findItemByElement(selected_target);
			if(!this.item) {
				console.warn("could not find a table-item for selected-element");
				return;
			}
			
			// record starting size and location, of item
			var bounds = Object.assign({}, this.item.bounds);
			var position = Object.assign({}, this.item.position);
			
			var rot_amount = this.element.data("dir") == 'ccw' ? 90 : -90;
			var rotation = this.item.rotation + rot_amount;
			rotation = (rotation + 360) % 360;
			
			// calculate rotation bounds, and update
			calcBounds(rot_amount, position, bounds);
			this.item.move(position, bounds, rotation);
			
			// update element position
			this.item.resize();
			// update graphic
			TableDisplay.redraw();
			// update edit-box's position
			positionSelectBox();
		}
		
		drag(e) { }
		cancel(e) { }
	}
	
// ============================================ ========= ============================================ \\
// ============================================ Drag-Item ============================================ \\
	
	class TableItem extends Draggable {
		init() {
			this.item = TableDisplay.findItemByElement(this.element);
			if(!this.item) {
				console.warn("could not find a table-item for provided element");
				return;
			}
			
			// record starting location, of item
			this.base_position = Object.assign({}, this.item.position);
		}
		
		drag(e) {
			if(!this.item) return;
			
			var x = event.screenX, y = event.screenY;
			var dx = x - this.start_x, dy = y - this.start_y;
			dx = Math.round(dx / edit.scaleX); dy = Math.round(dy / edit.scaleY);
			
			var pos = {
				x: this.base_position.x + dx,
				y: this.base_position.y + dy
			};
			
			this.item.move(pos, this.item.bounds);
			
			// update element position
			this.item.resize();
			// update graphic
			TableDisplay.redraw();
			// update edit-box's position
			positionSelectBox();
		}
		
		cancel(e) {
			if(!this.item) return;
			this.item.position = this.base_position;
			
			// update element position
			this.item.resize();
			// update graphic
			TableDisplay.redraw();
			// update edit-box's position
			positionSelectBox();
		}
	}
	
// ============================================ =========== ============================================ \\
// ============================================ Select Item ============================================ \\

	var selected_target = null;
	var drag_target = null;
	
	function selectItem(e) {
		// e: item-element
		
		selected_target = e;
		positionSelectBox();
	}
	
	function removeSelection() {
		selected_target = null;
		$(".tables-edit_wrapper").removeClass("table-edit_active");
	}
	
	function positionSelectBox() {
		var bounds = selected_target[0].getBoundingClientRect();
		var wrapper = $(".tables-edit_wrapper");
		
		wrapper.css({
			top: selected_target.css("top"),
			left: selected_target.css("left"),
			
			width: bounds.width + "px",
			height: bounds.height + "px",
			
		}).addClass("table-edit_active");
		
		// if selected-item is scalable
		if(selected_target.hasClass("table-edit_scalable")) 
			wrapper.addClass("table-edit_show_strech") 
		else 
			wrapper.removeClass("table-edit_show_strech");

		// if selected-item is rotatable
		if(selected_target.hasClass("table-edit_rotatable")) 
			wrapper.addClass("table-edit_show_rotate") 
		else 
			wrapper.removeClass("table-edit_show_rotate");
	}
	
// ============================================ ========= ============================================ \\
// ============================================ Listeners ============================================ \\

	// on mouse-up (anywhere) drop target
	$(window).on("mouseup", e => (drag_target && drag_target.drop(e), drag_target = null));
	$(window).on("mousedown", e => 
		// if right-mouse was pressed
		e.button == 2 ?
			(drag_target && drag_target.cancel(e), drag_target = null) : 
			// else, if non-draggable item selected
			!$(e.target).closest(".table-draggable").length && removeSelection()
	);
	
	$(window).on("mousemove", e => drag_target && drag_target.drag(e));
	
	
	// mouse-down listener added to table/wall/handle elements with "table-draggable" class
	function onDraggableMouseDown(event) {
		// ignore right-clicks
		if(event.button == 2) return;
		var e = $(event.target).closest(".table-draggable");
		
		// yah... just making sure everyone knows, could cause issues
		if(drag_target != null) console.warn("mousedown without calling mouseup first!")
		
		drag_target = 
			e.hasClass("tables-edit_strech_arrow") && new ScaleHandle(e, event) ||
			e.hasClass("tables-edit_rotate_arrow") && new RotationHandle(e, event) ||
			new TableItem(e, event);
		
		// check if we can/should select the item
		if(e.hasClass("table-selectable")) {
			selectItem(e);
		}
	}
	
// ============================================ ===== ============================================ \\
// ============================================ Setup ============================================ \\

	edit.setup = function(e) {
		if(!e) {
			$(".table-draggable:not(.table-edit_setup)")
				.on("mousedown", onDraggableMouseDown)
				.bind("contextmenu", e => false )
				.addClass("table-edit_setup");
			
			$("#table_canvas").bind("contextmenu", e => false);
			
		} else {
			e.on("mousedown", onDraggableMouseDown)
			.bind("contextmenu", e => false )
			.addClass("table-edit_setup");
		}
	};
	
	$(function() { 
		edit.setup(); 
	})
	
})(TableDisplay);