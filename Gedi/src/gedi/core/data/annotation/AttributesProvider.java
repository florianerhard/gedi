package gedi.core.data.annotation;

import java.util.Set;

/**
 * Properties
 * @author erhard
 *
 */
public interface AttributesProvider {

	
	Set<String> getAttributeNames();
	Object getAttribute(String name);
	
	
	default int getIntAttribute(String name) { return (Integer)getAttribute(name);}
	default double getDoubleAttribute(String name) { return (Double)getAttribute(name);}
	default String getStringAttribute(String name) { return (String)getAttribute(name);}
	

	
}
