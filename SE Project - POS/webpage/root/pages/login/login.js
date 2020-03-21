// two-way PBKDF2-SHA256 password exchange with server
var Login = Login || {};
(function(Login) {

	// define hash parameters for second pass
	Login.key_size = 512;
	Login.salt_size = 32;
	Login.iterations = 1000;
	
	// requests details such as (salt, key-size, and iterations) used in the first pass
	function requestDetails(username, callback) {
		$.ajax({
			url: "/api/login/details",
			method: "POST",
			
			data: {
				"employee_id": username
			},
		
			headers: {
				"Non-Session": true
			}
		})
		.done(function(data, status, xhr) {
			callback && callback(data);
		});
	}
	
	// generate salt used in second pass of hash
	function generateSalt(size) {
		return sjcl.random.randomWords(Math.ceil(size / 8));
	}
	
	// HMAC for SHA256 used as the "encryption" function for PDKDF2
	var hmac_sha256 = function(key) {
		var hash = new sjcl.misc.hmac(key, sjcl.hash.sha256);
		this.encrypt = function() {
			return hash.encrypt.apply(hash, arguments);
		}
	}
	
	// performs hash with given parameters 
	function hash(password, salt, iteration, keysize) {
		return sjcl.misc.pbkdf2(password, salt, iteration, keysize, hmac_sha256);
	}
	
	// callback function for request login-details
	function submit_password(username, password, pass, fail) {
		return function(details) {
			var server_salt = sjcl.codec.base64.toBits(details.salt);
			// hash password based on Server-spec
			var stored_value = hash(password, server_salt, details.iterations, details.keysize);
			
			var salt = generateSalt(Login.salt_size);
			var server_password = sjcl.codec.base64.fromBits(stored_value);
			// rehash with our own spec
			var transmit_value = hash(server_password, salt, Login.iterations, Login.key_size);
			
			// convert to base-64 for transmission 
			var salt_b64 = sjcl.codec.base64.fromBits(salt);
			var password_b64 = sjcl.codec.base64.fromBits(transmit_value);
			
			// send results to server
			$.ajax({
				url: "/api/login/token",
				method: "POST",
				
				data: {
					"employee_id": username,
					"password": password_b64,

					"salt": salt_b64,
					"key_size": Login.key_size,
					"iterations": Login.iterations
				},
				
				headers: {
					"Non-Session": true
				}
			})
			.done(pass)
			.fail(fail);
		};
	}
	
	// attempt to submit a password to the server
	Login.submit = function(username, password, pass, fail) {
		// get spec from server
		requestDetails(username, submit_password(username, password, pass, fail));
	};
	
	Login.setPassword = function(password, onComplete) {
		// send results to server
		$.ajax({
			url: "/api/login/set_password",
			method: "POST",
			
			data: {
				// bad; don't send passwords in plain text
				"password": password,
			},
			
			headers: {
				"Non-Secure-Session": true
			}
		})
		.done(onComplete)
		.fail(function() {
			alert("Reset Failed!");
		});
	};
})(Login);