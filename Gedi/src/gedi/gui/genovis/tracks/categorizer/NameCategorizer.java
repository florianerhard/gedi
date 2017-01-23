package gedi.gui.genovis.tracks.categorizer;

import gedi.core.data.annotation.NameProvider;
import gedi.util.dynamic.DynamicObject;

import java.util.function.ToIntFunction;

public class NameCategorizer implements ToIntFunction<NameProvider> {

	
	private DynamicObject features;

	private String getType(NameProvider a) {
		return a.getName();
	}

	
	public void setFeatures(DynamicObject features) {
		this.features = features;
	}


	@Override
	public int applyAsInt(NameProvider a) {
		if (features==null) return -1;
		String t = getType(a);
		if (t==null) return -1;
		return features.getEntry(t).getEntry("category").asInt();
	}
	
	


}
