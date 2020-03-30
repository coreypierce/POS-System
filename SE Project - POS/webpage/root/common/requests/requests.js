var Request = Request || {};
(function(Request) {
	Request.timeout_interval = 5 * 60 * 1000;
	
	Request.ajax = function(args) {
		// upon request, reset timeout
		resetTimeout();
		
		$.ajax({
			url: args.url,
			method: args.method,
			
			contentType: args.contentType,
			
			headers: args.headers,
			data: args.data
		
		}).done(function(data, success, xhr) {
			checkForLogout(xhr);
			args.done && args.done(data, success, xhr);
			
		}).fail(function(xhr, status, errorMessage) {
			checkForLogout(xhr);
			args.fail && args.fail(xhr, status, errorMessage);
		});
	};
	
	function checkForLogout(xhr) {
		// check for logout response
		if(xhr.status == 440) {
			// delete session-token cookie
			document.cookie = 'Session-Token=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
			window.location.replace("/");
		}
	}

	function resetTimeout() {
		if(Request.timeout) window.clearTimeout(Request.timeout);
		
		Request.timeout = window.setTimeout(function() {
			// check with the server if we should be logged-out
			Request.ping();
			
			// wait slightly longer the the timeout-interval
		}, Request.timeout_interval + 10 * 1000);
	}
	
	Request.ping = function() {
		Request.ajax({
			url: "/api/login/ping",
			method: "POST"
		});
	};
	
	Request.logout = function() {
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
	
})(Request);