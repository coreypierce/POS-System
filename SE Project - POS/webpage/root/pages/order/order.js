var Order = Order || {};
(function(Order) {
	// the active order
	var current_order = null;

// ============================================ ================= ============================================ \\
// ============================================ Element Functions ============================================ \\
	
	function generateItemRow(item) {
		if(!current_order) return;
		
		var ele = $("<div />")
			.attr("data-item_id", item.id)
			.addClass("order-item")
			.append($("<i />")
				.on("click", e => removeItem(item.id))
				.css("background", "red")
			)
			.append($("<span />")
				.addClass("order-item_label")
				.text(item.name)
			)
			.append($("<span />")
				.addClass("order-item-amount_wrapper")
				.text("x")
				.append($("<input />")
					.on("change", e => adjustAmount(item.id, Number(e.target.value)))
					.attr("type", "number")
					.attr("value", "1")
					.attr("min", "1")
				)
			);
		
		$(".order-items").append(ele);
	}
	
	function resetOrderDisplay() {
		$(".order-items").empty();
		
		if(!current_order) return;
		for(var order_item of current_order.items) {
			var item;
			if(typeof(order_item.item) !== 'number') {
				// convert "item" to number format
				item = order_item.item;
				order_item.item = item.id;
			} else {
				item = Menu.getItem(order_item.item);
			}
			
			generateItemRow(item);
			// update row-element quantity display
			$(".order-item[data-item_id=" + item.id + "]").find("input").val(order_item.quantity);
		}
	}
	
// ============================================ ================ ============================================ \\
// ============================================ Server Functions ============================================ \\
	
	function startOrder(table_id) {
		$.ajax({
			url: "/api/order/new",
			method: "POST",
			
			data: {
				"table_id": table_id
			}
		})
		.done(function(data, status, xhr) {
			if(status == "success") {
				current_order = data;
				resetOrderDisplay();
			}
		});
	}
	
	function loadOrder(table_id) {
		$.ajax({
			url: "/api/order/details",
			method: "POST",
			
			data: {
				"table_id": table_id
			}
		})
		.done(function(data, status, xhr) {
			if(status == "success") {
				current_order = data;
				resetOrderDisplay();
			}
		});
	}
	
	function submitOrder() {
		if(!current_order) return;
		
		$.ajax({
			url: "/api/order/edit",
			method: "POST",
			
			contentType: "application/json",
			data: JSON.stringify(current_order)
		})
		.done(function(data, status, xhr) {
			if(status == "success") {
				if(current_order && current_order.id == data.id) {
					current_order = data;
				}
			}
		});
	}
	
// ============================================ =============== ============================================ \\
// ============================================ Order Functions ============================================ \\

	function removeItem(item_id) {
		if(!current_order) return;
		
		var items = current_order.items;
		for(var i in items) {
			// find the item in the list
			if(items[i].item == item_id) {
				// remove it
				items.splice(i, 1);
			}
		}
		
		// remove row-element
		$(".order-item[data-item_id=" + item_id + "]").remove();
	}
	
	function addItem(item) {
		if(!current_order) return;
		
		var items = current_order.items;
		for(var i in items) {
			// find the item in the list
			if(items[i].item == item.id) {
				items[i].quantity ++;
				// update row-element
				$(".order-item[data-item_id=" + item.id + "]").find("input").val(items[i].quantity);
				return;
			}
		}
		
		// if item not found
		items.push({
			"item": item.id,
			"quantity": 1
		});
		
		// add row-element
		generateItemRow(item);
	}
	
	function adjustAmount(item_id, amount) {
		if(!current_order) return;

		var items = current_order.items;
		for(var i in items) {
			// find the item in the list
			if(items[i].item == item_id) {
				items[i].quantity = amount;
				// update row-element
				$(".order-item[data-item_id=" + item_id + "]").find("input").val(amount);
				return;
			}
		}
	}
	
// ============================================ ============= ============================================ \\
// ============================================ API Functions ============================================ \\
	
	Order.addItem = function(item) {
		if(!current_order) return;
		addItem(item);
	};

	// ================================== Order Actions ================================== \\
	
	Order.newOrder = function(table_id) {
		startOrder(table_id);
	};
	
	Order.openOrder = function(table_id) {
		loadOrder(table_id);
	};
	
	Order.submitOrder = function() {
		if(!current_order) return;
		submitOrder();
	};

	// ================================== Reset / Clear ================================== \\
	
	Order.clearOrder = function() {
		current_order = null;
		resetOrderDisplay();
	};
	
})(Order)