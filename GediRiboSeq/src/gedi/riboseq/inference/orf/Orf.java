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

import gedi.core.data.annotation.Transcript;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.riboseq.inference.codon.CodonType;
import gedi.util.ArrayUtils;
import gedi.util.functions.EI;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.io.text.LineWriter;

import java.io.IOException;
import java.util.Arrays;

public class Orf implements BinarySerializable {

	
	int cluster;
	int cc;
	int index;
	
	/**
	 * If codons were distributed across multiple orfs, this is the total activity of this orf using the simple model (i.e. without the possibility to give distinct proporations to orfs in distinct conditions)
	 */
	double activityUnique = 1;
	/**
	 * If codons were distributed across multiple orfs, this is the total activity of this orf using the simple model (i.e. without the possibility to give distinct proporations to orfs in distinct conditions)
	 */
	double clusterFraction = 1;
	/**
	 * If codons were distributed across multiple orfs, these are the total activities per condition of this orf using the complex model (i.e. with the possibility to give distinct proporations to orfs in distinct conditions)
	 */
	double[] activities;
	/**
	 * P value of the likelihood ratio test; if small, the simple model is not appropriate (i.e. differential usage of isoforms!) 
	 */
	double uniquePval = 1;
	
	/**
	 * All codons that are in range (even overlapping with other orfs)
	 */
	double[][] codons;
	/**
	 * Codons estimated to belong to this orf (using simple model, i.e. without the possibility to give distinct proporations to orfs in distinct conditions)
	 */
	double[][] estCodonsUnique;
	/**
	 * Codons estimated to belong to this orf (using complex model, i.e. with the possibility to give distinct proporations to orfs in distinct conditions)
	 */
	double[][] estCodonsEach;
	
	String sequence;
	
	
	double[] startScores;
	double[] changePointScores;
	
	double internalPval;
	
	int inferredStartPosition;
	double gapPval;
	double presentPval;
	double stopScore;
	boolean hasStop;
	double activityFraction;
	double[] activitiesFractions;
	double tmCov;
	double inframefraction;
	
	int ri;
	
	boolean passesAllFilters;
	
	ImmutableReferenceGenomicRegion<Transcript> reference;
	OrfType orfType;
	double gof;
	public double uniformityPval;

	public double getClusterFraction() {
		return clusterFraction;
	}
	
	public String getStartCodon() {
		return getCodonTriplett(Math.max(0,inferredStartPosition));
	}
	
	public double[] getStartCodonActivities() {
		double[] re = new double[activities.length];
		for (int i=0; i<re.length; i++)
			re[i] = getEstimatedCodons()[i][Math.max(0,inferredStartPosition)];
		return re;
	}
	
	public String getCodonTriplett(int aapos) {
		return sequence.substring(aapos*3, aapos*3+3);
	}
	public CodonType getCodonType(int aapos) {
		return CodonType.get(getCodonTriplett(aapos));
	}
	
	public boolean passesAllFilters() {
		return passesAllFilters;
	}
	
	public OrfType getOrfType() {
		return orfType;
	}
	
	public double getUniformityPval() {
		return uniformityPval;
	}
	
	public double getGof() {
		return gof;
	}
	
