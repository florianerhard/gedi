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

package gedi.core.region;

import gedi.util.datastructure.tree.redblacktree.Interval;

public class GenomicRegionPart implements Interval {
	
	private int partIndex;
	private GenomicRegion genomicRegion;

	
	public GenomicRegionPart(int partIndex, GenomicRegion genomicRegion) {
		this.partIndex = partIndex;
		this.genomicRegion = genomicRegion;
	}
	public int getPartIndex() {
		return partIndex;
	}
	public GenomicRegion getGenomicRegion() {
		return genomicRegion;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((genomicRegion == null) ? 0 : genomicRegion.hashCode());
		result = prime * result + partIndex;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenomicRegionPart other = (GenomicRegionPart) obj;
		if (genomicRegion == null) {
			if (other.genomicRegion != null)
				return false;
		} else if (!genomicRegion.equals(other.genomicRegion))
			return false;
		if (partIndex != other.partIndex)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DefaultGenomicRegionPart [partIndex=" + partIndex
				+ ", genomicRegion=" + genomicRegion + "]";
	}
	@Override
	public int getStart() {
		return getGenomicRegion().getStart(getPartIndex());
	}
	@Override
	public int getStop() {
		return getGenomicRegion().getEnd(getPartIndex())-1;
	}
	
	
}
