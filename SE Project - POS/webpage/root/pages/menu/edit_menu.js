var Menu = Menu || {};
(function(Menu) {

// ============================================ ================ ============================================ \\
// ============================================ Server Functions ============================================ \\
	
	function addItem(category, name, price) {
		$.ajax({
			url: "/api/menu/new",
			method: "POST",
			
			data: {
				"category": category,
				"name": name,
				"price": price
			}
		})
		.done(function(data, status, xhr) {
			// if request was successful
			if(status == "success") {
				Menu.appendItem(data);
			}
		});
	}
	
	function updateItem(id, category, name, price) {
		$.ajax({
			url: "/api/menu/update",
			method: "POST",
			
			data: {
				"id": id,
				"category": category,
				"name": name,
				"price": price
			}
		})
		.done(function(data, status, xhr) {
			// if request was successful
			if(status == "success") {
				var ele = $("#menu-item_" + id);
				ele.html(data.name + "<span>$" + parseFloat(data.price).toFixed(2) + "</span>");
				
				if(!Menu.getCategories()[data.category_id]) {
					Menu.appendCategory(data.category_id, category);
				}
				
				$("#menu-group_" + data.category_id).append(ele);
				Object.assign(Menu.getItem(id), data);
			}
		});
	}
	
	function deleteItem(id) {
		$.ajax({
			url: "/api/menu/delete",
			method: "POST",
			
			data: {
				"id": id,
			}
		})
		.done(function(data, status, xhr) {
			// if request was successful
			if(status == "success") {
				$("#menu-item_" + id).remove();
			}
		});
	}
	
// ============================================ ============= ============================================ \\
// ============================================ API Functions ============================================ \\
	
	Menu.addItem = function(category, name, price) {
		addItem(category, name, price.replace(/[\$,]/g, ''));
	};
	
	Menu.updateItem = function(id, category, name, price) {
		updateItem(id, category, name, price.replace(/[\$,]/g, ''));
	};
	
	Menu.deleteItem = function(id) {
		deleteItem(id);
	};

// ============================================ ===== ============================================ \\
// ============================================ Setup ============================================ \\
	
	Menu.onCategoryAdd = function(id, name) {
		var category_list = $("#MenuSelection");
		category_list.append($("<option>").attr("value", name));
	};
	
})(Menu);