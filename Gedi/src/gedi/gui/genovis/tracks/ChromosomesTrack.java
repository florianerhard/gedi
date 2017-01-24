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
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.gui.genovis.VisualizationTrackAdapter;
import gedi.gui.genovis.VisualizationTrackPickInfo;
import gedi.util.PaintUtils;
import gedi.util.gui.PixelBasepairMapper;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.List;


@GenomicRegionDataMapping(fromType=Void.class)
public class ChromosomesTrack extends VisualizationTrackAdapter<Void,ReferenceSequence> {

	private static final double DEFAULT_HEIGHT = 15;
	private HashSet<String> even;
	
	private String suffix = null;
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

	public ChromosomesTrack(String suffix) {
		this();
		setSuffix(suffix);
	}
	
	public ChromosomesTrack() {
		super(Void.class);
		this.minPixelPerBasePair=0;
		this.maxPixelPerBasePair=Double.POSITIVE_INFINITY;
		this.minHeight = this.prefHeight = this.maxHeight = styles.get("height").asDouble(DEFAULT_HEIGHT);
	}
	
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
	
	@Override
	public void pick(VisualizationTrackPickInfo<ReferenceSequence> info) {
		info.setData(info.getReference());
	}
	
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<Void> renderLabel(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<Void> context) {
		return context;
	}
	
	@Override
	protected TrackRenderContext<Void> renderTrack(
			TrackRenderContext<Void> context) {
		
		if (even==null) {
			even = new HashSet<String>();
			List<String> refs = viewer.getGenome().getReferenceSequences();
			for (int i=0; i<refs.size(); i+=2)
				even.add(refs.get(i));
		}
		
		Font f = context.g2.getFont();
		if (this.font!=null) context.g2.setFont(this.font);
		
		PixelBasepairMapper locationMapper = viewer.getLocationMapper();
		
		double h = styles.get("height").asDouble(DEFAULT_HEIGHT);
		

		double y = getBounds().getY();
		double y1 = y;
		double y2 = y1+h-1;
		boolean rtl = locationMapper.is5to3() && context.reference.getStrand()==Strand.Minus;
		
		Color bg  = PaintUtils.parseColor(even.contains(context.reference.getName())?styles.get("background-even").asString("#444444"):styles.get("background-even").asString("#CCCCCC"));
		Color fg = PaintUtils.isDarkColor(bg)?Color.white:Color.black;
		String label = context.reference.toString();
		if (suffix!=null)
			label = label+" "+suffix;
		
		// paint boxes
		double x1 = rtl?locationMapper.bpToPixel(context.reference,context.regionToRender.getEnd()):locationMapper.bpToPixel(context.reference,context.regionToRender.getStart());
		double x2 = rtl?locationMapper.bpToPixel(context.reference,context.regionToRender.getStart()):locationMapper.bpToPixel(context.reference,context.regionToRender.getEnd());
		Rectangle2D.Double tile = new Rectangle2D.Double(getBounds().getX()+x1,y1,x2-x1,y2-y1);
		
		
		context.g2.setPaint(bg);
		context.g2.fill(tile);
		context.g2.draw(tile);
		
		
		context.g2.setPaint(fg);
		if (PaintUtils.getFitStringScale(label, context.g2, tile)*context.g2.getFont().getSize()>=3) {
			PaintUtils.paintString(label, context.g2, tile, 0, 0);
		}
			
		context.g2.setFont(f);
		return context;
	}

	
}
