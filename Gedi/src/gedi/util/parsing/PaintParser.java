package gedi.util.parsing;

import gedi.util.PaintUtils;

import java.awt.Color;
import java.awt.Paint;

public class PaintParser implements Parser<Paint> {

	@Override
	public Paint apply(String t) {
		return PaintUtils.parseColor(t);
	}

	@Override
	public Class<Paint> getParsedType() {
		return Paint.class;
	}

}
