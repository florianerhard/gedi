package gedi.plot.renderables;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.aesthetics.DoubleAesthetic;
import gedi.plot.aesthetics.x.XAesthetic;
import gedi.plot.primitives.LineType;
import gedi.plot.scale.DoubleScalingPreprocessed;
import gedi.plot.scale.GPlotTicks;
import gedi.util.PaintUtils;
import gedi.util.datastructure.dataframe.DoubleDataColumn;
import gedi.util.gui.Dimension2DDouble;
import gedi.util.mutable.MutableInteger;

public class GPlotScale implements GPlotRenderable {

	
	private GPlotRenderableLabel label = new GPlotRenderableLabel();
	private double labelDistance = 0.5;
	
	private Paint tickColor = Color.white;
	private double tickWidth = 2;
	private double tickSize = 0.3;
	private LineType tickType = LineType.SOLID;
	
	private double labelRotation = 0;
	private int side;
	
	private Function<GPlotContext, GPlotTicks> ticks;
	
	public GPlotScale(Function<GPlotContext,GPlotTicks> ticks) {
		this.ticks = ticks;
	}
	
	public GPlotScale labels(Paint labelColor, String labelFont, boolean labelBold, double labelSize, double labelDist) {
		this.label.label(labelFont, labelColor, labelFont, labelBold, labelSize);
		this.labelDistance = labelDist;
		return this;
	}
	
	public GPlotScale ticks(Paint tickColor, double tickWidth, double tickSize, LineType tickType) {
		this.tickColor = tickColor;
		this.tickWidth = tickWidth;
		this.tickSize = tickSize;
		this.tickType = tickType;
		return this;
	}
	
	public GPlotScale left() {
		side = 1;
		return this;
	}

	public GPlotScale  bottom() {
		side = 0;
		return this;
	}

	public GPlotScale  right() {
		side = 3;
		return this;
	}
	
	public GPlotScale  top() {
		side = 2;
		return this;
	}
	
	
	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
		
		double lab = getLabelSize(context);
		
		if(lab>0) lab+=isX()?sizeToPixelVertical(labelDistance, context):sizeToPixelHorizontal(labelDistance, context);
		
		if (isX())
			return new Dimension2DDouble(0, lab+sizeToPixelVertical(tickSize, context));
		
		return new Dimension2DDouble(lab+sizeToPixelHorizontal(tickSize, context),0);
	}


	
	private double getLabelSize(GPlotContext context) {
		
		GPlotTicks ticks = this.ticks.apply(context);
		if (ticks.count()==0) return 0;
		
		label.rotation(labelRotation);
		Dimension2D dim1 = label.label(ticks.label(0)).measureMinSize(context);
		Dimension2D dim2 = label.label(ticks.label(ticks.count()-1)).measureMinSize(context);
		
		if (isX())
			return Math.max(dim1.getHeight(), dim2.getHeight());
		
		return Math.max(dim1.getWidth(), dim2.getWidth());
	}

	private boolean isX() {
		return side%2==0;
	}

//	private DoubleScalingPreprocessed getPreprocess(GPlotContext context) {
//		if (isX())
//			return context.gplot.x().getPreprocess();
//		else
//			return context.gplot.y().getPreprocess();
//	}

	@Override
	public void render(GPlotContext context, Rectangle2D area) {
		
		GPlotTicks ticks = this.ticks.apply(context);
		
		if (ticks.count()>0) {
			if (isX()) {
				
				double off = side==0?area.getMinY():area.getMaxY()-sizeToPixelVertical(tickSize, context);
				context.g2.setPaint(tickColor);
				for (int r=0; r<ticks.count(); r++) {
					double xco = ticks.transformed(r);
					if (xco>area.getMinX() && xco<area.getMaxX())
						tickType.renderSegment(context.g2, xco, off, xco, off+sizeToPixelVertical(tickSize, context), 0, tickWidth);
				}
				
				label.rotation(labelRotation);
				label.align(0, side==0?-1:0);
				
				off = side==0?area.getMinY()+sizeToPixelVertical(tickSize, context)+sizeToPixelVertical(labelDistance, context):area.getMinY();
				Rectangle2D larea = new Rectangle2D.Double(0, off, 0, area.getHeight()-(sizeToPixelVertical(tickSize, context)+sizeToPixelVertical(labelDistance, context)));
				context.g2.setPaint(tickColor);
				for (int r=0; r<ticks.count(); r++) {
					double xco = ticks.transformed(r);
					if (xco>area.getMinX() && xco<area.getMaxX()) {
						
//						label.label(format(col.getDoubleValue(r), deci.N));
						label.label(ticks.label(r));
						double hw = 100;//Math.min(xco-area.getMinX(), area.getMaxX()-xco);
						larea.setRect(xco-hw, larea.getY(), 2*hw, larea.getHeight());
						label.render(context, larea);
						
					}
				}
			} else {
				
				double off = side==3?area.getMinX():area.getMaxX()-sizeToPixelHorizontal(tickSize, context);
				context.g2.setPaint(tickColor);
				for (int r=0; r<ticks.count(); r++) {
					double yco = ticks.transformed(r);
					if (yco>area.getMinY() && yco<area.getMaxY())
						tickType.renderSegment(context.g2, off, yco, off+sizeToPixelVertical(tickSize, context), yco, 0, tickWidth);
				}
				
				label.rotation(labelRotation);
				label.align(side==3?-1:1, 0);
				
				off = side==3?area.getMinX()+sizeToPixelHorizontal(tickSize, context)+sizeToPixelHorizontal(labelDistance, context):area.getMinX();
				Rectangle2D larea = new Rectangle2D.Double(off, 0, area.getWidth()-(sizeToPixelHorizontal(tickSize, context)+sizeToPixelHorizontal(labelDistance, context)),0);
				context.g2.setPaint(tickColor);
				for (int r=0; r<ticks.count(); r++) {
					double yco = ticks.transformed(r);
					if (yco>area.getMinY() && yco<area.getMaxY()) {
						
//						label.label(format(col.getDoubleValue(r), deci.N));
						label.label(ticks.label(r));
						double hw = 100;//Math.min(yco-area.getMinY(), area.getMaxY()-yco);
						larea.setRect(larea.getX(),yco-hw, larea.getWidth(), 2*hw);
						label.render(context, larea);
					}
				}
			}
			
			
			
		}
		
	}

	private String format(double lab, int decimals) {
		return String.format(Locale.US, "%."+decimals+"f",lab);
	}

	

}
