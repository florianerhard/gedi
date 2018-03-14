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
package gedi.plot.layout;


import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

import cern.colt.bitvector.BitVector;
import gedi.plot.GPlotContext;
import gedi.plot.GPlotSubPlot;
import gedi.plot.GPlotRenderable;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.bitcollections.BitList;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.gui.Dimension2DDouble;

import static gedi.util.Checker.*;

public class GPlotLayoutTable implements GPlotLayout {

	private Paint debug = null;
	
	private ArrayList<GPlotLayoutTableRow> rows = new ArrayList<>();
	private double[] innerLayout = {Double.NaN,  Double.NaN};
	
	public GPlotLayoutTable debug(Paint debug) {
		this.debug = debug;
		return this;
	}

	public GPlotLayoutTableRow row() {
		GPlotLayoutTableRow re = new GPlotLayoutTableRow();
		rows.add(re);
		return re;
	}
	
	public GPlotLayoutTableRow row(double weight) {
		GPlotLayoutTableRow re = new GPlotLayoutTableRow(weight);
		rows.add(re);
		return re;
	}
	
	public GPlotLayoutTable inner(double[] inner) {
		this.innerLayout = inner;
		return this;
	}
	
	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
		if (rows.size()==0) return null;
		
		for (int j=1; j<rows.size(); j++) 
			equal(rows.get(j).size(),rows.get(0).size());
		
		double[] horizontal = new double[rows.get(0).size()];
		double[] vertical = new double[rows.size()];

		for (int j=0; j<rows.size(); j++) {
			GPlotLayoutTableRow row = rows.get(j);
			for (int i=0; i<row.size(); i++) {
				double[] mar = row.getMargin(i, context);
				if (row.getSubplot(i)!=null)
					context.currentFacet = row.getSubplot(i);
				Dimension2D dim = row.getElement(i).measureMinSize(context);
				dim = new Dimension2DDouble(dim.getWidth()+mar[1]+mar[3], dim.getHeight()+mar[0]+mar[2]);
				if (dim!=null) {
					horizontal[i] = Math.max(horizontal[i], dim.getWidth());
					vertical[j] = Math.max(vertical[j], dim.getHeight());
				}
				if (row.getSubplot(i)!=null)
					context.currentFacet = null;
			}
		}
		
