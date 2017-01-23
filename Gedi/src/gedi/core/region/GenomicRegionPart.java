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
