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
package gedi.plot.primitives;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import gedi.util.gui.TransformedStroke;

/**
 * Each glyph corresponds to a {@link Shape} that is centered at 0,0 and has a size of 1x1
 * @author flo
 *
 */
public class Glyph {

	private Shape shape;
	private boolean filled;

	public Glyph(Shape shape, boolean filled) {
		this.shape = shape;
		this.filled = filled;
	}


	/**
	 * Render this glyph at the specified coordinates (in pixel space) in the given size (as a factor)
	 * @param g2
	 * @param x
	 * @param y
	 * @param size
	 */
	public void render(Graphics2D g2, double x, double y, double size) {
		AffineTransform at = new AffineTransform();
		at.translate(x, y);
		at.scale(size, size);
		Shape shape = at.createTransformedShape(this.shape);
		
		if (filled)
			g2.fill(shape);
		else
			g2.draw(shape);

	}


	public static Glyph DOT = new Glyph(new Ellipse2D.Double(-0.5, -0.5, 1, 1),true);
	public static Glyph UP_TRIANGLE = new Glyph(SymbolShape.createUpTriangle(),true);
	public static Glyph BOX = new Glyph(SymbolShape.createBox(),true);
	public static Glyph PLUS = new Glyph(SymbolShape.createPlus(),false);
	public static Glyph CROSSBOX = new Glyph(SymbolShape.concat(SymbolShape.createPlus(),SymbolShape.createBox()),false);
	public static Glyph STAR = new Glyph(SymbolShape.createStar(),false);
	

	public static final Glyph[] GLYPHS = {DOT,UP_TRIANGLE,BOX,PLUS,CROSSBOX,STAR};


	
	private static class SymbolShape implements Shape {

		private static final double SQRT_2 = Math.sqrt(2.);
		private static final double SQRT_3 = Math.sqrt(3.);


		private class ArrayPathIterator implements PathIterator {

			private int currentPoint = 0;

			private double[] points;

			private int[] type;


			private ArrayPathIterator(double[] points, int[] type) {
				this.points = points;
				this.type = type;
			}

			public boolean isDone() {
				return currentPoint >= type.length;
			}

			public void next() {
				currentPoint++;
			}

			public int currentSegment(double[] coords) {
				coords[0] = points[2 * currentPoint];
				coords[1] = points[2 * currentPoint + 1];
				return type[currentPoint];
			}

			public int currentSegment(float[] coords) {
				coords[0] = (float) points[2 * currentPoint];
				coords[1] = (float) points[2 * currentPoint + 1];
				return type[currentPoint];
			}

			public int getWindingRule() {
				return PathIterator.WIND_NON_ZERO;
			}
		}

		private double points[];
		private int type[];


		public SymbolShape(int n) {
			this.points = new double[n * 2];
			this.type = new int[n];
		}

		public boolean contains(double x, double y) {
			return getBounds2D().contains(x, y);
		}

		public boolean contains(double x, double y, double w, double h) {
			return contains(x, y) && contains(x + w, y) && contains(x, y + h)
					&& contains(x + w, y + h);
		}

		public boolean contains(Point2D p) {
			return contains(p.getX(), p.getY());
		}

		public boolean contains(Rectangle2D r) {
			return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}

		/** Returns true, if at least one of the points is contained by the shape. */
		public boolean intersects(double x, double y, double w, double h) {
			return contains(x, y) || contains(x + w, y) || contains(x, y + h)
					|| contains(x + w, y + h);
		}

		public boolean intersects(Rectangle2D r) {
			return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
		}

		public PathIterator getPathIterator(AffineTransform at, double flatness) {
			return getPathIterator(at);
		}

		public Rectangle2D getBounds2D() {
			return new Rectangle2D.Double(-.5, -.5, 1, 1);
		}

		public Rectangle getBounds() {
			return getBounds2D().getBounds();
		}

		public PathIterator getPathIterator(AffineTransform t) {
			ArrayPathIterator re = new ArrayPathIterator(points.clone(), type);
			if (t != null) 
				t.transform(re.points, 0, re.points, 0, re.points.length / 2);
			return re;
		}


		private static SymbolShape createPlus() {
			SymbolShape re = new SymbolShape(4);

			double length = .5;

			re.type[0] = PathIterator.SEG_MOVETO;
			re.points[0] = length;
			re.points[1] = 0;

			re.type[1] = PathIterator.SEG_LINETO;
			re.points[2] = -length;
			re.points[3] = 0;

			re.type[2] = PathIterator.SEG_MOVETO;
			re.points[4] = 0;
			re.points[5] = length;

			re.type[3] = PathIterator.SEG_LINETO;
			re.points[6] = 0;
			re.points[7] = - length;
			
			return re;
		}

