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
package gedi.gui.genovis.tracks.boxrenderer;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.SequenceUtils;
import gedi.util.gui.PixelBasepairMapper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

public class AlignedReadsDataBoxRenderer extends BoxRenderer<AlignedReadsData> {

	private double factor = 1;
	private int[] conditions;
	
	private ReadCountMode readCountMode = ReadCountMode.Weight;
	
	public AlignedReadsDataBoxRenderer() {
		setBorder();
		setBackground(r->Color.BLACK);
	}
	
	public void setConditions(int[] conditions) {
		this.conditions = conditions;
	}
	
	public void setFactor(double factor) {
		this.factor = factor;
	}
	
	public void setReadCountMode(ReadCountMode readCountMode) {
		this.readCountMode = readCountMode;
	}
	
	public ReadCountMode getReadCountMode() {
		return readCountMode;
	}

	@Override
	public GenomicRegion renderBox(Graphics2D g2,
			PixelBasepairMapper locationMapper, ReferenceSequence reference, Strand strand, GenomicRegion region,
			AlignedReadsData data, double xOffset, double y, double h) {
		

		GenomicRegion re;
		
		if (data.hasGeometry()) {
			
			GenomicRegion r1 = data.extractRead1(new ImmutableReferenceGenomicRegion<>(reference.toStrand(strand), region), 0).getRegion();
			GenomicRegion r2 = data.extractRead2(new ImmutableReferenceGenomicRegion<>(reference.toStrand(strand), region), 0).getRegion();
			
			setBackground(r->Color.GRAY);
			re = super.renderBox(g2, locationMapper, reference, strand, r1.union(r2), data, xOffset, y, h);
			setBackground(r->Color.BLACK);
			super.renderBox(g2, locationMapper, reference, strand, r1.intersect(r2), data, xOffset, y, h);
			
			
		} else
			re = super.renderBox(g2, locationMapper, reference, strand, region, data, xOffset, y, h);
		
		for (int d=0; d<data.getDistinctSequences(); d++) {
			double th = 0;
			
			
			int[] totals = data.getCountsForDistinctFloor(d, readCountMode);//data.getTotalCountForDistinctSequence(d);
			
			if (conditions==null)
				th = ArrayUtils.sum(totals);
			else
				for (int c : conditions)
					th+=totals[c];
			
			th*=factor;
			
			if (th>0 && strand!=Strand.Independent) {
				for (int v=0; v<data.getVariationCount(d); v++) {
					if(data.isMismatch(d, v)) {
						double s = locationMapper.bpToPixel(reference,data.positionToGenomic(data.getMismatchPos(d, v),reference.toStrand(strand),region));
						double e = locationMapper.bpToPixel(reference,data.positionToGenomic(data.getMismatchPos(d, v),reference.toStrand(strand),region)+1);
						
						g2.setPaint(SequenceUtils.getNucleotideColorizer().apply(data.getMismatchRead(d, v).charAt(0)));
						Shape tile = new Rectangle2D.Double(xOffset+s, y, e-s, th-1);
						
						if (data.hasGeometry() && data.isPositionInOverlap(d, data.getMismatchPos(d, v))) {
							if (data.isVariationFromSecondRead(d, v))
								tile = getLowerTriangle((Rectangle2D)tile);
							else
								tile = getUpperTriangle((Rectangle2D)tile);
						}
						g2.draw(tile);
						g2.fill(tile);
					}
				}
				y+=th;
			}
		}
		
		return re;
	}

	private Shape getUpperTriangle(Rectangle2D tile) {
		GeneralPath re = new GeneralPath();
		re.moveTo(tile.getX(), tile.getY());
		re.lineTo(tile.getMaxX(), tile.getY());
		re.lineTo(tile.getX(), tile.getMaxY());
		re.closePath();
		return re;
	}

	private Shape getLowerTriangle(Rectangle2D tile) {
		GeneralPath re = new GeneralPath();
		re.moveTo(tile.getMaxX(), tile.getY());
		re.lineTo(tile.getMaxX(), tile.getMaxY());
		re.lineTo(tile.getX(), tile.getMaxY());
		re.closePath();
		return re;
	}

	@Override
	public double prefHeight(ReferenceSequence ref, GenomicRegion reg, AlignedReadsData d) {
		int re = 0;
		if (conditions==null)
			for (int i=0; i<d.getNumConditions(); i++)
				re+=d.getTotalCountForConditionFloor(i, readCountMode);
		else 
			for (int i : conditions)
				re+=d.getTotalCountForConditionFloor(i, readCountMode);
		return re*factor;
	}

}
