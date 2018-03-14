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
package gedi.util.math.stat.counting;

import gedi.util.datastructure.array.MemoryDoubleArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.decorators.NumericArraySlice;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.math.function.StepFunction;
import gedi.util.mutable.MutableInteger;
import gedi.util.userInteraction.progress.Progress;

import java.io.IOException;

import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;

/**
 * Computes rolling statistics of data over numerical covariates.
 * In Equisize mode, the statistic is computed for the same number of values, in BinCovariate mode, it is computed for a specific range of the covariate values
 * @author erhard
 *
 */
public class RollingStatistics implements BinarySerializable {
	
	
	private DoubleArrayList cov;
	private DoubleArrayList val;
	private Progress progress;
	
	public RollingStatistics() {
		this.cov = new DoubleArrayList();
		this.val = new DoubleArrayList();
	}
	
	public RollingStatistics setProgress(Progress progress) {
		this.progress = progress;
		return this;
	}
	
	public void add(double covariate, double value) {
		cov.add(covariate);
		val.add(value);
	}
	
	public void addAll(double[] covariate, double[] value) {
		cov.addAll(covariate);
		val.addAll(value);
	}
	
	public int size() {
		return cov.size();
	}
	
	public double getCovariateRange() {
		return cov.evaluate(new Max())-cov.evaluate(new Min());
	}
	
	/**
	 * Always includes the first and last halfwindow and all windows that fit in between and are step entries apart from each other
	 * It also takes care of ties in the covariate by always including adding all values for all ties within range
	 * @param halfBinsize
	 * @param step
	 * @return
	 */
	public ExtendedIterator<RollingBin> iterateEquiSize(int halfBinsize, int step) {
		
		if (size()==0) return EI.empty();
		cov.parallelSort(val);
		
		if (size()<halfBinsize+1) {
			NumericArraySlice slice = NumericArray.wrap(val.getRaw(),0,val.size());
			if (size()==1) return EI.wrap(new RollingBin(cov.getDouble(0), slice));
			return EI.wrap(new RollingBin(cov.getDouble(0), slice),new RollingBin(cov.getDouble(size()-1), slice));
		}
		if (size()<halfBinsize*2+1) {
			
			return EI.wrap(
					new RollingBin(cov.getDouble(0), NumericArray.wrap(val.getRaw(),0,halfBinsize+1)),
					new RollingBin(cov.getDouble(size()-1), NumericArray.wrap(val.getRaw(),size()-1-halfBinsize, size())));
		}
		
		IntArrayList ties = new IntArrayList();
		ties.add(0);
		for (int i=1; i<size(); i++)
			if (cov.getDouble(i-1)!=cov.getDouble(i))
				ties.add(i);
		ties.add(size());
		

		ExtendedIterator<RollingBin> ei = new ExtendedIterator<RollingBin>() {
			
			private int i = halfBinsize-step;
			private RollingBin rb;
			private NumericArraySlice slice;
			private boolean dirty = false;
			private boolean lastDone = false;
			
			{
				int start = 0;
				int end = ties.binarySearch(halfBinsize+1);
				if (end<0) end = -end-1;
				slice = NumericArray.wrap(val.getRaw(),ties.getInt(start),ties.getInt(end));
				rb  = new RollingBin(cov.getDouble(0),slice);
			}
			
			@Override
			public boolean hasNext() {
				computeIfNecessary();
				return !Double.isNaN(rb.covariate);
			}

			@Override
			public RollingBin next() {
				computeIfNecessary();
				dirty = true;
				return rb;
			}
			
			private void computeIfNecessary() {
				if (dirty && !Double.isNaN(rb.covariate)) {
					i+=step;
					if (i<size()-halfBinsize) {
						int start = bin(i-halfBinsize,0);
						int end = bin(i+halfBinsize,1)+1;
						
						slice.setSlice(ties.getInt(start), ties.getInt(end));
						rb.covariate = cov.getDouble(i);
					} else if (!lastDone) {
						int start = ties.binarySearch(size()-1-halfBinsize);
						if (start<0) start = -start-2;
						int end = size();
						
						slice.setSlice(ties.getInt(start), end);
						rb.covariate = cov.getDouble(size()-1);
						lastDone = true;
					} else {
						rb.covariate = Double.NaN;
					}
					
					dirty = false;
				}
			}

			private int[][] fastBin = new int[2][];
			private int bin(int s, int which) {
				if (fastBin[which]==null) {
					fastBin[which] = new int[2];
				}
				else if (cov.getDouble(fastBin[which][0])==cov.getDouble(s)) {
					return fastBin[which][1];
				}
				fastBin[which][0] = s;
				fastBin[which][1] = ties.binarySearch(s);
				
				if (fastBin[which][1]<0) fastBin[which][1] = -fastBin[which][1]-2;
				
				return fastBin[which][1];
			}
		};
		int count = (int) Math.ceil((size()-halfBinsize*2)/(double)step)+2;
		if (progress!=null) ei = ei.progress(progress,count, rb->String.format("%.2f: [%.2f,%.2f]",rb.covariate,rb.values.getDouble(0),rb.values.getDouble(rb.getValues().length()-1)));
		return ei;
				
		
	}
	
