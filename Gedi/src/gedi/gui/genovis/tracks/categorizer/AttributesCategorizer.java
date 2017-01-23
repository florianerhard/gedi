package gedi.gui.genovis.tracks.categorizer;

import gedi.core.data.annotation.AttributesProvider;
import gedi.core.data.annotation.NameProvider;
import gedi.util.dynamic.DynamicObject;

import java.util.function.ToIntFunction;

public class AttributesCategorizer implements ToIntFunction<AttributesProvider> {

	
	private DynamicObject features;
	private String attribute = "name";

	private String getType(AttributesProvider a) {
		if (attribute.equals("name") && a instanceof NameProvider)
			return ((NameProvider)a).getName();
		return a.getStringAttribute(attribute);
	}


	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	
	public void setFeatures(DynamicObject features) {
		this.features = features;
	}


	@Override
	public int applyAsInt(AttributesProvider a) {
		if (features==null) return -1;
		String t = getType(a);
		if (t==null) return -1;
		return features.getEntry(t).getEntry("category").asInt();
	}
	
	


}
