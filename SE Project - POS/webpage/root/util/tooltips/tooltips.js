var Tooltips = Tooltips || {};
(function(Tooltips) {

// ============================================ ================= ============================================ \\
// ============================================ Element Functions ============================================ \\

	function buildTooltip(e) {
		var text = e.data("tooltip");
		
		var ele = $("<span />")
			.addClass("tooltip-text")
			.text(text);
		
		e.addClass("tooltip").append(ele);
	}

// ============================================ ============= ============================================ \\
// ============================================ API Functions ============================================ \\
	
	Tooltips.create = function(e) {
		buildTooltip(e);
	};
	
	Tooltips.scanAndCreate = function(e) {
		(e || $(window)).find("*[data-tooltip]:not(.tooltip)").each(function(i, ele) {
			Tooltips.create($(ele));
		});
	};
	
})(Tooltips)