package edu.wit.se16.util;

public class ErrorUtil {
	@SuppressWarnings("unchecked")
	public static <E extends Throwable> SneekyException sneekyThrow(Throwable e) throws E { 
		throw (E) e; 
	}
	
	public static void ignore(ViolentRunnable r) {
		try {
			r.run();
		} catch(Exception ignore) {
			
		}
	}
	
	public static <T> T sneak(ViolentProvider<T> provider) {
		try {
			return provider.access();
		} catch (Exception e) {
			throw sneekyThrow(e);
		}
	} 
	
	public static interface ViolentProvider<T> {
		public T access() throws Exception;
	}
	
	public static interface ViolentRunnable {
		public void run() throws Exception;
	}
	
	private static class SneekyException extends RuntimeException {
		private static final long serialVersionUID = 3567071349286597461L;
	}
}
