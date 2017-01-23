package gedi.gui.genovis.tracks;


import gedi.core.reference.ReferenceSequence;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.gui.genovis.VisualizationTrackAdapter;
import gedi.gui.genovis.VisualizationTrackPickInfo;
import gedi.util.PaintUtils;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.mutable.MutableDouble;
import gedi.util.mutable.MutableInteger;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.DoubleBinaryOperator;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

public abstract class NumericTrack<T> extends VisualizationTrackAdapter<T,Double> {

	protected double fixedMin = Double.NaN;
	protected double fixedMax = Double.NaN;
	
	protected Paint axisColor = Color.BLACK;
	protected double minTicDistance = 10;
	protected double ticWidth = 3;
	protected double logbase = 0;
	protected boolean topDown = false;
	
	protected boolean isAggregatorMax = true;
	protected DoubleBinaryOperator aggregator = Math::max;
	
	// For stacked visualization, overwrite this (sum!)
	protected NumericArrayFunction rowOpMin = NumericArrayFunction.SaveMin;
	protected NumericArrayFunction rowOpMax = NumericArrayFunction.SaveMax;
	protected String labelFormat = "%.2f";
	
	NumericTrackGroup group;
	ScaleLimitLinker limitLinker;

	public NumericTrack(Class<T> cls) {
		super(cls);
		this.minHeight = this.prefHeight = this.maxHeight = 100;
		this.leftMarginWidth = 100;
		
		addListener(t->createPopup().show((SwingGenoVisViewer)viewer, (int)t.getPixelX(), (int)t.getPixelY()), 
				VisualizationTrackPickInfo.TrackEventType.RightClicked
				);
		
	}
	

	private JPopupMenu createPopup() {
		JPopupMenu men = new JPopupMenu();
		
		JCheckBoxMenuItem log = new JCheckBoxMenuItem("Log scale",isLogScale());
		log.addActionListener(e->setLogScale(isLogScale()?0:10));
		men.add(log);
		
		return men;
	}

	public void setTopDown(boolean topDown) {
		this.topDown = topDown;
	}

	public void setHeight(double height) {
		this.minHeight = this.prefHeight = this.maxHeight = height;
	}
	
	public void setLogScale(double base) {
		this.logbase  = base;
		if (viewer!=null)
			viewer.repaint(true);
	}
	
	public void setLimitLinker(ScaleLimitLinker limitLinker) {
		if (group!=null) throw new IllegalArgumentException("Cannot set limit linker when in a group!");
		this.limitLinker = limitLinker;
	}
	
	protected double getLogbase() {
		return logbase;
	}
	
	public boolean isLogScale() {
		return getLogbase()>0;
	}
	
	public void setAggregator(DoubleBinaryOperator aggregator) {
		this.aggregator = aggregator;
		isAggregatorMax = false;
	}
	
	public void setMaxAggregator() {
		this.aggregator = Math::max;
		isAggregatorMax = true;
	}
	

	public void setFixedMax(double fixedMax) {
		this.fixedMax = fixedMax;
	}
	public void setFixedMin(double fixedMin) {
		this.fixedMin = fixedMin;
	}
	
	protected abstract double computeCurrentMin(T data);
	protected abstract double computeCurrentMax(T data);
	
	protected static final int MIN_VALUE = 0;
	protected static final int MAX_VALUE = 1;
	protected static final int MIN_SCALE = 10;
	protected static final int MAX_SCALE = 11;

	protected VisualizationTrackAdapter.TrackRenderContext<T> renderBegin(
			HashMap<ReferenceSequence,MutableReferenceGenomicRegion<Void>> current, 
			HashMap<ReferenceSequence,MutableReferenceGenomicRegion<T>> data, 
			TrackRenderContext<T> context) {
		
		double min = Double.NaN;
		double max = Double.NaN;
		for (MutableReferenceGenomicRegion<T> d : data.values()) {
			double cmin = Double.isNaN(fixedMin)?computeCurrentMin(d.getData()):fixedMin;
			double cmax = Double.isNaN(fixedMax)?computeCurrentMax(d.getData()):fixedMax;
			if (Double.isNaN(min) || cmin<min) min = cmin;
			if (Double.isNaN(max) || cmax>max) max = cmax;
		}
		ScaleLimitLinker ll = limitLinker;
		if (group!=null) ll = group.getLimitLinker();
		if (ll!=null) {
			ll.updateMinMax(this,min,max);
			context.putValue(MIN_VALUE, ll.computeCurrentMin(null));
			context.putValue(MAX_VALUE, ll.computeCurrentMax(null));
		}
		else {
			context.putValue(MIN_VALUE, new MutableDouble(min));
			context.putValue(MAX_VALUE, new MutableDouble(max));
		}
		
		return context;
	}
	
	@Override
	protected TrackRenderContext<T> renderBackground(
			TrackRenderContext<T> context) {
		
		if (group==null) {
			context.g2.setPaint(getBackground());
			context.g2.fill(bounds);
			
			context.g2.setPaint(axisColor );
			context.g2.draw(new Rectangle2D.Double(getBounds().getX(),getBounds().getY(),getBounds().getWidth()-1,getBounds().getHeight()-1));
		}

		return context;
	}
	
	@Override
	public void pick(VisualizationTrackPickInfo<Double> info) {
		double min = context.contains(MIN_SCALE)?context.get(MIN_SCALE):context.<MutableDouble>get(MIN_VALUE).N;
		double max = context.contains(MAX_SCALE)?context.get(MAX_SCALE):context.<MutableDouble>get(MAX_VALUE).N;
		info.setData(invtransform(getValue(info.getPixelY(), min,max)));
	}
	