	public Orf merge(Orf o, int myStart, int oStart) {
		int len = codons[0].length-myStart;
		assert len==o.codons[0].length-oStart;
		
		Orf re = new Orf();
		re.cluster = cluster;
		re.cc = cc;
		re.index = index;
		re.activityUnique = activityUnique+o.activityUnique;
		re.clusterFraction = clusterFraction*activityUnique/(activityUnique+o.activityUnique)+o.clusterFraction*o.activityUnique/(activityUnique+o.activityUnique);
		re.activities = ArrayUtils.concat(activities,o.activities);
		re.uniquePval = Math.min(uniquePval,o.uniquePval);
		re.codons = ArrayUtils.concat(strip(len,codons),strip(len,o.codons));
		if (codons!=estCodonsEach || o.codons!=o.estCodonsEach) {
			re.estCodonsEach = ArrayUtils.concat(strip(len,estCodonsEach),strip(len,o.estCodonsEach));
			re.estCodonsUnique = ArrayUtils.concat(strip(len,estCodonsEach),strip(len,o.estCodonsUnique));
		} else {
			re.estCodonsEach = re.estCodonsUnique = re.codons;
		}
		re.sequence = sequence.substring(sequence.length()-len);
		re.startScores = mergeArray(len, startScores, o.startScores);
		re.changePointScores = mergeArray(len, changePointScores, o.changePointScores);
		re.internalPval = Math.min(internalPval,o.internalPval);
		re.inferredStartPosition = Math.max(inferredStartPosition-myStart, o.inferredStartPosition-oStart);
		if (re.inferredStartPosition<0) re.inferredStartPosition = -1;
		re.gapPval = Math.min(gapPval,o.gapPval);
		re.presentPval = Math.min(presentPval,o.presentPval);
		re.stopScore = stopScore+o.stopScore;
		re.hasStop = hasStop||o.hasStop;
		re.activityFraction = activityFraction*activityUnique/(activityUnique+o.activityUnique)+o.activityFraction*o.activityUnique/(activityUnique+o.activityUnique);
		re.activitiesFractions = ArrayUtils.concat(activitiesFractions,o.activitiesFractions);
		re.tmCov = tmCov+o.tmCov;
		re.inframefraction = Math.min(inframefraction, o.inframefraction);
		re.ri = ri+o.ri;
		re.passesAllFilters = passesAllFilters||o.passesAllFilters;
		re.reference = re.inferredStartPosition==inferredStartPosition-myStart?reference:o.reference;
		re.orfType = re.inferredStartPosition==inferredStartPosition-myStart?orfType:o.orfType;
		re.gof = Math.min(gof,o.gof);
		re.uniformityPval = Math.min(uniformityPval,o.uniformityPval);
		return re;
	}
	
	private double[] mergeArray(int len, double[] a,
			double[] b) {
		double[] re = new double[len];
		ArrayUtils.add(re,Arrays.copyOfRange(a, a.length-len, a.length));
		ArrayUtils.add(re,Arrays.copyOfRange(b, b.length-len, b.length));
		return re;
	}
	
