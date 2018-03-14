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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import gedi.core.data.annotation.Transcript;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.array.functions.NumericArrayTransformation;
import gedi.util.functions.EI;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.function.StepFunction;
import gedi.util.math.stat.NumericSample;
import jdistlib.Beta;
import jdistlib.math.spline.SmoothSpline;
import jdistlib.math.spline.SmoothSplineResult;

/**
 * Provides the probability of observing an in- or off-frame codon with a given level within an ORF of given translation strength
 * 
 * i.e. is basically a function (strength, level, frame)->probability
 * 
 * where strength is the transformed mean level of in-frame codon activities
 * level is the activity level of the codon, for which the probability is desired
 * and frame is 0,1 or 2, where 0 corresponds to an in-frame codon, 1 and 2 to off-frame codons.
 * 
 * @author erhard
 *
 */
public class NoiseModel implements BinarySerializable {
	
	private SingleNoiseModel[] data;
	private double[] x;
	private double[] x2;
	private double precision = 1E-3;
	private StepFunction edst;
	
	public NoiseModel() {
	}
	
	public NoiseModel(SingleNoiseModel[] models) {
		this.data = models;
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(data.length);
		for (SingleNoiseModel m : data)
			m.serialize(out);
	}
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		data = new SingleNoiseModel[in.getCInt()];
		for (int j=0; j<data.length; j++) {
			data[j] = new SingleNoiseModel();
			data[j].deserialize(in);
		}
	}
	
	private static class PrecisionSpline {
		SmoothSplineResult mean;
		SmoothSplineResult var;
		boolean preciseDown;
		public PrecisionSpline(SmoothSplineResult mean, SmoothSplineResult var) {
			this.mean = mean;
			this.var = var;
		}
		public double predict(double transformedX, double quantile) {
			if (Double.isNaN(quantile))
				return SmoothSpline.predict(mean,transformedX,0);
			
			double m = SmoothSpline.predict(mean,transformedX,0);
			double v = SmoothSpline.predict(var,transformedX,0);
			double c = m*(1-m)/v-1;
			return Beta.quantile(quantile, m*c, (1-m)*c, true, false);
		}
	}
	
	public int[] getCacheSize() {
		return new int[] {cache[0].size(),cache[1].size(),cache[2].size()};
	}
	
	private TreeMap<Double,PrecisionSpline>[] cache = new TreeMap[] {new TreeMap(), new TreeMap(), new TreeMap()};
	public double getProbability1(double level, double fraction) {
		return getProbability1(level,fraction,Double.NaN);
	}
	public double getProbability2(double level, double fraction) {
		return getProbability2(level,fraction,Double.NaN);
	}
	public double getProbability0(double level, double fraction, double quantile) {
		return getProbability(transform(level), fraction, 0, ()->getX(),m->m.getProbability0(fraction),quantile);
	}
	public double getProbability1(double level, double fraction, double quantile) {
		return getProbability(transform(level), fraction, 1, ()->getX(),m->m.getProbability1(fraction),quantile);
	}
	public double getProbability2(double level, double fraction, double quantile) {
		return getProbability(transform(level), fraction, 2, ()->getX(),m->m.getProbability2(fraction),quantile);
	}
	
	public double getAbortiveStartLevelProbability(double startSmean, double fraction) {
		return getProbability(transform2(startSmean), fraction, 0, ()->getX2(),s->s.getProbability0(fraction*s.mean/s.startRegionMean),Double.NaN);
	}
	
	
	private double getProbability(double transformedX, double fraction, int cacheIndex, Supplier<double[]> x, ToDoubleFunction<SingleNoiseModel> prob, double quantile) {
			
		fraction = Math.round(fraction/precision)*precision;
		
		PrecisionSpline pl = cache[cacheIndex].get(fraction);
		if (pl==null) {
			SmoothSplineResult mean = computeMeanSpline(x,prob);
			SmoothSplineResult var = computeVarSpline(mean,x,prob);
			pl = new PrecisionSpline(mean,var);
			TreeMap<Double, PrecisionSpline> tm = new TreeMap<>(cache[cacheIndex]); // copy the whole cache, do the change on the local version and save back; may overwrite changes from other threads, but this works without synchronization!
			tm.put(fraction, pl);
			cache[cacheIndex] = tm;
		}
		
		return pl.predict(transformedX, quantile);
		
//			Entry<Double, PrecisionSpline> up = cache[cacheIndex].ceilingEntry(fraction);
//			Entry<Double, PrecisionSpline> down = cache[cacheIndex].floorEntry(fraction);
//			if (up==null || down==null || !up.getValue().preciseDown) {
//				SmoothSplineResult mean = computeMeanSpline(x,prob);
//				SmoothSplineResult var = computeVarSpline(mean,x,prob);
//				PrecisionSpline pl = new PrecisionSpline(mean,var);
//				TreeMap<Double, PrecisionSpline> tm = new TreeMap<>(cache[cacheIndex]); // copy the whole cache, do the change on the local version and save back; may overwrite changes from other threads, but this works without synchronization!
//				tm.put(fraction, pl);
//				cache[cacheIndex] = tm;
//				double re = pl.predict(transformedX,quantile);
//				
//				if (up!=null && down!=null) {
//					double m = 0.5*(up.getValue().predict(transformedX,quantile)+down.getValue().predict(transformedX,quantile));
//					if (Math.abs(m-re)<precision) {
//						up.getValue().preciseDown = true;
//						pl.preciseDown = true;
//					}
//				}
//				return re;
//			}
//			if (up==down)
//				return up.getValue().predict(transformedX, quantile);
//			
//			return 0.5*(up.getValue().predict(transformedX,quantile)+down.getValue().predict(transformedX,quantile));
	}
	
	
	private SmoothSplineResult computeMeanSpline(Supplier<double[]> x, ToDoubleFunction<SingleNoiseModel> prob) {
		double[] xx = x.get();
		double[] yy = EI.wrap(data).mapToDouble(prob).toDoubleArray();
//		try {
//			LineWriter p = new LineOrientedFile("spline").write();
//			for (int i=0; i<xx.length; i++)
//				p.writef("%.5f\t%.5f\n", xx[i],yy[i]);
//				p.close();
//		} catch (IOException e) {
//		}
		SmoothSplineResult spl = SmoothSpline.fitDFMatch(xx, yy, 10);
		return spl; 
	}
	
	private SmoothSplineResult computeVarSpline(SmoothSplineResult mean, Supplier<double[]> x, ToDoubleFunction<SingleNoiseModel> prob) {
		double[] xx = x.get();
		double[] yy = EI.wrap(data).mapToDouble(prob).toDoubleArray();
		for (int i=0; i<xx.length; i++) {
			yy[i] = SmoothSpline.predict(mean,xx[i],0)-yy[i];
			yy[i] = yy[i]*yy[i];
		}
		SmoothSplineResult spl = SmoothSpline.fitDFMatch(xx, yy, 3);
		return spl; 
	}
	

	public double[] getX() {
		if (x==null) 
			x = getTransformedMeans();
		return x;
	}
	
	public double[] getX2() {
		if (x2==null) 
			x2 = getTransformedAbortiveStartLevels();
		return x2;
	}
	
	public double[] getTransformedMeans() {
		return EI.wrap(data).mapToDouble(m->transform(m.mean)).toDoubleArray();
	}
	
	public double[] getTransformedAbortiveStartLevels() {
		return EI.wrap(data).mapToDouble(m->transform2(m.startRegionMean)).toDoubleArray();
	}
	
	public double[] getRawAbortiveStartLevels() {
		return EI.wrap(data).mapToDouble(m->m.startRegionMean).toDoubleArray();
	}

	public double[] getY0(double fraction) {
		return EI.wrap(data).mapToDouble(s->s.getProbability0(fraction)).toDoubleArray();
	}
	
	public double[] getY1(double fraction) {
		return EI.wrap(data).mapToDouble(s->s.getProbability1(fraction)).toDoubleArray();
	}
	
	public double[] getY2(double fraction) {
		return EI.wrap(data).mapToDouble(s->s.getProbability2(fraction)).toDoubleArray();
	}
	
	public double[] getYStart(double fraction) {
		return EI.wrap(data).mapToDouble(s->s.getProbability0(fraction*s.mean/s.startRegionMean)).toDoubleArray();
	}
	
	public double[] getDownSinh() {
		return EI.wrap(data).mapToDouble(s->s.computeSinhMean()).toDoubleArray();
	}

	
	public StepFunction getEmpiricalDownToStartDistribution() {
		if (edst==null) {
			NumericSample sam = new NumericSample();
			double[] x = getRawAbortiveStartLevels();
			double[] y = getDownSinh();
			for (int i=0; i<x.length; i++) 
				sam.add(Math.log(y[i]/x[i])/Math.log(2));
			edst = sam.ecdf();
		}
		return edst;
	}
	
	
	private static double transform(double c) {
		return Math.log(c);
	}
	
	private static double transform2(double c) {
		return Math.log(c+Math.sqrt(c*c+1));
	}
	
	public static class SingleNoiseModel implements BinarySerializable, Comparable<SingleNoiseModel> {
	
		private ImmutableReferenceGenomicRegion<Transcript> transcript;
		private double totalFrame0;
		private double mean;
		private double startRegionMean;
		private double[] frame0;
		private double[] frame1;
		private double[] frame2;
		private double start;
		private double beforeStop;
		
		public SingleNoiseModel() {
		}
		
		public SingleNoiseModel(ImmutableReferenceGenomicRegion<Transcript> transcript, double totalFrame0, double mean, double startRegionMean, double start, double beforeStop, double[] frame0, double[] frame1, double[] frame2) {
			super();
			this.start = start;
			this.beforeStop = beforeStop;
			this.transcript = transcript;
			this.totalFrame0 = totalFrame0;
			this.mean = mean;
			this.startRegionMean = startRegionMean;
			this.frame0 = frame0;
			this.frame1 = frame1;
			this.frame2 = frame2;
		}
		public ImmutableReferenceGenomicRegion<Transcript> getTranscript() {
			return transcript;
		}
		public double getTotalFrame0() {
			return totalFrame0;
		}
		public double computeSinhMean() {
			return NumericArray.wrap(frame0.clone()).transform(NumericArrayTransformation.mult(mean)).evaluate(NumericArrayFunction.SinhMean);
		}
		public double[] getFrame0() {
			return frame0;
		}
		public double[] getFrame1() {
			return frame1;
		}
		public double[] getFrame2() {
			return frame2;
		}
		public double getStart() {
			return start;
		}
		public double getBeforeStop() {
			return beforeStop;
		}
		
		public double getProbability0(double gmeanFraction) {
			return getProbability(frame0, gmeanFraction);
		}
		public double getProbability1(double gmeanFraction) {
			return getProbability(frame1, gmeanFraction);
		}
		public double getProbability2(double gmeanFraction) {
			return getProbability(frame2, gmeanFraction);
		}
		
		private static double getProbability(double[] frame, double gmeanFraction) {
			int b = Arrays.binarySearch(frame, gmeanFraction);
			if (b<0) b = -b-1;
			double r = (frame.length-b)/(double)frame.length;
			return r;
//			int c = 0;
//			for (int i=0; i<frame.length; i++)
//				if (frame[i]>gmeanFraction)
//					c++;
//			if (c/(double)frame.length!=r)
//				throw new RuntimeException(r+" "+(c/(double)frame.length));
//			return c/(double)frame.length;
		}

		
	
		@Override
		public String toString() {
			return "SingleNoiseModel [transcript=" + transcript + ", totalFrame0=" + totalFrame0 + ", mean=" + mean
					+ ", frame1=" + Arrays.toString(frame1) + ", frame2=" + Arrays.toString(frame2) + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(frame0);
			result = prime * result + Arrays.hashCode(frame1);
			result = prime * result + Arrays.hashCode(frame2);
			long temp;
			temp = Double.doubleToLongBits(mean);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(totalFrame0);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((transcript == null) ? 0 : transcript.hashCode());
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
			SingleNoiseModel other = (SingleNoiseModel) obj;
			if (!Arrays.equals(frame0, other.frame0))
				return false;
			if (!Arrays.equals(frame1, other.frame1))
				return false;
			if (!Arrays.equals(frame2, other.frame2))
				return false;
			if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean))
				return false;
			if (Double.doubleToLongBits(totalFrame0) != Double.doubleToLongBits(other.totalFrame0))
				return false;
			if (transcript == null) {
				if (other.transcript != null)
					return false;
			} else if (!transcript.equals(other.transcript))
				return false;
			return true;
		}

		@Override
		public void serialize(BinaryWriter out) throws IOException {
			FileUtils.writeReferenceSequence(out,transcript.getReference());
			FileUtils.writeGenomicRegion(out,transcript.getRegion());
			transcript.getData().serialize(out);
			out.putDouble(totalFrame0);
			out.putDouble(mean);
			out.putDouble(startRegionMean);
			out.putDouble(start);
			out.putDouble(beforeStop);
			FileUtils.writeDoubleArray(out, frame0);
			FileUtils.writeDoubleArray(out, frame1);
			FileUtils.writeDoubleArray(out, frame2);
		}
	
		@Override
		public void deserialize(BinaryReader in) throws IOException {
			transcript = new ImmutableReferenceGenomicRegion<>(FileUtils.readReferenceSequence(in), FileUtils.readGenomicRegion(in),new Transcript());
			transcript.getData().deserialize(in);
			totalFrame0 = in.getDouble();
			mean = in.getDouble();
			startRegionMean = in.getDouble();
			start = in.getDouble();
			beforeStop = in.getDouble();
			frame0 = FileUtils.readDoubleArray(in);
			frame1 = FileUtils.readDoubleArray(in);
			frame2 = FileUtils.readDoubleArray(in);
		}
	
		@Override
		public int compareTo(SingleNoiseModel o) {
			int re = Double.compare(totalFrame0, o.totalFrame0);
			if (re==0) re = transcript.compareTo(o.transcript);
			return re;
		}
	
	}
	
	
}