		private static SymbolShape createCross() {
			SymbolShape re = new SymbolShape(4);
			double side = .5;

			re.type[0] = PathIterator.SEG_MOVETO;
			re.points[0] = -side;
			re.points[1] = -side;

			re.type[1] = PathIterator.SEG_LINETO;
			re.points[2] = side;
			re.points[3] = side;

			re.type[2] = PathIterator.SEG_MOVETO;
			re.points[4] = side;
			re.points[5] = -side;

			re.type[3] = PathIterator.SEG_LINETO;
			re.points[6] = -side;
			re.points[7] = side;
			
			return re;
		}

		private static SymbolShape createStar() {
			SymbolShape re = new SymbolShape(8);

			double delta = .5;

			re.type[0] = PathIterator.SEG_MOVETO;
			re.points[0] = 0;
			re.points[1] = -delta;

			re.type[1] = PathIterator.SEG_LINETO;
			re.points[2] = 0;
			re.points[3] = delta;

			re.type[2] = PathIterator.SEG_MOVETO;
			re.points[4] = -delta;
			re.points[5] = 0;

			re.type[3] = PathIterator.SEG_LINETO;
			re.points[6] = delta;
			re.points[7] = 0;

			delta = .5 / SQRT_2;

			re.type[4] = PathIterator.SEG_MOVETO;
			re.points[8] = -delta;
			re.points[9] = -delta;

			re.type[5] = PathIterator.SEG_LINETO;
			re.points[10] = delta;
			re.points[11] = delta;

			re.type[6] = PathIterator.SEG_MOVETO;
			re.points[12] = delta;
			re.points[13] = -delta;

			re.type[7] = PathIterator.SEG_LINETO;
			re.points[14] = -delta;
			re.points[15] = delta;
			
			return re;
		}

		private static SymbolShape createUpTriangle() {
			SymbolShape re = new SymbolShape(4);
		
			re.type[0] = PathIterator.SEG_MOVETO;
			re.points[0] = 0;
			re.points[1] = -.5;

			re.type[1] = PathIterator.SEG_LINETO;
			re.points[2] = -1/SQRT_3;
			re.points[3] = .5;

			re.type[2] = PathIterator.SEG_LINETO;
			re.points[4] = 1/SQRT_3;
			re.points[5] = .5;

			re.type[3] = PathIterator.SEG_CLOSE;
			
			return re;
		}

		private static SymbolShape createDownTriangle() {
			SymbolShape re = new SymbolShape(4);
		
			re.type[0] = PathIterator.SEG_MOVETO;
			re.points[0] = 0;
			re.points[1] = 1 / SQRT_3;

			re.type[1] = PathIterator.SEG_LINETO;
			re.points[2] = .5;
			re.points[3] = (-1 / SQRT_3 + SQRT_3 / 2.);

			re.type[2] = PathIterator.SEG_LINETO;
			re.points[4] = .5;
			re.points[5] = (-1 / SQRT_3 + SQRT_3 / 2.);

			re.type[3] = PathIterator.SEG_CLOSE;
			
			return re;
		}

		private static SymbolShape createDiamond() {
			SymbolShape re = new SymbolShape(5);
		
			double length = .5;

			re.type[0] = PathIterator.SEG_MOVETO;
			re.points[0] = length;
			re.points[1] = 0;

			re.type[1] = PathIterator.SEG_LINETO;
			re.points[2] = 0;
			re.points[3] = length;

			re.type[2] = PathIterator.SEG_LINETO;
			re.points[4] = -length;
			re.points[5] = 0;

			re.type[3] = PathIterator.SEG_LINETO;
			re.points[6] = 0;
			re.points[7] = -length;

			re.type[4] = PathIterator.SEG_CLOSE;
			return re;
		}

		private static SymbolShape createBox() {
			SymbolShape re = new SymbolShape(5);
			double side = .5;

			re.type[0] = PathIterator.SEG_MOVETO;
			re.points[0] = -side;
			re.points[1] = -side;

			re.type[1] = PathIterator.SEG_LINETO;
			re.points[2] = side;
			re.points[3] = -side;

			re.type[2] = PathIterator.SEG_LINETO;
			re.points[4] = side;
			re.points[5] = side;

			re.type[3] = PathIterator.SEG_LINETO;
			re.points[6] = -side;
			re.points[7] = side;

			re.type[4] = PathIterator.SEG_CLOSE;
			return re;
		}
		
		private static SymbolShape concat(SymbolShape a, SymbolShape b) {
			SymbolShape re = new SymbolShape(a.type.length+b.type.length);
			System.arraycopy(a.type, 0, re.type, 0, a.type.length);
			System.arraycopy(b.type, 0, re.type, a.type.length, b.type.length);
			System.arraycopy(a.points, 0, re.points, 0, a.points.length);
			System.arraycopy(b.points, 0, re.points, a.points.length, b.points.length);
			return re;
		}

	}
}
