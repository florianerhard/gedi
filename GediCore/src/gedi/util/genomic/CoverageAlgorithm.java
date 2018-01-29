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

package gedi.util.genomic;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.GeneralUtils;
import gedi.util.datastructure.array.NumericArray;

public class CoverageAlgorithm {

	private NumericArray empty;
	private NumericArray[] coverages;
	private ImmutableReferenceGenomicRegion<?> parentRegion;
	private boolean finished = false;
	
	
	public CoverageAlgorithm(CoverageAlgorithm a) {
		this.empty = a.empty;
		this.coverages = new NumericArray[a.coverages.length];
		for (int i=0; i<coverages.length; i++)
			this.coverages[i] = a.coverages[i]!=null?a.coverages[i].copy():null;
		this.parentRegion = a.parentRegion;
		this.finished = a.finished;
	}
	
	public CoverageAlgorithm(ReferenceSequence reference, GenomicRegion region) {
		this(new ImmutableReferenceGenomicRegion<>(reference, region));
	}
	
	public CoverageAlgorithm(ImmutableReferenceGenomicRegion<?> parentRegion) {
		coverages = new NumericArray[parentRegion.getRegion().getTotalLength()];
		this.parentRegion = parentRegion;
	}
	
	/**
	 * Invoke this if it is possible that no region at all is added!
	 * @param empty
	 * @return
	 */
	public CoverageAlgorithm setExample(NumericArray ex) {
		empty = NumericArray.createMemory(ex.length(), ex.getType());
		return this;
	}
	
	public CoverageAlgorithm add(ReferenceGenomicRegion<NumericArray> rgr) {
		if (!rgr.getReference().equals(parentRegion.getReference()))
			throw new RuntimeException("Wrong reference!");
		return add(rgr.getRegion(),rgr.getData());
	}
	
	public CoverageAlgorithm add(GenomicRegion region, NumericArray counts) {
		if (empty==null)
			empty = NumericArray.createMemory(counts.length(), counts.getType());
		
		if (finished) throw new RuntimeException("Do not mix add and get operations!");
		for (int p=0; p<region.getNumParts(); p++) {
			ArrayGenomicRegion inter = region.getPart(p).asRegion().intersect(parentRegion.getRegion());
			if (!inter.isEmpty()) {
				int s = parentRegion.induce(inter.getStart());
				int e = parentRegion.induce(inter.getStop());
				if (e<s) {
					int t = s; s=e; e=t;
				}
				e++;
				if (coverages[s]==null)
					coverages[s] = NumericArray.createMemory(counts.length(), counts.getType());
				coverages[s].add(counts);
				if (e<coverages.length) {
					if (coverages[e]==null)
						coverages[e] = NumericArray.createMemory(counts.length(), counts.getType());
					coverages[e].subtract(counts);
				}
			}
		}
//		if (!checkNegativity(0)) {
//			System.out.println(region);
//		}
		return this;
	}
	

	public ImmutableReferenceGenomicRegion<?> getParentRegion() {
		return parentRegion;
	}
	
	/**
	 * 
	 * @param pInParent 0 - parentRegion.getTotalLength()
	 * @return
	 */
	public NumericArray getCoverages(int pInParent) {
		if (!finished) {
			int n = parentRegion.getRegion().getTotalLength();
			NumericArray last = null;
			for (int i=0; i<n; i++) {
				
				if (coverages[i]!=null) {
					if (last!=null) {
						coverages[i].add(last);
					} else {
						// everythings allright!
					}
				}
				else {
					if (last!=null)
						coverages[i] = last;
					else
						coverages[i] = empty;
				}
				if (coverages[i]!=null)
					last=coverages[i];
			}
			finished = true;
			
		}
		return coverages[pInParent];
	}
	
	public NumericArray getProfile(int condition) {
		getCoverages(0);
		NumericArray re = NumericArray.createMemory(coverages.length, empty.getType());
		for (int i=0; i<re.length(); i++)
			re.copy(coverages[i], condition, i);
		return re;
	}
	
//	public boolean checkNegativity(int condition) {
//		CoverageAlgorithm a = new CoverageAlgorithm(this);
//		NumericArray p = a.getProfile(condition);
//		for (int i=0; i<p.length(); i++)
//			if (p.getDouble(i)<0) {
//				System.out.println(p);
//				return false;
//			}
//		return true;
//	}

	
}
