var Popup = Popup || {};
(function(lib) {
	if(!$) throw "Missing jQuery! Table-Display requires page to include jQuery";
	
	lib.__popups = {};
	lib.__ids = {};

	lib.__currently_open = null;
	
	lib.__backdrop = $("<div/>").addClass("popup-backdrop");
	// once page has loaded, append backdrop
	$(function() {
		lib.__backdrop.appendTo($("body"));
	})
	
	// Generates a unique-id and maps the given name to that ID
	lib.register = function(element, name) {
		if(!name) console.warn("No name for popup provided!");
		if(lib.__popups[name]) console.warn("Multiple popup's with name '" + name + "'");
		
		do {
			// Generate 6-digit ID number
			var id = Math.floor(Math.random() * 899999 + 100000);
		// check if ID is already mapped 
		} while(lib.__ids[id]);
		
		// record popup details
		name = name || "popup_" + id;
		lib.__popups[name] = {
			"id": id,
			"element": element
		};
		
		// update popup's ID
		element.attr("id", "popup_" + id);
		
		return id;
	};
	
	lib.hide = lib.close = function() {
		// check if there's an open popup
		if(lib.__currently_open) {
			var popup = lib.__currently_open;
			
			popup.element
				.addClass("popup-transtion").removeClass("popup-open")
				.delay(250) // allows transition to finish, then remove class
				.queue(() => popup.element.removeClass("popup-transtion").dequeue());
			
			lib.__backdrop
				.addClass("popup-backdrop_transtion").removeClass("popup-backdrop_open")
				.delay(100) // allows transition to finish, then remove class
				.queue(() => lib.__backdrop.removeClass("popup-backdrop_transtion").dequeue());
			
			// mark no popup open
			lib.__currently_open = null;
		}
	};
	
	lib.show = lib.open = function(name) {
		var popup = lib.__popups[name];
		// validate popup exists 
		if(!popup) {
			console.error("No popup found with name '" + name + "'");
			return;
		}
		
		var open = () => popup.element.addClass("popup-open");
		
		if(lib.__currently_open) {
			// popup is already open
			if(lib.__currently_open.id == popup.id) return;
		
			// close current popup
			lib.close();
			window.setTimeout(open, 250);
		} else {
			open();
		}
		
		lib.__backdrop.addClass("popup-backdrop_open");
		
		// track open popup
		lib.__currently_open = popup;
	};
})(Popup);