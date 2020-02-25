package edu.wit.se16.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Stack;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.StringBuilderWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;

public class JsonBuilder { 
	private static final JsonFactory JSON_FACTORY = new JsonFactory();
	private static final JsonNodeFactory FACTORY = new JsonNodeFactory(true);
	private static final SerializerProvider SERIALIZER_PROVIDER = new ObjectMapper(JSON_FACTORY).getSerializerProviderInstance();
	
	public static JsonBuilder create() { return new JsonBuilder(); }

	private Stack<NodeWritter> current = new Stack<>(); { 
		current.push(new NodeWritter(FACTORY.objectNode())); 
	}
	
	public static JsonBuilder from(JsonNode root) {
		if(!(root instanceof ContainerNode)) {
			throw new IllegalArgumentException("Root node must be of type ContainerNode<?>");
		}
		
		JsonBuilder builder = new JsonBuilder();
		builder.current.clear();
		builder.current.push(new NodeWritter((ContainerNode<?>) root));
		return builder;
	}
	
	public JsonBuilder append(byte[] data, int offset, int length) { return this.append(data, offset, length); }
	public JsonBuilder append(byte[] data) { return this.append(null, data); }
	public JsonBuilder append(boolean v) { return this.append(null, v); }
	public JsonBuilder append(BigDecimal v) { return this.append(null, v); }
	public JsonBuilder append(BigInteger v) { return this.append(null, v); }
	public JsonBuilder append(byte v) { return this.append(null, v); }
	public JsonBuilder append(Byte value) { return this.append(null, value); }
	public JsonBuilder append(double v) { return this.append(null, v); }
	public JsonBuilder append(Double value) { return this.append(null, value); }
	public JsonBuilder append(float v) { return this.append(null, v); }
	public JsonBuilder append(Float value) { return this.append(null, value); }
	public JsonBuilder append(int v) { return this.append(null, v); }
	public JsonBuilder append(Integer value) { return this.append(null, value); }
	public JsonBuilder append(long v) { return this.append(null, v); }
	public JsonBuilder append(Long v) { return this.append(null, v); }
	public JsonBuilder append(short v) { return this.append(null, v); }
	public JsonBuilder append(Short value) { return this.append(null, value); }
	public JsonBuilder append(Object pojo) { return this.append(null, pojo); }
	public JsonBuilder append(String text) { return this.append(null, text); }
	public JsonBuilder append(JsonNode node) { return this.append(null, node); }

	public JsonBuilder appendNull() { return this.appendNull(null); }
	public JsonBuilder appendRaw(String raw) { return this.append(null, raw); }
	
	public JsonBuilder newObject() { return this.newObject(null); }	
	public JsonBuilder newArray() { return this.newArray(null); }

	public JsonBuilder append(String name, byte[] data, int offset, int length) { 
		current.peek().append(name, FACTORY.binaryNode(data, offset, length)); return this; }
	public JsonBuilder append(String name, byte[] data) { current.peek().append(name, FACTORY.binaryNode(data)); return this; }
	public JsonBuilder append(String name, boolean v) { current.peek().append(name, FACTORY.booleanNode(v)); return this; }
	public JsonBuilder append(String name, BigDecimal v) { current.peek().append(name, FACTORY.numberNode(v)); return this; }
	public JsonBuilder append(String name, BigInteger v) { current.peek().append(name, FACTORY.numberNode(v)); return this; }
	public JsonBuilder append(String name, byte v) { current.peek().append(name, FACTORY.numberNode(v)); return this; }
	public JsonBuilder append(String name, Byte value) { current.peek().append(name, FACTORY.numberNode(value)); return this; }
	public JsonBuilder append(String name, double v) { current.peek().append(name, FACTORY.numberNode(v)); return this; }
	public JsonBuilder append(String name, Double value) { current.peek().append(name, FACTORY.numberNode(value)); return this; }
	public JsonBuilder append(String name, float v) { current.peek().append(name, FACTORY.numberNode(v)); return this; }
	public JsonBuilder append(String name, Float value) { current.peek().append(name, FACTORY.numberNode(value)); return this; }
	public JsonBuilder append(String name, int v) { current.peek().append(name, FACTORY.numberNode(v)); return this; }
	public JsonBuilder append(String name, Integer value) { current.peek().append(name, FACTORY.numberNode(value)); return this; }
	public JsonBuilder append(String name, long v) { current.peek().append(name, FACTORY.numberNode(v)); return this; }
	public JsonBuilder append(String name, Long v) { current.peek().append(name, FACTORY.numberNode(v)); return this; }
	public JsonBuilder append(String name, short v) { current.peek().append(name, FACTORY.numberNode(v)); return this; }
	public JsonBuilder append(String name, Short value) { current.peek().append(name, FACTORY.numberNode(value)); return this; }
	public JsonBuilder append(String name, Object pojo) { current.peek().append(name, FACTORY.pojoNode(pojo)); return this; }
	public JsonBuilder append(String name, String text) { current.peek().append(name, FACTORY.textNode(text)); return this; }
	public JsonBuilder append(String name, JsonNode node) { current.peek().append(name, node); return this; }

	public JsonBuilder appendNull(String name) { current.peek().append(name, FACTORY.nullNode()); return this; }
	public JsonBuilder appendRaw(String name, String raw) { current.peek().append(name, FACTORY.rawValueNode(new RawValue(raw))); return this; }

	public JsonBuilder append(String name, String[] array) { if(array.length == 0) return this; newArray(name); for(String s : array) append(s); return end(); }
	
	public JsonBuilder newObject(String name) { 
		ObjectNode node = FACTORY.objectNode();
		current.peek().append(name, node); 
		current.push(new NodeWritter(node));
		return this; 
	}
	
	public JsonBuilder newArray(String name) { 
		ArrayNode node = FACTORY.arrayNode();
		current.peek().append(name, node); 
		current.push(new NodeWritter(node));
		return this; 
	}
	
	public JsonBuilder end() { current.pop(); return this; }
	public JsonNode build() { return current.firstElement().root; }
	
	public void build(HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		
		JsonGenerator generator = JSON_FACTORY.createGenerator(response.getOutputStream());
		generator.disable(Feature.FLUSH_PASSED_TO_STREAM);
		build().serialize(generator, SERIALIZER_PROVIDER);
		generator.flush();
	}
	
	public static String buildString(JsonNode node) {
		StringBuilderWriter buffer = new StringBuilderWriter();
		
		try {
			JsonGenerator generator = JSON_FACTORY.createGenerator(buffer);
			generator.useDefaultPrettyPrinter();
			node.serialize(generator, SERIALIZER_PROVIDER);
			generator.flush();
			
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return buffer.toString();
	} 
	
	private static class NodeWritter {
		private ContainerNode<?> root;
		
		public NodeWritter(ContainerNode<?> root) { this.root = root; }
		
		public void append(String name, JsonNode value) {
			if(root instanceof ObjectNode) {
				if(name == null || name.isEmpty()) throw new IllegalArgumentException("Cannot add value without name to Object!");
				((ObjectNode) root).set(name, value);
			
			} else if(root instanceof ArrayNode) {
				if(name != null && !name.isEmpty()) throw new IllegalArgumentException("Names cannot be used in Arrays");
				((ArrayNode) root).add(value);
			}
		}
	}
}
