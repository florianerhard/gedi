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
