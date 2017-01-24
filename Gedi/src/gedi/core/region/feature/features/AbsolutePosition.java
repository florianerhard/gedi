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

package gedi.core.region.feature.features;

import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.Set;


@GenomicRegionFeatureDescription(fromType=ReferenceGenomicRegion.class,toType=Integer.class)
public class AbsolutePosition extends AbstractFeature<Integer> {

	
	
	protected GenomicRegionPosition readPosition = GenomicRegionPosition.FivePrime;
	protected int readOffset = 0;
	protected GenomicRegionPosition annotationPosition = GenomicRegionPosition.FivePrime;
	protected int annotationOffset = 0;
	protected int upstream = 50;
	protected int downstream = 50;
	
	protected boolean reportFurtherUpstream = true;
	protected boolean reportFurtherDownstream = true;
	
	
	
	public AbsolutePosition() {
		minInputs = maxInputs = 1;
	}
	
	@Override
	public GenomicRegionFeature<Integer> copy() {
		AbsolutePosition re = new AbsolutePosition();
		re.copyProperties(this);
		re.readPosition = readPosition;
		re.readOffset = readOffset;
		re.annotationOffset = annotationOffset;
		re.annotationPosition = annotationPosition;
		re.upstream = upstream;
		re.downstream = downstream;
		re.reportFurtherDownstream = reportFurtherDownstream;
		re.reportFurtherUpstream = reportFurtherUpstream;
		return re;
	}

	public void setReadOffset(int readOffset) {
		this.readOffset = readOffset;
	}
	
	public void setReadPosition(GenomicRegionPosition readPosition) {
		this.readPosition = readPosition;
	}
	
	public void setAnnotationOffset(int annotationOffset) {
		this.annotationOffset = annotationOffset;
	}
	
	public void setAnnotationPosition(GenomicRegionPosition annotationPosition) {
		this.annotationPosition = annotationPosition;
	}
	
	public void all() {
		this.upstream = Integer.MAX_VALUE;
		this.downstream = Integer.MAX_VALUE;
	}
	
	public void setUpstream(int upstream) {
		this.upstream = upstream;
	}
	
	public void setDownstream(int downstream) {
		this.downstream = downstream;
	}
	
	public void setReportFurtherDownstream(boolean reportFurtherDownstream) {
		this.reportFurtherDownstream = reportFurtherDownstream;
	}
	
	public void setReportFurtherUpstream(boolean reportFurtherUpstream) {
		this.reportFurtherUpstream = reportFurtherUpstream;
	}
	
	public void setReportOutside(boolean report) {
		this.reportFurtherDownstream = this.reportFurtherUpstream = report;
	}

	@Override
	protected void accept_internal(Set<Integer> values) {
	
		Set<ReferenceGenomicRegion<?>> inputs = getInput(0);
		
		for (ReferenceGenomicRegion<?> rgr : inputs) {
		
			if (!readPosition.isValidInput(referenceRegion) || !annotationPosition.isValidInput(rgr))
				continue;
			
		
			
			int r = readPosition.position(referenceRegion,readOffset);
			int a = annotationPosition.position(rgr,annotationOffset);
			
			if (rgr.getRegion().isIntronic(r) || rgr.getRegion().isIntronic(a))
				continue;
			
			r = rgr.induceMaybeOutside(r);
			a = rgr.induceMaybeOutside(a);
			
			
			int p = r-a;
			if (p<0 && -p>upstream) {
				if (reportFurtherUpstream)
					values.add(-upstream-1);
			}
			else if (p>0 && p>downstream) {
				if (reportFurtherDownstream)
					values.add(downstream+1);
			}
			else {
				values.add(p);
			}
			
		}
		
	}
	
	public void setFrame() {
		readPosition = GenomicRegionPosition.FivePrime;
		annotationPosition = GenomicRegionPosition.StartCodon;
		all();
		addFunction(p->(p%3+3)%3);
	}

}