	private double[][] strip(int len, double[][] a) {
		double[][] re = new double[a.length][len];
		for (int i=0; i<re.length; i++)
			re[i] = Arrays.copyOfRange(a[i], a[i].length-len, a[i].length);
		return re;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			writeTableLine(new LineWriter() {
				
				@Override
				public void write(String line) throws IOException {
					sb.append(line);
				}
				@Override
				public void flush() throws IOException {
				}
				@Override
				public void close() throws IOException {
				}
			}, null);
		} catch (IOException e) {
		}
		return sb.toString();
	}
	
	
	public int getCluster() {
		return cluster;
	}
	public int getCc() {
		return cc;
	}
	public int getIndex() {
		return index;
	}
	public double getTmCov() {
		return tmCov;
	}
	public double getInframefraction() {
		return inframefraction;
	}
	public double getActivityUnique() {
		return activityUnique;
	}
	public double[] getActivities() {
		return activities;
	}
	

	public double getMeanActivity(int start, int end) {
		double[][] a = getEstimatedCodons();
		double re = 0;
		for (int i=start; i<end; i++) 
			for (int c = 0;c<a.length; c++)
				re+=a[c][i];
		return re/(end-start);
	}
	
	public double getUniquePval() {
		return uniquePval;
	}
	public double[][] getCodons() {
		return codons;
	}
	public double[][] getEstCodonsUnique() {
		return estCodonsUnique;
	}
	public double[][] getEstCodonsEach() {
		return estCodonsEach;
	}
	public double[][] getEstimatedCodons() {
		return uniquePval<0.01?getEstCodonsEach():getEstCodonsUnique();
	}
	
	public double[] getEstimatedTotalCodons() {
		double[][] mat = getEstimatedCodons();
		double[] re = new double[mat[0].length];
		for (int i=0; i<re.length; i++)
			for (int j=0; j<mat.length; j++)
				re[i] += mat[j][i];
		return re;
	}
	
	public double[] getTotalCodons() {
		double[][] mat = getCodons();
		double[] re = new double[mat[0].length];
		for (int i=0; i<re.length; i++)
			for (int j=0; j<mat.length; j++)
				re[i] += mat[j][i];
		return re;
	}
	
	
	
	public String getSequence() {
		return sequence;
	}
	
	public String getCdsSequence() {
		return sequence.substring(Math.max(0, inferredStartPosition)*3);
	}
	
	
	public double[] getStartScores() {
		return startScores;
	}
	public double[] getChangePointScores() {
		return changePointScores;
	}
	public double getInternalPval() {
		return internalPval;
	}
	public int getInferredStartPosition() {
		return inferredStartPosition;
	}
	public double getGapPval() {
		return gapPval;
	}
	public double getPresentPval() {
		return presentPval;
	}
	public double getStopScore() {
		return stopScore;
	}
	public boolean hasStop() {
		return hasStop;
	}
	public boolean hasStart() {
		return inferredStartPosition>=0;
	}
	public double getActivityFraction() {
		return activityFraction;
	}
	
	public double[] getActivitiesFractions() {
		return activitiesFractions;
	}
	public boolean isPassesAllFilters() {
		return passesAllFilters;
	}
	public ImmutableReferenceGenomicRegion<Transcript> getReference() {
		return reference;
	}
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(codons.length);
		out.putCInt(codons[0].length);
		int bits = ((reference!=null?1:0) << 3) | ((codons==estCodonsEach?1:0) << 2) | ((hasStop?1:0) << 1) | ((passesAllFilters?1:0) << 0);
		out.putCInt(bits);
		out.putCInt(cluster);
		out.putCInt(cc);
		out.putCInt(index);
		out.putFloat(activityUnique);
		out.putFloat(clusterFraction);
		out.putFloat(tmCov);
		out.putFloat(inframefraction);
		for (double d:activities) out.putFloat(d);
		out.putFloat(uniquePval);
		for (double[] c : codons) for (double d:c) out.putFloat(d);
		if (codons!=estCodonsEach) {
			for (double[] c : estCodonsUnique) for (double d:c) out.putFloat(d);
			for (double[] c : estCodonsEach) for (double d:c) out.putFloat(d);
		}
		out.putCInt(ri);
		out.putString(sequence);
		for (double d:startScores) out.putFloat(d);
		for (double d:changePointScores) out.putFloat(d);
		out.putFloat(internalPval);
		out.putCInt(inferredStartPosition==-1?codons[0].length:inferredStartPosition);
		out.putFloat(gapPval);
		out.putFloat(presentPval);
		out.putFloat(uniformityPval);
		out.putFloat(stopScore);
		out.putFloat(gof);
		out.putFloat(activityFraction);
		for (double d:activitiesFractions) out.putFloat(d);
		out.putCInt(orfType.ordinal());
		if (reference!=null)
			reference.toMutable().serialize(out);
	}
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		int numCond = in.getCInt();
		int l = in.getCInt();
		int bits = in.getCInt();
		boolean hasref = (bits & (1<<3))!=0;
		boolean estSame = (bits & (1<<2))!=0;
		hasStop = (bits & (1<<1))!=0;
		passesAllFilters = (bits & (1<<0))!=0;
		cluster = in.getCInt();
		cc = in.getCInt();
		index = in.getCInt();
		activityUnique = in.getFloat();
		clusterFraction = in.getFloat();
		tmCov = in.getFloat();
		inframefraction = in.getFloat();
		activities = readArray(numCond,in);
		uniquePval = in.getFloat();
		codons = new double[numCond][]; for (int c=0; c<numCond; c++)codons[c] = readArray(l, in);
		if (!estSame) {
			estCodonsUnique = new double[numCond][]; for (int c=0; c<numCond; c++)estCodonsUnique[c] = readArray(l, in);
			estCodonsEach = new double[numCond][]; for (int c=0; c<numCond; c++)estCodonsEach[c] = readArray(l, in);
		} else {
			estCodonsEach = estCodonsUnique = codons;
		}
		ri = in.getCInt();
		sequence = in.getString();
		startScores = readArray(l, in);
		changePointScores = readArray(l, in);
		internalPval = in.getFloat();
		inferredStartPosition = in.getCInt();
		if (inferredStartPosition==l) inferredStartPosition=-1;
		gapPval = in.getFloat();
		presentPval = in.getFloat();
		uniformityPval = in.getFloat();
		stopScore = in.getFloat();
		gof = in.getFloat();
		activityFraction = in.getFloat();
		activitiesFractions = readArray(numCond, in);
		orfType = OrfType.values()[in.getCInt()];
		if (hasref) {
			MutableReferenceGenomicRegion<Transcript> mut = new MutableReferenceGenomicRegion<Transcript>().setData(new Transcript());
			mut.deserialize(in);
			reference = mut.toImmutable();
		}
	}
	private double[] readArray(int n, BinaryReader in) throws IOException {
		double[] re = new double[n];
		for (int i=0; i<re.length; i++) re[i] = in.getFloat();
		return re;
	}
	
	public static void writeTableHeader(LineWriter out, String[] conditions) throws IOException {
		out.writeTsv(
				"Cluster","Connected component","Index", "Location", "CDS Location",
				"Gene","Transcript","Transcript location","Orf type",
				"Start codon","Orf genomic length","CDS genomic length","Reproducibility index",
				"Activity","Cluster fraction","Robust coverage","Inframe fraction",EI.wrap(conditions).map(c->"Activity "+c).concat("\t"),
				"Isoform fraction",EI.wrap(conditions).map(c->"Isoform fraction "+c).concat("\t"),
				"Has start","Has stop",
				"Isoform usage pval","Internal pval","Gap pval","Codon pval","Uniformity pval","Start log odds","Changepoint log odds","Stop log odds","Goodness of fit",
				"Passes all filters"
				);
	}
	
	public void writeTableLine(LineWriter out, ReferenceGenomicRegion<?> location) throws IOException {
		out.writef("%d\t%d\t%d\t%s",cluster,cc,index,location==null?"":location.toLocationString());
		if (inferredStartPosition>=0 && location!=null)
			out.writef("\t%s",location.getReference()+":"+location.map(new ArrayGenomicRegion(inferredStartPosition*3,location.getRegion().getTotalLength())));
		else
			out.writef("\t");
		out.writef("\t%s\t%s\t%s\t%s",
				reference==null?"":reference.getData().getGeneId(),
				reference==null?"":reference.getData().getTranscriptId(),
				reference==null?"":reference.toLocationString(),
				orfType.toString());
		out.writef("\t%s\t%s\t%s\t%d",
				getStartCodon(),
				startScores.length*3,
				(startScores.length-Math.max(0,inferredStartPosition))*3,
				ri);
		
		out.writef("\t%.1f",activityUnique);
		out.writef("\t%.1f",clusterFraction);
		out.writef("\t%.4f",tmCov);
		out.writef("\t%.4f",inframefraction);
		for (int i=0; i<activities.length; i++) out.writef("\t%.1f",activities[i]);
		out.writef("\t%.4g",activityFraction);
		for (int i=0; i<activities.length; i++) out.writef("\t%.1f",activitiesFractions[i]);
		out.writef("\t%d\t%d",inferredStartPosition>=0?1:0,hasStop?1:0);
		out.writef("\t%.4g\t%.4g\t%.4g\t%.4g\t%.4g\t%.4g\t%.4g\t%.4g\t%.4g",uniquePval,internalPval,gapPval,presentPval,uniformityPval,
				inferredStartPosition>=0?startScores[inferredStartPosition]:0,
				inferredStartPosition>=0?changePointScores[inferredStartPosition]:0,
				stopScore,gof);
		out.writef("\t%d\n",passesAllFilters?1:0); 
	}
	public GenomicRegion getStartToStop(ReferenceSequence ref,
			GenomicRegion reg) {
		if (inferredStartPosition<0 || !hasStop) return reg;
		ImmutableReferenceGenomicRegion<Void> r = new ImmutableReferenceGenomicRegion<Void>(ref, reg);
		return r.map(new ArrayGenomicRegion(inferredStartPosition*3,reg.getTotalLength()));
	}


	
	
	
	
}

