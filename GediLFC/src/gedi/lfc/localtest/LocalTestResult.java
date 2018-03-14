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
package gedi.lfc.localtest;

import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.genomic.CoverageAlgorithm;
import gedi.util.mutable.MutableDouble;

public class LocalTestResult {


	private ImmutableReferenceGenomicRegion<String> gene;
	private double pval;
	
	private MemoryIntervalTreeStorage<MutableDouble> localPvalues;

	public LocalTestResult(ImmutableReferenceGenomicRegion<String> gene, double pval,
			MemoryIntervalTreeStorage<MutableDouble> localPvalues) {
		this.gene = gene;
		this.pval = pval;
		this.localPvalues = localPvalues;
	}

	
	public ImmutableReferenceGenomicRegion<String> getGene() {
		return gene;
	}
	
	
	public double getPvalue() {
		return pval;
	}
	
	public MemoryIntervalTreeStorage<MutableDouble> getLocalPvalues() {
		return localPvalues;
	}
	
}
