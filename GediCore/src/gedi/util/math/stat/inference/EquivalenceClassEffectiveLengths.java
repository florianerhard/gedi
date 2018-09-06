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
package gedi.util.math.stat.inference;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.mutable.MutablePair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import jdistlib.Normal;

public class EquivalenceClassEffectiveLengths<T> {
	
	private double[] eff;
	private HashMap<ReferenceSequence,EquivalenceClassEffectiveLengthSingleReference> len = new HashMap<>();
	private HashMap<T,MutablePair<ReferenceSequence, Integer>> index = new HashMap<>();
	
	public EquivalenceClassEffectiveLengths(Iterator<? extends ReferenceGenomicRegion<T>> regions, double[] eff) {
		this.eff = eff;
		
		HashMap<ReferenceSequence,ArrayList<GenomicRegion>> regmap = new HashMap<>();
		while (regions.hasNext()) {
			ReferenceGenomicRegion<T> n = regions.next();

			ArrayList<GenomicRegion> l = regmap.computeIfAbsent(n.getReference(),x->new ArrayList<>());
			index.put(n.getData(), new MutablePair<>(n.getReference(),l.size()));
			l.add(n.getRegion());
		}
		
		for (ReferenceSequence ref : regmap.keySet()) 
			len.put(ref, new EquivalenceClassEffectiveLengthSingleReference(regmap.get(ref).toArray(new GenomicRegion[0])));
	}
	
	public double getEffectiveLength(T[] o) {
		for (T oo : o)
			if (!index.containsKey(oo))
				throw new RuntimeException("Reference not in index: "+oo);
		
		ReferenceSequence ref = index.get(o[0]).Item1;
		BitSet bs = new BitSet();
		for (T oo : o)
			if (!index.get(oo).Item1.equals(ref))
				throw new RuntimeException("Not supported to supply regions from different references: "+StringUtils.toString(o));
			else
				bs.set(index.get(oo).Item2);
		return len.get(ref).effectiveLength(bs, eff);
	}
	
	
	public static double[] preprocessEff(double[] readperlen) {
		return preprocessEff(readperlen,1000000);
	}
	
	public static double[] preprocessEff(double[] readperlen, int maxLen) {
		
		double[] dens = new double[maxLen+1];
		System.arraycopy(readperlen, 0, dens, 0, Math.min(dens.length, readperlen.length));
		ArrayUtils.normalize(dens);
		
		double[] cum = new double[maxLen+1];
		for (int i=1; i<=maxLen; i++) {
			cum[i] = cum[i-1]+dens[i];
		}
		for (int i=1; i<=maxLen; i++) {
			dens[i]/=cum[maxLen];
			cum[i]/=cum[maxLen];
		}
		
		double[] re = new double[maxLen+1];
		for (int i=1; i<=maxLen; i++)
			re[i] = re[i-1] + cum[i-1] + dens[i];
		
		return re;
	}

	public static double[] preprocessEff(double mean, double sd) {
		return preprocessEff(mean,sd,1000000);
	}
	public static double[] preprocessEff(double mean, double sd, int maxLen) {
		
		double[] dens = new double[maxLen+1];
		double[] cum = new double[maxLen+1];
		for (int i=1; i<=maxLen; i++) {
			dens[i] = Normal.density(i, mean, sd, false);
			cum[i] = cum[i-1]+dens[i];
		}
		for (int i=1; i<=maxLen; i++) {
			dens[i]/=cum[maxLen];
			cum[i]/=cum[maxLen];
		}
		
		double[] re = new double[maxLen+1];
		for (int i=1; i<=maxLen; i++)
			re[i] = re[i-1] + cum[i-1] + dens[i];
		
		return re;
	}
	
	

}