	public ExtendedIterator<RollingBin> iterateBinCovariate(double binRange, double absoluteOverlap) {
		
		if (size()==0) return EI.empty();
		cov.parallelSort(val);
		
		
		ExtendedIterator<RollingBin> ei = new ExtendedIterator<RollingBin>() {
			
			private RollingBin rb;
			private NumericArraySlice slice = NumericArray.wrap(val.getRaw(), 0, 0);
			private boolean dirty = true;
			
			private int nextIndex = 0;
			
			@Override
			public boolean hasNext() {
				computeIfNecessary();
				return !dirty;
			}

			@Override
			public RollingBin next() {
				computeIfNecessary();
				dirty = true;
				return rb;
			}
			
			private void computeIfNecessary() {
				if (dirty && slice!=null) {
					// search forward in cov until we have the range
					int start = nextIndex;
					int end = start;
					for (; end<cov.size() && cov.getDouble(end)-cov.getDouble(start)<binRange; end++) {
						if (cov.getDouble(end)-cov.getDouble(start)<binRange-absoluteOverlap)
							nextIndex = end+1;
					}
					rb = new RollingBin(cov.getDouble((start+end)/2),slice.setSlice(start, end));
					if (end==cov.size())
						slice=null;
					dirty = false;
				}
			}

		};
		int count = (int) ((cov.getLastDouble()-cov.getDouble(0))/(binRange-absoluteOverlap));
		if (progress!=null) ei = ei.progress(progress,count, rb->String.format("%.2f: [%.2f,%.2f]",rb.covariate,rb.values.getDouble(0),rb.values.getDouble(rb.getValues().length()-1)));
		return ei;
				
		
	}
	
	
	public StepFunction computeEquiSize(int halfBinsize, int step, NumericArrayFunction statistic) {
		
		double[] x = new double[(int) Math.ceil((size()-halfBinsize*2)/(double)step)+2];
		double[] y = new double[(int) Math.ceil((size()-halfBinsize*2)/(double)step)+2];
		MutableInteger ind = new MutableInteger();
		
		iterateEquiSize(halfBinsize, step).forEachRemaining(rb-> {
			x[ind.N] = rb.covariate;
			y[ind.N++] = statistic.applyAsDouble(rb.values);
		});
		
		return new StepFunction(x, y);
	}
	public StepFunction[] computeEquiSize(int halfBinsize, int step, NumericArrayFunction...statistics) {
		if ((int) Math.ceil((size()-halfBinsize*2)/(double)step)+2<=0) {
			StepFunction[] re = new StepFunction[statistics.length];
			for (int i=0; i<re.length; i++)
				re[i] = new StepFunction(new double[]{0},new double[]{0});
			return re;
		}
		double[] x = new double[(int) Math.ceil((size()-halfBinsize*2)/(double)step)+2];
		double[][] y = new double[statistics.length][(int) Math.ceil((size()-halfBinsize*2)/(double)step)+2];
		MutableInteger index = new MutableInteger();
		
		iterateEquiSize(halfBinsize, step).forEachRemaining(rb-> {
			for (int i=0; i<statistics.length; i++)
				y[i][index.N] = statistics[i].applyAsDouble(rb.values);
			x[index.N++] = rb.covariate;
		});
		
		StepFunction[] re = new StepFunction[statistics.length];
		for (int i=0; i<re.length; i++)
			re[i] = new StepFunction(x.clone(),y[i]);
		return re;
		
	}
	
	
	public StepFunction computeBinCovariate(double binRange, double absoluteOverlap, NumericArrayFunction statistic) {
			
		DoubleArrayList x = new DoubleArrayList();
		DoubleArrayList y = new DoubleArrayList();
		
		
		iterateBinCovariate(binRange,absoluteOverlap).forEachRemaining(rb-> {
			x.add(rb.covariate);
			y.add(statistic.applyAsDouble(rb.values));
		});
		
		return new StepFunction(x.toDoubleArray(), y.toDoubleArray());
	}



	@Override
	public void serialize(BinaryWriter out) throws IOException {
		cov.toNumericArray().serialize(out);
		val.toNumericArray().serialize(out);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		MemoryDoubleArray c = new MemoryDoubleArray();
		MemoryDoubleArray v = new MemoryDoubleArray();
		c.deserialize(in);
		v.deserialize(in);
		cov.clear();
		val.clear();
		for (int i=0; i<c.length(); i++)
			cov.add(c.getDouble(i));
		for (int i=0; i<v.length(); i++)
			val.add(v.getDouble(i));
	}
	
	

	public static class RollingBin {
		private double covariate;
		private NumericArraySlice values;
		
		public RollingBin(double covariate, NumericArraySlice values) {
			this.covariate = covariate;
			this.values = values;
		}
		public double getCovariate() {
			return covariate;
		}
		public NumericArraySlice getValues() {
			return values;
		}
		@Override
		public String toString() {
			return covariate + values.toArrayString();
		}

		
		
	}



	

}
