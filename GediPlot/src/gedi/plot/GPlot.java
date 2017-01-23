package gedi.plot;

import gedi.app.Gedi;

import gedi.cascadingProperty.CpsList;
import gedi.cascadingProperty.CpsReader;
import gedi.plot.aesthetics.Aesthetic;
import gedi.plot.aesthetics.DoubleAesthetic;
import gedi.plot.aesthetics.paint.PaintAesthetic;
import gedi.plot.aesthetics.x.XAesthetic;
import gedi.plot.aesthetics.y.YAesthetic;
import gedi.plot.layout.GPlotLayout;
import gedi.plot.layout.GPlotLayoutStacking;
import gedi.plot.layout.GPlotLayoutTable;
import gedi.plot.primitives.GPoints;
import gedi.plot.primitives.GPrimitive;
import gedi.plot.primitives.Glyph;
import gedi.plot.primitives.LineType;
import gedi.plot.renderables.GPlotPlaneBackground;
import gedi.plot.renderables.GPlotEmptyRenderable;
import gedi.plot.renderables.GPlotRenderableLabel;
import gedi.plot.renderables.GPlotScale;
import gedi.plot.renderables.legend.AlphaLegend;
import gedi.plot.renderables.legend.GPlotLegends;
import gedi.plot.renderables.legend.GlyphLegend;
import gedi.plot.renderables.legend.LineTypeLegend;
import gedi.plot.renderables.legend.LineWidthLegend;
import gedi.plot.renderables.legend.PaintLegend;
import gedi.plot.renderables.legend.SizeLegend;
import gedi.plot.scale.AestheticScale;
import gedi.plot.scale.DoubleAestheticScale;
import gedi.plot.scale.DoubleScalingPreprocessed;
import gedi.plot.scale.GlyphScalingPreprocessed;
import gedi.plot.scale.PaintScalingPreprocessed;
import gedi.plot.scale.transformer.DoubleInvertibleTransformer;
import gedi.plot.scale.transformer.ToContinuousScale;
import gedi.plot.scale.LineTypeScalingPreprocessed;
import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.dynamic.DynamicObject;
import gedi.util.gui.ColorPalettes;
import gedi.util.io.text.tsv.formats.Csv;
import gedi.util.nashorn.JS;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;

import javax.imageio.ImageIO;
import javax.script.ScriptException;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;



/**
 * Phase 1: Design phase
 * Define all the structure of the plot, but do not do any real work (e.g. infer min max)
 * 
 * Phase 2: First preprocessing
 * Preprocess everything from the data that is independent on the layout and sizes (e.g. min, max, scaling, colors)
 * 
 * Phase 3: Compute Min Layout
 * 
 * Phase 4: Propagate actual layout to components
 * 
 * Phase 5: Second preprocessing
 * Everything that is dependent on the layout and actual sizes
 * 
 * 
 * Each visual component is associated with at most one subplot; A subplot contains a unique x and y {@link ToContinuousScale} as well as the plotting area
 * 
 * @author flo
 *
 */
public class GPlot implements GPlotRenderable {


	private DataFrame df;
	
	private XAesthetic x; 
	private YAesthetic y;
	private PaintAesthetic color;
	private Aesthetic<Paint,PaintScalingPreprocessed> fill;
	private Aesthetic<LineType,LineTypeScalingPreprocessed> lty;
	private Aesthetic<Glyph,GlyphScalingPreprocessed> glyph;
	private DoubleAesthetic alpha; 
	private DoubleAesthetic size;
	private DoubleAesthetic lwd;
	
	private String title;
	private String xlab;
	private String ylab;
	
	private JS theme;
	
	private GPlotSubPlot[][] subplots = new GPlotSubPlot[0][0];
	
	private ArrayList<GPrimitive> layers = new ArrayList<>();
	

