package gedi.core.region;

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

	
	
}
