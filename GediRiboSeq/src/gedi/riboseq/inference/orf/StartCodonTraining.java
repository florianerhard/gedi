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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.riboseq.inference.orf.StartCodonTraining.RangeAndStartClassifier;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.array.functions.NumericArrayTransformation;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.DoubleRanking;
import gedi.util.math.stat.NumericSample;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.math.stat.classification.CompleteRocAnalysis;
import gedi.util.orm.Orm;
import gedi.util.orm.OrmSerializer;
import jline.internal.Log;
import smile.classification.LogisticRegression;
import smile.classification.SoftClassifier;

public class StartCodonTraining {

	
	private ArrayList<StartCodonPredictionOrf> list = new ArrayList<>();
	private int minOrfs;
	
	private int negatives = 5;
	private int upstream = 0;
	private int downstream = 0;
	
	
	private RandomNumbers rnd = RandomNumbers.getGlobal();
	
	public StartCodonTraining(int minOrfs) {
		this.minOrfs = minOrfs;
	}


	public void add(ImmutableReferenceGenomicRegion<PriceOrf> orf) {
		StartCodonPredictionOrf p = new StartCodonPredictionOrf(orf.getData(), orf.getData().getPredictedStartAminoAcid());
		list.add(p);
	}

	public int getNumExamples() {
		return list.size();
	}
	

	public void clear() {
		list.clear();
	}
	
//	public void writeDirichletModel(int startRange, int stopRange, int numOrfs) throws IOException {
//		double[][] model = new double[startRange+stopRange+1][list.get(0).ucds.length];
//		double[] sf = new double[model[0].length];
//		double[] buff = new double[model[0].length];
//		
//		
//		StartCodonPredictionOrf[] a = list.toArray(new StartCodonPredictionOrf[0]);
//		Arrays.sort(a,(x,y)->Double.compare(y.mean,x.mean));
//		
//		for (int i=0; i<numOrfs; i++){
//			StartCodonPredictionOrf orf = a[i];
//			Arrays.fill(sf, 0);
//			for (int p=orf.start; p<orf.ucds[0].length(); p++) {
//				downsample(orf.ucds,p,buff);
//				ArrayUtils.add(sf, buff);
//			}
//			ArrayUtils.normalize(sf);
//			
//			for (int p=orf.start; p<orf.ucds[0].length(); p++) {
//				normalize(orf.ucds,p,buff,sf);
//				
//				int index = Math.min(p-orf.start, startRange);
//				if (p>=orf.ucds[0].length()-stopRange)
//					index = startRange+p+1-(orf.ucds[0].length()-stopRange);
//				
//				ArrayUtils.add(model[index], buff);
//			}
//		}
//		
//		LineWriter wr = new LineOrientedFile("dirichlet.tsv").write();
//		for (int i=0; i<model.length; i++) {
//			wr.write("Pos");
//			for (int j=0; j<model[i].length; j++)
//				wr.writef("\t%.4f", model[i][j]);
//			wr.writeLine();
//		}
//		wr.close();
//	}
//	
//	private void downsample(NumericArray[] cds, int p, double[] buff) {
//		for (int i=0; i<cds.length; i++)
//			buff[i] = cds[i].getDouble(p);
//		double m = ArrayUtils.max(buff);
//		if (m>0)
//			ArrayUtils.mult(buff, 1/m);
//	}
//	
//	private void normalize(NumericArray[] cds, int p, double[] buff, double[] sf) {
//		for (int i=0; i<cds.length; i++)
//			buff[i] = sf[i]==0?0:cds[i].getDouble(p)/sf[i];
//		double s = ArrayUtils.sum(buff);
//		if (s>0)
//			ArrayUtils.mult(buff, 1/s);
//	}


	public void setRnd(RandomNumbers rnd) {
		this.rnd = rnd;
	}
	
	public StartCodonScorePredictor train() {
		TrainData data = new TrainData(list, rnd, negatives,upstream,downstream);
		return new StartCodonScorePredictor(trainModel(data.x,data.y),upstream,downstream);
	}
	
	
	public static class RangeAndStartClassifier implements BinarySerializable {

		
		private double[] means;
		private LogisticRegression[] ranges;
		private LogisticRegression[] starts;
		
//		private NumericSample[][] start;
//		private LogisticRegression[] start;
		
