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
