var Specials = Specials || {};
(function(Specials) {
	
// ============================================ ================= ============================================ \\
// ============================================ Element Functions ============================================ \\
		
	function generateSpecialRow(item) {
		var ele = $("<div />")
			.attr("data-item_id", item.id)
			.addClass("specials-item")
			.append($("<i />")
				.on("click", e => removeSpecial(item))
				.css("background", "red")
			)
			.append($("<span />")
				.addClass("specials-item_label")
				.text(item.name)
			)
			.append($("<span />")
				.addClass("specials-item-amount_wrapper")
				.text("$")
				.append($("<input />")
					.on("change", e => setSpecialPrice(item, Number(e.target.value)))
					.attr("type", "number")
					.attr("value", item.price.toFixed(2))
					.attr("step", 0.01)
					.attr("min", "0")
				)
			);
		
		$(".specials-items").append(ele);
	}

// ============================================ ================ ============================================ \\
// ============================================ Server Functions ============================================ \\
	
	function setSpecialPrice(item, price) {
		Request.ajax({
			url: "/api/menu/specials/set",
			method: "POST",
			
			data: {
				"item_id": item.id,
				"price": price
			},
			
			done: Menu.updateItemPrice
		});
	}
	
	function removeSpecial(item) {
		$(".specials-item[data-item_id=" + item.id + "]").remove();
		
		Request.ajax({
			url: "/api/menu/specials/remove",
			method: "POST",
			
			data: {
				"item_id": item.id
			},
			
			done: Menu.updateItemPrice
		});
	}

// ============================================ ============= ============================================ \\
// ============================================ API Functions ============================================ \\
	
	Specials.addSpecial = function(item) {
		setSpecialPrice(item, item.price);
		if(!$(".specials-item[data-item_id=" + item.id + "]").length)
			generateSpecialRow(item);
	}
	
// ============================================ ===== ============================================ \\
// ============================================ Setup ============================================ \\
	
	Menu.onItemsAvailable = function() {
		for(var item_id in Menu.getAllItem()) {
			var item = Menu.getItem(item_id);
			if(typeof item.default_price !== 'undefined') {
				generateSpecialRow(item);
			}
		}
	}
	
})(Specials);