package gedi.gui.genovis.style;

import gedi.util.gui.ColorPalettes;

import java.awt.Color;
import java.util.HashMap;
import java.util.function.IntFunction;

public class StylePalette {

	private IntFunction<String> namer = i->i+"";
	private IntFunction<Color> palette = ColorPalettes.Set1.getCircularDiscreteMapper();
	
	private HashMap<Integer,StyleObject> cache = new HashMap<Integer, StyleObject>(); 
	
	
	public void setNamer(IntFunction<String> namer) {
		this.namer = namer;
	}
	
	public void setPalette(IntFunction<Color> palette) {
		this.palette = palette;
	}
	
	public void setPalette(ColorPalettes palette) {
		this.palette = palette.getCircularDiscreteMapper();
	}
	
	
	public StyleObject get(int index) {
		if (!cache.containsKey(index)) {
			StyleObject re = new StyleObject();
			cache.put(index, re);
			
			re.setName(namer.apply(index));
			re.setColor(palette.apply(index));
		}
		return cache.get(index);
	}
	
}
