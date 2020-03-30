var Menu = Menu || {};
(function(Menu) {
	// map of item-id to MenuItems
	var items = {};
	// map of category-id to category-names
	var categories = {};
	// map of category-names to category-ids
	var category_ids = {};
	
	// function called when menu-item is clicked
	var onclick_fucntion = null;
	
// ============================================ ================= ============================================ \\
// ============================================ Element Functions ============================================ \\

	function generateCategory(id) {
		var name = categories[id];
		
		//hsla(343, 80%, 45%, 1)
		var ele = $("<div />")
			.on("click", e => displayCategory(id))
			.attr("id", "menu-category_" + id)
			.addClass("menu-category_item")
			.text(name);
		
		var item_group = $("<div />")
			.attr("id", "menu-group_" + id)
			.addClass("menu-item_group");
		
		$(".menu-categories").append(ele);
		$(".menu-items").append(item_group);

		Menu.onCategoryAdd && Menu.onCategoryAdd(id, name);
		
		// always display the latest category
		displayCategory(id);
	}
	
	function generateItem(item) {
		var ele = $("<div />")
			.on("click", e => fireOnClick(item))
			.attr("id", "menu-item_" + item.id)
			.css("order", item.id)
			.addClass("menu-item")
			.text(item.name)
			
			.append($("<span />")
				.text("$" + parseFloat(item.price).toFixed(2)));
		
		$("#menu-group_" + item.category_id).append(ele);
	}
	
// ============================================ ===================== ============================================ \\
// ============================================ Element Click Actions ============================================ \\
	
	function displayCategory(id) {
		$(".menu-item_group_selected").removeClass("menu-item_group_selected");
		$("#menu-group_" + id).addClass("menu-item_group_selected");
		
		$(".menu-category_selected").removeClass("menu-category_selected");
		$("#menu-category_" + id).addClass("menu-category_selected");
	} 

	function fireOnClick(item) {
		// if there is an onclick function, call it with the selected item
		onclick_fucntion && onclick_fucntion(item);
	}
	
// ============================================ ================ ============================================ \\
// ============================================ Server Functions ============================================ \\

	function queryItems() {
		Request.ajax({
			url: "/api/menu/list",
			method: "POST",
		
			done: function(data, status, xhr) {
				for(var category_id in data.categories) {
					// add category to mappings
					categories[category_id] = data.categories[category_id];
					category_ids[categories[category_id]] = category_id;
					// add header for category
					generateCategory(category_id);
				}
				
				for(var item of data.items) {
					// add item to mapping
					items[item.id] = item;
					// add tile for item
					generateItem(item);
				}
				
				Menu.onItemsAvailable && Menu.onItemsAvailable();
			}
		});
	}
	
// ============================================ ============= ============================================ \\
// ============================================ API Functions ============================================ \\
	
	Menu.getItem = function(id) {
		return items[id];
	};
	
	Menu.getCategories = function(id) {
		return categories;
	};

	// ================================== On Click ================================== \\
	
	Menu.setOnClick = function(action) {
		onclick_fucntion = action;
	};
	
	// ================================== Append ================================== \\
	
	Menu.appendCategory = function(id, name) {
		categories[id] = name;
		category_ids[categories[id]] = id;
		generateCategory(id);
	};
	
	Menu.appendItem = function(item) {
		if(!categories[item.category_id]) {
			// if this is a new category
			// it's expected that the category name is provided in object
			Menu.appendCategory(item.category_id, item.category);
		}
		
		// add item to mapping
		items[item.id] = item;
		// add tile for item
		generateItem(item);
	};

// ============================================ ===== ============================================ \\
// ============================================ Setup ============================================ \\
	
	$(function() {
		queryItems();
	});
	
})(Menu);