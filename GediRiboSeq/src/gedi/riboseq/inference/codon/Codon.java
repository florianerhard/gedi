package gedi.riboseq.inference.codon;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.riboseq.inference.orf.OrfWithCodons;
import gedi.util.ArrayUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.mutable.MutableInteger;

import java.util.HashSet;
import java.util.Locale;

public class Codon extends ArrayGenomicRegion {
	private String seq;
	public double[] activity;
	public double totalActivity = 0;
	double goodness = 0;

	
	public boolean isFaux() {
		return getStart()<0;
	}
	
	
	public Codon(GenomicRegion region, double total) {
		super(region);
		totalActivity = total;
	}
	
	
	public Codon(GenomicRegion region, CharSequence seq) {
		super(region);
		this.seq = seq!=null?seq.toString():null;
	}
	
	
	public Codon(GenomicRegion region, double[] activity, double totalActivity, double goodness, CharSequence seq) {
		super(region);
		this.totalActivity = totalActivity;
		this.activity = activity;
		this.goodness = goodness;
		this.seq = seq!=null?seq.toString():null;
	}
	
	public Codon(GenomicRegion region, Codon data) {
		super(region);
		this.totalActivity = data.totalActivity;
		this.activity = data.activity;
		this.goodness = data.goodness;
		this.seq = data.seq;
	}
	
	public double getTotalActivity() {
		return totalActivity;
	}
	
	public double[] getActivity() {
		return activity;
	}
	
	public double getGoodness() {
		return goodness;
	}
	
	public String getSeq() {
		return seq;
	}
	
	public boolean isStop() {
		return seq.equals("TAG") || seq.equals("TAA") || seq.equals("TGA");
	}
	
	public boolean isStart() {
		return seq.equals("ATG");
	}
	
	public CodonType getType() {
		if (isStart()) return CodonType.Start;
		if (isStop()) return CodonType.Stop;
		if (isNearStart()) return CodonType.NoncanonicalStart;
		return CodonType.Any;
	}
	
	/**
	 * Not true for ATG!
	 * @return
	 */
	public boolean isNearStart() {
		return StringUtils.hamming("ATG", seq)==1;
	}
	
	public char getAminoAcid() {
		return SequenceUtils.translate(seq).charAt(0);
	}
	
	@Override
	public String toString() {
		return toRegionString()+":"+String.format(Locale.US, "%.3g",totalActivity);
	}

	public Codon createProportionalUnique(OrfWithCodons ref, HashSet<OrfWithCodons> orfs) {
		double fac = ref.getEstimatedActivityFraction()/EI.wrap(orfs).mapToDouble(o->o.getEstimatedActivityFraction()).sum();
		double[] a = activity.clone();
		ArrayUtils.mult(a, fac);
		return new Codon(this,a,totalActivity*fac,goodness,seq);
	}
	
	public Codon createProportionalEach(OrfWithCodons ref, HashSet<OrfWithCodons> orfs) {
		double[] a = activity.clone();
		for (MutableInteger i=new MutableInteger(); i.N<a.length; i.N++)
			a[i.N] *= ref.getEstimatedActivityFraction(i.N)/EI.wrap(orfs).mapToDouble(o->o.getEstimatedActivityFraction(i.N)).sum();
		double tot = ArrayUtils.sum(a);
		return new Codon(this,a,tot,goodness,seq);
	}
	
}