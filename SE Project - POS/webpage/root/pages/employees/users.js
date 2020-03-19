var Employees = Employees || {};
(function(Employees) {
	// mapping of employee-numbers to employee-object
	var employees = {};

// ============================================ ================= ============================================ \\
// ============================================ Element Functions ============================================ \\
	
	function generateEmployeeRow(user) {
		var ele = $("<div />")
			.attr("id", "employee_row_" + user.id)
			.addClass("user-disp_wrapper")
				.append($("<div />")
					.addClass("user-disp_contents")
					.append($("<span />").addClass("user-disp_number").text("#" + user.id))
					.append($("<span />").addClass("user-disp_name").text(user.firstname + " " + user.lastname))
					.append($("<span />").addClass("user-disp_role").text(user.role))
					
					.append($("<div />")
						.addClass("user-actions_wrapper")
						.append($("<span />").append($("<a />")
								.attr("data-tooltip", "Delete")
								.addClass("user-action_delete")
								.on("click", e => deleteUser(user.id))))
								
						.append($("<span />").append($("<a />")
								.attr("data-tooltip", "Toggle Activation")
								.addClass("user-action_active")
								.on("click", e => toggleActivation(user.id))))
								
						.append($("<span />").append($("<a />")
								.attr("data-tooltip", "Reset Password")
								.addClass("user-action_reset_password")
								.on("click", e => resetPassword(user.id))))
				));
		

		!user.active && ele.find(".user-action_active").addClass("state-deactive");
				
		$(".user-disp_list").append(ele);
		Tooltips && Tooltips.scanAndCreate(ele);
	}

// ============================================ ================ ============================================ \\
// ============================================ Button Functions ============================================ \\

	function deleteUser(id) {
		// TODO: Need better method of promitting user
		if(!confirm("Are you sure you want to delete User?")) return;
		
		$.ajax({
			url: "/api/employees/delete",
			method: "POST",
			
			data: {
				"id": id
			}
		})
		.done(function(data, status, xhr) {
			if(status == "success") {
				employees[id] = undefined;
				$("#employee_row_" + id).remove();
			}
		});
	}
	
	function resetPassword(id) {
		$.ajax({
			url: "/api/employees/reset_password",
			method: "POST",
			
			data: {
				"id": id
			}
		})
		.done(function(data, status, xhr) {
			if(status == "success") {
				// TODO: Find better way to display to admin
				alert("TEMP-PASSWORD: " + data.password);
			}
		});
	}
	
	function toggleActivation(id) {
		if(employees[id].active) {
			deactivateEmployee(id);
		} else {
			activateEmployee(id);
		}
	}
	
	function deactivateEmployee(id) {
		$.ajax({
			url: "/api/employees/deactivate",
			method: "POST",
			
			data: {
				"id": id
			}
		})
		.done(function(data, status, xhr) {
			$("#employee_row_" + id + " .user-action_active").addClass("state-deactive");
			employees[id].active = false;
		});
	}
	
	function activateEmployee(id) {
		$.ajax({
			url: "/api/employees/activate",
			method: "POST",
			
			data: {
				"id": id
			}
		})
		.done(function(data, status, xhr) {
			$("#employee_row_" + id + " .user-action_active").removeClass("state-deactive");
			employees[id].active = true;
		});
	}
	
// ============================================ ================ ============================================ \\
// ============================================ Server Functions ============================================ \\
	
	function queryAllEmployees() {
		$.ajax({
			url: "/api/employees/list",
			method: "POST",
		})
		.done(function(data, status, xhr) {
			for(var user of data.employees) {
				// add employee to mapping
				employees[user.id] = user;
				// add row for employee
				generateEmployeeRow(user);
			}
		});
	}
	
// ============================================ ============= ============================================ \\
// ============================================ API Functions ============================================ \\
	
	Employees.appendEmployee = function(user) {
		// add employee to mapping
		employees[user.id] = user;
		// add row for employee
		generateEmployeeRow(user);
	};
	
// ============================================ ===== ============================================ \\
// ============================================ Setup ============================================ \\
	
	$(function() {
		queryAllEmployees();
	})
	
})(Employees);