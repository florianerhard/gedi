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
package gedi.lfc.localtest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.special.Downsampling;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.genomic.Coverage;
import gedi.util.genomic.CoverageAlgorithm;
import gedi.util.math.stat.testing.DirichletLikelihoodRatioTest;
import gedi.util.mutable.MutableDouble;
import jdistlib.BetaBinomial;
import jdistlib.Uniform;
import jdistlib.disttest.DistributionTest;
import jdistlib.disttest.TestKind;

public class LocalCoverageTest {

	private Genomic genomic;
	private Function<ReferenceGenomicRegion<?>,ExtendedIterator<ReferenceGenomicRegion<NumericArray>>> quantifier;
	private HashMap<String, ArrayList<ImmutableReferenceGenomicRegion<Transcript>>> gene2Trans;
	
	
	
	public LocalCoverageTest(Genomic genomic, GenomicRegionStorage<AlignedReadsData> storage, int minLength, Downsampling down) {
		this(genomic,r->storage.ei(r).filter(read->read.getRegion().getTotalLength()>=minLength).map(
			read->new ImmutableReferenceGenomicRegion<>(
					read.getReference(), 
					read.getRegion(),
					down.downsample(read.getData().getTotalCountsForConditions(null,ReadCountMode.Weight)))
			));
	}
	
	public LocalCoverageTest(Genomic genomic, Function<ReferenceGenomicRegion<?>,ExtendedIterator<ReferenceGenomicRegion<NumericArray>>> quantifier) {
		this.genomic = genomic;
		this.quantifier = quantifier;
		
		gene2Trans = genomic.getTranscripts().ei().indexMulti(t->t.getData().getGeneId(), t->t);
	}
	
	public LocalTestResult test(String gene){
		ReferenceGenomicRegion<String> rr = (ReferenceGenomicRegion<String>) genomic.getNameIndex().getUniqueWithPrefix(gene);
		return test(genomic.getGenes().ei(rr).filter(x->x.compareTo(rr)==0).getUniqueResult(true, true));		
	}
	public LocalTestResult test(ImmutableReferenceGenomicRegion<String> gene){
		ArrayList<ImmutableReferenceGenomicRegion<Transcript>> tr = gene2Trans.get(gene.getData());
		
		
		NumericArray totalRc = null;
		
		GenomicRegion covered = new ArrayGenomicRegion();
		
		for (ReferenceGenomicRegion<NumericArray> read : quantifier.apply(gene)
			.loop()) {
			if (EI.wrap(tr).filter(t->t.getRegion().containsUnspliced(read.getRegion())).count()>0) {
				// consistent with a transcript: count!
				if (totalRc==null)
					totalRc = read.getData().copy();
				else
					totalRc.add(read.getData());
				
				covered = covered.union(read.getRegion());
			}
		}
		
		CoverageAlgorithm cov = new CoverageAlgorithm(gene.getReference(), covered);
		quantifier.apply(gene).forEachRemaining(r->cov.add(r));
		
		
		DoubleArrayList pvalList = new DoubleArrayList();
		MemoryIntervalTreeStorage<MutableDouble> pvals = new MemoryIntervalTreeStorage<>(MutableDouble.class);
		int n = covered.getTotalLength();
		for (int i=0; i<n; i++) {
//				if (cov.getParentRegion().map(new ArrayGenomicRegion(i,i+1)).getStart()==20457140)
//					System.out.println();
			double pval = test(cov.getCoverages(i),totalRc);
			if (!Double.isNaN(pval)) {
				pvals.add(
						gene.getReference(), 
						cov.getParentRegion().map(new ArrayGenomicRegion(i,i+1)), 
						new MutableDouble(pval)
						);
				pvalList.add(pval);
			}
		}
		
		double pval = pvals.size()>10?DistributionTest.kolmogorov_smirnov_test(pvalList.toDoubleArray(), new Uniform(0, 1), TestKind.GREATER)[1]:Double.NaN;
		
		return new LocalTestResult(gene,pval,pvals);
	}
	
	private double test(NumericArray a, NumericArray total) {
		double[] A = a.toDoubleArray();
		double[] B = total.toDoubleArray();
		for (int i=0; i<A.length; i++)
			B[i]-=A[i];
		
		return DirichletLikelihoodRatioTest.testMultinomials(1, A,B);
	}

	
}