		public RangeAndStartClassifier() {}
		
		public RangeAndStartClassifier(double[][] x, int[] y, int minOrfs) {
			
			ArrayUtils.parallelSort(x, y, (a,b)->Double.compare(a[0], b[0]));
			
			DoubleArrayList means = new DoubleArrayList();
			ArrayList<LogisticRegression> starts = new ArrayList<>();
			ArrayList<LogisticRegression> ranges = new ArrayList<>();
			
			int start = 0;
			for (int i=minOrfs; i<x.length; i+=minOrfs) {
				i = Math.min(x.length, i);
				for (;i<x.length && x[i][0]==x[i-1][0]; i++);
				if (i+minOrfs>x.length)
					i = x.length;
				
				means.add(i==x.length?Double.POSITIVE_INFINITY:x[i][0]);
				starts.add(new LogisticRegression(slice(ArrayUtils.slice(x,start,i),2,x[0].length),ArrayUtils.slice(y, start, i)));
				ranges.add(new LogisticRegression(slice(ArrayUtils.slice(x,start,i),1,2),ArrayUtils.slice(y, start, i)));
				start = i;
			}
			if (start<x.length) {
				int i = x.length;;
				means.add(i==x.length?Double.POSITIVE_INFINITY:x[i][0]);
				starts.add(new LogisticRegression(slice(ArrayUtils.slice(x,start,i),2,x[0].length),ArrayUtils.slice(y, start, i)));
				ranges.add(new LogisticRegression(slice(ArrayUtils.slice(x,start,i),1,2),ArrayUtils.slice(y, start, i)));
				start = i;
			}
			this.means = means.toDoubleArray();
			this.ranges = ranges.toArray(new LogisticRegression[0]);
			this.starts = starts.toArray(new LogisticRegression[0]);
		}
		
		private double[][] slice(double[][] x, int start, int end) {
			double[][] re = new double[x.length][end-start];
			for (int i=0; i<re.length; i++)
				re[i] = ArrayUtils.slice(x[i], start, end);
			return re;
		}


		public double predictRange(double[] x, double[] buff) {
			int ind = Arrays.binarySearch(means, x[0]);
			if (ind<0) ind = -ind-1;
			ranges[ind].predict(ArrayUtils.slice(x, 1, 2), buff);
			return buff[1];
		}

		
		public double predictStart(double[] x, double[] buff) {
			int ind = Arrays.binarySearch(means, x[0]);
			if (ind<0) ind = -ind-1;
			starts[ind].predict(ArrayUtils.slice(x, 2, x.length), buff);
			return buff[1];
		}

		@Override
		public void serialize(BinaryWriter out) throws IOException {
			OrmSerializer seri = new OrmSerializer();
			out.putCInt(means.length);
			for (int i=0; i<means.length; i++) {
				out.putDouble(means[i]);
				seri.serializeWithoutClass(out, ranges[i]);
				seri.serializeWithoutClass(out, starts[i]);
			}
		}

		@Override
		public void deserialize(BinaryReader in) throws IOException {
			OrmSerializer seri = new OrmSerializer();
			means = new double[in.getCInt()];
			ranges = new LogisticRegression[means.length];
			starts = new LogisticRegression[means.length];
			
			for (int i=0; i<means.length; i++) {
				means[i] = in.getDouble();
				ranges[i] = Orm.create(LogisticRegression.class);
				starts[i] = Orm.create(LogisticRegression.class);
				seri.deserializeWithoutClass(in, ranges[i]);
				seri.deserializeWithoutClass(in, starts[i]);
			}
		}
		
	}
	
	private RangeAndStartClassifier trainModel(double[][] x, int[] y) {
//		int range = upstream+1+downstream;
//		int numCond = x[0].length/range-1;
//		int[] sizes = new int[1+numCond];
//		sizes[0] = numCond;
//		for (int i=1; i<sizes.length; i++)
//			sizes[i] = range;
//		
//		return new HierarchicalSoftClassifier(x, y, sizes);
		
		
		int range = upstream+1+downstream;
		int numCond = x[0].length/(range+1);
		return new RangeAndStartClassifier(x, y, Math.min(minOrfs*(negatives+1),y.length));
		
//		return new RandomForest(x,y,10);
		// C 100
		// gamma 0.5
//		SVM<double[]> re = new SVM<double[]>(new GaussianKernel(1), 10);
//		re.learn(x, y);
//		re.finish();
//		re.trainPlattScaling(x, y);
//		return re;
	}
	
