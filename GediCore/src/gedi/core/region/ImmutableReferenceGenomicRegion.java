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

import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.util.StringUtils;

public class ImmutableReferenceGenomicRegion<D> implements ReferenceGenomicRegion<D> {

	private ReferenceSequence reference;
	private GenomicRegion region;
	private D data;
	
	private transient int hashcode = -1;

	public ImmutableReferenceGenomicRegion(ReferenceSequence reference,
			GenomicRegion region) {
		this(reference,region,null);
	}
	
	public ImmutableReferenceGenomicRegion(ReferenceSequence reference,
			GenomicRegion region, D data) {
		this.reference = reference;
		this.region = region;
		this.data = data;
	}
	
	@Override
	public boolean isMutable() {
		return false;
	}

	public ReferenceSequence getReference() {
		return reference;
	}

	public GenomicRegion getRegion() {
		return region;
	}
	
	public D getData() {
		return data;
	}
	
	@Override
	public ImmutableReferenceGenomicRegion<D> toImmutable() {
		return this;
	}

	@Override
	public String toString() {
		return toString2();
	}
	
	@Override
	public int hashCode() {
		if (hashcode==-1)
			hashcode = hashCode2();
		return hashcode;
	}

	@Override
	public boolean equals(Object obj) {
		return equals2(obj);
	}

	public static <D> ImmutableReferenceGenomicRegion<D> parse(String pos) {
		return parse(pos,null);
	}
		
	public static <D> ImmutableReferenceGenomicRegion<D> parse(String pos, D data) {
		String[] p = StringUtils.split(pos,':');
		if (p.length!=2) throw new RuntimeException("Cannot parse as location: "+pos);
		return new ImmutableReferenceGenomicRegion<D>(Chromosome.obtain(p[0]), GenomicRegion.parse(p[1]),data);
	}

	
	public static <D> ImmutableReferenceGenomicRegion<D> parse(Genomic g, String pos) {
		return parse(g,pos,null);
	}
		
	public static <D> ImmutableReferenceGenomicRegion<D> parse(Genomic g, String pos, D data) {
		if (pos==null || !pos.contains(":")) {
			ReferenceGenomicRegion<?> rgr = g.getNameIndex().get(pos);
			if (rgr!=null) {
				rgr = rgr.toMutable().transformRegion(r->r.extendFront(1)).toImmutable();// workaround for IndexGenome bug
				pos = rgr.toLocationString();
			} else {
				rgr = g.getGeneMapping().apply(pos);
				if (rgr!=null) {
					pos = rgr.toLocationString();
				} else {
					rgr = g.getTranscriptMapping().apply(pos);
					if (rgr!=null) {
						pos = rgr.toLocationString();
					} else {
						ReferenceSequence ref = pos==null?g.getTranscripts().getReferenceSequences().iterator().next():Chromosome.obtain(pos);
						GenomicRegion reg = g.getTranscripts().getTree(ref).getRoot().getKey().removeIntrons();
						reg = reg.extendAll(reg.getTotalLength()/3, reg.getTotalLength()/3);
						pos = ref.toPlusMinusString()+":"+reg.toRegionString();
					}
				}
			}
		}
		return parse(pos,data);
	}
	
	
	
}
