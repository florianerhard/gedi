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

package gedi.core.region;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.util.StringUtils;

import java.util.function.UnaryOperator;



/**
 * Contracts:
 * compareTo does not consider the data
 * 
 * equals and hashcode do!
 * 
 * @author erhard
 *
 * @param <D>
 */
public interface ReferenceGenomicRegion<D> extends Comparable<ReferenceGenomicRegion<D>>  {

	ReferenceSequence getReference();
	GenomicRegion getRegion();
	D getData();
	
	default MutableReferenceGenomicRegion<D> toMutable() {
		return new MutableReferenceGenomicRegion<D>().set(getReference(), getRegion(), getData());
	}
	
	default ImmutableReferenceGenomicRegion<D> toImmutable() {
		return new ImmutableReferenceGenomicRegion<D>(getReference(), getRegion(), getData());
	}

	default ImmutableReferenceGenomicRegion<D> toImmutable(UnaryOperator<D> dataToImmutable) {
		return new ImmutableReferenceGenomicRegion<D>(getReference(), getRegion(), dataToImmutable.apply(getData()));
	}
	
	default int compareTo(ReferenceGenomicRegion<D> o) {
		int re = getReference().compareTo(o.getReference());
		if (re==0)
			re = getRegion().compareTo(o.getRegion());
		return re;
	}
	
	default boolean intersects(ReferenceGenomicRegion<?> o) {
		return getReference().equals(o.getReference()) && getRegion().intersects(o.getRegion());
	}
	
	default boolean contains(ReferenceGenomicRegion<?> o) {
		return getReference().equals(o.getReference()) && getRegion().contains(o.getRegion());
	}
	
	default int map(int position) {
		return getReference().getStrand()==Strand.Minus?getRegion().map(getRegion().getTotalLength()-1-position):getRegion().map(position);
	}
	default GenomicRegion map(GenomicRegion region) {
		return getReference().getStrand()==Strand.Minus?getRegion().map(region.reverse(getRegion().getTotalLength())):getRegion().map(region);
	}
	
	default <T> ImmutableReferenceGenomicRegion<T> map(ReferenceGenomicRegion<T> region) {
		ReferenceSequence ref = getReference();
		if (region.getReference().getStrand()==Strand.Minus) ref = ref.toOppositeStrand();
		else if (region.getReference().getStrand()==Strand.Independent) ref = ref.toStrandIndependent();
		return new ImmutableReferenceGenomicRegion<T>(ref, map(region.getRegion()),region.getData());
	}
	
	default <T> ImmutableReferenceGenomicRegion<T> induce(ReferenceGenomicRegion<T> region, String referenceName) {
		if (!getReference().getName().equals(region.getReference().getName()))
			throw new IllegalArgumentException("Cannot induce a region from another reference!");
		
		ReferenceSequence ref = Chromosome.obtain(referenceName, Strand.Plus);
		if (region.getReference().getStrand()!=getReference().getStrand()) ref = ref.toOppositeStrand();
		else if (region.getReference().getStrand()==Strand.Independent) ref = ref.toStrandIndependent();
		return new ImmutableReferenceGenomicRegion<T>(ref, induce(region.getRegion()),region.getData());
	}
	
	
	default int induce(int position) {
		return getReference().getStrand()==Strand.Minus?getRegion().getTotalLength()-1-getRegion().induce(position):getRegion().induce(position);
	}
	default int induceMaybeOutside(int position) {
		return getReference().getStrand()==Strand.Minus?getRegion().getTotalLength()-1-getRegion().induceMaybeOutside(position):getRegion().induceMaybeOutside(position);
	}
	default ArrayGenomicRegion induce(GenomicRegion region) {
		return getReference().getStrand()==Strand.Minus?getRegion().induce(region).reverse(getRegion().getTotalLength()):getRegion().induce(region);
	}
	
	default String toLocationString() {
		return getReference().toPlusMinusString()+":"+getRegion().toRegionString();
	}
	
	default String toLocationStringRemovedIntrons() {
		return getReference().toPlusMinusString()+":"+getRegion().removeIntrons().toRegionString();
	}
	
	/**
	 * If the 5' end of region is downstream of the 5' end of this.
	 * @param region
	 * @return
	 */
	default boolean isDownstream(GenomicRegion region) {
		if (getReference().getStrand()==Strand.Minus) {
			return getRegion().getEnd()>region.getEnd();
		}
		return getRegion().getStart()<region.getStart();
	}
	
	/**
	 * If the 5' end of region is upstream of the 5' end of this.
	 * @param region
	 * @return
	 */
	default boolean isUpstream(GenomicRegion region) {
		if (getReference().getStrand()==Strand.Minus) {
			return getRegion().getEnd()<region.getEnd();
		}
		return getRegion().getStart()>region.getStart();
	}
	
	
	default int hashCode2() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getData() == null) ? 0 : getData().hashCode());
		result = prime * result
				+ ((getReference() == null) ? 0 : getReference().hashCode());
		result = prime * result + ((getRegion() == null) ? 0 : getRegion().hashCode());
		return result;
	}

	default boolean equals2(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ReferenceGenomicRegion))
			return false;
		ReferenceGenomicRegion<D> other = (ReferenceGenomicRegion<D>) obj;
		if (getData() == null) {
			if (other.getData() != null)
				return false;
		} else if (!getData().equals(other.getData()))
			return false;
		if (getReference() == null) {
			if (other.getReference() != null)
				return false;
		} else if (!getReference().equals(other.getReference()))
			return false;
		if (getRegion() == null) {
			if (other.getRegion() != null)
				return false;
		} else if (!getRegion().equals(other.getRegion()))
			return false;
		return true;
	}

	default String toString2() {
		return getData()==null?getReference()+":"+getRegion():getReference()+":"+getRegion()+" "+StringUtils.toString(getData());
	}
	

}
