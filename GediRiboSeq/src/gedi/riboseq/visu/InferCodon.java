package gedi.riboseq.visu;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.math.stat.kernel.GaussianKernel;
import gedi.util.math.stat.kernel.PreparedIntKernel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=PixelBlockToValuesMap.class)
public class InferCodon implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,AlignedReadsData>,PixelBlockToValuesMap> {

	
	private PreparedIntKernel kernel = new GaussianKernel(15).prepare();
	
	private RiboModel[] models;
	private ContrastMapping mapping;
	private boolean merge = true;
	
	private Strand fixedStrand;
	
	public InferCodon(String path) throws IOException {
		this(path,null);
	}
	
	public InferCodon(String path, Strand strand) throws IOException {
		this.fixedStrand = strand;
		ArrayList<RiboModel> models = new ArrayList<RiboModel>();
		PageFile f = new PageFile(path);
		while (!f.eof()) {
			RiboModel model = new RiboModel();
			model.deserialize(f);
			models.add(model);
		}
		f.close();
		this.models = models.toArray(new RiboModel[0]);
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
		
//		double[][] full = new double[mapping.getNumMergedConditions()][region.getTotalLength()];
//		double[][] full1;
//		for (int c=0; c<full.length; c++) 
//			Arrays.fill(full[c], 1);
//		
//		for (int em=0; em<10; em++) {
//			
//			full1 = full;
//			full = new double[mapping.getNumMergedConditions()][region.getTotalLength()];
//
////			HashMap<MutableTriple<GenomicRegion, Integer, Boolean>,MutableDouble> dmap = new HashMap<MutableTriple<GenomicRegion,Integer,Boolean>, MutableDouble>(); 
//			
//			Iterator<Entry<GenomicRegion, AlignedReadsData>> it = data.entrySet().iterator();
//			while (it.hasNext()) {
//				Entry<GenomicRegion, AlignedReadsData> n = it.next();
//				
//				for (int c=0; c<n.getValue().getNumConditions(); c++) {
//					double[] f1c = full1[mapping.getMappedIndex(c)];
//					for (int d=0; d<n.getValue().getDistinctSequences(); d++) {
//						CleavageModel mod = models.length==1?models[0]:models[c];
//						double[] m = mod.getModel(RiboUtils.hasLeadingMismatch(n.getValue(), d), n.getKey().getTotalLength());
//						for (int i=0; i<m.length; i++) {
//							int pos = n.getKey().map(strand==Strand.Minus?m.length-1-i:i);
//							if (region.contains(pos)) {
//								int pi = region.induce(pos);
//								double w = pi>0 && pi<f1c.length-1?f1c[pi]/
//										(f1c[pi+1]+f1c[pi-1]+f1c[pi]):1;
//								full[mapping.getMappedIndex(c)][region.induce(pos)]+=w*m[i]*n.getValue().getCount(d, c)/n.getValue().getMultiplicity(d);
////								if (pos>=28574 && pos<=28579){
////									MutableTriple<GenomicRegion, Integer, Boolean> tr = new MutableTriple<>(n.getKey(),pos,RiboUtils.hasLeadingMismatch(n.getValue(), d));
////									dmap.computeIfAbsent(tr, x->new MutableDouble()).N+=m[i]*n.getValue().getCount(d, c)/n.getValue().getMultiplicity(d);
////								}
//							}
//						}
//					}
//				}
//			}
////			for (MutableTriple<GenomicRegion, Integer, Boolean> t : dmap.keySet()) {
////				if (dmap.get(t).N>1)
////					System.out.printf("%s\t%d\t%b\t%.2f\n",t.Item1,t.Item2,t.Item3,dmap.get(t).N);
////			}
////			System.out.println();
//		}
		
		
		
		double[][] full = new double[mapping.getNumMergedConditions()][region.getTotalLength()];
		
		Iterator<Entry<GenomicRegion, AlignedReadsData>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<GenomicRegion, AlignedReadsData> n = it.next();
			
			for (int c=0; c<n.getValue().getNumConditions(); c++)
				for (int d=0; d<n.getValue().getDistinctSequences(); d++) {
					RiboModel mod = models.length==1?models[0]:models[c];
					
					boolean lm = RiboUtils.hasLeadingMismatch(n.getValue(), d);
					int l = n.getKey().getTotalLength();
					if (lm && !RiboUtils.isLeadingMismatchInsideGenomicRegion(n.getValue(), d))
						l++;
					
					double[] m = mod.getModel(lm, l);
					for (int i=0; i<m.length; i++) {
						int pos = n.getKey().map(strand==Strand.Minus?m.length-1-i:i);
						if (region.contains(pos))
							full[mapping.getMappedIndex(c)][region.induce(pos)]+=m[i]*n.getValue().getCount(d, c)/n.getValue().getMultiplicity(d);
					}
						
			}
			
		}
		
		
//		// Frame kernel
//		int l = (kernel.getMinAffectedIndex(0)/3)*3;
//		int r = (kernel.getMaxAffectedIndex(0)/3)*3;
//		double[] tmp = new double[region.getTotalLength()];
//		for (int c=0; c<full.length; c++) {
//			
//			for (int ite=0; ite<10; ite++) {
//				double[] a = full[c];
//				for (int p=0; p<a.length; p++) {
//					for (int w=l; w<=r; w+=3) {
//						if (p+w>=0 && p+w<a.length)
//							tmp[p]+=kernel.applyAsDouble(w)*a[p+w];
//					}
//				}
//				
//				Arrays.fill(full[c],0);
//				it = data.entrySet().iterator();
//				while (it.hasNext()) {
//					Entry<GenomicRegion, AlignedReadsData> n = it.next();
//					
//					for (int c2 : mapping.getMergeConditions(c))
//					for (int d=0; d<n.getValue().getDistinctSequences(); d++) {
//						CleavageModel mod = models.length==1?models[0]:models[c2];
//						double[] m = mod.getModel(RiboUtils.hasLeadingMismatch(n.getValue(), d), n.getKey().getTotalLength());
//						for (int i=0; i<m.length; i++) {
//							int pos = n.getKey().map(strand==Strand.Minus?m.length-1-i:i);
//							if (region.contains(pos)){
//								int lw = region.induce(pos)-1; if (lw<0) lw+=3;
//								int rw = region.induce(pos)+1; if (rw>=region.getTotalLength()) rw-=3;
//								double itw = 2*tmp[region.induce(pos)]/(tmp[region.induce(pos)]+tmp[lw]+tmp[rw]);
//							
//								full[c][region.induce(pos)]+=itw*m[i]*n.getValue().getCount(d, c2)/n.getValue().getMultiplicity(d);
//							}
//						}
//					}
//					
//				}
//				
//				
//				Arrays.fill(tmp, 0);
//			}
//		}
		
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, 3*mapping.getNumMergedConditions(), NumericArrayType.Double);
		
		for (int p=0; p<region.getTotalLength(); p++) {
			int frame = region.map(p)%3;
			int ind = re.getBlockIndex(reference, region.map(p));
			NumericArray val = re.getValues(ind);
			
			for (int c=0; c<full.length; c++) {
				val.setDouble(c*3+frame, Math.max(val.getDouble(c*3+frame),full[c][p]));
			}
		}
		for (int i=1; i<re.getBlocks().size(); i++) {
			if (re.getBlock(i).getBasePairs()==1 && re.getBlock(i-1).getBasePairs()==1) {
				int bp = re.getBlock(i).getStartBp();
				if (bp%3!=0) 
					for (int c=0; c<re.getValues(i).length(); c+=3)
						re.getValues(i).setDouble(c+0, re.getValues(i-1).getDouble(c+0));
				
				if (bp%3!=1) 
					for (int c=0; c<re.getValues(i).length(); c+=3)
						re.getValues(i).setDouble(c+1, re.getValues(i-1).getDouble(c+1));
				
				if (bp%3!=2) 
					for (int c=0; c<re.getValues(i).length(); c+=3)
						re.getValues(i).setDouble(c+2, re.getValues(i-1).getDouble(c+2));
			}
		}
		
		return re;
		
		
	}

}
