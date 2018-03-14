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
package gedi.riboseq.inference.orf;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.riboseq.inference.codon.Codon;
import gedi.util.ArrayUtils;
import gedi.util.functions.EI;

import java.util.ArrayList;
import java.util.Arrays;

public class OrfWithCodons {

	
	private int cluster;
	private int cc;
	private int index;
	private boolean hasStop;
	private GenomicRegion region;
	private ArrayList<Codon> codons;
	private ArrayList<Codon> estCodonsUnique;
	private ArrayList<Codon> estCodonsEach;
	
	private double activityUnique = 1;
	private double activityFraction = 1;
	private double[] activities;
	private double[] activitiesFractions;
	private double uniquePval = 1;
	

	public OrfWithCodons(int cluster, int cc, int index,
			ArrayGenomicRegion orfRegion, ArrayList<Codon> codons, boolean hasStop) {
		this.region = orfRegion;

		this.cluster = cluster;
		this.cc = cc;
		this.index = index;
		this.hasStop = hasStop;
		this.codons = this.estCodonsUnique = this.estCodonsEach = codons;
		
		activityUnique = EI.wrap(codons).mapToDouble(c->c.getTotalActivity()).sum();
		activities = new double[codons.get(0).getActivity().length];
		EI.wrap(codons).forEachRemaining(c->ArrayUtils.add(activities, c.getActivity()));
		activitiesFractions = new double[codons.get(0).getActivity().length];
		Arrays.fill(activitiesFractions, 1);
	}
	
	public void stitchWith(OrfWithCodons other, GenomicRegion inbetween) {
		cluster = Math.min(cluster,other.cluster);
		cc = Math.min(cc,other.cc);
		index = Math.min(index,other.index);
		hasStop = hasStop||other.hasStop;
		region = region.union(inbetween).union(other.region);
		if (this.codons!=estCodonsEach || other.codons!=other.estCodonsEach) {
			if (codons==estCodonsEach) {
				estCodonsEach = new ArrayList<Codon>(codons);
				estCodonsUnique = new ArrayList<Codon>(codons);
			}
			estCodonsEach.addAll(other.estCodonsEach);
			estCodonsUnique.addAll(other.estCodonsUnique);
		}
		this.codons.addAll(other.codons);
		activityUnique += other.activityUnique;
		activityFraction = (activityFraction+other.activityFraction)/2; // there is not reasonable way to do this...
		ArrayUtils.add(activities,other.activities);
		ArrayUtils.add(activitiesFractions,other.activitiesFractions); ArrayUtils.mult(activitiesFractions, 0.5);
		uniquePval = Math.min(uniquePval,other.uniquePval);
	}
	
	public boolean hasStopCodon() {
		return hasStop;
	}

	public ArrayList<Codon> getCodons() {
		return codons;
	}
	
	public double getEffectiveLength() {
		return region.getTotalLength();
	}
	
	public void setEstimatedCodons(ArrayList<Codon> estCodonsUnique,
			ArrayList<Codon> estCodonsEach) {
		this.estCodonsEach = estCodonsEach;
		this.estCodonsUnique = estCodonsUnique;
	}
	
	public ArrayList<Codon> getEstCodons() {
		return uniquePval<0.01?estCodonsEach:estCodonsUnique;
	}
	
	public void setEstimatedTotalActivity(double activity, double fraction) {
		activityUnique = activity;
		activityFraction = fraction;
	}

	public void setEstimatedTotalActivity(int condition, double activity, double fraction) {
		activities[condition] = activity;
		activitiesFractions[condition] = fraction;
	}
	
	public void setUniqueProportionPval(double uniquePval) {
		this.uniquePval = uniquePval;
	}

	public double getEstimatedTotalActivity() {
		return activityUnique;
	}
	
	public double getEstimatedActivityFraction() {
		return activityFraction;
	}
	
	public double getEstimatedTotalActivity(int condition) {
		return activities[condition];
	}
	public double getEstimatedActivityFraction(int condition) {
		return activitiesFractions[condition];
	}
	
	public double getUniquePval() {
		return uniquePval;
	}


//	/**
//	 * Assumes that all coordinates (region, codons) are on the induced coordinate system of ref. Map
//	 * them to the same coordinate system as ref
//	 * @param ref
//	 */
//	public void map(
//			ReferenceGenomicRegion<?> ref) {
//		region = ref.map(region);
//		mapList(ref,codons);
//		if (estCodonsEach!=codons)
//			mapList(ref,estCodonsEach);
//		if (estCodonsUnique!=codons)
//			mapList(ref,estCodonsUnique);
//	}


//	private static void mapList(ReferenceGenomicRegion<?> ref, ArrayList<Codon> l) {
//		for (int i=0; i<l.size(); i++) 
//			l.set(i, new Codon(ref.map(l.get(i)), l.get(i)));
//	}


	public GenomicRegion getRegion() {
		return region;
	}

	@Override
	public String toString() {
		return region.toRegionString()+" "+getEffectiveLength();
		
	}

	public Orf toOrf() {
		Orf re = new Orf();
		re.cluster = cluster;
		re.cc = cc;
		re.index = index;
		re.activityUnique = activityUnique;
		re.activities = activities;
		re.activitiesFractions = activitiesFractions;
		re.uniquePval = uniquePval;
		
		re.activityFraction = activityFraction;
		
		re.codons = mapToMatrix(codons);
		re.estCodonsEach = codons==estCodonsEach?re.codons:mapToMatrix(estCodonsEach);
		re.estCodonsUnique = codons==estCodonsUnique?re.codons:mapToMatrix(estCodonsUnique);
		return re;
	}

	private double[][] mapToMatrix(ArrayList<Codon> codons) {
		int numCond = codons.get(0).getActivity().length;
		double[][] re = new double[numCond][getRegion().getTotalLength()/3];
//		CodonType[] types = new CodonType[getRegion().getTotalLength()/3];
		for (Codon cc : codons) {
			int pos = getRegion().induce(cc.getStart())/3;
			for (int c=0; c<numCond; c++)
				re[c][pos]+=cc.getActivity()[c];
//			types[pos] = cc.getType();
		}
		return re;
	}


	
}

