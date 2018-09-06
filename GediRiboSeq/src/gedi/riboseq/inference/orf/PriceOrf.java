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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.functions.EI;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.StringLineWriter;
import gedi.util.math.stat.DoubleRanking;

public class PriceOrf implements BinarySerializable {

	String geneId = "";
	String transcriptId;
	int orfid;
	int predictedStartAminoAcid;
	int[] alternativeStartAminoAcids;
	double[] startScores;
	double[] startRangeScores;
	double combinedP;
	
	double[][] codonProfiles;
	double deconvolvedFraction;

	String startCodon;
	PriceOrfType type;
	
	transient double[] geommeanbuffer;
	transient int[] geommeanbuffernonzero;
	
	public ImmutableReferenceGenomicRegion<PriceOrf> restrictToLongestStart(
			ImmutableReferenceGenomicRegion<PriceOrf> orf) {
		
		int offset = alternativeStartAminoAcids[0];
		PriceOrf re = new PriceOrf(transcriptId, orfid, null, deconvolvedFraction);
		re.geneId = geneId;
		re.predictedStartAminoAcid = predictedStartAminoAcid-offset;
		re.alternativeStartAminoAcids = EI.wrap(alternativeStartAminoAcids).mapToInt(p->p-offset).toIntArray();
		re.startScores = ArrayUtils.slice(startScores, offset,startScores.length);
		re.startRangeScores = ArrayUtils.slice(startRangeScores, offset,startRangeScores.length);
		re.combinedP = combinedP;
		re.codonProfiles = EI.wrap(codonProfiles).map(a->ArrayUtils.slice(a, offset,a.length)).toArray(double[].class);
		re.type = type;
		
		return new ImmutableReferenceGenomicRegion(orf.getReference(),
				orf.map(new ArrayGenomicRegion(offset*3,orf.getRegion().getTotalLength())),
				re);
	}
	
	public PriceOrf(String transcriptId, int orfid, double[][] codonProfiles, double deconvolvedFraction) {
		this.transcriptId = transcriptId;
		this.orfid = orfid;
		this.codonProfiles = codonProfiles;
		this.deconvolvedFraction = deconvolvedFraction;
	}


	public double getTotalActivityFromPredicted() {
		return EI.wrap(codonProfiles).mapToDouble(a->ArrayUtils.sum(a, predictedStartAminoAcid, a.length)).sum();
	}
	
	
	public double getTotalActivity() {
		return EI.wrap(codonProfiles).mapToDouble(a->ArrayUtils.sum(a)).sum();
	}
	
	
	public String getGeneId() {
		return geneId;
	}
	

	public int getPredictedStartAminoAcid() {
		return predictedStartAminoAcid;
	}
	
	public double[] getStartScores() {
		return startScores;
	}
	
	public double[] getStartRangeScores() {
		return startRangeScores;
	}
	
	public double[] getTotalActivities() {
		return getTotalActivities(predictedStartAminoAcid);
	}
	
	public double[] getTotalActivities(int start) {
		double[] re = new double[codonProfiles[0].length-start];
		for (int i=0; i<re.length; i++)
			for (int c=0; c<codonProfiles.length; c++)
				re[i]+=codonProfiles[c][i+start];
		return re;
	}
	
	public double[] getTotalActivities(int start, int end) {
		double[] re = new double[end-start];
		for (int i=0; i<re.length; i++)
			for (int c=0; c<codonProfiles.length; c++)
				re[i]+=codonProfiles[c][i+start];
		return re;
	}
	
	public double[] getActivitiesPerCondition() {
		return getActivitiesPerCondition(predictedStartAminoAcid);
	}
	public double[] getActivitiesPerCondition(int start) {
		double[] re = new double[codonProfiles.length];
		for (int i=0; i<re.length; i++)
			re[i] = ArrayUtils.sum(codonProfiles[i], start,codonProfiles[i].length);
		return re;
	}
	
	public double getDeconvolvedFraction() {
		return deconvolvedFraction;
	}

	public PriceOrfType getType() {
		return type;
	}
	
