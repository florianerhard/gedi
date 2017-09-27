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

package gedi.util.math.stat.kernel;

import gedi.util.ArrayUtils;

public class PreparedIntKernel implements Kernel {

	private Kernel kernel;
	private double[] weights;
	private double[] cumWeights;
	private int halfInt;
	
	public PreparedIntKernel(Kernel kernel) {
		this.kernel = kernel;
		halfInt = (int)Math.floor(kernel.halfSize());
		weights = new double[halfInt*2+1];
		for (int i=0; i<weights.length; i++)
			weights[i] = kernel.applyAsDouble(i-halfInt);
		cumWeights = weights.clone();
		ArrayUtils.cumSumInPlace(cumWeights, 1);
	}
	
	
	@Override
	public String name() {
		return kernel.name();
	}
	
	@Override
	public String toString() {
		return name();
	}
	
	public int getMinAffectedIndex(int index) {
		return index-halfInt;
	}
	
	public int getMaxAffectedIndex(int index) {
		return index+halfInt;
	}
	
	@Override
	public double applyAsDouble(double operand) {
		if ((int)operand!=operand) throw new RuntimeException("Only ints allowed!");
		int p = (int)operand;
		if (p<-halfInt||p>halfInt) return 0;
		return weights[p+halfInt];
	}

	@Override
	public double halfSize() {
		return halfInt;
	}

	
	public double applyToArraySlice(double[] a, int pos) {
		
		int start = Math.max(0, pos-halfInt);
		int stop = Math.min(a.length-1, pos+halfInt);
		
		double re = 0;
		for (int i=start; i<=stop; i++)
			re+=weights[i-pos+halfInt]*a[i];
		
		return re;
	}

	
	public double applyToArraySlice(int[] a, int pos) {
		
		int start = Math.max(0, pos-halfInt);
		int stop = Math.min(a.length-1, pos+halfInt);
		
		double re = 0;
		for (int i=start; i<=stop; i++)
			re+=weights[i-pos+halfInt]*a[i];
		
		return re;
	}
	public double applyToArraySlice(long[] a, int pos) {
		
		int start = Math.max(0, pos-halfInt);
		int stop = Math.min(a.length-1, pos+halfInt);
		
		double re = 0;
		for (int i=start; i<=stop; i++)
			re+=weights[i-pos+halfInt]*a[i];
		
		return re;
	}


	public void processInPlace(double[] a, int from, int to) {
		double[] buff = new double[halfInt+1];
		
		for (int i=from; i<to; i++) {
			buff[i%buff.length] = applyToArraySlice(a, i);
			if (i-halfInt>=from)
				a[i-halfInt] = buff[(i+1)%buff.length];
		}
		for (int i=to; i<to+Math.min(halfInt, to-from); i++)
			a[i-halfInt] = buff[(i+1)%buff.length];
	}
	
	
	public void processInPlace(int[] a, int from, int to, double fac) {
		int[] buff = new int[halfInt+1];
		
		for (int i=from; i<to; i++) {
			buff[i%buff.length] = (int) (fac*applyToArraySlice(a, i));
			if (i-halfInt>=from)
				a[i-halfInt] = buff[(i+1)%buff.length];
		}
		for (int i=to; i<to+Math.min(halfInt, to-from); i++)
			a[i-halfInt] = buff[(i+1)%buff.length];
	}

	public void processInPlace(long[] a, int from, int to, double fac) {
		long[] buff = new long[halfInt+1];
		
		for (int i=from; i<to; i++) {
			buff[i%buff.length] = (long) (fac*applyToArraySlice(a, i));
			if (i-halfInt>=from)
				a[i-halfInt] = buff[(i+1)%buff.length];
		}
		for (int i=to; i<to+Math.min(halfInt, to-from); i++)
			a[i-halfInt] = buff[(i+1)%buff.length];
	}

	
	
}