	public GPlot(DataFrame df) {
		this.df = df;
		try {
			theme = new JS();
			theme.injectObject(this);
			theme.injectObject(new GPlotComfortHelper());
			theme.execScript(getClass().getResourceAsStream("/gedi/plot/themes/default.js"));
			
		} catch (IOException | ScriptException e) {
			throw new RuntimeException("Could not load theme!",e);
		}
	}
	
	public DataFrame df() {
		return df;
	}
	
	public GPlotSubPlot getSubplot(int row, int col) {
		if (row>=subplots.length)  {
			subplots = ArrayUtils.redimPreserve(subplots, row+1);
			for (int i=0; i<subplots.length; i++)
				subplots[i] = new GPlotSubPlot[col+1];
		}
		if(col>=subplots[row].length) 
			for (int i=0; i<subplots.length; i++)
				subplots[i] = ArrayUtils.redimPreserve(subplots[i], col+1);
		
		if (subplots[row][col]==null)
			subplots[row][col] = new GPlotSubPlot(FunctorUtils.constantFunction(GPlotUtils.inferScaling(x().getColumn())),FunctorUtils.constantFunction(GPlotUtils.inferScaling(y().getColumn())));
		return subplots[row][col];
	}
	
	public GPlot title(String title) {
		this.title = title;
		return this;
	}
	
	public GPlot xlab(String xlab) {
		this.xlab = xlab;
		return this;
	}
	
	public GPlot ylab(String ylab) {
		this.ylab = ylab;
		return this;
	}
	
	public GPlot labels(String title, String xlab, String ylab) {
		this.title = title;
		this.xlab = xlab;
		this.ylab = ylab;
		return this;
	}
	
	public List<GPrimitive> getLayersForSubplot(GPlotSubPlot subplot) {
		return layers;
	}
	
	public List<GPrimitive> getLayers() {
		return layers;
	}
	
	public XAesthetic x() 			{ return x;}
	public GPlot x(int column) 			{ this.x = XAesthetic.infer(df.getColumn(column)); return this; }
	public GPlot x(String column) 		{ this.x = XAesthetic.infer(df.getColumn(column)); return this; }
	
	public YAesthetic y() 			{ return y;}
	public GPlot y(int column) 			{ this.y = YAesthetic.infer(df.getColumn(column)); return this; }
	public GPlot y(String column) 			{ this.y = YAesthetic.infer(df.getColumn(column)); return this; }
	
	public PaintAesthetic color() 			{ return color;}
	public GPlot color(int column) 			{ this.color = PaintAesthetic.infer(df.getColumn(column)); return this; }
	public GPlot color(String column) 			{ this.color = PaintAesthetic.infer(df.getColumn(column)); return this; }
	

	public Aesthetic<Paint,PaintScalingPreprocessed> fill() 			{ return fill;}
	public GPlot fill(Aesthetic<Paint,PaintScalingPreprocessed> fill) 	{ this.fill = fill; return this; }
	public GPlot fill(int column) 			{ this.fill = new Aesthetic<Paint,PaintScalingPreprocessed>(df.getColumn(column),PaintScalingPreprocessed.compute(df.getColumn(column),this),(p,c,r)->p.getPaint(c,r),new PaintLegend()); return this; }
	public GPlot fill(String column) 		{ this.fill = new Aesthetic<Paint,PaintScalingPreprocessed>(df.getColumn(column),PaintScalingPreprocessed.compute(df.getColumn(column),this),(p,c,r)->p.getPaint(c,r),new PaintLegend()); return this; }

