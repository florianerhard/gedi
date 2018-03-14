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
package gedi.gui.genovis.tracks;


import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.gui.genovis.VisualizationTrackAdapter;
import gedi.gui.genovis.VisualizationTrackPickInfo;
import gedi.util.PaintUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;


@GenomicRegionDataMapping(fromType=Void.class)
public class PositionTrack extends VisualizationTrackAdapter<Void,Integer> {

	private Color background = new Color(230, 230, 230);
	private Color foreground = Color.black;
	private Font font;
	
	public void setHeight(double height) {
		this.minHeight = this.prefHeight = this.maxHeight = height;
		if (viewer!=null) 
			this.viewer.relayout();
	}
	
	public void setFont(String name, int size, boolean bold, boolean italic) {
		int style = Font.PLAIN;
		if (bold) style|=Font.BOLD;
		if (italic) style|=Font.ITALIC;
		
		this.font = new Font(name, style, size);
	}

	public PositionTrack() {
		super(Void.class);
		this.minHeight = this.prefHeight = this.maxHeight = 15;
	}
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<Void> renderLabel(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<Void> context) {
		return context;
	}
	
	@Override
	public void pick(VisualizationTrackPickInfo<Integer> info) {
		info.setData(info.getBp());
	}
	
	@Override
	protected TrackRenderContext<Void> renderTrack(TrackRenderContext<Void> context) {
		Rectangle2D tile = new Rectangle2D.Double(
				0,
				getBounds().getY(),
				0,
				getBounds().getHeight()
				);
		Font f = context.g2.getFont();
		if (this.font!=null) context.g2.setFont(this.font);
		
		boolean drawn = false;
		for (int p=0; p<context.regionToRender.getNumParts(); p++) {
			
			double s = viewer.getLocationMapper().bpToPixel(context.reference,context.regionToRender.getStart(p));
			double e = viewer.getLocationMapper().bpToPixel(context.reference,context.regionToRender.getEnd(p));
			
			double l = Math.abs(e-s);
			
			
			tile.setRect(bounds.getX()+s, tile.getY(), e-s, tile.getHeight());
			PaintUtils.normalize(tile);
			context.g2.setPaint(background);
			context.g2.fill(tile);
			
			context.g2.setPaint(foreground);
			
			double size = context.g2.getFontMetrics().getStringBounds(context.regionToRender.getEnd()+"", context.g2).getWidth();
			
			if (size>Math.abs(e-s)) // no space to draw even a single position!
				continue;
			
			char un = '\0';
			size*=1.3;
			if (size>Math.abs(e-s)) { // no space to draw more than one
//				paintTick(context.g2, tile, tile.getCenterX(),(context.regionToRender.getStart(p)+context.regionToRender.getEnd(p))/2,un);
			} else {
				size/=l;
				size*=context.regionToRender.getEnd(p)-context.regionToRender.getStart(p);
				int show = PaintUtils.findNiceNumberGreater((int)Math.ceil(size));
				int start = (int)(Math.ceil((double)context.regionToRender.getStart(p)/show)*show);
				if (show>=500)
					un = 'k';
				if (show>=500_000)
					un = 'M';
				for (int loc=start; loc<context.regionToRender.getEnd(p); loc+=show)
					paintTick(context.g2, tile, bounds.getX()+viewer.getLocationMapper().bpToPixel(context.reference,loc), loc,un);
				drawn = true;
			}
		}
		if (!drawn) {
			// TODO system for disabling in viewer
		}
		context.g2.setFont(f);
		
		return context;
	}


	
	private void paintTick(Graphics2D g2, Rectangle2D rect, double x, int loc, char un) {
		g2.draw(new Line2D.Double(x,rect.getMaxY()-3,x,rect.getMaxY()));
		double dist = Math.min(rect.getMaxX()-x,x-rect.getMinX());
		Rectangle2D legend = new Rectangle2D.Double(
				x-dist,
				rect.getMinY(),
				2*dist,
				rect.getMaxY()-rect.getMinY()-3
				);
		
		String s;
		if (un=='k')
			s = correct(loc/1000.0)+"k";
		else if (un=='M')
			s = correct(loc/1000000.0)+"M";
		else
			s = correct(loc);
		
		PaintUtils.paintString(s, g2, legend,0 ,0);
	}

	private static String correct(double s) {
		
		int i = (int) s;
		String re = i+"";

		StringBuilder sb = new StringBuilder();
		sb.append(re.substring(0, re.length()%3));
		for (int d=re.length()%3; d<=re.length()-3; d+=3) {
			if (sb.length()>0) sb.append(",");
			sb.append(re.substring(d,d+3));
		}
		
		if (s>i) sb.append(((s-i)+"").substring(1));
		
		return sb.toString();
	}




	
}
