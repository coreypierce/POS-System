package edu.wit.se16._example;

public class ExamplePassword {
	
	/**
	 * 	Example password handler
	 * 	Bad in many regards: Plain text passwords, Usernames instead of employee-ids, Hard-coded, ect
	 */
	public static boolean validatePassword(String employeeID, String password) {
		if(employeeID.equalsIgnoreCase("Josh")) {
			return password.equals("123");
		}
		
		if(employeeID.equalsIgnoreCase("Andy")) {
			return password.equals("asd");
		}
		
		if(employeeID.equalsIgnoreCase("Corey")) {
			return password.equals("apple");
		}
		
		if(employeeID.equalsIgnoreCase("John")) {
			return password.equals("wordpass");
		}
		
		return false;
	}
}
