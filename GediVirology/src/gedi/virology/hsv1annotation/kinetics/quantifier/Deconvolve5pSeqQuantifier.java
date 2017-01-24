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
import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.math.stat.inference.EquivalenceClassCountEM;
import gedi.util.math.stat.inference.EquivalenceClassMinimizeFactors;

import java.util.HashMap;
import java.util.HashSet;

public class Deconvolve5pSeqQuantifier extends KineticQuantifier {

	private DiskGenomicNumericProvider tss;
	private int skip5p = 0;
	private boolean em;
	
	public Deconvolve5pSeqQuantifier(String label,
			String repl, NumericArray sizeFactors,DiskGenomicNumericProvider tss, boolean em ) {
		super(label,repl, sizeFactors);
		this.tss = tss;
		this.em = em;
	}
	
	public Deconvolve5pSeqQuantifier skip5p(int skip5p) {
		this.skip5p = skip5p;
		return this;
	}

	@Override
	public NumericArray[] quantify(ReferenceGenomicRegion<?> fullRegion, ReferenceGenomicRegion<NameAnnotation>[] regions) {
		if (regions.length==0) return new NumericArray[0];
		
		NumericArray[] re = new NumericArray[regions.length];
		for (int i=0; i<re.length; i++)
			re[i] = NumericArray.createMemory(tss.getNumDataRows(), NumericArrayType.Double);
		
		double[] buff = new double[tss.getNumDataRows()];
		
		IntervalTree<GenomicRegion, ReferenceGenomicRegion<?>> tree = new IntervalTree<GenomicRegion, ReferenceGenomicRegion<?>>(regions[0].getReference());
		GenomicRegion union = new ArrayGenomicRegion();
		for (ReferenceGenomicRegion<?> r : regions) {
			tree.put(r.getRegion(), r);
			union = union.union(r.getRegion());
		}
		
		
		
		HashMap<HashSet<ReferenceGenomicRegion<?>>,double[]> count = new HashMap<>();
		
		HashSet<ReferenceGenomicRegion<?>> equi = new HashSet<ReferenceGenomicRegion<?>>();
		PositionNumericIterator pit = tss.iterateValues(new ImmutableReferenceGenomicRegion<Void>(tree.getReference(), union));
		while (pit.hasNext()) {
			int pos = pit.nextInt();
			// identify the regions where pos is in
			EI.wrap(tree.iterateIntervalsIntersecting(pos, pos, r->r.contains(pos))).map(e->e.getValue()).toCollection(equi);
			// check whether it is TSS of any
			if (!equi.isEmpty() && EI.wrap(equi).filter(r->r.induce(pos)<skip5p).count()==0) {
				double[] b = count.get(equi);
				if (b==null) count.put(new HashSet<>(equi), b=new double[tss.getNumDataRows()]);
				pit.getValues(buff);
				downsampling.getDownsampled(buff, buff);
				ArrayUtils.add(b, buff);
			}
			equi.clear();
		}
		
		ReferenceGenomicRegion<?>[][] E = new ReferenceGenomicRegion[count.size()][];
		int index = 0;
		for (HashSet<ReferenceGenomicRegion<?>> k : count.keySet()) 
			E[index++] = k.toArray(new ReferenceGenomicRegion[0]);
		
		HashMap<ReferenceGenomicRegion<?>, Integer> indexMap = ArrayUtils.createIndexMap(regions);
		
		for (int c=0; c<tss.getNumDataRows(); c++) {
			double[] a = new double[E.length];
			
			
			HashMap<ReferenceGenomicRegion<?>, Double> p = new HashMap<ReferenceGenomicRegion<?>, Double>();
			if (em) {
				index = 0;
				for (HashSet<ReferenceGenomicRegion<?>> k : count.keySet()) 
					a[index++] = count.get(k)[c];
				EquivalenceClassCountEM<ReferenceGenomicRegion<?>> em = new EquivalenceClassCountEM<>(E, a, r->r.getRegion().getTotalLength()-skip5p);
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
				re[i].setDouble(c, re[i].getDouble(c)/(regions[i].getRegion().getTotalLength()-skip5p));
		}
		
		return re;
	}
	
	private double effectiveLength(HashSet<ReferenceGenomicRegion<?>> k) {
		ArrayGenomicRegion intersection = EI.wrap(k).collect(new ArrayGenomicRegion(0,Integer.MAX_VALUE),(r,u)->u.intersect(r.getRegion()));
		int re = intersection.getTotalLength();
		if (EI.wrap(k).filter(r->r.induce(intersection).getStart()==0).count()>0)
			re-=skip5p;
		return re;
	}

	

}
