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

import java.io.IOException;

public class SpanFeaturePosition extends AbstractFeaturePosition {

	private boolean exact = true;
	
	public SpanFeaturePosition(GenbankFeature feature, String descriptor) {
		super(feature, descriptor);
		
		int dotsPos = descriptor.indexOf("..");
		leftMost = parsePos(descriptor.substring(0,dotsPos))-1;
		rightMost = parsePos(descriptor.substring(dotsPos+2));
	}
	
	private int parsePos(String pos) {
		if (pos.charAt(0)=='<' || pos.charAt(0)=='>') {
			exact = false;
			return Integer.parseInt(pos.substring(1));
		}
		else
			return Integer.parseInt(pos);
	}

	@Override
	public String extractFeatureFromSource() throws IOException {
		return getFeature().getFile().getSource(leftMost,rightMost);
	}
	
	@Override
	public String extractDownstreamFromSource(int numBases) throws IOException {
		return getFeature().getFile().getSource(rightMost,rightMost+numBases);
	}

	@Override
	public String extractUpstreamFromSource(int numBases) throws IOException {
		return getFeature().getFile().getSource(leftMost-numBases,leftMost);
	}
	
	@Override
	public boolean isExact() {
		return exact;
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
