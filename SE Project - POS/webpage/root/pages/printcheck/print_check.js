var PrintCheck = PrintCheck || {};
(function(PrintCheck) {
	// the active order
	var current_check = null;

// ============================================ ================= ============================================ \\
// ============================================ Element Functions ============================================ \\
	
	function generateItemRow(item, amount) {
		if(!current_check) return;
		
		var ele = $("<div />")
			.addClass("printcheck-food_item")
			.append($("<div />")
				.addClass("food-name")
				.text(item.name)
			).append($("<div />")
				.addClass("food-amount")
				.text("x" + amount)
			).append($("<div />")
				.addClass("food-name_price")
				.text("$" + parseFloat(item.price * amount).toFixed(2))
			);
		
		$(".printcheck-order_wrapper").append(ele);
	}
	
	function resetCheckDisplay() {
		$(".printcheck-order_wrapper").empty();
		if(!current_check) return;
		
		// add line-item for each order item
		for(var order_item of current_check.order_details.items) {
			var item;
			if(typeof(order_item.item) !== 'number') {
				// convert "item" to number format
				item = order_item.item;
				order_item.item = item.id;
			} else {
				item = Menu.getItem(order_item.item);
			}
			
			generateItemRow(item, order_item.quantity);
			// update row-element quantity display
			$(".order-item[data-item_id=" + item.id + "]").find("input").val(order_item.quantity);
		}
		
		// set total values in display
		$(".subtotal-price").text("$" + parseFloat(current_check.sub_total).toFixed(2));
		
		$(".tax").text("Tax (" + current_check.tax_rate + "%): ");
		$(".tax-price").text("$" + parseFloat(current_check.tax_ammount).toFixed(2));
		
		$(".total-price").text("$" + parseFloat(current_check.total).toFixed(2));
	}
	
// ============================================ ================ ============================================ \\
// ============================================ Server Functions ============================================ \\
	
	function printCheck(table_id) {
		Request.ajax({
			url: "/api/table/print_check",
			method: "POST",
			
			data: {
				"table_id": table_id
			},
			
			done: function(data, status, xhr) {
				if(status == "success") {
					current_check = data;
					resetCheckDisplay();
					
					TableDisplay && TableDisplay.menuActionCallback(data);
				}
			}
		});
	}
	
// ============================================ ============= ============================================ \\
// ============================================ API Functions ============================================ \\
	
	PrintCheck.printCheck = function(table_id) {
		printCheck(table_id);
	};

})(PrintCheck)