	/**
	 * Returns AUROC
	 * @param out
	 * @param k
	 * @return
	 * @throws IOException 
	 */
	public CompleteRocAnalysis crossValidation(int k, boolean useStart, boolean useRange) {
		
		TrainData data = new TrainData(list, rnd, negatives,upstream,downstream);
		data.shuffle(rnd);
		int foldsize = data.x.length/k;
		
		double[] buff = new double[2];
		double[] posteriors = new double[data.x.length];
		
		for (int i=0; i/foldsize<k; i+=foldsize) {
			int to = i/foldsize==k-1?data.x.length:(i+foldsize);
			
			double[][] trainx = ArrayUtils.concat(ArrayUtils.slice(data.x, 0, i),ArrayUtils.slice(data.x, to,data.x.length));
			int[] trainy = ArrayUtils.concat(ArrayUtils.slice(data.y, 0, i),ArrayUtils.slice(data.y, to,data.x.length));
			
			RangeAndStartClassifier model = trainModel(trainx,trainy);
			for (int j=i; j<to; j++) {
				posteriors[j] = 1;
				if (useStart) posteriors[j]*=model.predictStart(data.x[j],buff);
				if (useRange) posteriors[j]*=model.predictRange(data.x[j],buff);
			}
		}
		
		CompleteRocAnalysis roc = new CompleteRocAnalysis();
		for (int i=0; i<posteriors.length ; i++) 
			roc.addScore(posteriors[i], data.y[i]==1);	
		
		return roc;
	}
	
	
	
	
	/**
	 * x consists of numcond blocks, each having upstream+1+downstream consecutive values
	 * @param p
	 * @param pos
	 * @param upstream
	 * @param downstream
	 * @param buffer
	 * @return
	 */
	static double[] getX(StartCodonPredictionOrf p, int pos, int upstream, int downstream, double[] buffer) {
		int numCond = p.getNumConditions();
		int range = upstream+1+downstream;
		
		double[] re = buffer!=null&&buffer.length==1+1+numCond*range?buffer:new double[1+1+numCond*range];
		re[0] = p.mean;
		
		if (pos<0) re[1] = 0;
		else if (pos>=p.length()) re[1]=1;
		else re[1] = p.cumu.getDouble(pos);
		for (int c=0; c<numCond; c++) {
			for (int off=0; off<range; off++) {
				re[2+c*range+off] = pos-upstream+off>=0 && pos-upstream+off<p.length()?p.cds[c].getDouble(pos-upstream+off):0;
			}
		}
		
		return re;
	}
	
	
	static class TrainData {
		double[][] x;
		int[] y;
		public TrainData(ArrayList<StartCodonPredictionOrf> a, RandomNumbers rnd, int negatives, int upstream, int downstream) {
			int numCond = a.get(0).getNumConditions();
			int range = upstream+1+downstream;
			
			x = new double[a.size()*(negatives+1)][numCond*(range)];
			y = new int[a.size()*(negatives+1)];
			
			int index = 0;
			for (int i=0; i<a.size(); i++) {
				
				StartCodonPredictionOrf p = a.get(i);
				x[index] = getX(p,p.start,upstream,downstream,x[index]);
				y[index] = 1;
				index++;
				for (int r=0; r<negatives; r++) {
					int pos = rnd.getUnif(p.start+5, p.total.length()-downstream-1);
					x[index] = getX(p,pos,upstream,downstream,x[index]);
					y[index] = 0;
					index++;
				}
			}
		}
		public void shuffle(RandomNumbers rnd) {
			int[] perm = rnd.getPermutation(x.length);
			x = ArrayUtils.select(x,perm);
			y = ArrayUtils.select(y,perm);
		}
	}
	


	static class StartCodonPredictionOrf implements Comparable<StartCodonPredictionOrf> {
		
		private NumericArray total;
		private NumericArray[] cds;
		private NumericArray[] ucds;
		private NumericArray cumu;
		