		return new Dimension2DDouble(ArrayUtils.sum(horizontal), ArrayUtils.sum(vertical));
	}
	
	@Override
	public void render(GPlotContext context, Rectangle2D area) {
		
		if (rows.size()==0) return;
		
		for (int j=1; j<rows.size(); j++) 
			equal(rows.get(j).size(),rows.get(0).size());
		
		double[] minHorizontal = new double[rows.get(0).size()];
		double[] minVertical = new double[rows.size()];
		
		double[] resizeHorizontal = new double[rows.get(0).size()];
		double[] resizeVertical = new double[rows.size()];

		
		for (int j=0; j<rows.size(); j++) {
			GPlotLayoutTableRow row = rows.get(j);
			resizeVertical[j] = row.getRowWeight();
			
			for (int i=0; i<row.size(); i++) {
				resizeHorizontal[i] = Math.max(resizeHorizontal[i], row.getColWeight(i));
				resizeVertical[j] = Math.max(resizeVertical[j], row.getColWeight(i));
				double[] mar = row.getMargin(i,context);
				
				if (row.getSubplot(i)!=null)
					context.currentFacet = row.getSubplot(i);
				
				Dimension2D dim = row.getElement(i).measureMinSize(context);
				minHorizontal[i] = Math.max(minHorizontal[i], dim.getWidth()+mar[1]+mar[3]);
				minVertical[j] = Math.max(minVertical[j], dim.getHeight()+mar[0]+mar[2]);
				
				if (debug!=null) {
					System.out.println(j+","+i+" Min: "+dim+" r="+row.getRowWeight()+" c="+row.getColWeight(i));
				}
				if (row.getSubplot(i)!=null)
					context.currentFacet = null;
				
			}
		}
		
		ArrayUtils.add(resizeHorizontal, 1E-4);
		ArrayUtils.add(resizeVertical, 1E-4);
		ArrayUtils.normalize(resizeHorizontal);
		ArrayUtils.normalize(resizeVertical);
				
		
		double[] horizontal = computeLayout(minHorizontal,resizeHorizontal, area.getWidth(), innerLayout[0]);
		double[] vertical = computeLayout(minVertical,resizeVertical, area.getHeight(), innerLayout[1]);
		
		if (debug!=null) {
			System.out.println("Min: "+Arrays.toString(minHorizontal)+" "+Arrays.toString(minVertical));
			System.out.println("Wei: "+Arrays.toString(resizeHorizontal)+" "+Arrays.toString(resizeVertical));
			System.out.println("Res: "+Arrays.toString(horizontal)+" "+Arrays.toString(vertical));
		}
		
		
		ArrayUtils.cumSumInPlace(vertical, 1);
		ArrayUtils.cumSumInPlace(horizontal, 1);

		
		process(context, area, vertical, horizontal, minVertical, minHorizontal,(row,col,sub,outer)->{
			if (row.determinesSubplot(col)) {
				row.getSubplot(col).setArea(new Rectangle2D.Double(sub.getX(), sub.getY(), sub.getWidth(), sub.getHeight()));
			}
		});
		process(context, area, vertical, horizontal, minVertical, minHorizontal, (row,col,sub,outer)->{
			row.getElement(col).render(context, sub);
			if (debug!=null)
				renderDebug(context.g2,outer,sub);
		});
		
		
	}
	
	private void process(GPlotContext context, Rectangle2D area, double[] vertical, double[] horizontal, double[] minVertical, double[] minHorizontal, LayoutAction action) {
		Rectangle2D outer = new Rectangle2D.Double();
		Rectangle2D sub = debug==null?outer:new Rectangle2D.Double();
		
		for (int j=0; j<rows.size(); j++) {
			GPlotLayoutTableRow row = rows.get(j);
			double lasty = vertical[j];
			
			for (int i=0; i<row.size(); i++) {
				
				if (row.getSubplot(i)!=null)
					context.currentFacet = row.getSubplot(i);
				
				double[] mar = row.getMargin(i,context);
				double hscale = (horizontal[i+1]-horizontal[i]) / minHorizontal[i];
				double vscale = (vertical[j+1]-vertical[j]) / minVertical[j];
				scaleMargin(mar,hscale,vscale);
				
				GPlotRenderable ren= row.getElement(i);
				double lastx = i==0?0:horizontal[i];
				outer.setRect(
						lastx+area.getX(), 
						lasty+area.getY(), 
						(horizontal[i+1]-lastx), 
						(vertical[j+1]-lasty)
							);
				
				Paint bg = row.getBackground(i);
				if (bg!=null) {
					context.g2.setPaint(bg);
					context.g2.fill(outer);
				}
				
				if (debug!=null)
					sub.setRect(outer);
				
				addMargin(sub,mar);
				if (ren.keepAspectRatio())
					correctAspect(sub,ren.measureMinSize(context));
				correctInner(sub,row.getElement(i).measureMinSize(context), row.getInner(i));
				
				action.perform(row,i,sub,outer);
				
				if (row.getSubplot(i)!=null)
					context.currentFacet = null;
			}
		}
	}

	private static interface LayoutAction {
		void perform(GPlotLayoutTableRow row, int col, Rectangle2D sub, Rectangle2D outer);
	}
	
	private void correctInner(Rectangle2D rect, Dimension2D min, double[] inner) {
		double x = rect.getX();
		double y = rect.getY();
		double weight = rect.getWidth();
		double height = rect.getHeight();
		
		if (min.getWidth()<rect.getWidth() && !Double.isNaN(inner[0])) {
			double off = rect.getWidth()-min.getWidth();
			weight-=off;
			x+=off*(inner[0]*0.5+0.5);
		}
		
		if (min.getHeight()<rect.getHeight() && !Double.isNaN(inner[1])) {
			double off = rect.getHeight()-min.getHeight();
			height-=off;
			y+=off*(inner[1]*0.5+0.5);
		}
		
		rect.setRect(x, y, weight, height);
	}

	private void scaleMargin(double[] mar, double hscale, double vscale) {
		mar[0]*=vscale;
		mar[1]*=hscale;
		mar[2]*=vscale;
		mar[3]*=hscale;
	}

	private void renderDebug(Graphics2D g2, Rectangle2D outer, Rectangle2D inner) {
		g2.setPaint(debug);
		g2.draw(inner);
		g2.draw(outer);
	}

	private double[] computeLayout(double[] min, double[] weights, double total, double inner) {
		double[] re = new double[weights.length+1];
		
		double room = total - ArrayUtils.sum(min);
		if (room<=0) {
			double s=0;
			for (int i=0; i<min.length; i++)
				s+=min[i]*weights[i];
			double f = room/s;
			for (int i=0; i<weights.length; i++) {
				re[i+1] = min[i]+min[i]*weights[i]*f;
			}
		} else {
			if (Double.isNaN(inner)) { // expand
				for (int i=0; i<weights.length; i++)
					re[i+1] = min[i]+room*weights[i];
			} else {
				re[0] = room*(inner*0.5+0.5);
				System.arraycopy(min, 0, re, 1, min.length);
			}
		}
		return re;
	}



	public class GPlotLayoutTableRow {
		
		private ArrayList<GPlotRenderable> elements = new ArrayList<>();
		private ArrayList<double[]> margin = new ArrayList<>();
		
		/**
		 * [h,v]: -1 to 1 for not expanding fields, Double.NaN for expanding
		 */
		private ArrayList<double[]> innerLayout = new ArrayList<>();
		private ArrayList<Paint> background = new ArrayList<>();
		private DoubleArrayList colWeights = new DoubleArrayList();
		private ArrayList<GPlotSubPlot> subplots = new ArrayList<>();
		private BitList determinesSub = new BitList();
		private double rowWeight = 0;

		public GPlotLayoutTableRow() {
		}
		public Paint getBackground(int i) {
			return background.get(i);
		}
		public double[] getInner(int i) {
			return innerLayout.get(i);
		}
		public GPlotLayoutTableRow(double weight) {
			this.rowWeight = weight;
		}
		
		public double getColWeight(int i) {
			return colWeights.getDouble(i);
		}

		public double getRowWeight() {
			return rowWeight;
		}

		public GPlotLayoutTableRow weight(double weight) {
			this.colWeights.set(colWeights.size()-1, weight);
			return this;
		}
		
		public GPlotLayoutTableRow margin(double[] margin) {
			this.margin.set(this.margin.size()-1, margin);
			return this;
		}

		public GPlotLayoutTableRow sp(GPlotSubPlot subplot) {
			this.subplots.set(this.subplots.size()-1, subplot);
			return this;
		}
		public GPlotLayoutTableRow spm(GPlotSubPlot subplot) {
			this.determinesSub.set(this.determinesSub.size()-1, true);
			return sp(subplot);
		}

		public GPlotLayoutTableRow inner(double[] inner) {
			this.innerLayout.set(this.innerLayout.size()-1, inner);
			return this;
		}
		
		public GPlotLayoutTableRow background(Paint background) {
			this.background.set(this.background.size()-1, background);
			return this;
		}
		
		public GPlotLayoutTableRow col(GPlotRenderable ren) {
			return col(ren,0,new double[] {0,0,0,0}, new double[] {Double.NaN, Double.NaN}, null);
		}
		public GPlotLayoutTableRow col(GPlotRenderable ren, double weight, double[] margin, double[] inner, Paint background) {
			this.elements.add(ren);
			this.colWeights.add(weight);
			this.margin.add(margin);
			this.innerLayout.add(inner);
			this.background.add(background);
			this.subplots.add(null);
			this.determinesSub.add(false);
			return this;
		}
		
		public GPlotRenderable getElement(int i) {
			return elements.get(i);
		}
		
		public GPlotSubPlot getSubplot(int i) {
			return subplots.get(i);
		}
		
		public boolean determinesSubplot(int i) {
			return determinesSub.getBit(i);
		}
		
		public double[] getMargin(int index, GPlotContext context) {
			double[] un = margin.get(index);
			return new double[] {
					sizeToPixelVertical(un[0], context),
					sizeToPixelHorizontal(un[1], context),
					sizeToPixelVertical(un[2], context),
					sizeToPixelHorizontal(un[3], context)
			};
		}
		
		public int size() {
			return elements.size();
		}

	}




	
	
}
