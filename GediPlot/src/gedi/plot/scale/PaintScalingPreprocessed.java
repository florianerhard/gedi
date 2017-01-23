package gedi.plot.scale;

import java.awt.Color;
import java.awt.Paint;
import java.util.function.DoubleFunction;
import java.util.function.IntFunction;

import gedi.plot.GPlot;
import gedi.plot.GPlotUtils;
import gedi.plot.aesthetics.DoubleAesthetic;
import gedi.plot.scale.transformer.DoubleInvertibleTransformer;
import gedi.util.PaintUtils;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.gui.ColorPalettes;

public class PaintScalingPreprocessed {

	private IntFunction<? extends Paint> discrete;
	private int count;
	
	private DoubleFunction<? extends Paint> continuous;
	private DoubleAesthetic contiAes;

	public PaintScalingPreprocessed() {
	}

	public PaintScalingPreprocessed(IntFunction<? extends Paint> discrete, int count) {
		this.discrete = discrete;
		this.count = count;
	}

	public PaintScalingPreprocessed(DoubleFunction<? extends Paint> continuous, DataColumn<?> c, DoubleScalingPreprocessed cpre, DoubleInvertibleTransformer scaling, int ticks) {
		this.continuous = continuous;
		this.contiAes = new DoubleAesthetic(c, cpre, new DoubleAestheticScale(scaling, DoubleInvertibleTransformer.identity), null).tickCount(ticks);
	}

	public Paint getPaint(DataColumn<?> col, int row) {
		if (continuous!=null) 
			return continuous.apply(contiAes.transform(col, row));
		else if (discrete!=null) 
			return discrete.apply(col.getFactorValue(row).getIndex());
		return Color.black;
	}
	
	public int getCount() {
		return count;
	}

	public DoubleAesthetic getContinuousAes() {
		return contiAes;
	}
	
	public DoubleFunction<? extends Paint> getContinuous() {
		return continuous;
	}
	
	public boolean isDiscrete() {
		return discrete!=null;
	}
	public boolean isContinuous() {
		return continuous!=null;
	}
	
	
	public static PaintScalingPreprocessed compute(DataColumn<?> c, GPlot gplot) {
		if  (c.size()==0) return new PaintScalingPreprocessed();
		if (GPlotUtils.isContinuousColumn(c)) 
			return computeContinuous(c, GPlotUtils.inferScaling(c), gplot.getThemeInt("plot.ticks"));
		return computeDiscrete(c);
	}

	public static PaintScalingPreprocessed computeDiscrete(DataColumn<?> c) {
		int l = c.getFactorValue(0).getNames().length;
		IntFunction<Color> mapper = ColorPalettes.Set1.getDiscreteMapper(l);
		return new PaintScalingPreprocessed(mapper, l);
	}
	public static PaintScalingPreprocessed computeContinuous(DataColumn<?> c, DoubleInvertibleTransformer scaling) {
		DoubleScalingPreprocessed mm = DoubleScalingPreprocessed.compute(c);
		DoubleFunction<Color> mapper = ColorPalettes.Blues.getContinuousMapper(2,-1);
		return new PaintScalingPreprocessed(mapper,c, mm, scaling, -1);
	}
	public static PaintScalingPreprocessed computeContinuous(DataColumn<?> c, DoubleInvertibleTransformer scaling, int nticks) {
		DoubleScalingPreprocessed mm = DoubleScalingPreprocessed.compute(c);
		DoubleAesthetic tmp = new DoubleAesthetic(null, mm, new DoubleAestheticScale(scaling, DoubleInvertibleTransformer.identity), null);
		double[] ticks = PaintUtils.findNiceTicks(tmp.getScale().pretransform(mm.getMin()), tmp.getScale().pretransform(mm.getMax()), nticks);
		
		if (ticks.length<2) return computeContinuous(c, scaling);
		
		double[] off = new double[ticks.length+1];
		off[0] = tmp.transform(mm,scaling.inverse(ticks[0]-(ticks[1]-ticks[0])/2));
		for (int i=1; i<ticks.length; i++) 
			off[i] = tmp.transform(mm,scaling.inverse(ticks[i-1]+(ticks[i]-ticks[i-1])/2));
		off[off.length-1] = tmp.transform(mm,scaling.inverse(ticks[ticks.length-1]+(ticks[ticks.length-1]-ticks[ticks.length-2])/2));
		
		
		DoubleFunction<Color> mapper = ColorPalettes.Blues.getContinuousMapper(off);
		return new PaintScalingPreprocessed(mapper,c, mm, scaling, nticks);
	}

	
	
}