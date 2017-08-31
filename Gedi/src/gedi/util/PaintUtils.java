/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package gedi.util;

import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.mutable.MutableInteger;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.swing.svg.GVTTreeBuilderAdapter;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;
import org.apache.batik.swing.svg.JSVGComponent;
import org.apache.batik.util.XMLResourceDescriptor;
import org.freehep.graphicsbase.util.export.ExportFileType;
import org.w3c.dom.svg.SVGDocument;

import cern.colt.bitvector.BitMatrix;

public class PaintUtils {


	public static Color getLabelColor(Color background) {
		return isDarkColor(background) ? Color.white : Color.black;
	}


	/**
	 * Gets if the color is dark, i.e. if the average value of the 
	 * RGB channels is <127
	 * @param color the color
	 * @return if is is dark
	 */
	public static boolean isDarkColor(Color color) {
		return (color.getRed()+color.getGreen()+color.getBlue())/3<127;
	}




	public static String toIntRgb(Color color) {
		if (color==null) color = Color.black;
		StringBuilder sb = new StringBuilder();
		sb.append(color.getRed());
		sb.append(",");
		sb.append(color.getGreen());
		sb.append(",");
		sb.append(color.getBlue());
		return sb.toString();
	}


	public static HashMap<String,Color> colorcache = new HashMap<String, Color>(); 
	
	public static Color parseColor(Object c) {
		if (c instanceof Color) return (Color)c;
		if (c instanceof Integer) return new Color((Integer)c);
		if (c instanceof int[] && ((int[])c).length==3) return new Color(((int[])c)[0],((int[])c)[1],((int[])c)[2]);
		if (c instanceof float[] && ((float[])c).length==3) return new Color(((float[])c)[0],((float[])c)[1],((float[])c)[2]);
		if (c instanceof int[] && ((int[])c).length==4) return new Color(((int[])c)[0],((int[])c)[1],((int[])c)[2],((int[])c)[3]);
		if (c instanceof float[] && ((float[])c).length==4) return new Color(((float[])c)[0],((float[])c)[1],((float[])c)[2],((float[])c)[3]);
		if (c instanceof String) {
			if (colorcache.containsKey(c)) return colorcache.get(c);
			Color re = parseColor1((String)c);
			colorcache.put((String)c, re);
			return re;
		}
		return null;
	}
	
