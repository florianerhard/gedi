package gedi.util.properties;

import java.util.TreeMap;

import javafx.beans.property.Property;

public interface PropertyProvider {

	
	TreeMap<String,Property<?>> getProperties();
	TreeMap<String,Class<?>> getPropertyClasses();
	
	
	
	default <T> Property<T> getProperty(String name) {
		return (Property<T>) getProperties().get(name);
	}
	
	default <T> Class<T> getPropertyClass(String name) {
		return (Class<T>) getPropertyClasses().get(name);
	}
	
	
	
	
	
}