	public String getStartCodon() {
		return startCodon;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + orfid;
		result = prime * result + ((transcriptId == null) ? 0 : transcriptId.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PriceOrf other = (PriceOrf) obj;
		if (orfid != other.orfid)
			return false;
		if (transcriptId == null) {
			if (other.transcriptId != null)
				return false;
		} else if (!transcriptId.equals(other.transcriptId))
			return false;
		return true;
	}


	@Override
	public String toString() {
		LineWriter wr = new StringLineWriter();
		wr.writef2("id=%d start=%.2f range=%.2f p=%.5g act=%.1f",
				orfid,
				startScores==null?Double.NaN:startScores[predictedStartAminoAcid],
				startRangeScores==null?Double.NaN:startRangeScores[predictedStartAminoAcid],
				combinedP,
				getTotalActivityFromPredicted());
		return wr.toString();
	}


	public double getCombinedP() {
		return combinedP;
	}

	
	public int getOrfid() {
		return orfid;
	}
	

	public ImmutableReferenceGenomicRegion<PriceOrf> getStartStop(ImmutableReferenceGenomicRegion<?> rgr, boolean includeStop) {
		return new ImmutableReferenceGenomicRegion<PriceOrf>(
				rgr.getReference(), 
				rgr.map(new ArrayGenomicRegion(predictedStartAminoAcid*3,rgr.getRegion().getTotalLength()-(includeStop?0:3))),
				this);
	}
	
	public ImmutableReferenceGenomicRegion<PriceOrf> getStartStop(ImmutableReferenceGenomicRegion<?> rgr, int alternative, boolean includeStop) {
		return new ImmutableReferenceGenomicRegion<PriceOrf>(
				rgr.getReference(), 
				rgr.map(new ArrayGenomicRegion(alternativeStartAminoAcids[alternative]*3,rgr.getRegion().getTotalLength()-(includeStop?0:3))),
				this);
	}
	
	public ImmutableReferenceGenomicRegion<PriceOrf> getPositionToStop(ImmutableReferenceGenomicRegion<?> rgr, int aaPos, boolean includeStop) {
		return new ImmutableReferenceGenomicRegion<PriceOrf>(
				rgr.getReference(), 
				rgr.map(new ArrayGenomicRegion(aaPos*3,rgr.getRegion().getTotalLength()-(includeStop?0:3))),
				this);
	}
	
	public int getNumAlternativeStartCodons() {
		return alternativeStartAminoAcids.length;
	}
	
	public boolean isPredictedStartIndex(int alternative) {
		return alternativeStartAminoAcids[alternative] == predictedStartAminoAcid;
	}
	

	public static void writeTableHeader(LineWriter wr, String[] conditions) throws IOException {
		wr.write("Gene\tId\tLocation\tCandidate Location\tCodon\tType\tStart\tRange\tp value");
		for (String c : conditions) { 
			wr.write("\t");
			wr.write(c);
		}
		wr.write("\tTotal");
		wr.writeLine();
	}
	public void writeTableLine(LineWriter wr, ImmutableReferenceGenomicRegion<PriceOrf> r) throws IOException {
		wr.writef("%s\t%s_%s_%d\t%s\t%s\t%s\t%s\t%.2f\t%.2f\t%.5g",
				geneId,transcriptId,type.toString(),orfid,
				getStartStop(r,true).toLocationString(),
				r.toLocationString(),
				startCodon,
				type,
				startScores[predictedStartAminoAcid],
				startRangeScores[predictedStartAminoAcid],combinedP);
		double[] act = getActivitiesPerCondition(predictedStartAminoAcid);
		for (int i=0; i<act.length; i++)
			wr.writef("\t%.1f", act[i]);
		wr.writef("\t%.1f\n",getTotalActivityFromPredicted());
	}




	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putString(geneId);
		out.putString(transcriptId);
		out.putCInt(orfid);
		out.putCInt(predictedStartAminoAcid);
		FileUtils.writeIntArray(out, alternativeStartAminoAcids);
		FileUtils.writeDoubleArray(out, startScores);
		FileUtils.writeDoubleArray(out, startRangeScores);
		out.putDouble(combinedP);
		out.putString(startCodon);
		out.putCInt(type.ordinal());
		out.putCInt(codonProfiles.length);
		
		if (dense()) {
			out.putByte(1);
			for (int i=0; i<codonProfiles.length; i++) 
				FileUtils.writeDoubleArray(out, codonProfiles[i]);
		} else {
			out.putByte(0);
			out.putCInt(codonProfiles[0].length);
			for (int c=0; c<codonProfiles.length; c++) {
				for (int p=0; p<codonProfiles[c].length; p++) {
					if (codonProfiles[c][p]!=0) {
						out.putCInt(c);
						out.putCInt(p);
						out.putDouble(codonProfiles[c][p]);
					}
				}
			}
			out.putCInt(codonProfiles.length);
		}
		
		out.putDouble(deconvolvedFraction);
	}

