package gedi.util.io.text.genbank;

import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;

import java.io.IOException;

public interface GenbankFeaturePosition {
	
	public abstract String getDescriptor();
	
	public abstract String extractFeatureFromSource() throws IOException;
	/**
	 * In 5' utr
	 * @param numBases
	 * @return
	 * @throws IOException
	 */
	public abstract String extractUpstreamFromSource(int numBases) throws IOException;
	/**
	 * In 3' utr
	 * @param numBases
	 * @return
	 * @throws IOException
	 */
	public abstract String extractDownstreamFromSource(int numBases) throws IOException;
	
	public abstract GenbankFeature getFeature();
	
	public abstract boolean isExact();
	
	/**
	 * Inclusive, zero based
	 * @return
	 */
	public abstract int getStartInSource();
	
	/**
	 * Exclusive, zero based
	 * @return
	 */
	public abstract int getEndInSource();
	public abstract GenbankFeaturePosition[] getSubPositions();
	
	/**
	 * Disregards strand and converts to java coordinate system.
	 */
	public abstract GenomicRegion toGenomicRegion();
	
	public abstract Strand getStrand();
	
}
