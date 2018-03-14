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
package gedi.lfc;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.ReadCountMode;

import java.util.Arrays;

public abstract class Downsampling {
	
	
	/**
	 * Read counts are summed per contrast, then downsampling is applied
	 * @param data
	 * @param contrast
	 * @param re
	 * @return
	 */
	public double[] getDownsampled(AlignedReadsData data, ContrastMapping contrast, double[] re) {
		if (re==null || re.length!=contrast.getNumMergedConditions()) re = new double[contrast.getNumMergedConditions()];
		Arrays.fill(re, 0);
		
		for (int i=0; i<data.getNumConditions(); i++)
			if (contrast.getMappedIndex(i)!=-1)
				re[contrast.getMappedIndex(i)] += data.getTotalCountForCondition(i, ReadCountMode.Weight);
		
		downsample(re);
		return re;
	}
	
	public double[] getDownsampled(AlignedReadsData data, double[] re) {
		if (re==null || re.length!=data.getNumConditions()) re = new double[data.getNumConditions()];
		Arrays.fill(re, 0);
		
		for (int i=0; i<data.getNumConditions(); i++)
				re[i] += data.getTotalCountForCondition(i, ReadCountMode.Weight);
		
		downsample(re);
		return re;
	}
	
	public double[] getDownsampled(double[] counts, double[] re) {
		if (counts==re) {
			downsample(counts);
			return counts;
		}
		if (re==null || re.length!=counts.length) re = counts.clone();
		else System.arraycopy(counts, 0, re, 0, counts.length);
		downsample(re);
		return re;
	}
	

	private double[] buffBefore;
	
	/**
	 * Read counts are summed per contrast (in both given mappings), then downsampling is applied and finally, reads are summed for each mapping
	 * @param data
	 * @param contrast
	 * @param re
	 * @return
	 */
	public double[] getDownsampled(AlignedReadsData data, ContrastMapping before, ContrastMapping after, double[] re) {
		if (buffBefore==null || buffBefore.length!=before.getNumMergedConditions()) buffBefore = new double[before.getNumMergedConditions()];
		if (re==null || re.length!=after.getNumMergedConditions()) re = new double[after.getNumMergedConditions()];
		Arrays.fill(buffBefore, 0);
		Arrays.fill(re, 0);
		
		for (int i=0; i<data.getNumConditions(); i++) {
			if (before.getMappedIndex(i)!=-1)
				buffBefore[before.getMappedIndex(i)] += data.getTotalCountForCondition(i, ReadCountMode.Weight);
		}
	
		downsample(buffBefore);
		
		for (int i=0; i<buffBefore.length; i++)
			if (after.getMappedIndex(i)!=-1)
				re[after.getMappedIndex(i)]+=buffBefore[i];
		return re;
	}
	
	/**
	 * Each of the v contains the local counts for one condition
	 * @param v
	 * @return
	 */
	public double[][] downsampleVectors(double[]...v) {
		double[] buff = new double[v.length];
		for (int i=0; i<v[0].length; i++) {
			for (int j=0; j<v.length; j++)
				buff[j] = v[j][i];
			downsample(buff);
			for (int j=0; j<v.length; j++)
				v[j][i]=buff[j];
		}
		return v;
	}

	protected abstract void downsample(double[] counts);
	
}
