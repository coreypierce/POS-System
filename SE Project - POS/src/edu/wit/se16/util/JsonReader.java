package edu.wit.se16.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class JsonReader {
	private static final JsonFactory JSON_FACTORY = new JsonFactory();
	
	public static Map<String, Object> read(InputStream inputStream) throws IOException {
		JsonParser parser = JSON_FACTORY.createParser(inputStream);
		
		AddableCollection current = null;
		Stack<AddableCollection> stack = new Stack<>();
		
		while(true) {
			try {
				switch(parser.nextToken()) {
					case VALUE_NULL: current.append(parser.currentName().toLowerCase(), null); break;
					
					case VALUE_NUMBER_FLOAT: current.append(parser.currentName().toLowerCase(), parser.getDoubleValue()); break;
					case VALUE_NUMBER_INT: 	 current.append(parser.currentName().toLowerCase(), parser.getIntValue());   break;
					
					case VALUE_STRING: current.append(parser.currentName().toLowerCase(), parser.getText()); break;
					
					case VALUE_TRUE:  current.append(parser.currentName().toLowerCase(), true);  break;
					case VALUE_FALSE: current.append(parser.currentName().toLowerCase(), false); break;
	
	
					case START_ARRAY:
					case START_OBJECT:
						stack.push(current);
						AddableCollection nextCollection = AddableCollection.newCollect(parser.currentToken());
						if(current != null) current.append(parser.currentName().toLowerCase(), nextCollection.get()); 
						current = nextCollection;
					break;
					
					case END_ARRAY: 
					case END_OBJECT: 
						if(stack.isEmpty() || stack.peek() == null) return current.map;
						current = stack.pop();
					break;
					
					case FIELD_NAME:
					case NOT_AVAILABLE:
					case VALUE_EMBEDDED_OBJECT:
					default: break;
				}
			} catch(NullPointerException e) {
				throw new JsonParseException(parser, "Unbounded Data: Expected '{'");
			}
		}
	}

	private static class AddableCollection {
		private Map<String, Object> map;
		private ArrayList<Object> array;
 		
		public static AddableCollection newCollect(JsonToken token) { return token == JsonToken.START_ARRAY ? newArray() : newMap(); }
		
		public static AddableCollection newMap() { return new AddableCollection(new HashMap<>()); }
		public static AddableCollection newArray() { return new AddableCollection(new ArrayList<>()); }
		
		private AddableCollection(Map<String, Object> map) { this.map = map; }
		private AddableCollection(ArrayList<Object> array) { this.array = array; }
		
		public void append(String name, Object value) {
			if(map != null) {
				if(name == null || name.isEmpty()) throw new IllegalArgumentException("Cannot add value with Name!");
				map.put(name, value);
			
			} else if(array != null) {
				if(name != null && !name.isEmpty()) throw new IllegalArgumentException("Names cannot be used in Arrays");
				array.add(value);
			}
		}
		
		public Object get() { return map != null ? map : array != null ? array : null; }
	}
}
