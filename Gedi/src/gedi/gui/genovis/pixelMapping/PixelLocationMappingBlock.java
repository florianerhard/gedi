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

package gedi.gui.genovis.pixelMapping;

import gedi.core.reference.ReferenceSequence;
import gedi.util.gui.PixelBasepairMapper;
import gedi.util.gui.VisualizationLocationMapper;

import java.util.Comparator;

public class PixelLocationMappingBlock {
	private ReferenceSequence reference;
	private int startBp;
	private int stopBp;
	
	PixelLocationMappingBlock(){}
	void set(ReferenceSequence reference, int startBp, int stopBp) {
		this.reference = reference;
		this.startBp = startBp;
		this.stopBp = stopBp;
	}
	
	
	public int getStartBp() {
		return startBp;
	}
	
	public int getStopBp() {
		return stopBp;
	}
	
	public ReferenceSequence getReference() {
		return reference;
	}
	
	@Override
	public String toString() {
		return String.format("%s: %d-%d",reference.toString(),startBp,stopBp);
	}
	
	@Override
	public int hashCode() {
		int re = reference.hashCode();
		re=re*13+startBp;
		re=re*13+stopBp;
		return re;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		if (!(obj instanceof PixelLocationMappingBlock)) return false;
		PixelLocationMappingBlock o = (PixelLocationMappingBlock)obj;
		return reference.equals(o.reference) && startBp==o.startBp && stopBp==o.stopBp;
	}
	
//	public static Comparator<PixelLocationMappingBlock> pixelComparator = new Comparator<PixelLocationMappingBlock>() {
//		@Override
//		public int compare(PixelLocationMappingBlock o1,
//				PixelLocationMappingBlock o2) {
//			return o1.startPixel-o2.startPixel;
//		}
//	};

	public double getCenterPixel(PixelBasepairMapper locationMapper) {
		return (locationMapper.bpToPixel(reference,startBp+1)+locationMapper.bpToPixel(reference,stopBp))/2.0;
	}
	public double getStartPixel(PixelBasepairMapper locationMapper) {
		return locationMapper.bpToPixel(reference,startBp);
	}
	public double getStopPixel(PixelBasepairMapper locationMapper) {
		return locationMapper.bpToPixel(reference,stopBp+1);
	}
	public double getWidth(PixelBasepairMapper locationMapper) {
		return locationMapper.bpToPixel(reference,stopBp+1)-locationMapper.bpToPixel(reference,startBp);
	}
	public int compareToBp(int bp) {
		if (bp>=startBp && bp<=stopBp) return 0;
		if (bp<startBp) return 1;
		return -1;
	}
	public boolean containsBp(int bp) {
		return (bp>=startBp && bp<=stopBp);
	}
	public int getBasePairs() {
		return stopBp-startBp+1;
	}
	
	
}