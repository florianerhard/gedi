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
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.cleavage.SimpleCodonModel;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;

import java.io.IOException;
import java.util.Set;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=PixelBlockToValuesMap.class)
public class EmInferCodon implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,AlignedReadsData>,PixelBlockToValuesMap> {

	
	private RiboModel[] models;
	private ContrastMapping mapping;
	private boolean merge = true;
	
	private Strand fixedStrand;
	private double lambda = Double.NaN;
	private double rho = Double.NaN;
	
	
	public EmInferCodon(String path, Strand strand) throws IOException {
		this(path,strand,null);
	}
	
	public EmInferCodon(String path, Strand strand, String simple) throws IOException {
		this.fixedStrand = strand;
		models = RiboModel.fromFile(path, false);
		if (simple!=null && !simple.equals("null"))
			models[0].setSimple(new SimpleCodonModel(StringUtils.split(simple, ' ')));
	}
	
	public void setMerge(boolean merge) {
		this.merge = merge;
	}
	
	public void setRegularization(double lambda) {
		this.lambda = lambda;
	}
	
	public void setPrior(double rho) {
		this.rho = rho;
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
			CodonInference inf = new CodonInference(models);
			if (!Double.isNaN(lambda))
				inf.setRegularization(lambda);
//			if (!Double.isNaN(rho))
//				inf.setRho(rho);
			
			
			ContrastMapping mmapping = new ContrastMapping();
			for (int o : mapping.getMergeConditions(c))
				mmapping.addMapping(o, 0);
			Set<Codon> codons = inf.inferCodons(null,()->EI.wrap(data.entrySet().iterator()).<ReferenceGenomicRegion<AlignedReadsData>>map(e->rgr.set(rref,e.getKey(),new ConditionMappedAlignedReadsData(e.getValue(),mmapping))),null);
			
//			System.out.println(inf.getIterations()+" iterations "+inf.getLastDifference()+" difference");
			
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
		
		
		
//		for (int i=1; i<re.getBlocks().size(); i++) {
//			if (re.getBlock(i).getBasePairs()==1 && re.getBlock(i-1).getBasePairs()==1) {
//				int bp = re.getBlock(i).getStartBp();
//				if (bp%3!=0) 
//					for (int c=0; c<re.getValues(i).length(); c+=3)
//						re.getValues(i).setDouble(c+0, re.getValues(i-1).getDouble(c+0));
//				
//				if (bp%3!=1) 
//					for (int c=0; c<re.getValues(i).length(); c+=3)
//						re.getValues(i).setDouble(c+1, re.getValues(i-1).getDouble(c+1));
//				
//				if (bp%3!=2) 
//					for (int c=0; c<re.getValues(i).length(); c+=3)
//						re.getValues(i).setDouble(c+2, re.getValues(i-1).getDouble(c+2));
//			}
//		}
		
		return re;
		
		
	}

}
