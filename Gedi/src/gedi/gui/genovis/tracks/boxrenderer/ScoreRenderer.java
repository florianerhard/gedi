package gedi.gui.genovis.tracks.boxrenderer;

import gedi.core.data.annotation.ScoreProvider;
import gedi.util.MathUtils;
import gedi.util.PaintUtils;
import gedi.util.gui.ValueToColorMapper;

import java.awt.Color;

public class ScoreRenderer extends BoxRenderer<ScoreProvider> {

	private ValueToColorMapper mapper = new ValueToColorMapper(Color.WHITE,Color.BLACK);
	
	public ScoreRenderer() {
		setBackground(t->mapper.apply(t.getScore()));
		setForeground(t->PaintUtils.isDarkColor(mapper.apply(t.getScore()))?Color.WHITE:Color.BLACK);
		setStringer(s->s.toString());
	}
	
	public void linear(double min, double max) {
		mapper = new ValueToColorMapper(MathUtils.linearRange(min,max), mapper.getColors());
	}
	
	
	public void colors(Color... colors) {
		mapper = new ValueToColorMapper(mapper.getRange(), colors);
	}
	


}
