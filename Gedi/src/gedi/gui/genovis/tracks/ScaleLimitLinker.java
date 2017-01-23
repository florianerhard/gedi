package gedi.gui.genovis.tracks;

import gedi.gui.genovis.GenoVisViewer;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.util.MathUtils;
import gedi.util.mutable.MutableDouble;

public class ScaleLimitLinker  {

	private MutableDouble fixedMin = new MutableDouble(Double.NaN);
	private MutableDouble fixedMax = new MutableDouble(Double.NaN);
	
	
	private MutableDouble min = new MutableDouble(Double.NaN);
	private MutableDouble max = new MutableDouble(Double.NaN);
	
	private boolean hasRegisteredListener = false;
	
	
	public void updateMinMax(NumericTrack<?> t, double min, double max) {
		if (!hasRegisteredListener) {
			t.getViewer().addPrepaintListener(v->{
				this.min.N = Double.NaN;
				this.max.N = Double.NaN;
			});
			hasRegisteredListener = true;
		}
		
		double tmin = MathUtils.saveMin(min,this.min.N);
		double tmax = MathUtils.saveMax(max,this.max.N);
//		boolean refresh = (Double.isNaN(fixedMin) && tmin!=this.min) || (Double.isNaN(fixedMax) && tmax!=this.max);
		this.min.N = tmin;
		this.max.N = tmax;
//		if (refresh)
//			t.getViewer().repaint();
	}

	public MutableDouble computeCurrentMin(Void data) {
		return Double.isNaN(fixedMin.N)?this.min:fixedMin;
	}

	public MutableDouble computeCurrentMax(Void data) {
		return Double.isNaN(fixedMax.N)?this.max:fixedMax;
	}
	
}
