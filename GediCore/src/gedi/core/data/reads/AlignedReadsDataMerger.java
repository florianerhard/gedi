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

package gedi.core.data.reads;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.ArrayUtils;

import java.util.Arrays;

import org.h2.util.IntIntHashMap;

import cern.colt.bitvector.BitVector;

public class AlignedReadsDataMerger {
	
	private int[] offsets;
	private AlignedReadsDataFactory fac;
	
	private IntIntHashMap[] idMapping;
	private int nextId = 1; // start with 1 as IntIntHashMap does not support 0->0
	
//	public AlignedReadsDataMerger(){
//	}
	
	public AlignedReadsDataMerger(int... numConditions) {
		
		fac = new AlignedReadsDataFactory(ArrayUtils.sum(numConditions));
		offsets = new int[numConditions.length+1];
		for (int i=1; i<offsets.length; i++) 
			offsets[i] = offsets[i-1]+numConditions[i-1];
		idMapping = new IntIntHashMap[numConditions.length];
		for (int i=0; i<idMapping.length; i++)
			idMapping[i] = new IntIntHashMap();
		
	}


	public ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> merge(ImmutableReferenceGenomicRegion<? extends AlignedReadsData>... data) {
		AlignedReadsData[] d2 = new AlignedReadsData[data.length];
		ReferenceSequence ref = null;
		GenomicRegion region = null;
		for (int i=0; i<d2.length; i++) {
			if (data[i]!=null) {
				d2[i] = data[i].getData();
				if (ref==null) ref = data[i].getReference();
				else if (!ref.equals(data[i].getReference())) throw new IllegalArgumentException("ReferenceRegions must be equal!");
				if (region==null) region = data[i].getRegion();
				else if (!region.equals(data[i].getRegion())) throw new IllegalArgumentException("ReferenceRegions must be equal!");
			}
		}
		return new ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>(ref,region,merge(d2));
	}

	public DefaultAlignedReadsData merge(AlignedReadsData... data) {
//		if (fac==null) {
//			fac = new AlignedReadsDataFactory(data.length);
//			offsets = new int[data.length+1];
//			for (int i=1; i<offsets.length; i++)
//				offsets[i] = i;
//		}
		fac.start();
		
		int max = 1;
		for (int i=0; i<data.length; i++)
			if (data[i]!=null)
				max = Math.max(max,data[i].getDistinctSequences());
		
		BitVector done = new BitVector(data.length*max);
		
		// do a linear search, as only few variations are expected!
		for (int i=0; i<data.length; i++)
			if (data[i]!=null) {
				for (int v=0; v<data[i].getDistinctSequences(); v++) {
					if (done.getQuick(i+v*data.length)) continue;
					fac.newDistinctSequence();
					fac.setId(getId(i,data[i].getId(v), data[i].getMultiplicity(v)>1));
					fac.setMultiplicity(data[i].getMultiplicity(v));
					fac.setWeight(data[i].getWeight(v));
					for (int c=0; c<data[i].getNumConditions(); c++)
						fac.setCount(offsets[i]+c, data[i].getCount(v, c));
					
					AlignedReadsVariation[] vars = data[i].getVariations(v);
					Arrays.sort(vars);
					
					for (AlignedReadsVariation vari : vars)
						fac.addVariation(vari);
					
					for (int j=i+1; j<data.length; j++) 
						if (data[j]!=null) {
							for (int w=0; w<data[j].getDistinctSequences(); w++) {
								if (done.getQuick(j+w*data.length)) continue;
								
								AlignedReadsVariation[] wars = data[j].getVariations(w);
								Arrays.sort(wars);
								
								if (Arrays.equals(vars, wars)) {
									for (int c=0; c<data[j].getNumConditions(); c++)
										fac.setCount(offsets[j]+c, data[j].getCount(w, c));
									done.putQuick(j+w*data.length,true);
									break;
								}
							}
						}
					
				}
			}
		
		DefaultAlignedReadsData re = fac.create();
//		System.out.println(Arrays.toString(data)+" -> "+re);
		return re;
	}


	private int getId(int file, int oldId, boolean save) {
		int id = idMapping[file].get(oldId);
		if (id==IntIntHashMap.NOT_FOUND) {
			if (save)
				idMapping[file].put(oldId, nextId);
			id = nextId++;
		}
		return id;
	}
	
}