	@Override
	protected TrackRenderContext<T> renderMargin(
			TrackRenderContext<T> context) {
		
		if (group==null) {
			if (labelFormat.length()==0) return context;
			
			// margin
			Rectangle2D left = getLeftMargin();
			context.g2.setPaint(getBackground());
			context.g2.fill(left);
			
			// scale bar and zero line
			context.g2.setPaint(axisColor );
		
			double min = context.contains(MIN_SCALE)?context.get(MIN_SCALE):context.<MutableDouble>get(MIN_VALUE).N;
			double max = context.contains(MAX_SCALE)?context.get(MAX_SCALE):context.<MutableDouble>get(MAX_VALUE).N;
			
			MutableInteger digits = new MutableInteger();
			double[] tics = PaintUtils.findNiceTicks(min, max, 8, digits );
			
			if (isLogScale() && digits.N>0) {
				double logScale = getLogbase();
				tics = PaintUtils.findNiceTicks(Math.pow(logScale,min), Math.pow(logScale,max), 4, digits );
				for (int i=0; i<tics.length; i++)
					tics[i] = Math.log(tics[i])/Math.log(logScale);
			}
			
			for (double m : tics) {
				if (m>=min && m<=max) {
					double y = getY(m, min,max);
					context.g2.draw(new Line2D.Double(getBounds().getX()-ticWidth,y,getBounds().getX(),y));
					boolean log = isLogScale() && !context.contains(MIN_SCALE);
					paintLabel(context.g2, getBounds().getX()-ticWidth, -ticWidth, log?Math.pow(getLogbase(),m):m, y, digits.N);
				}
			}
//			// tics
//			double exp = PaintUtils.findNiceNumberGreater((int)Math.ceil((max-min)/(getBounds().getHeight()/minTicDistance))); //Math.pow(10, Math.ceil(Math.log10((max-min)/(height/minTicDistance/2))))/2;
//			for (double m = exp*Math.ceil(min/exp); m<=max; m+=exp) {
//				double y = getY(m, min,max);
//				context.g2.draw(new Line2D.Double(getBounds().getX()-ticWidth,y,getBounds().getX(),y));
//				boolean log = isLogScale() && !context.contains(MIN_SCALE);
//				paintLabel(context.g2, getBounds().getX()-ticWidth, -ticWidth, log?Math.pow(logScale,m):m, y);
//			}
		}
		return context;
	}
	
	@Override
	public TrackRenderContext<T> renderTrack(TrackRenderContext<T> context) {
		
		// data
		int passes = getPassesCount(context); 
		
		for (int p=0; p<passes; p++) {
			beginPass(context,p);
			renderPass(context,p);
			endPass(context,p);
		}

		return context;
	}
	

	protected void renderPass(
			TrackRenderContext<T> context,int pass) {
	}


	protected Rectangle2D paintLabel(Graphics2D g2, double right, double width, double v, double y) {
		double stringHeight = g2.getFont().getSize2D();
		Rectangle2D legend = new Rectangle2D.Double(right-width,y-stringHeight/2,width,stringHeight);
		Rectangle2D re = PaintUtils.paintString(String.format(Locale.US,labelFormat,v), g2, legend,1 ,0);
		return re;
	}
	
	protected Rectangle2D paintLabel(Graphics2D g2, double right, double width, double v, double y, int digits) {
		String labelFormat = "%."+digits+"f";
		double stringHeight = g2.getFont().getSize2D();
		Rectangle2D legend = new Rectangle2D.Double(right-width,y-stringHeight/2,width,stringHeight);
		Rectangle2D re = PaintUtils.paintString(String.format(Locale.US,labelFormat,v), g2, legend,1 ,0);
		return re;
	}


	protected double transform(double value) {
		if (isLogScale())
			value = Math.log(value+1)/Math.log(getLogbase());
		return value;
	}
	
	protected double invtransform(double value) {
		if (isLogScale())
			value = Math.pow(getLogbase(),value)-1;
		return value;
	}
	
	/**
	 * all values must be (log-) transformed already!
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	protected double getY(double value, double min, double max) {
		if (topDown)
			return getBounds().getMinY()+clamp((value-min)/(max-min))*getBounds().getHeight();
		else
			return getBounds().getMaxY()-clamp((value-min)/(max-min))*getBounds().getHeight();
	}

	protected double getValue(double y, double min, double max) {
		if (topDown)
			return (y-getBounds().getMaxY())/getBounds().getHeight()*(max-min)+min;
		else
			return (getBounds().getMaxY()-y)/getBounds().getHeight()*(max-min)+min;
	}

	protected double clamp(double d) {
		if (d<0) return 0;
		if (d>1) return 1;
		return d;
	}

	/**
	 * Gets the number of passes for rendering
	 * @param context
	 * @return
	 */
	protected int getPassesCount(
			TrackRenderContext<T> context) {
		return 1;
	}
	
	protected void beginPass(
			TrackRenderContext<T> context, int pass) {
	}

	
	protected void endPass(
			TrackRenderContext<T> context, int pass) {
	}


//	private double computeMax(PerBasePairValues[] values) {
//		double re = Double.NEGATIVE_INFINITY;
//		for (PerBasePairValues v : values)
//			re = Math.max(re,v.getMax());
//		return re;
//	}

	
	
	private Line2D.Double line = new Line2D.Double();
	protected void line(Graphics2D g2, double x1, double y1, double x2, double y2) {
		if (!Double.isNaN(x1+x2+y1+y2)) {
			line.setLine(x1, y1, x2, y2);
			g2.draw(line);
		}
	}
	
	private Ellipse2D.Double point = new Ellipse2D.Double();
	protected void point(Graphics2D g2, double x, double y, double w, double h) {
		if (!Double.isNaN(x+y+w+h)) {
			point.setFrame(x, y, w, h);
			g2.fill(point);
		}
	}


	
	
	
}
