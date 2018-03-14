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
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

public class LineType {

	private float[] dash;
	private float total;
	public LineType(float... dash) {
		this.dash = dash;
		if (dash!=null)
			for (float f : dash)
				total+=f;
	}
	
	
	public void renderSegment(Graphics2D g2, double x1, double y1, double x2, double y2, double lengthSoFar, double width) {
		if (width==0) return;
		float[] dash = this.dash;
		if (dash!=null && width!=1) {
			dash = dash.clone();
			for (int i=0; i<dash.length; i++) dash[i]*=width;
		}
		g2.setStroke(new BasicStroke((float)width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,1.0f, dash, (float)lengthSoFar/total));
		g2.draw(new Line2D.Double(x1, y1, x2, y2));
	}
	
	public void renderShape(Graphics2D g2, Shape shape, double width) {
		if (width==0) return;
		float[] dash = this.dash;
		if (dash!=null && width!=1) {
			dash = dash.clone();
			for (int i=0; i<dash.length; i++) dash[i]*=width;
		}
		g2.setStroke(new BasicStroke((float)width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,1.0f, dash, 0));
		g2.draw(shape);
	}
	
	

	public static final LineType SOLID   		= new LineType(null);
	public static final LineType DOTTED			= new LineType(1f,3f);
	public static final LineType DASHED			= new LineType(3f,3f);
	public static final LineType DASHDOTTED		= new LineType(3f,3f,1f,3f);
	public static final LineType SHORTDOTTED	= new LineType(1f,2f);
	public static final LineType SHORTDASHED	= new LineType(2f,2f);
	public static final LineType SHORTDASHDOTTED	= new LineType(2f,2f,1f,2f);
	public static final LineType LONGDOTTED		= new LineType(1f,4f);
	public static final LineType LONGDASHED		= new LineType(4f,4f);
	public static final LineType LONGDASHDOTTED		= new LineType(4f,4f,1f,4f);
	

	public static final LineType[] TYPES = {SOLID,DOTTED,DASHED,DASHDOTTED,SHORTDOTTED,SHORTDASHED,SHORTDASHDOTTED,LONGDOTTED,LONGDASHED,LONGDASHDOTTED};
	
}
