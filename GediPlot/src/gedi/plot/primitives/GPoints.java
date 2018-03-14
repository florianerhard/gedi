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


import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import gedi.plot.GPlot;
import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.aesthetics.Aesthetic;
import gedi.plot.aesthetics.Aesthetic2;
import gedi.plot.aesthetics.DoubleAesthetic;
import gedi.plot.aesthetics.paint.PaintAesthetic;
import gedi.plot.aesthetics.x.XAesthetic;
import gedi.plot.aesthetics.y.YAesthetic;
import gedi.plot.scale.AestheticScale;
import gedi.plot.scale.DoubleAestheticScale;
import gedi.plot.scale.GlyphScalingPreprocessed;
import gedi.plot.scale.LineTypeScalingPreprocessed;
import gedi.plot.scale.PaintScalingPreprocessed;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.gui.Dimension2DDouble;
import gedi.util.gui.TransformedStroke;

import static gedi.util.Checker.*;

public class GPoints implements GPrimitive {

	private DataFrame df;
	private XAesthetic x; 
	private YAesthetic y;
	private PaintAesthetic paint;
	private Aesthetic<Glyph,GlyphScalingPreprocessed> glyph;
	private DoubleAesthetic alpha; 
	private DoubleAesthetic size;

	
	@Override
	public void init(GPlot gp) {
		this.df = gp.df();
		this.x = gp.x();
		this.y = gp.y();
		this.paint = gp.color();
		this.glyph = gp.glyph();
		this.alpha = gp.alpha();
		this.size = gp.size();
	}
	
	@Override
	public ExtendedIterator<Aesthetic<?,?>> getAesthetics() {
		return EI.wrap(glyph,alpha,size).removeNulls();
	}
	
	@Override
	public ExtendedIterator<Aesthetic2<?>> getAesthetics2() {
		return EI.<Aesthetic2<?>>wrap(paint).removeNulls();
	}
	
	@Override
	public void render(GPlotContext context, Rectangle2D area) {
		
		Graphics2D g2 = context.g2;
		
		Composite composite = g2.getComposite();
		Paint paint = g2.getPaint();
		Stroke stroke = g2.getStroke();
		
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
		g2.setPaint(Color.black);
		
		for (int r=1; r<df.rows(); r++) {
			if (alpha!=null)
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)alpha.transformAsDouble(r)));
			if (this.paint!=null)
				g2.setPaint(this.paint.transform(context,r));

			Glyph g = glyph==null?Glyph.DOT:glyph.transform(r);
			double s = sizeToPixel(size==null?1:size.transformAsDouble(r),context);
			
			g.render(g2, x.transform(context, r), y.transform(context, r), s);
			
		}
		
		g2.setPaint(paint);
		g2.setComposite(composite);
		g2.setStroke(stroke);
	}


	

	
}
