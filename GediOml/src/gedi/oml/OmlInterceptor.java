package gedi.oml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public interface OmlInterceptor {

	
	public default LinkedHashMap<String,String> getAttributes(OmlNode node, LinkedHashMap<String,String> attributes, HashMap<String,Object> context) {
		return attributes;
	}
	
	public default ArrayList<OmlNode> getChildren(OmlNode node, ArrayList<OmlNode> children, HashMap<String,Object> context) {
		return children;
	}

	public default String getName(OmlNode node, String name, HashMap<String,Object> context) {
		return name;
	}

	public default void setObject(OmlNode node, Object o, String id, String[] classes, HashMap<String,Object> context) {
		
	}
	
//	public default void newId(String id, OmlNode node, Object o, HashMap<String,Object> context) {
//		
//	}
	
	/**
	 * Non inlined children!
	 * @param parentNode
	 * @param childNode
	 * @param parent
	 * @param child
	 * @param context
	 */
	public default void childProcessed(OmlNode parentNode, OmlNode childNode, Object parent, Object child, HashMap<String,Object> context){
		
	}

//	public default void newClasses(String[] classes, OmlNode node, Object o, HashMap<String,Object> context) {
//		
//	}

	public default boolean useForSubtree() {
		return false;
	}
	
}
