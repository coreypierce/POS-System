var Sections = Sections || {};
(function(Sections) {
	var sections = [];
	var boxes = [];

	var colors = [
		"#F00", "#FF0", "#0F0", "#0FF", "#00F"
	];
	
	var NONE = 0;
	
	var TOP = 1;
	var LEFT = 2;
	var RIGHT = 3;
	var BOTTOM = 4;

// ============================================ =============== ============================================ \\
// ============================================ Section Lookups ============================================ \\
	
	Sections.getSectionColor = function(section) {
		return section && colors[section.number % colors.length] || "#FFF";
	};
	
	function getSectionFromID(section_id) {
		for(var section of sections) {
			if(section.id == section_id) {
				return section;
			}
		}
		
		return null;
	}
	
	function getSection(table_id) {
		for(var section of sections) {
			for(var table of section.tables) {
				if(table == table_id) return section;
			}
		}
		
		return null;
	}
	
	Sections.forEachSection = function(action) {
		for(var section of sections) {
			action(section);
		}
	}
	
	function getBox(table_id) {
		for(var box of boxes) {
			if(box.table_id == table_id) {
				return box;
			}
		}
		
		return null;
	}
	
	Sections.getBoxFromElement = function(ele) {
		var id = ele.attr("id");
		
		for(var box of boxes) {
			if("section_box_" + box.box_id == id) {
				return box;
			}
		}
		
		return null;
	};

// ============================================ ============= ============================================ \\
// ============================================ Section Boxes ============================================ \\
	
	function areTouching(a, b) {
		if(a.top == b.bottom && a.left < b.right && b.left < a.right) return TOP;
		if(a.left == b.right && a.top < b.bottom && b.top < a.bottom) return LEFT;
		if(a.right == b.left && a.top < b.bottom && b.top < a.bottom) return RIGHT;
		if(a.bottom == b.top && a.left < b.right && b.left < a.right) return BOTTOM;
		
		return NONE;
	}
	
	var lastBoxId = 0;
	class SectionBox {
		constructor(item) {
			this.item = item;
			
			this.top = item.position.y;
			this.left = item.position.x;
			this.right = item.position.x + item.bounds.width;
			this.bottom = item.position.y + item.bounds.height;
			
			this.box_top = [];
			this.box_left = [];
			this.box_right = [];
			this.box_bottom = [];
			
			this.table_id = item.table.id;
			this.section = getSection(this.table_id);
			
			this.box_id = lastBoxId ++;
		} 

		assignSection(section_id) {
			if(this.section && this.section.id == section_id) return;
			
			this.section = getSectionFromID(section_id);
			this.section.tables.push(this.table_id);
			redrawBox(this);
			
			addTableToSection(this.section, this.table_id);
		}
		
		reset() {
			this.top = this.item.position.y;
			this.left = this.item.position.x;
			this.right = this.item.position.x + this.item.bounds.width;
			this.bottom = this.item.position.y + this.item.bounds.height;
			
			this.box_top = [];
			this.box_left = [];
			this.box_right = [];
			this.box_bottom = [];
			
			this.section = getSection(this.table_id);
		}
		
		grow() {
			var top_covered = this.box_top.length || this.top <= 0;
			var left_covered = this.box_left.length || this.left <= 0;
			var right_covered = this.box_right.length || this.right >= TableDisplay.layout_width;
			var bottom_covered = this.box_bottom.length || this.bottom >= TableDisplay.layout_height;
			
			// if all sides are covered then return
			if(top_covered && left_covered && right_covered && bottom_covered) return false;
			
			for(var b of boxes) {
				if(this == b) continue;
				
				switch(areTouching(this, b)) {
					case TOP: this.box_top.push(b); top_covered = true; break;
					case LEFT: this.box_left.push(b); left_covered = true; break;
					case RIGHT: this.box_right.push(b); right_covered = true; break;
					case BOTTOM: this.box_bottom.push(b); bottom_covered = true; break; 
					
					case NONE: continue;
				}

				// if all sides are covered then return
				if(top_covered && left_covered && right_covered && bottom_covered) return false;
			} 
			
			if(!top_covered) this.top --;
			if(!left_covered) this.left --;
			if(!right_covered) this.right ++;
			if(!bottom_covered) this.bottom ++;
			
			return true;
		}
		
		draw() {
			if(!this.element) {
				this.element = $("<div />")
					.attr("id", "section_box_" + this.box_id)
					.addClass("section-box");
				
				$("#tables_wrapper").append(this.element);
			}
			
			this.element && this.element.css({
				"top": 	this.top * TableDisplay.scaleY,
				"left": this.left * TableDisplay.scaleX,
				
				"width": 	(this.right - this.left)  * TableDisplay.scaleX + "px",
				"height": 	(this.bottom - this.top) * TableDisplay.scaleY + "px",
				
				"background": 	Sections.getSectionColor(this.section)
			});
			
			top: {
				for(var neighbor of this.box_top) 
					if(neighbor.section != this.section) break top;
				this.element && this.element.css("border-top", "none");
			}
			
			left: {
				for(var neighbor of this.box_left) 
					if(neighbor.section != this.section) break left;
				this.element && this.element.css("border-left", "none");
			}
			
			right: {
				for(var neighbor of this.box_right) 
					if(neighbor.section != this.section) break right;
				this.element && this.element.css("border-right", "none");
			}
			
			bottom: {
				for(var neighbor of this.box_bottom) 
					if(neighbor.section != this.section) break bottom;
				this.element && this.element.css("border-bottom", "none");
			}
		}
	};
	
// ============================================ ================= ============================================ \\
// ============================================ Section Functions ============================================ \\
	
	function addTable(section, table_id) {
		var box = getBox(table_id);
		if(!box) throw "what table?";

		// if the section doesn't exists
		if(!section) {
			// create a new section, and try again
			createSection(sec => addTable(sec, table_id));
			return;
		}
		
		// remove table from old section
		spliceTable(box);
		
		// update table in local status
		section.tables.push(table_id);
		box.section = section;
		
		// notify server of change
		addTableToSection(section, table_id);

		redrawBox(box);
	}
	
	function removeTable(table_id) {
		var box = getBox(table_id);
		if(!box) throw "what table?";

		// remove table from old section
		spliceTable(box);
		
		// notify server of change
		removeTableFromSection(table_id);

		redrawBox(box);
	}
	
	function spliceTable(box) {
		// check if table is already in a section
		if(box.section) {
			// remove table from list of section tables
			for(var index in box.section.tables) {
				if(box.section[index] == table_id) {
					box.section.splice(index, 1);
				}
			}
			
			// assign the section to null
			box.section = null;
			redrawBox(box);
		}
	}
	
	function redrawBox(box) {
		// redraw box, and it neighbors
		box.draw();
		
		for(var neighbor of box.box_top) neighbor.draw();
		for(var neighbor of box.box_left) neighbor.draw();
		for(var neighbor of box.box_right) neighbor.draw();
		for(var neighbor of box.box_bottom) neighbor.draw();
	}
	
	Sections.removeSection = function(section_id) {
		for(var index in sections) {
			if(sections[index].id == section_id) {
				for(var table_id of sections[index].tables) {
					var box = getBox(table_id);
					if(box) {
						box.section = null;
					}
				}
				
				Sections.draw();
				sections.splice(index, 1);
			}
		}
	}
	
// ============================================ ================ ============================================ \\
// ============================================ Server Functions ============================================ \\
	
	function querySections() {
		$.ajax({
			url: "/api/layout/section/list",
			method: "POST"
				
		}).done(function(data, status, xhr) {
			if(status == "success") {
				sections = data.sections;
				Sections.draw();
				
				// after the sections have been queried, if we're in edit mode
				Sections.setupEdit && Sections.setupEdit();

				Sections.regrow();
				Sections.draw();
			}
		});
	}
	
	Sections.createSection = function(callback) {
		$.ajax({
			url: "/api/layout/section/new",
			method: "POST"
				
		}).done(function(data, status, xhr) {
			if(status == "success") {
				var section = data;
				
				// record section
				sections.push(section);
				// invoke callback
				callback && callback(section);
			}
		});
	};
	
	function addTableToSection(section, table_id){
		$.ajax({
			url: "/api/layout/section/add_table",
			method: "POST",
			
			data: {
				"section_id": section.id,
				"table_id": table_id,
			}
				
		}).done(function(data, status, xhr) {
			if(status == "success") {
				var section = data;
			}
		});
	}
	
	function removeTableFromSection(table_id){
		$.ajax({
			url: "/api/layout/section/remove_table",
			method: "POST",
			
			data: {
				"table_id": table_id,
			}
				
		}).done(function(data, status, xhr) {
			if(status == "success") {
				var section = data;
			}
		});
	}
	
// ============================================ ================== ============================================ \\
// ============================================ External Functions ============================================ \\

	Sections.regrow = function() {
		for(var box of boxes) {
			box.reset();
		}
		
		var growing = true;
		while(growing) {
			growing = false;
			
			for(var box of boxes) {
				growing = box.grow() || growing;
			}
		}
	};
	
	Sections.draw = function() {
		for(var box of boxes) {
			box.draw();
		}
	};
	
	// ----------------------------------- Modify Sections ----------------------------------- \\
	
	Sections.addTable = function(section_id, table_id) {
		var section = getSectionFromID(section_id);
		if(section != null) throw "No section found with ID";
		
		addTable(section, table_id);
	};
	
	Sections.makeSection = function(table_id) {
		addTable(null, table_id);
	};
	
// ============================================ ===== ============================================ \\
// ============================================ Setup ============================================ \\
	
	Sections.setup = function() {
		TableDisplay.forEachTable(function(table) {
			boxes.push(new SectionBox(table));
		});
		
		querySections();
	};
	
})(Sections);