	private boolean dense() {
		int count = 0;
		for (int c=0; c<codonProfiles.length; c++) {
			for (int p=0; p<codonProfiles[c].length; p++) {
				if (codonProfiles[c][p]!=0)
					count++;
			}
		}
		return count*2>codonProfiles.length*codonProfiles[0].length;
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		geneId = in.getString();
		transcriptId = in.getString();
		orfid = in.getCInt();
		predictedStartAminoAcid = in.getCInt();
		alternativeStartAminoAcids = FileUtils.readIntArray(in);
		startScores = FileUtils.readDoubleArray(in);
		startRangeScores = FileUtils.readDoubleArray(in);
		combinedP = in.getDouble();
		startCodon = in.getString();
		type = PriceOrfType.values()[in.getCInt()];
		
		codonProfiles = new double[in.getCInt()][];
		
		if (in.getByte()==1) {
			for (int i=0; i<codonProfiles.length; i++)
				codonProfiles[i] = FileUtils.readDoubleArray(in);
		} else {
			int len = in.getCInt();
			for (int c=0; c<codonProfiles.length; c++) 
				codonProfiles[c]=new double[len];
			for (;;) {
				int c = in.getCInt();
				if (c==codonProfiles.length) break;
				int p = in.getCInt();
				codonProfiles[c][p] = in.getDouble();
			}
		}
		
		deconvolvedFraction = in.getDouble();
	}


	public double getIsoformFraction() {
		return deconvolvedFraction;
	}


	public int getOrfAaLength() {
		return codonProfiles[0].length-predictedStartAminoAcid;
	}
	
	public int getOrfAaLength(int startAminoAcidPos) {
		return codonProfiles[0].length-startAminoAcidPos;
	}


	public String getTranscript() {
		return transcriptId;
	}


//	public double getSinhMean(int startAminoAcid) {
//		double[] act = getTotalActivities(startAminoAcid);
//		return NumericArray.wrap(act).evaluate(NumericArrayFunction.SinhMean);
//	}
//	
//	public double getSinhMean(int startAminoAcid, int endAminoAcid) {
//		double[] act = getTotalActivities(startAminoAcid, endAminoAcid);
//		return NumericArray.wrap(act).evaluate(NumericArrayFunction.SinhMean);
//	}
	
	public double getGeomMean(int startAminoAcid) {
		return getGeomMean(startAminoAcid,codonProfiles[0].length);
//		double[] act = getTotalActivities(startAminoAcid);
//		return NumericArray.wrap(act).evaluate(NumericArrayFunction.GeometricMeanRemoveNonPositive);
	}
	
	public double getGeomMean(int startAminoAcid, int endAminoAcid) {
		if (geommeanbuffer==null) {
			geommeanbuffer = getTotalActivities(0, codonProfiles[0].length);
			geommeanbuffernonzero = new int[geommeanbuffer.length];
			double last = 0;
			for (int i=0; i<geommeanbuffer.length; i++) {
				double a = 0;
				int b = 0;
				if (geommeanbuffer[i]>0) {
					a = Math.log(geommeanbuffer[i]);
					b = 1;
				}
				last = geommeanbuffer[i]=a+last;
				geommeanbuffernonzero[i]=b+(i>0?geommeanbuffernonzero[i-1]:0);
			}
		}
//		double[] act = getTotalActivities(startAminoAcid, endAminoAcid);
//		double old =  NumericArray.wrap(act).evaluate(NumericArrayFunction.GeometricMeanRemoveNonPositive);
		double buffered;
		if (startAminoAcid==0) 
			buffered = Math.exp((geommeanbuffer[endAminoAcid-1]) / (geommeanbuffernonzero[endAminoAcid-1]));
		else
			buffered = Math.exp((geommeanbuffer[endAminoAcid-1]-geommeanbuffer[startAminoAcid-1]) / (geommeanbuffernonzero[endAminoAcid-1]-geommeanbuffernonzero[startAminoAcid-1]));
//		System.out.println(Math.abs(old-buffered));
		return buffered;
	}
	

	public int getNumConditions() {
		return codonProfiles.length;
	}
	
	public int getNumPositions() {
		return codonProfiles[0].length;
	}


	public double getProfile(int condition, int position) {
		return codonProfiles[condition][position];
	}


	void clearProfile() {
		for (int c=0; c<codonProfiles.length; c++)
			Arrays.fill(codonProfiles[c], 0);
	}


	


	
	
	
	
	 
}
