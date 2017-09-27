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

public class JoinedFeaturePosition extends AbstractFeaturePosition {

	final static String prefix = "join(";
	
	
	private boolean exact = true;
	private GenbankFeaturePosition[] subPositions;
	
	public JoinedFeaturePosition(GenbankFeature feature, String descriptor) {
		super(feature, descriptor);
		
		if (!descriptor.startsWith(prefix) || !descriptor.endsWith(")"))
			throw new RuntimeException("does not match "+prefix+".*)!");
		
		String braces = descriptor.substring(prefix.length(),descriptor.length()-1);
		String[] subDescriptors = braces.split(",");
		
		subPositions = new GenbankFeaturePosition[subDescriptors.length];
		for (int i=0; i<subPositions.length; i++) {
			subPositions[i] = feature.parsePosition(subDescriptors[i]);
			exact &= subPositions[i].isExact(); 
		}
		
		leftMost = subPositions[0].getStartInSource();
		rightMost = subPositions[subPositions.length-1].getEndInSource();
	}
	
	@Override
	public String extractFeatureFromSource() throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<subPositions.length; i++)
			sb.append(subPositions[i].extractFeatureFromSource());
		return sb.toString();
	}
	
	@Override
	public String extractDownstreamFromSource(int numBases) throws IOException {
		return subPositions[subPositions.length-1].extractDownstreamFromSource(numBases);
	}

	@Override
	public String extractUpstreamFromSource(int numBases) throws IOException {
		return subPositions[0].extractUpstreamFromSource(numBases);
	}
	
	@Override
	public boolean isExact() {
		return exact;
	}

	public GenbankFeaturePosition[] getSubPositions() {
		return subPositions;
	}
	
	@Override
	public GenomicRegion toGenomicRegion() {
		ArrayGenomicRegion re = new ArrayGenomicRegion();
		for (GenbankFeaturePosition s : subPositions)
			re = re.union(s.toGenomicRegion());
		
		return re;
	}

	@Override
	public Strand getStrand() {
		Strand re = subPositions[0].getStrand();
		for (GenbankFeaturePosition s : subPositions)
			if (s.getStrand()!=re) throw new RuntimeException("Illegal Genbank position: "+toString());
		return re;
	}
	
}
