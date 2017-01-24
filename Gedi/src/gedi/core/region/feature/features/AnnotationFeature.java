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

import gedi.core.genomic.Genomic;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.ReferenceSequenceConversion;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;


@GenomicRegionFeatureDescription(toType=Object.class)
public class AnnotationFeature<T> extends AbstractFeature<Object> {

	private ArrayList<GenomicRegionStorage<T>> storages = new ArrayList<GenomicRegionStorage<T>>();
	private ReferenceSequenceConversion referenceSequenceConversion = ReferenceSequenceConversion.none;
	
	private boolean toMemory = true;
	private BiPredicate<MutableReferenceGenomicRegion<T>,MutableReferenceGenomicRegion<Object>> checker = (r,referenceRegion)->r.getRegion().intersects(referenceRegion.getRegion());
	private boolean dataonly;
	
	public AnnotationFeature() {
		this(true);
	}
	
	@Override
	public GenomicRegionFeature<Object> copy() {
		AnnotationFeature<T> re = new AnnotationFeature<T>();
		re.copyProperties(this);
		re.storages = storages;
		re.referenceSequenceConversion = referenceSequenceConversion;
		re.toMemory = toMemory;
		re.checker = checker;
		re.dataonly = dataonly;
		return re;
	}
	
	public AnnotationFeature(boolean dataonly) {
		minValues = 0;
		maxValues = Integer.MAX_VALUE;
		minInputs = maxInputs = 0;
		this.dataonly = dataonly;
	}
	
	public void add(GenomicRegionStorage<T> storage) {
		if (toMemory)
			storage = storage.toMemory();
		this.storages.add(storage);
	}
	
	public void addTranscripts(Genomic genomic) {
		this.storages.add((GenomicRegionStorage<T>) genomic.getTranscripts());
	}
	
	
	public void setReferenceSequenceConversion(
			ReferenceSequenceConversion referenceSequenceConversion) {
		this.referenceSequenceConversion = referenceSequenceConversion;
	}
	
	public void setToMemory(boolean toMemory) {
		this.toMemory = toMemory;
	}
	
	public boolean isToMemory() {
		return this.toMemory;
	}
	
	public void convertAnnotation(UnaryOperator<MemoryIntervalTreeStorage<?>> converter) {
		for (int i=0; i<storages.size(); i++) 
			storages.set(i, (GenomicRegionStorage)converter.apply(storages.get(i).toMemory()));
	}
	
	
	public void setExact(int tolerance5p, int tolerance3p) {
		
		checker = (r,referenceRegion)->{
			if (!r.getRegion().isIntronConsistent(referenceRegion.getRegion())) return false;
			if (Math.abs(GenomicRegionPosition.FivePrime.position(r)-GenomicRegionPosition.FivePrime.position(referenceRegion))>tolerance5p) return false;
			if (Math.abs(GenomicRegionPosition.ThreePrime.position(r)-GenomicRegionPosition.ThreePrime.position(referenceRegion))>tolerance3p) return false;
			return true;
		};
	}
	
	public void setContainsPosition(GenomicRegionPosition position) {
		checker = (r,referenceRegion)->r.getRegion().contains(position.position(referenceRegion.getReference(),referenceRegion.getRegion()));
	}
	public void setContainsPosition(GenomicRegionPosition position, int offset) {
		checker = (r,referenceRegion)->r.getRegion().contains(position.position(referenceRegion.getReference(),referenceRegion.getRegion(),offset));
	}
	
	@Override
	public Iterator<?> getUniverse() {
		ExtendedIterator<ImmutableReferenceGenomicRegion<T>> it = null;
		for (GenomicRegionStorage<T> storage : storages) {
			ExtendedIterator<ImmutableReferenceGenomicRegion<T>> tit = EI.wrap(storage.iterateReferenceGenomicRegions());
			if (it==null) it = tit;
			else it = it.chain(tit);
		}
		if (dataonly)
			return it.map(rgr->rgr.getData());
			
		return it;
	}

	@Override
	protected void accept_internal(Set<Object> values) {
		ReferenceSequence reference = referenceSequenceConversion.apply(this.referenceRegion.getReference());
		
		for (GenomicRegionStorage<T> storage : storages) 
			storage.iterateIntersectingMutableReferenceGenomicRegions(reference, referenceRegion.getRegion()).forEachRemaining(rgr->{
				if (checker.test(rgr,referenceRegion))
					values.add(dataonly?rgr.getData():new MutableReferenceGenomicRegion().set(rgr));
			});
		
		if (reference.getStrand()==Strand.Independent)
			for (GenomicRegionStorage<T> storage : storages) {
				storage.iterateIntersectingMutableReferenceGenomicRegions(reference.toPlusStrand(), referenceRegion.getRegion()).forEachRemaining(rgr->{
					if (checker.test(rgr,referenceRegion))
						values.add(dataonly?rgr.getData():new MutableReferenceGenomicRegion().set(rgr));
				});
				storage.iterateIntersectingMutableReferenceGenomicRegions(reference.toMinusStrand(), referenceRegion.getRegion()).forEachRemaining(rgr->{
					if (checker.test(rgr,referenceRegion))
						values.add(dataonly?rgr.getData():new MutableReferenceGenomicRegion().set(rgr));
				});
			}
	}


	public void addPositionData(GenomicRegionPosition readPosition, int readOffset, GenomicRegionPosition annotationPosition, int annotationOffset, int upstream, int downstream) {
		addFunction(d->{
			ReferenceGenomicRegion<?> rgr = (ReferenceGenomicRegion<?>)d;
			
			if (!readPosition.isValidInput(referenceRegion) || !annotationPosition.isValidInput(rgr))
				return null;
			
			int r = readPosition.position(referenceRegion,readOffset);
			int a = annotationPosition.position(rgr,annotationOffset);
			
			if (!rgr.getRegion().contains(r) || !rgr.getRegion().contains(a))
				return null;
			
			r = rgr.induceMaybeOutside(r);
			a = rgr.induceMaybeOutside(a);
			
			int p = r-a;
			if (p<0 && -p>upstream)
				return "U";
			if (p>0 && p>downstream)
				return "D";
			
//			if ((p<0 && -p<=upstream) || (p>=0 && p<=downstream))
				return p+"";
//			return null;
		});
	}
	
}