	public Aesthetic<Glyph,GlyphScalingPreprocessed> glyph() 			{ return glyph;}
	public GPlot glyph(Aesthetic<Glyph,GlyphScalingPreprocessed> glyph) 	{ this.glyph = glyph; return this; }
	public GPlot glyph(int column) 			{ this.glyph = new Aesthetic<Glyph,GlyphScalingPreprocessed>(df.getColumn(column),new GlyphScalingPreprocessed(df.getColumn(column),Glyph.GLYPHS),(p,c,r)->p.get(c.getFactorValue(r).getIndex()),new GlyphLegend()); return this; }
	public GPlot glyph(String column) 		{ this.glyph = new Aesthetic<Glyph,GlyphScalingPreprocessed>(df.getColumn(column),new GlyphScalingPreprocessed(df.getColumn(column),Glyph.GLYPHS),(p,c,r)->p.get(c.getFactorValue(r).getIndex()),new GlyphLegend()); return this; }

	public Aesthetic<LineType,LineTypeScalingPreprocessed> lty() 			{ return lty;}
	public GPlot lty(Aesthetic<LineType,LineTypeScalingPreprocessed> lty) 	{ this.lty = lty; return this; }
	public GPlot lty(int column) 			{ this.lty = new Aesthetic<LineType,LineTypeScalingPreprocessed>(df.getColumn(column),new LineTypeScalingPreprocessed(LineType.TYPES),(p,c,r)->p.get(c.getFactorValue(r).getIndex()), new LineTypeLegend()); return this; }
	public GPlot lty(String column) 		{ this.lty = new Aesthetic<LineType,LineTypeScalingPreprocessed>(df.getColumn(column),new LineTypeScalingPreprocessed(LineType.TYPES),(p,c,r)->p.get(c.getFactorValue(r).getIndex()), new LineTypeLegend()); return this; }

	
	public DoubleAesthetic alpha() 			{ return alpha;}
	public GPlot alpha(DoubleAesthetic alpha) 	{ this.alpha = alpha; return this; }
	public GPlot alpha(int column) 			{ this.alpha = new DoubleAesthetic(df.getColumn(column),DoubleScalingPreprocessed.compute(df.getColumn(column)),new DoubleAestheticScale(DoubleInvertibleTransformer.identity,DoubleInvertibleTransformer.identity), new AlphaLegend()).tickCount(getThemeInt("plot.ticks")); return this; }
	public GPlot alpha(String column) 		{ this.alpha = new DoubleAesthetic(df.getColumn(column),DoubleScalingPreprocessed.compute(df.getColumn(column)),new DoubleAestheticScale(DoubleInvertibleTransformer.identity,DoubleInvertibleTransformer.identity), new AlphaLegend()).tickCount(getThemeInt("plot.ticks")); return this; }
	
	public DoubleAesthetic size() 			{ return size;}
	public GPlot size(DoubleAesthetic size) 	{ this.size= size; return this; }
	public GPlot size(int column) 			{ this.size = new DoubleAesthetic(df.getColumn(column),DoubleScalingPreprocessed.compute(df.getColumn(column)),new DoubleAestheticScale(DoubleInvertibleTransformer.identity,DoubleInvertibleTransformer.iscale(getThemeDouble("size.min"),getThemeDouble("size.max"))), new SizeLegend()).tickCount(getThemeInt("plot.ticks")); return this; }
	public GPlot size(String column) 		{ this.size = new DoubleAesthetic(df.getColumn(column),DoubleScalingPreprocessed.compute(df.getColumn(column)),new DoubleAestheticScale(DoubleInvertibleTransformer.identity,DoubleInvertibleTransformer.iscale(getThemeDouble("size.min"),getThemeDouble("size.max"))), new SizeLegend()).tickCount(getThemeInt("plot.ticks")); return this; }
	
