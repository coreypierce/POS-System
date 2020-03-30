var SessionUtil = SessionUtil || {};
(function(SessionUtil) {
	function ping_server() {
		if(SessionUtil.keepAlive) {
			$.ajax({
				url: "/api/login/ping",
				method: "POST"
			});
			
		} else {
			$.ajax({
				url: "/api/login/logout",
				method: "POST"
			})
			.done(function() {
				// delete session-token cookie
				document.cookie = 'Session-Token=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
				window.location.replace("/");
			});
		}
	}

	function markAction() {
		SessionUtil.keepAlive = true;
		
		if(SessionUtil.timeout) window.clearTimeout(SessionUtil.timeout);
		// attempt logout after 5-minutes of no-activity
		SessionUtil.timeout = window.setTimeout(() => SessionUtil.keepAlive = false, 5 * 60 * 1000);
	}
	
	// actions that mark the user as active
	$(window).on("click", markAction).on("mousedown", markAction).on("focus", markAction).on("mouseup");
	
	// ping server every 3-minutes (keeps session-token valid)
	window.setInterval(ping_server, 3 * 60 * 1000);
	
})(SessionUtil);