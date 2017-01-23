package gedi.util.io.text.genbank;

import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;

import java.io.FileNotFoundException;
import java.io.IOException;

public class SingleFeaturePosition extends AbstractFeaturePosition {

	private int position;
	
	public SingleFeaturePosition(GenbankFeature feature, String descriptor) {
		super(feature, descriptor);
		position = Integer.parseInt(descriptor);
		leftMost = position-1;
		rightMost = position;
	}

	@Override
	public String extractFeatureFromSource() throws IOException {
		return getFeature().getFile().getSource(position-1,position);
	}
	
	@Override
	public boolean isExact() {
		return true;
	}

	@Override
	public String extractDownstreamFromSource(int numBases) throws IOException {
		return getFeature().getFile().getSource(position+1,position+1+numBases);
	}

	@Override
	public String extractUpstreamFromSource(int numBases) throws IOException {
		return getFeature().getFile().getSource(position-1-numBases,position-1);
	}
	
	@Override
	public GenbankFeaturePosition[] getSubPositions() {
		return new GenbankFeaturePosition[] {this};
	}
	
	@Override
	public GenomicRegion toGenomicRegion() {
		return new ArrayGenomicRegion(leftMost,rightMost);
	}

	@Override
	public Strand getStrand() {
		return Strand.Plus;
	}
	
}