	private static Color parseColor1(String c) {
		String[] parts = StringUtils.split((String)c, ',');
		if (parts.length==1){
			try {
				if (parts[0].startsWith("#") && parts[0].length()==7)
					return new Color(
							Integer.parseInt(parts[0].substring(1,3), 16),
							Integer.parseInt(parts[0].substring(3,5), 16),
							Integer.parseInt(parts[0].substring(5,7), 16));
			} catch (NumberFormatException e) {
			}
			try {
				if (parts[0].startsWith("#") && parts[0].length()==9)
					return new Color(
							Integer.parseInt(parts[0].substring(1,3), 16),
							Integer.parseInt(parts[0].substring(3,5), 16),
							Integer.parseInt(parts[0].substring(5,7), 16),
							Integer.parseInt(parts[0].substring(7,9), 16));
			} catch (NumberFormatException e) {
			}

			try {
				Field f = Color.class.getField(parts[0]);
				return (Color) f.get(null);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
			}
			
			try {
				if (parts[0].length()==6)
					return new Color(
							Integer.parseInt(parts[0].substring(0,2), 16),
							Integer.parseInt(parts[0].substring(2,4), 16),
							Integer.parseInt(parts[0].substring(4,6), 16));
			} catch (NumberFormatException e) {
			}

			try {
				if (parts[0].length()==8)
					return new Color(
							Integer.parseInt(parts[0].substring(0,2), 16),
							Integer.parseInt(parts[0].substring(2,4), 16),
							Integer.parseInt(parts[0].substring(4,6), 16),
							Integer.parseInt(parts[0].substring(6,8), 16));
			} catch (NumberFormatException e) {
			}

			

		}
		if (parts.length==3) {
			if (StringUtils.isInt(parts[0]) && StringUtils.isInt(parts[1]) && StringUtils.isInt(parts[2]))
				return new Color(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]));
			else if (StringUtils.isNumeric(parts[0]) && StringUtils.isNumeric(parts[1]) && StringUtils.isNumeric(parts[2]))
				return new Color(Float.parseFloat(parts[0]),Float.parseFloat(parts[1]),Float.parseFloat(parts[2]));
		}
		else if (parts.length==4) {
			if (StringUtils.isInt(parts[0]) && StringUtils.isInt(parts[1]) && StringUtils.isInt(parts[2]) && StringUtils.isInt(parts[3]))
				return new Color(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]),Integer.parseInt(parts[3]));
			else if (StringUtils.isNumeric(parts[0]) && StringUtils.isNumeric(parts[1]) && StringUtils.isNumeric(parts[2]) && StringUtils.isNumeric(parts[2]))
				return new Color(Float.parseFloat(parts[0]),Float.parseFloat(parts[1]),Float.parseFloat(parts[2]),Float.parseFloat(parts[3]));
		}
		return null;
	}

	/**
	 * Encodes a color to a string value equal to the html representation.
	 * @see #decodeColor(String)
	 * @param color the color
	 * @return string representation
	 */
	public static String encodeColor(Color color) {
		return String.format("#%02X%02X%02X", color.getRed(),color.getGreen(),color.getBlue());
	}

	/**
	 * horizontal is from -1 to 1 where -1 means text is aligned left, 0 centered and 1 right
	 * vertical likewise (-1 is bottom)
	 * 
	 * shrink text if necessary!
	 * @param s
	 * @param g2
	 * @param rect
	 * @param vertical
	 * @param horizontal
	 */
	public static Rectangle2D paintString(String s, Graphics2D g2,
			Rectangle2D rect, float horizontal, float vertical) {
		return paintString(s, g2, rect, 0, 0, 0, horizontal, vertical,null);
	}
	
	public static Rectangle2D paintString(String s, Graphics2D g2,
			Rectangle2D rect, double rotation, float horizontal, float vertical) {
		return paintString(s, g2, rect, rotation, rect.getCenterX(), rect.getCenterY(), horizontal, vertical,null);
	}
	
	public static Rectangle2D paintString(String s, Graphics2D g2,
			Rectangle2D rect, double rotation, double rotX, double rotY, float horizontal, float vertical, Paint outline) {

		if (s==null) return new Rectangle2D.Double(rect.getX(),rect.getY(),0,0);

		AffineTransform trans = g2.getTransform();
		g2.setTransform(new AffineTransform());
		
		
		Font original = g2.getFont();
		vertical=vertical/2f+.5f;
		horizontal=horizontal/2f+.5f;

		Rectangle2D des = g2.getFontMetrics().getStringBounds(s, g2);

		double w = rect.getWidth();
		double h = rect.getHeight();
		
		if (rotation!=0) {
			double w1 = w*Math.cos(rotation)+h*Math.sin(rotation);
			h = h*Math.cos(rotation)+w*Math.sin(rotation);
			w = w1;
		}
		
		double xratio = des.getWidth()/w;
		double yratio = des.getHeight()/h;
		double maxratio = Math.max(xratio,yratio);

		if (maxratio>1) {
			g2.setFont(g2.getFont().deriveFont(AffineTransform.getScaleInstance(1/maxratio, 1/maxratio)));
			des = g2.getFontMetrics().getStringBounds(s, g2);	
		}

		double x = rect.getX()+horizontal*(rect.getWidth()-des.getWidth());
		double y = rect.getY()+g2.getFontMetrics().getAscent()+vertical*(rect.getHeight()-des.getHeight());

		g2.rotate(rotation,rotX,rotY);
		g2.drawString(s, (float)x, (float)y);
		if (outline!=null) {
			Shape outl =  g2.getFont().createGlyphVector(g2.getFontRenderContext(), s).getOutline();
			g2.translate(x, y);
			g2.setPaint(outline);
			g2.draw(outl);

		}

		g2.setFont(original);

		des.setRect(x, y-des.getHeight(), des.getWidth(), des.getHeight());
		
		g2.setTransform(trans);
		
		return des;
	}

	public static boolean fitString(String s, Graphics2D g2,
			Rectangle2D rect) {
		return getFitStringScale(s, g2, rect)>=1;
	}
	
	public static double getFitStringScale(String s, Graphics2D g2,
			Rectangle2D rect) {


		Rectangle2D des = g2.getFontMetrics().getStringBounds(s, g2);
		double xratio = rect.getWidth()/des.getWidth();
		double yratio = rect.getHeight()/des.getHeight();
		double min = Math.min(xratio,yratio);

		return min;
	}

	public static double getFitStringWidth(String s, Graphics2D g2, double h) {
		Rectangle2D des = g2.getFontMetrics().getStringBounds(s, g2);
		double min = Math.min(h/des.getHeight(),1);
		return des.getWidth()*min;
	}


	public static void paintMatrix(BitMatrix m, Graphics2D g2, Rectangle2D rect) {
		paintMatrix(m, g2, rect, Color.black, null, false);
	}

	public static void paintMatrix(BitMatrix m, Graphics2D g2, Rectangle2D rect, Paint truePaint, Paint falsePaint, boolean fill) {
		Paint p = g2.getPaint();
		Rectangle2D tile = (Rectangle2D) rect.clone();
		int w = m.columns();
		for (int i=0; i<w; i++) {
			int h = m.rows();
			for (int j=0; j<h; j++) {
				if (truePaint!=null && m.getQuick(w-i-1,j)) {
					g2.setPaint(truePaint);
					tile.setRect(rect.getMinX()+rect.getWidth()/h*j, rect.getMinY()+rect.getHeight()/w*i, rect.getWidth()/h, rect.getHeight()/w);
					if (fill)
						g2.fill(tile);
					else
						g2.draw(tile);
				} else if (falsePaint!=null && !m.getQuick(w-i-1,j)) {
					g2.setPaint(falsePaint);
					tile.setRect(rect.getMinX()+rect.getWidth()/h*j, rect.getMinY()+rect.getHeight()/w*i, rect.getWidth()/h, rect.getHeight()/w);
					if (fill)
						g2.fill(tile);
					else
						g2.draw(tile);
				}
			}
		}
		g2.setPaint(p);
	}

	public static void paintMatrix(int[][] m, Graphics2D g2, Rectangle2D rect, Function<Integer,? extends Paint> coloring) {
		Paint p = g2.getPaint();
		Rectangle2D tile = (Rectangle2D) rect.clone();
		int w = m.length;
		for (int i=0; i<w; i++) {
			int h = m[i].length;
			for (int j=0; j<h; j++) {
				g2.setPaint(coloring.apply(m[w-i-1][j]));
				tile.setRect(rect.getMinX()+rect.getWidth()/h*j, rect.getMinY()+rect.getHeight()/w*i, rect.getWidth()/h, rect.getHeight()/w);
				g2.fill(tile);
			}
		}
		g2.setPaint(p);
	}


	public static BufferedImage mergeImages(Image[][] images, float  horizontal, float vertical) {
		return mergeImages(images,new float[][] {{horizontal}},new float[][] {{vertical}});
	}

	/**
	 * Alignments: -1 means left/bottom
	 * @param images
	 * @param horizontal
	 * @param vertical
	 * @return
	 */
	public static BufferedImage mergeImages(Image[][] images, float[][] horizontal, float[][] vertical) {
		int[] cumh = new int[images.length];
		int[] cumw = new int[images[0].length];

		for (int i=0; i<images.length; i++)
			for (int j=0; j<images[i].length; j++) {
				cumh[i] = Math.max(cumh[i],images[i][j].getHeight(null));
				cumw[j] = Math.max(cumw[j],images[i][j].getWidth(null));
			}
		cumh=ArrayUtils.cumSum(cumh, 1);
		cumw=ArrayUtils.cumSum(cumw, 1);

		BufferedImage re = new BufferedImage(cumw[cumw.length-1], cumh[cumh.length-1], BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = re.createGraphics();
		for (int i=0; i<images.length; i++)
			for (int j=0; j<images[i].length; j++) {

				float vA=1-((i<vertical.length&&j<vertical[i].length?vertical[i][j]:vertical[0][0])/2f+.5f);
				float hA=((i<horizontal.length&&j<horizontal[i].length?horizontal[i][j]:horizontal[0][0])/2f+.5f);

				int w = images[i][j].getWidth(null);
				int aw = cumw[j]-(j>0?cumw[j-1]:0);
				int h = images[i][j].getHeight(null);
				int ah = cumh[i]-(i>0?cumh[i-1]:0);
				int x = (int) (cumw[j]-aw+((aw-w)*hA));
				int y = (int) (cumh[i]-ah+((ah-h)*vA));
				g2.drawImage(images[i][j],x,y,w,h,null);
			}
		return re;
	}


	public static class UpperLowerBoundTransformer<N extends Number> implements Function<N,Color> {

		private Color lowerBoundColor;
		private Color upperBoundColor;
		private Color nanColor;
		private N lowerBound;
		private N upperBound;
		private Function<N, Color> transformer;

		public UpperLowerBoundTransformer(Color nanColor, Color lowerBoundColor,
				Color upperBoundColor, N lowerBound, N upperBound,
				Function<N, Color> transformer) {
			this.nanColor = nanColor;
			this.lowerBoundColor = lowerBoundColor;
			this.upperBoundColor = upperBoundColor;
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
			this.transformer = transformer;
		}



		@Override
		public Color apply(N n) {
			if (Double.isNaN(n.doubleValue()))
				return nanColor;
			if (lowerBoundColor!=null && n.equals(lowerBound))
				return lowerBoundColor;
			else if (upperBoundColor!=null && n.equals(upperBound))
				return upperBoundColor;
			else
				return transformer.apply(n);
		}

	}

	public static class PaintMatrixTransformer implements Function<Point,Paint> {

		private Paint[][] matrix;

		public PaintMatrixTransformer(Paint[][] matrix) {
			this.matrix = matrix;
		}

		@Override
		public Paint apply(Point p) {
			return matrix[p.y][p.x];
		}

	}

	public static void paintSequenceMatrix(Graphics2D g2,Rectangle2D rect, String sequence, Function<Point,Paint> coloring, Function<Point,Paint> border) {
		Paint p = g2.getPaint();
		Rectangle2D tile = (Rectangle2D) rect.clone();
		int h=sequence.length()+2;
		int w=sequence.length()+2;

		Point point = new Point();
		for (int i=0; i<sequence.length(); i++) {
			point.y=i;
			for (int j=0; j<sequence.length(); j++) {
				point.x=j;
				Paint c = coloring.apply(point);
				if (c!=null) {
					tile.setRect(rect.getMinX()+rect.getWidth()/h*(j+1), rect.getMinY()+rect.getHeight()/w*(i+1), rect.getWidth()/h, rect.getHeight()/w);
					g2.setPaint(c);
					g2.fill(tile);
				}
			}
		}
		for (int i=0; i<sequence.length(); i++) {
			point.y=i;
			tile.setRect(rect.getMinX(), rect.getMinY()+rect.getHeight()/w*(i+1), rect.getWidth()/h, rect.getHeight()/w);
			g2.setPaint(Color.black);
			g2.fill(tile);
			g2.setPaint(Color.white);
			paintString(sequence.substring(i,i+1), g2, tile, 0, 0);
			tile.setRect(rect.getMaxX()-rect.getWidth()/h, rect.getMinY()+rect.getHeight()/w*(i+1), rect.getWidth()/h, rect.getHeight()/w);
			g2.setPaint(Color.black);
			g2.fill(tile);
			g2.setPaint(Color.white);
			paintString(sequence.substring(i,i+1), g2, tile, 0, 0);
			for (int j=0; j<sequence.length(); j++) {
				point.x=j;
				Paint b = border.apply(point);
				tile.setRect(rect.getMinX()+rect.getWidth()/h*(j+1), rect.getMinY()+rect.getHeight()/w*(i+1), rect.getWidth()/h, rect.getHeight()/w);
				if (b!=null) {
					g2.setPaint(b);
					g2.draw(tile);
				}
			}
		}

		for (int j=0; j<h-2; j++) {
			tile.setRect(rect.getMinX()+rect.getWidth()/h*(j+1), rect.getMinY(), rect.getWidth()/h, rect.getHeight()/w);
			g2.setPaint(Color.black);
			g2.fill(tile);
			g2.setPaint(Color.white);
			paintString(sequence.substring(j,j+1), g2, tile, 0, 0);
			tile.setRect(rect.getMinX()+rect.getWidth()/h*(j+1), rect.getMaxY()-rect.getHeight()/w, rect.getWidth()/h, rect.getHeight()/w);
			g2.setPaint(Color.black);
			g2.fill(tile);
			g2.setPaint(Color.white);
			paintString(sequence.substring(j,j+1), g2, tile, 0, 0);
		}
		g2.setPaint(p);

	}


	public static void normalize(Rectangle2D rect) {
		if (rect.getWidth()>=0 && rect.getHeight()>=0)
			return;
		double x = rect.getWidth()<0?rect.getX()+rect.getWidth():rect.getX();
		double y = rect.getHeight()<0?rect.getY()+rect.getHeight():rect.getY();
		double w = Math.abs(rect.getWidth());
		double h = Math.abs(rect.getHeight());
		rect.setRect(x, y, w, h);
	}





	/** A very dark red color. */
	public static final Color VERY_DARK_RED = new Color(0x80, 0x00, 0x00);

	/** A dark red color. */
	public static final Color DARK_RED = new Color(0xc0, 0x00, 0x00);

	/** A light red color. */
	public static final Color LIGHT_RED = new Color(0xFF, 0x40, 0x40);

	/** A very light red color. */
	public static final Color VERY_LIGHT_RED = new Color(0xFF, 0x80, 0x80);

	/** A very dark yellow color. */
	public static final Color VERY_DARK_YELLOW = new Color(0x80, 0x80, 0x00);

	/** A dark yellow color. */
	public static final Color DARK_YELLOW = new Color(0xC0, 0xC0, 0x00);

	/** A light yellow color. */
	public static final Color LIGHT_YELLOW = new Color(0xFF, 0xFF, 0x40);

	/** A very light yellow color. */
	public static final Color VERY_LIGHT_YELLOW = new Color(0xFF, 0xFF, 0x80);

	/** A very dark green color. */
	public static final Color VERY_DARK_GREEN = new Color(0x00, 0x80, 0x00);

	/** A dark green color. */
	public static final Color DARK_GREEN = new Color(0x00, 0xC0, 0x00);

	/** A light green color. */
	public static final Color LIGHT_GREEN = new Color(0x40, 0xFF, 0x40);

	/** A very light green color. */
	public static final Color VERY_LIGHT_GREEN = new Color(0x80, 0xFF, 0x80);

	/** A very dark cyan color. */
	public static final Color VERY_DARK_CYAN = new Color(0x00, 0x80, 0x80);

	/** A dark cyan color. */
	public static final Color DARK_CYAN = new Color(0x00, 0xC0, 0xC0);

	/** A light cyan color. */
	public static final Color LIGHT_CYAN = new Color(0x40, 0xFF, 0xFF);

	/** Aa very light cyan color. */
	public static final Color VERY_LIGHT_CYAN = new Color(0x80, 0xFF, 0xFF);

	/** A very dark blue color. */
	public static final Color VERY_DARK_BLUE = new Color(0x00, 0x00, 0x80);

	/** A dark blue color. */
	public static final Color DARK_BLUE = new Color(0x00, 0x00, 0xC0);

	/** A light blue color. */
	public static final Color LIGHT_BLUE = new Color(0x40, 0x40, 0xFF);

	/** A very light blue color. */
	public static final Color VERY_LIGHT_BLUE = new Color(0x80, 0x80, 0xFF);

	/** A very dark magenta/purple color. */
	public static final Color VERY_DARK_MAGENTA = new Color(0x80, 0x00, 0x80);

	/** A dark magenta color. */
	public static final Color DARK_MAGENTA = new Color(0xC0, 0x00, 0xC0);

	/** A light magenta color. */
	public static final Color LIGHT_MAGENTA = new Color(0xFF, 0x40, 0xFF);

	/** A very light magenta color. */
	public static final Color VERY_LIGHT_MAGENTA = new Color(0xFF, 0x80, 0xFF);

	/**
	 * Convenience method to return an array of <code>Paint</code> objects that
	 * represent the pre-defined colors in the <code>Color</code> and
	 * <code>ChartColor</code> objects.
	 *
	 * @return An array of objects with the <code>Paint</code> interface.
	 */
	public static Color[] createColorArray() {

		return new Color[] {
				new Color(0xFF, 0x55, 0x55),
				new Color(0x55, 0x55, 0xFF),
				new Color(0x55, 0xFF, 0x55),
				new Color(0xFF, 0xFF, 0x55),
				new Color(0xFF, 0x55, 0xFF),
				new Color(0x55, 0xFF, 0xFF),
				Color.pink,
				Color.gray,
				DARK_RED,
				DARK_BLUE,
				DARK_GREEN,
				DARK_YELLOW,
				DARK_MAGENTA,
				DARK_CYAN,
				Color.darkGray,
				LIGHT_RED,
				LIGHT_BLUE,
				LIGHT_GREEN,
				LIGHT_YELLOW,
				LIGHT_MAGENTA,
				LIGHT_CYAN,
				Color.lightGray,
				VERY_DARK_RED,
				VERY_DARK_BLUE,
				VERY_DARK_GREEN,
				VERY_DARK_YELLOW,
				VERY_DARK_MAGENTA,
				VERY_DARK_CYAN,
				VERY_LIGHT_RED,
				VERY_LIGHT_BLUE,
				VERY_LIGHT_GREEN,
				VERY_LIGHT_YELLOW,
				VERY_LIGHT_MAGENTA,
				VERY_LIGHT_CYAN
		};
	}
	public static double[] findNiceTicks(double min, double max, double count) {
		return findNiceTicks(min, max, count, null);
	}
	public static double[] findNiceTicks(double min, double max, double count, MutableInteger digits) {
		double range = nicenum(max-min,false);
		double d = nicenum(range/(count-1),true);
		double graphmin = Math.floor(min/d)*d;
		double graphmax = Math.ceil(max/d)*d;
		if (digits!=null)
			digits.N = Math.max(-(int)Math.floor(Math.log10(d)), 0);
		
		DoubleArrayList re = new DoubleArrayList();
		for (double x=graphmin; x<graphmax+0.5*d; x+=d)
			re.add(x);
		return re.toDoubleArray();
	}

	private static double nicenum(double x, boolean round) {
		int exp = (int)Math.floor(Math.log10(x));
		double f = x / Math.pow(10, exp);
		double nf;
		if (round) {
			if (f<1.5) nf = 1;
			else if (f<3) nf = 2;
			else if (f<7) nf = 5;
			else nf = 10;
		} else {
			if (f<=1) nf = 1;
			else if (f<=2) nf = 2;
			else if (f<=5) nf = 5;
			else nf = 10;
		}
		return nf*Math.pow(10, exp);
	}


	public static int findNiceNumberGreater(int dist) {
		int re = (int) Math.pow(10, Math.ceil(Math.log10(dist)));
		re = Math.min(re, (int) Math.pow(10, Math.ceil(Math.log10(dist*2)))/2);
		re = Math.min(re, (int) Math.pow(10, Math.ceil(Math.log10(dist*5)))/5);
		return re;
	}

	public static double[] getLinearTics(double min, double max, double minDistance) {
		int maxNumberTics = (int) Math.floor((max-min)/minDistance);
		
		double minTicDist = (max-min)/maxNumberTics;
		double ticDist = Math.pow(10, Math.ceil(Math.log10(minTicDist)));
		if (ticDist/5>=minTicDist)
			ticDist/=5;
		else if (ticDist/2>=minTicDist)
			ticDist/=2;
		
		if (ticDist==0) 
			return new double[] {min,max};
		
		DoubleArrayList tics = new DoubleArrayList();
		double m = max;
		for (double t = ticDist*Math.floor(min/ticDist); t<m; t+=ticDist)
			tics.add((float)t);
		return tics.toDoubleArray();
	}

	/**
	 * all in [0,1] !
	 * @param hue
	 * @param sat
	 * @param lum
	 * @return
	 */
	public static int HSLtoRGB(float hue, float sat, float lum)
	{
	    float v;
	    float red, green, blue;
	    float m;
	    float sv;
	    int sextant;
	    float fract, vsf, mid1, mid2;
	 
	    red = lum;   // default to gray
	    green = lum;
	    blue = lum;
	    v = (lum <= 0.5f) ? (lum * (1.0f + sat)) : (lum + sat - lum * sat);
	    m = lum + lum - v;
	    sv = (v - m) / v;
	    hue *= 6;  //get into range 0..6
	    sextant = (int) Math.floor(hue);  // int32 rounds up or down.
	    fract = hue - sextant;
	    vsf = v * sv * fract;
	    mid1 = m + vsf;
	    mid2 = v - vsf;
	 
	    if (v > 0)
	    {
	        switch (sextant)
	        {
	            case 0: red = v; green = mid1; blue = m; break;
	            case 1: red = mid2; green = v; blue = m; break;
	            case 2: red = m; green = v; blue = mid1; break;
	            case 3: red = m; green = mid2; blue = v; break;
	            case 4: red = mid1; green = m; blue = v; break;
	            case 5: red = v; green = m; blue = mid2; break;
	        }
	    }
	    int r = (int) (red * 255);
	    int g = (int) (green * 255);
	    int b = (int) (blue * 255);
	    return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
	}
	
	/**
	 * Layouts the jcomponent and saves it via freehep
	 * @param file
	 * @param comp
	 * @return whether this was successful
	 * @throws IOException 
	 */
	public static boolean screenshot(String file, JComponent comp) throws IOException {
		Properties p = new Properties();
		File f = new File(file);
		for (ExportFileType type : ExportFileType.getExportFileTypes(FileUtils.getExtension(f))) {
			if (type.fileHasValidExtension(f)) {
				JFrame packer = new JFrame();
				try {
					SwingUtilities.invokeAndWait(()->{
						packer.setContentPane(comp);
						packer.pack();
					});
				} catch (HeadlessException | InvocationTargetException
						| InterruptedException e) {
					return false;
				}
//				Stack<MutablePair<Component,String>> dfs = new Stack<MutablePair<Component,String>>();
//				dfs.add(new MutablePair<Component, String>(comp,""));
//				while (!dfs.isEmpty()) {
//					MutablePair<Component, String> pa = dfs.pop();
//					System.out.println(pa.Item2+pa.Item1.getClass()+" "+pa.Item1.getBounds());
//					if (pa.Item1 instanceof Container) {
//						for (Component c : ((Container) pa.Item1).getComponents())
//							dfs.add(new MutablePair<Component, String>(c, " "+pa.Item2));
//					}
//				}
				
				type.exportToFile(f, comp, null, p, "Gedi");
				packer.dispose();
				return true;
			}
		}
		
		return false;
	}


	public static JSVGComponent loadSvg(String path) {
		JSVGComponent re = new JSVGComponent();
		re.setRecenterOnResize(false);
		AtomicBoolean err = new AtomicBoolean(true);
		re.addGVTTreeBuilderListener(new GVTTreeBuilderAdapter() {
			@Override
			public void gvtBuildFailed(GVTTreeBuilderEvent e) {
				err.set(true);
				synchronized (re) {
					re.notify();	
				}
			}
			@Override
			public void gvtBuildCompleted(GVTTreeBuilderEvent e) {
				err.set(false);
				synchronized (re) {
					re.notify();	
				}
			}
		});
		re.loadSVGDocument(path);
		synchronized (re) {
			try {
				re.wait();
			} catch (InterruptedException e1) {
			}	
		}
		return err.get()?null:re;
	}
	
	public static SVGDocument loadSvgDoc(String path) throws IOException {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
	    SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
	    return (SVGDocument) f.createDocument(new File(path).toURI().toString());
	}


	public static BufferedImage resize(BufferedImage img, int nw, int nh) {
		BufferedImage re = new BufferedImage(nw, nh, img.getType());
		Graphics2D g2 = re.createGraphics();
	    g2.drawImage(img, 0, 0, null);
	    g2.dispose();
		return re;
	}
	
}