		private double mean;
		private int start;

		private static int lwin = 5;
		private static int rwin = 10;
		
		public StartCodonPredictionOrf(PriceOrf orf, int start) {
			cds = new NumericArray[orf.codonProfiles.length];
			ucds = new NumericArray[orf.codonProfiles.length];
			cumu = NumericArray.createMemory(orf.codonProfiles[0].length, NumericArrayType.Double);
			for (int i=0; i<cds.length; i++) {
				cds[i] = NumericArray.wrap(orf.codonProfiles[i].clone());
				cds[i].transform(NumericArrayTransformation.function(v->Math.max(0, v-0.1))); // thats important for stability
				ucds[i] = NumericArray.wrap(orf.codonProfiles[i].clone());
				ucds[i].transform(NumericArrayTransformation.function(v->Math.max(0, v-0.1))); // thats important for stability
				cumu.add(cds[i]);
			}
			
			double sum = cumu.evaluate(NumericArrayFunction.Sum);
			if (sum==0)
				cumu.transform(NumericArrayTransformation.constant(0.5));
			else {
				NumericArray cum = cumu.copy();
				cum.transform(NumericArrayTransformation.add(0.1));
				cum.transform(NumericArrayTransformation.InverseCumSum);
				for (int p=0; p<cumu.length(); p++) {
					sum = cum.getDouble(Math.max(0, p-lwin))-cum.getDouble(Math.min(cum.length()-1, p+rwin+1))-0.1*Math.min(0, p-lwin);
					double asum = cum.getDouble(Math.max(0, p))-cum.getDouble(Math.min(cum.length()-1, p+rwin+1));
					cumu.setDouble(p,asum/sum);
				}
			}
			
			this.start = start;
					
			total = NumericArray.wrap(orf.getTotalActivities(0).clone());
			mean = total.evaluate(NumericArrayFunction.GeometricMeanRemoveNonPositive);
			
			
			asinh();
			unitize();
			
		}
		

		@Override
		public int compareTo(StartCodonPredictionOrf o) {
			return Double.compare(o.mean, mean);
		}
		
		
		// extremely bad when there are ties (zeros!)
//		private void rank() {
//			for (int c=0; c<cds.length; c++) {
//				cds[c].transform(NumericArrayTransformation.RelativeRank);
//			}
//		}
		
		private void unitize() {
			for (int c=0; c<cds.length; c++) {
				double max = cds[c].evaluate(NumericArrayFunction.Max);
				for (int i=0; i<cds[c].length(); i++) {
					double val = cds[c].getDouble(i)/max;
					if (Double.isNaN(val))
						val = 0;
					cds[c].setDouble(i, val);
				}
			}
		}
		
//		public void unitize() {
//			for (int c=0; c<cds.length; c++) {
////				NumericArray rmqa = cds[c].copy();
////				for (int i=0; i<rmqa.length(); i++)
////					rmqa.setDouble(i, -rmqa.getDouble(i));
////				SuccinctRmq rmq = new SuccinctRmq(rmqa); // is range MINIMUM query!
////				for (int i=0; i<cds[c].length()-startRange*2; i++) {
////					double max = (-rmqa.getDouble(rmq.query(i+startRange, cds[c].length()-2)));
////					cds[c].setDouble(i, cds[c].getDouble(i)/(max==0?1:max));
////				}
//				double max = cds[c].evaluate(NumericArrayFunction.Max);
//				for (int i=0; i<cds[c].length(); i++) {
//					double val = cds[c].getDouble(i)/max;
//					if (Double.isNaN(val))
//						val = 0;
//					cds[c].setDouble(i, val);
//				}
//			}
//		}
		
		private void asinh() {
			for (int c=0; c<cds.length; c++) { 
				cds[c].transform(NumericArrayTransformation.function(a->Math.log(a+Math.sqrt(a*a+1))));
			}
		}
		
		private void log() {
			for (int c=0; c<cds.length; c++) { 
				cds[c].transform(NumericArrayTransformation.function(a->Math.log10(1+a)));
			}
		}

		public int getNumConditions() {
			return cds.length;
		}

		public int length() {
			return cds[0].length();
		}
		
	}




	
}
