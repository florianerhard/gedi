package gedi.util.parsing;

import gedi.util.PaintUtils;

import java.awt.Color;

public class ColorParser implements Parser<Color> {

	@Override
	public Color apply(String t) {
		return PaintUtils.parseColor(t);
	}

	@Override
	public Class<Color> getParsedType() {
		return Color.class;
	}

}