	public DoubleAesthetic lwd() 			{ return lwd;}
	public GPlot lwd(DoubleAesthetic lwd) 	{ this.lwd= lwd; return this; }
	public GPlot lwd(int column) 			{ this.lwd = new DoubleAesthetic(df.getColumn(column),DoubleScalingPreprocessed.compute(df.getColumn(column)),new DoubleAestheticScale(DoubleInvertibleTransformer.identity,DoubleInvertibleTransformer.iscale(getThemeDouble("lwd.min"),getThemeDouble("lwd.max"))), new LineWidthLegend()).tickCount(getThemeInt("plot.ticks")); return this; }
	public GPlot lwd(String column) 		{ this.lwd = new DoubleAesthetic(df.getColumn(column),DoubleScalingPreprocessed.compute(df.getColumn(column)),new DoubleAestheticScale(DoubleInvertibleTransformer.identity,DoubleInvertibleTransformer.iscale(getThemeDouble("lwd.min"),getThemeDouble("lwd.max"))), new LineWidthLegend()).tickCount(getThemeInt("plot.ticks")); return this; }
	
	
	
	private HashMap<String,?> themeBuffer = new HashMap<>();
	@SuppressWarnings("unchecked")
	public <T> T getTheme(String key) {
		return (T) themeBuffer.computeIfAbsent(key, k->{
			try {
				return theme.eval("style."+key);
			} catch (Exception e) {
				return null;
			}
		});
	}
	public double getThemeDouble(String key) {
		Number n = getTheme(key);
		return n==null?0:n.doubleValue();
	}
	public boolean getThemeBoolean(String key) {
		Boolean re = getTheme(key);
		return re!=null && re;
	}
	public int getThemeInt(String key) {
		Number re = getTheme(key);
		return re.intValue();
	}

	
	public GPlot layer(GPrimitive layer) {
		layer.init(this);
		layers.add(layer);
		return this;
	}
	
	
	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
		try {
			GPlotLayout layout = theme.invokeFunctionOn("createLayout",this,context);
			return layout.measureMinSize(context);
		} catch (ScriptException e) {
			throw new RuntimeException("Could not create layout from theme!",e);
		}
		
	}
	
	@Override
	public void render(GPlotContext context, Rectangle2D area) {
		context.gplot = this;
		
		context.g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		context.g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		Paint bg = getTheme("plot.background");
		if (bg!=null) {
			context.g2.setPaint(bg);
			context.g2.fill(area);
		}
		
		GPlotLayout layout = null;
		try {
			layout = theme.invokeFunctionOn("createLayout",this,context);
		} catch (ScriptException e) {
			throw new RuntimeException("Could not create layout from theme!",e);
		}
		layout.render(context, area);
		
	}
	
	public void renderImage(String path) throws IOException {
		BufferedImage img = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
		render(new GPlotContext(new Rectangle2D.Double(0, 0, img.getWidth(), img.getHeight()),img.createGraphics()),new Rectangle2D.Double(0, 0, img.getWidth(), img.getHeight()));
		ImageIO.write(img, path.substring(path.lastIndexOf('.')+1), new File(path));
	}
	
	public static void main(String[] args) throws IOException {
		Gedi.startup(true);
//		DataFrame df = Csv.toDataFrame(GPlot.class.getResource("/resources/plot/mpg.csv"));
//		new GPlot(df).title("Test").x(2).y(8).size(2).color("class").glyph("fl").layer(new GPoints()).renderImage("test.png");
		DataFrame df = Csv.toDataFrame(GPlot.class.getResource("/resources/plot/normal.csv"));
		new GPlot(df).title("Test").x("x").y("y").color("y").layer(new GPoints()).renderImage("test.png");
		
		
	}
	

	
	
	public class GPlotComfortHelper {
		public GPlotRenderableLabel title() {
			return new GPlotRenderableLabel().label(title,getTheme("title.color"),getTheme("title.font"),getThemeBoolean("title.bold"),getThemeDouble("title.size"));
		}
		
		public GPlotEmptyRenderable empty() {
			return new GPlotEmptyRenderable();
		}
		
		public GPlotRenderableLabel xlab() {
			String xlab = GPlot.this.xlab;
			if (xlab==null && x!=null)
				xlab = x.getColumn().name();
			return new GPlotRenderableLabel().label(xlab,getTheme("xlab.color"),getTheme("xlab.font"),getThemeBoolean("xlab.bold"),getThemeDouble("xlab.size"));
		}

		public GPlotRenderableLabel ylab() {
			String ylab = GPlot.this.ylab;
			if (ylab==null && y!=null)
				ylab = y.getColumn().name();
			return new GPlotRenderableLabel().label(ylab,getTheme("ylab.color"),getTheme("ylab.font"),getThemeBoolean("ylab.bold"),getThemeDouble("ylab.size")).left();
		}

		public GPlotSubPlot subplot(int row, int col) {
			return getSubplot(row, col);
		}
		
		public GPlotLayoutTable table() {
			return new GPlotLayoutTable();
		}
		
		public GPlotPlaneBackground background() {
			return new GPlotPlaneBackground()
					.color(getTheme("background.color"))
					.ticks(getTheme("background.tickColor"), getThemeDouble("ticks.width"), getTheme("ticks.type"))
					.subTicks(getTheme("background.subtickColor"), getThemeDouble("subticks.width"), getTheme("subticks.type"));
		}
		
		public GPlotLegends legends() {
			return new GPlotLegends()
					.labels(getTheme("legend.labels.color"), getTheme("legend.labels.font"),getThemeBoolean("legend.labels.bold"), getThemeDouble("legend.labels.size"))
					.title(getTheme("legend.title.background"), getTheme("legend.title.color"), getTheme("legend.title.font"),getThemeBoolean("legend.title.bold"), getThemeDouble("legend.title.size"))
					.style(getTheme("legend.background"), getTheme("legend.border.linetype"),getThemeDouble("legend.border.width"), getTheme("legend.border.color"))
					.field(getTheme("legend.field.background"),getThemeDouble("legend.field.size"))
					.distance(getThemeDouble("legend.distance"));
		}
		
		public GPlotScale scaleBottom() {
			return new GPlotScale(ctx->x().getScale().getTicks(ctx))
					.bottom()
					.ticks(getTheme("ticks.color"), getThemeDouble("ticks.width"), getThemeDouble("ticks.size"), getTheme("ticks.type"))
					.labels(getTheme("ticklabels.color"), getTheme("ticklabels.font"),getThemeBoolean("ticklabels.bold"), getThemeDouble("ticklabels.size"), getThemeDouble("ticklabels.distance"));
		}
		
		public GPlotScale scaleLeft() {
			return new GPlotScale(ctx->y().getScale().getTicks(ctx))
					.left()
					.ticks(getTheme("ticks.color"), getThemeDouble("ticks.width"), getThemeDouble("ticks.size"), getTheme("ticks.type"))
					.labels(getTheme("ticklabels.color"), getTheme("ticklabels.font"),getThemeBoolean("ticklabels.bold"), getThemeDouble("ticklabels.size"), getThemeDouble("ticklabels.distance"));
		}

		public GPlotScale scaleRight() {
			return new GPlotScale(ctx->y().getScale().getTicks(ctx))
					.right()
					.ticks(getTheme("ticks.color"), getThemeDouble("ticks.width"), getThemeDouble("ticks.size"), getTheme("ticks.type"))
					.labels(getTheme("ticklabels.color"), getTheme("ticklabels.font"),getThemeBoolean("ticklabels.bold"), getThemeDouble("ticklabels.size"), getThemeDouble("ticklabels.distance"));
		}

		public GPlotScale scaleTop() {
			return new GPlotScale(ctx->x().getScale().getTicks(ctx))
					.top()
					.ticks(getTheme("ticks.color"), getThemeDouble("ticks.width"), getThemeDouble("ticks.size"), getTheme("ticks.type"))
					.labels(getTheme("ticklabels.color"), getTheme("ticklabels.font"),getThemeBoolean("ticklabels.bold"), getThemeDouble("ticklabels.size"), getThemeDouble("ticklabels.distance"));
		}

		public GPlotLayoutStacking stack() {
			return new GPlotLayoutStacking();
		}
		
	}
	
}
