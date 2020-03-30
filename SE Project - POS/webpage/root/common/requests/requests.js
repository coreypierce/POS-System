var Request = Request || {};
(function(Request) {
	Request.ajax = function(args) {
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
})(Request);