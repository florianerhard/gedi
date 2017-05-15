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

package gedi.virology.hsv1annotation.kinetics.quantifier;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.riboseq.inference.codon.Codon;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.math.stat.inference.EquivalenceClassCountEM;
import gedi.util.math.stat.inference.EquivalenceClassMinimizeFactors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;

public class DeconvolveRiboSeqQuantifier extends KineticQuantifier {

	private Function<ReferenceGenomicRegion<?>,ExtendedIterator<Codon>> codons;
	private boolean em;
	
	public DeconvolveRiboSeqQuantifier(String label,
			String repl, NumericArray sizeFactors, Function<ReferenceGenomicRegion<?>,ExtendedIterator<Codon>> codons, boolean em ) {
		super(label,repl, sizeFactors);
		this.codons = codons;
		this.em = em;
	}

	@Override
	public NumericArray[] quantify(ReferenceGenomicRegion<?> fullRegion, ReferenceGenomicRegion<NameAnnotation>[] regions) {
		
		if (regions.length==0) return new NumericArray[0];
		
		HashMap<HashSet<ReferenceGenomicRegion<?>>,double[]> count = new HashMap<>();
		
		HashSet<ReferenceGenomicRegion<?>> equi = new HashSet<ReferenceGenomicRegion<?>>();
		double[] buff = null;
		
		for (Codon c : codons.apply(fullRegion).loop()) {
			// identify the Orfs consistent with c
			EI.wrap(regions)
				.filter(o->o.getRegion().containsUnspliced(c))
				.filter(orf->orf.induce(c).getStart()%3==0)
				.toCollection(equi);
		
			if (!equi.isEmpty()) {
				double[] b = count.get(equi);
				if (b==null) count.put(new HashSet<>(equi), b=new double[c.getActivity().length]);
				buff = downsampling.getDownsampled(c.getActivity(), buff);
				ArrayUtils.add(b, buff);
			}
			
			equi.clear();
		}
		
		NumericArray[] re = new NumericArray[regions.length];
		for (int i=0; i<re.length; i++)
			re[i] = NumericArray.createMemory(buff==null?0:buff.length, NumericArrayType.Double);
		if (buff==null) return re;
		
		
		ReferenceGenomicRegion<?>[][] E = new ReferenceGenomicRegion[count.size()][];
		int index = 0;
		for (HashSet<ReferenceGenomicRegion<?>> k : count.keySet()) 
			E[index++] = k.toArray(new ReferenceGenomicRegion[0]);
		
		HashMap<ReferenceGenomicRegion<?>, Integer> indexMap = ArrayUtils.createIndexMap(regions);
		
		for (int c=0; c<buff.length; c++) {
			double[] a = new double[E.length];
			
			HashMap<ReferenceGenomicRegion<?>, Double> p = new HashMap<ReferenceGenomicRegion<?>, Double>();
			if (em) {
				index = 0;
				for (HashSet<ReferenceGenomicRegion<?>> k : count.keySet()) 
					a[index++] = count.get(k)[c];
				EquivalenceClassCountEM<ReferenceGenomicRegion<?>> em = new EquivalenceClassCountEM<>(E, a, r->r.getRegion().getTotalLength()/3);
				em.compute(100, 1000, (r,pi)->p.put(r,pi));//re[indexMap.get(r)].setDouble(uc,pi));
			} else {
				
				index = 0;
				for (HashSet<ReferenceGenomicRegion<?>> k : count.keySet()) 
					a[index++] = count.get(k)[c]/effectiveLength(k);
				EquivalenceClassMinimizeFactors<ReferenceGenomicRegion<?>> macoco = new EquivalenceClassMinimizeFactors<>(E, a);
				macoco.compute((r,pi)->p.put(r,pi));//re[indexMap.get(r)].setDouble(uc,pi));
			}
			
			for (HashSet<ReferenceGenomicRegion<?>> e : count.keySet()) {
				double sum = 0;
				for (ReferenceGenomicRegion<?> t : e)
					sum+=p.computeIfAbsent(t, x->0.0);
				
				for (ReferenceGenomicRegion<?> t : e)
					re[indexMap.get(t)].add(c,count.get(e)[c]*p.get(t)/sum);
			}
			
			
			for (int i=0; i<re.length; i++)
				re[i].setDouble(c, re[i].getDouble(c)/regions[i].getRegion().getTotalLength()*3);
			
		}
		
		return re;
	}
	
	private double effectiveLength(HashSet<ReferenceGenomicRegion<?>> k) {
		ArrayGenomicRegion intersection = EI.wrap(k).reduce(new ArrayGenomicRegion(0,Integer.MAX_VALUE),(r,u)->u.intersect(r.getRegion()));
		int re = intersection.getTotalLength()/3;
		return re;
	}

	

}
