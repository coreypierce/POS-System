var Sections = Sections || {};
(function(Sections) {
	var employees = [];
	var active_section_id = null;

// ============================================ ================== ============================================ \\
// ============================================ Element Generation ============================================ \\
	
	function generateSectionElement(section) {
		var ele = $("<div />")
			.attr("id", section.id)
			.on("click", onSelectSection)
			.addClass("edit-section_element")
			.append($("<div />")
				.addClass("edit-section_color")
				.css("background-color", Sections.getSectionColor(section))
			
			).append($("<span />")
				.addClass("edit-section_number")
				.text("#" + section.number)
				
			).append($("<select />")
				.on("change", onSelectEmployee)
				.append($("<option />")
					.attr("value", "")
					.text("<Unassigned>")
				)
			).append($("<i />")
				.on("click", event => deleteSection(section.id))
			);
		
		for(var employee of employees) {
			// if the employee is not selected
			if(!$("select option[value='" + employee.id + "']:selected").length) {
				ele.find("select")
					.append($("<option />")
						.attr("value", employee.id)
						.text(employee.firstname + " " + employee.lastname)
					);
			}
		}
		
		if(active_section_id == null) {
			ele.click();
		}
		
		$(".edit-section_list").append(ele);
	}
	
// ============================================ ============== ============================================ \\
// ============================================ Event Handlers ============================================ \\
	
	function deleteSection(section_id) {
		$(".edit-section_element[id='" + section_id + "']").remove();
		Sections.removeSection(section_id);
		sendDeleteSection(section_id);

		$(".edit-section_active").removeClass("edit-section_active");
		active_section_id = null;
		
		// force re-check of all employee options
		onSelectEmployee(null);
	}
	
	function onBoxClicked(event) {
		var box_ele = $(event.target).closest(".section-box")
		var box = Sections.getBoxFromElement(box_ele);
		
		if(box && active_section_id) {
			box.assignSection(active_section_id);
		}
	}
	
	function onSelectSection(event) {
		var select = $(event.target).closest(".edit-section_element");
		
		$(".edit-section_active").removeClass("edit-section_active");
		select.addClass("edit-section_active");
		
		active_section_id = select.attr("id");
	}
	
	function onSelectEmployee(event) {
		var new_assginee_id = event && $(event.target).val();

		for(var employee of employees) {
			if(!$("select option[value='" + employee.id + "']:selected").length) {
				// for every select that doesn't have the option
				$(".edit-section_element select:not(:has(option[value='" + employee.id + "']))")
					.each(function(index, ele) {
						$(ele).append($("<option />")
							.attr("value", employee.id)
							.text(employee.firstname + " " + employee.lastname)
						);
					});
			}
		}
		
		if(new_assginee_id) {
			$(".edit-section_element select option[value='" + new_assginee_id + "']:not(:selected)").remove();
		}
		
		event && sendAssignments(
				$(event.target).closest(".edit-section_element").attr("id"), 
				new_assginee_id || "<unassign>")
	}
	
// ============================================ ================ ============================================ \\
// ============================================ Server Functions ============================================ \\
	
	function queryActiveServers() {
		$.ajax({
			url: "/api/employees/list",
			method: "POST",
			
			data: {
				role: "Server",
				active: true
			}
		})
		.done(function(data, status, xhr) {
			employees = data.employees;
			// force re-check of all employee options
			onSelectEmployee(null);
			
			Sections.forEachSection(function(section) {
				if(section.assignee) {
					$(".edit-section_element[id='" + section.id + "']" +
						" select option[value='" + section.assignee.id + "']").prop('selected', true);
					$("select option[value='" + section.assignee.id + "']:not(:selected)").remove();
				}
			});
		});
	}
	
	function sendAssignments(section_id, assignee_id) {
		$.ajax({
			url: "/api/layout/section/assign",
			method: "POST",
				
			data: {
				"section_id": section_id,
				"assignee_id": assignee_id
			}
		});
	}
	
	function sendDeleteSection(section_id) {
		$.ajax({
			url: "/api/layout/section/delete",
			method: "POST",
				
			data: {
				"section_id": section_id
			}
		});
	}

// ============================================ ============= ============================================ \\
// ============================================ API Functions ============================================ \\

	Sections.makeNewSection = function() {
		Sections.createSection(function(section) {
			generateSectionElement(section);
		});
	};

// ============================================ ===== ============================================ \\
// ============================================ Setup ============================================ \\
	
	Sections.setupEdit = function() {
		queryActiveServers();
		$(".section-box").on("click", onBoxClicked);
		Sections.forEachSection(generateSectionElement);
	};
	
})(Sections)