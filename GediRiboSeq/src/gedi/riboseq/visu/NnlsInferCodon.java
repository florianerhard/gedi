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
package gedi.riboseq.visu;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ConditionMappedAlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.cleavage.SimpleCodonModel;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.DoubleArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.math.optim.NNLS;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.CholeskyDecomposition;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=PixelBlockToValuesMap.class)
public class NnlsInferCodon implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,AlignedReadsData>,PixelBlockToValuesMap> {

	
	private RiboModel[] models;
	private ContrastMapping mapping;
	private boolean merge = true;
	
	private Strand fixedStrand;
	
	
	public NnlsInferCodon(String path, Strand strand) throws IOException {
		this.fixedStrand = strand;
		models = RiboModel.fromFile(path, false);
	}
	
	public void setMerge(boolean merge) {
		this.merge = merge;
	}
	
			
	private void setMerge(boolean merge, AlignedReadsData data){
		if (merge) {
			mapping = new ContrastMapping();
			int c = data.getNumConditions();
			for (int i=0; i<c; i++)
				mapping.addMapping(i, 0);
		} else {
			mapping = new ContrastMapping();
			int c = data.getNumConditions();
			for (int i=0; i<c; i++)
				mapping.addMapping(i, i);
		}
	}
	
	public void setMapping(int[] indices) {
		mapping = new ContrastMapping();
		for (int i : indices)
				mapping.addMapping(i, 0);
	}
	

	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region, PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, AlignedReadsData> data) {
		
		
		if (data.isEmpty()) return new PixelBlockToValuesMap(pixelMapping, 0, NumericArrayType.Double);
		
		if (mapping==null) setMerge(merge,data.values().iterator().next());
		
		Strand strand = fixedStrand;
		if (strand==null && reference.getStrand()==Strand.Independent)
			throw new RuntimeException("Set fixed strand!");
		
		if (strand==null) strand = reference.getStrand();
		
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, 3*mapping.getNumMergedConditions(), NumericArrayType.Double);

		MutableReferenceGenomicRegion<AlignedReadsData> rgr = new MutableReferenceGenomicRegion<AlignedReadsData>();
		ReferenceSequence rref = reference.toStrand(strand);
		for (int c=0; c<mapping.getNumMergedConditions(); c++) {
			ContrastMapping mmapping = new ContrastMapping();
			for (int o : mapping.getMergeConditions(c))
				mmapping.addMapping(o, 0);
			Supplier<Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>>> reads = ()->EI.wrap(data.entrySet().iterator()).<ReferenceGenomicRegion<AlignedReadsData>>map(e->rgr.set(rref,e.getKey(),new ConditionMappedAlignedReadsData(e.getValue(),mmapping)));
			
			Set<Codon> codons = inferNR(reads,models[0]);
			
			
			
			for (Codon codon : codons) {
				int frame = codon.getStart()%3;
				
				for (int t=0; t<3; t++) {
					int ind = re.getBlockIndex(reference, codon.getStart()+t);
					if (ind>=0 && (t==0 || ind!=re.getBlockIndex(reference, codon.getStart()))) {
						NumericArray val = re.getValues(ind);
						val.setDouble(c*3+frame, Math.max(val.getDouble(c*3+frame),codon.getTotalActivity()));
					}
				}
			}
			
		}
		
		
		return re;
		
		
	}

	private Set<Codon> infer(Supplier<Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>>> reads,
			RiboModel riboModel) {
		
		IntervalTree<GenomicRegion, AlignedReadsData> tree = new IntervalTree<>(null);
		for (ReferenceGenomicRegion<AlignedReadsData> r : EI.wrap(reads.get()).loop()) 
			tree.put(r.getRegion(), r.getData());
		
		GenomicRegion reg = tree.asRegion();
		int N = reg.getTotalLength()+1;
		
		NNLS solver = new NNLS(N*2,N);
		for (ReferenceGenomicRegion<AlignedReadsData> r :tree.ei().loop()) {
			int s = reg.induce(r.getRegion().getStart());
			int e = reg.induceMaybeOutside(r.getRegion().getEnd());
			for (int d=0; d<r.getData().getDistinctSequences(); d++) {
				if (RiboUtils.hasLeadingMismatch(r.getData(), d)) 
					solver.b[s+1]+=r.getData().getTotalCountForDistinct(d, ReadCountMode.Weight);
				else
					solver.b[s]+=r.getData().getTotalCountForDistinct(d, ReadCountMode.Weight);
			}
			solver.b[N+e]+=r.getData().getTotalCountOverall(ReadCountMode.Weight);
		}
		
		
		DoubleArrayList p = new DoubleArrayList();
		IntArrayList offset = new IntArrayList();
		for (int i=0; i<riboModel.getPl().length; i++) {
			if (riboModel.getPl()[i]>0.01) {
				p.add(riboModel.getPl()[i]);
				offset.add(i);
			}
		}
		
		for (int i=0; i<N; i++)
			for (int oi=0; oi<offset.size(); oi++) {
				int j = i+offset.getInt(oi);
				if (j>=0 && j<N)
					solver.a[i][j] = p.getDouble(oi);
			}

		p = new DoubleArrayList();
		offset = new IntArrayList();
		for (int i=0; i<riboModel.getPr().length; i++) {
			if (riboModel.getPr()[i]>0.01) {
				p.add(riboModel.getPr()[i]);
				offset.add(-i-3);
			}
		}
		
		for (int i=0; i<N; i++) 
			for (int oi=0; oi<offset.size(); oi++) {
				int j = i+offset.getInt(oi);
				if (j>=0 && j<N)
					solver.a[i][j] = p.getDouble(oi);
			}

		solver.solve();
		
		HashSet<Codon> re = new HashSet<>();
		for (int i=0; i<solver.x.length; i++) {
			if (solver.x[i]>0.1) 
				re.add(new Codon(reg.map(new ArrayGenomicRegion(i,i+3)), solver.x[i]));
		}
		
		for (int i=solver.nsetp; i<solver.b.length; i++) {
			System.out.println(solver.b[i]);
		}
		System.out.println();
		
		return re;
	}
	

	private Set<Codon> inferNR(Supplier<Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>>> reads,
			RiboModel riboModel) {
		
		IntervalTree<GenomicRegion, AlignedReadsData> tree = new IntervalTree<>(null);
		for (ReferenceGenomicRegion<AlignedReadsData> r : EI.wrap(reads.get()).loop()) 
			tree.put(r.getRegion(), r.getData());
		
		GenomicRegion reg = tree.asRegion();
		int N = reg.getTotalLength()+1;
		
		DoubleMatrix2D A = new SparseDoubleMatrix2D(2*N, N);
		double[] r = new double[2*N];
		for (ReferenceGenomicRegion<AlignedReadsData> rgr :tree.ei().loop()) {
			int s = reg.induce(rgr.getRegion().getStart());
			int e = reg.induceMaybeOutside(rgr.getRegion().getEnd());
			for (int d=0; d<rgr.getData().getDistinctSequences(); d++) {
				if (RiboUtils.hasLeadingMismatch(rgr.getData(), d)) 
					r[s+1]+=rgr.getData().getTotalCountForDistinct(d, ReadCountMode.Weight);
				else
					r[s]+=rgr.getData().getTotalCountForDistinct(d, ReadCountMode.Weight);
			}
			r[N+e]+=rgr.getData().getTotalCountOverall(ReadCountMode.Weight);
		}
		
		
		DoubleArrayList p = new DoubleArrayList();
		IntArrayList offset = new IntArrayList();
		for (int i=0; i<riboModel.getPl().length; i++) {
			if (riboModel.getPl()[i]>0.01) {
				p.add(riboModel.getPl()[i]);
				offset.add(i);
			}
		}
		
		for (int i=0; i<N; i++)
			for (int oi=0; oi<offset.size(); oi++) {
				int j = i+offset.getInt(oi);
				if (j>=0 && j<N)
					A.setQuick(i,j,p.getDouble(oi));
			}

		p = new DoubleArrayList();
		offset = new IntArrayList();
		for (int i=0; i<riboModel.getPr().length; i++) {
			if (riboModel.getPr()[i]>0.01) {
				p.add(riboModel.getPr()[i]);
				offset.add(-i-3);
			}
		}
		
		for (int i=0; i<N; i++) 
			for (int oi=0; oi<offset.size(); oi++) {
				int j = i+offset.getInt(oi);
				if (j>=0 && j<N)
					A.setQuick(i,j,p.getDouble(oi));
			}

		
		int[] indices = EI.seq(0, N).toIntArray();
		double[] c = new double[N];
		Arrays.fill(c, ArrayUtils.sum(r)/c.length);
		boolean changed = true;
		
		while (changed) {
			changed = false;
			c = newton(r,A,c);
			
			IntArrayList notchanged = new IntArrayList();
			for (int i=0; i<c.length; i++) {
				if (c[i]<0) 
					changed = false;
				else
					notchanged.add(i);
			}
			if (changed) {
				A = A.viewSelection(null, notchanged.toIntArray());
				c = ArrayUtils.restrict(c, notchanged.toIntArray());
				indices = ArrayUtils.restrict(indices, notchanged.toIntArray());
			}
		}
		
		
		
		HashSet<Codon> re = new HashSet<>();
		for (int i=0; i<indices.length; i++) {
			if (c[i]>0.1) {
				int pos = indices[i];
				re.add(new Codon(reg.map(new ArrayGenomicRegion(pos,pos+3)), c[i]));
			}
		}
		return re;
	}

	private double lambda = 4;
	private double[] newton(double[] r, DoubleMatrix2D A, double[] c) {

		for (;;) {
			DenseDoubleMatrix2D gradient = new DenseDoubleMatrix2D(c.length, 1);
			
			DoubleMatrix1D Ac = A.zMult(new DenseDoubleMatrix1D(c), new DenseDoubleMatrix1D(c.length));
			
			
			for (int l=0; l<c.length; l++) {
				double total = 0;
				for (int i=0; i<A.rows(); i++) {
					total += r[i]*A.getQuick(i, l)/Ac.getQuick(i) - A.getQuick(i, l);
				}
				gradient.setQuick(l, 0, -(total-lambda));
			}
			
			SparseDoubleMatrix2D mH = new SparseDoubleMatrix2D(c.length,c.length);
			
			for (int l=0; l<c.length; l++) {
				for (int k=0; k<c.length; k++) {
					double total = 0;
					for (int i=0; i<A.rows(); i++) {
						total += r[i]*A.getQuick(i, l)*A.getQuick(i, k)/Ac.getQuick(i)/Ac.getQuick(i);
					}
					mH.setQuick(l, k, total);
				}
			}
			
			DoubleMatrix2D dc = new CholeskyDecomposition(mH).solve(gradient);
			double max = 0;
			for (int i=0; i<c.length; i++) {
				c[i]+=dc.getQuick(i, 0);
				max = Math.max(max, dc.get(i, 0));
			}
			
			if (max<0.1) return c;
		}
		
	}
	


}
