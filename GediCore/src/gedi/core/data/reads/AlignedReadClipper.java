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

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;

/**
 * Not for paired end reads!
 * @author erhard
 *
 */
public class AlignedReadClipper implements UnaryOperator<ReferenceGenomicRegion<AlignedReadsData>> {

	private ArrayGenomicRegion clip;
	
	public AlignedReadClipper(int start, int end) {
		clip = new ArrayGenomicRegion(start,end);
	}
	
	
	@Override
	public ReferenceGenomicRegion<AlignedReadsData> apply(
			ReferenceGenomicRegion<AlignedReadsData> t) {
		
		MutableReferenceGenomicRegion<AlignedReadsData> mut = t.toMutable();
		mut.setRegion(t.getRegion().intersect(t.map(clip.intersect(new ArrayGenomicRegion(0,t.getRegion().getTotalLength())))));
		
		AlignedReadsDataFactory fac = new AlignedReadsDataFactory(mut.getData().getNumConditions()).start();
		for (int d=0; d<mut.getData().getDistinctSequences(); d++)
			fac.add(mut.getData(), d, this::transformVariation);
		
		fac.makeDistinct();
		mut.setData(fac.create());
		checkCount(t.getData(),mut.getData());
		
		return t.isMutable()?mut:mut.toImmutable();
	}
	
	public AlignedReadsVariation transformVariation(AlignedReadsVariation var) {
		if (var.getPosition()<clip.getStart() || var.getPosition()>=clip.getEnd())
			return null;
		
		if (var.isMismatch()) {
			if (clip.getStart()>0)
				return new AlignedReadsMismatch(var.getPosition()-clip.getStart(), var.getReferenceSequence(), var.getReadSequence(), var.isFromSecondRead());
			return var;
		}
		else if(var.isDeletion()) {
			return var;
		}
		else if(var.isInsertion()) {
			return var; // that is not exactly right, but does not matter that much (had to adapt the region otherwise!)
		}
		else if (var.isSoftclip()) {
			if (var.getPosition()!=0 || (var.getPosition()==0 && clip.getStart()>0)) return null;
			return var;
		}
		
		return var;
	}


	private void checkCount(AlignedReadsData a, AlignedReadsData b) {
		if (a.getTotalCountOverallInt(ReadCountMode.All)!=b.getTotalCountOverall(ReadCountMode.All))
			throw new RuntimeException();
	}


}
