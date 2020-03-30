var SessionUtil = SessionUtil || {};
(function(SessionUtil) {
	function ping_server() {
		// is an action was taken to keep session active
		if(SessionUtil.keepAlive) {
			Request.ping();
		} else {
			Request.logout();
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
	
	// ping server slightly sooner then the timeout-interval (keeps session-token valid)
	window.setInterval(ping_server, Request.timeout_interval - 1 * 60 * 1000);
	
})(SessionUtil);