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
package gedi.core.region.feature.cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;

import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.core.region.feature.features.AbstractFeature;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;



@GenomicRegionFeatureDescription(toType=Void.class)
public class ClusterSplicedReads extends AbstractFeature<Void> {

	private int tolerance = 0;
	private boolean isCopy = false;
	private BiFunction<Object,NumericArray,NumericArray> dataToCounts;
	private int decimals = 2;
	
	private Genomic genomic;
	
	public ClusterSplicedReads(String file) {
		minValues = maxValues = 0;
		minInputs = maxInputs = 0;
		setFile(file);
	}
	
	
	public void setGenomic(Genomic genomic) {
		this.genomic = genomic;
	}
	
	public void setFile(String path) {
		setId(path);
	}

	public void setDecimals(int decimals) {
		this.decimals = decimals;
	}
	
	public void setDataToCounts(
			BiFunction<Object, NumericArray, NumericArray> dataToCounts) {
		this.dataToCounts = dataToCounts;
	}

	public void setTolerance(int tolerance) {
		this.tolerance = tolerance;
	}

	IntervalTree<GenomicRegion,NumericArray> openClusters = null;
	TreeMap<Integer,GenomicRegion> openClustersByStop = null;
	private NumericArray buffer;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void accept_internal(Set<Void> values) {
		buffer = dataToCounts==null?program.dataToCounts(referenceRegion.getData(), buffer):dataToCounts.apply(referenceRegion.getData(), buffer);
		if (anyNonZero(buffer)) {
		
			NumericArray currentCluster = findCluster(referenceRegion);
			currentCluster.add(buffer);
			
			buffer.clear();
		}
	}
	
	
	private boolean anyNonZero(NumericArray a) {
		for (int i=0; i<a.length(); i++)
			if (a.getDouble(i)!=0)
				return true;
		return false;
	}


	private NumericArray findCluster(
			ReferenceGenomicRegion<?> referenceRegion) {
		
		NumericArray re;
		if (openClusters==null) {
			// first cluster
			openClusters = new IntervalTree<GenomicRegion, NumericArray>(referenceRegion.getReference());
			openClusters.put(referenceRegion.getRegion(),re = proto());
			openClustersByStop = new TreeMap<>();
			openClustersByStop.put(referenceRegion.getRegion().getStop(), referenceRegion.getRegion());
		}
		else if (!openClusters.getReference().equals(referenceRegion.getReference())) {
			// first cluster on this chromosome
			writeCluster(openClusters.keySet());
			openClusters = new IntervalTree<GenomicRegion, NumericArray>(referenceRegion.getReference());
			openClusters.put(referenceRegion.getRegion(),re = proto());
			openClustersByStop = new TreeMap<>();
			openClustersByStop.put(referenceRegion.getRegion().getStop(), referenceRegion.getRegion());
		} else {
			ArrayList<GenomicRegion> overlaps = openClusters
				.keys(referenceRegion.getRegion().getStart(), referenceRegion.getRegion().getStop())
				.filter(reg->reg.intersects(referenceRegion.getRegion())).list();
			
			if (overlaps.size()>0) {
				// merge clusters 
				re = openClusters.get(overlaps.get(0));
				GenomicRegion r = overlaps.get(0);
				for (int i=1; i<overlaps.size(); i++) {
					re.add(openClusters.get(overlaps.get(i)));
					r = r.union(overlaps.get(i));
				}
				r = r.union(referenceRegion.getRegion());
				
				for (GenomicRegion reg : overlaps) {
					openClusters.remove(reg);
					openClustersByStop.remove(reg.getStop());
				}
				
				openClusters.put(r, re);
				openClustersByStop.put(r.getStop(), r);

			} else {
				openClusters.put(referenceRegion.getRegion(), re = proto());
				openClustersByStop.put(referenceRegion.getRegion().getStop(), referenceRegion.getRegion());
			}
			
			// prune interval tree (everything that ends left of the current start
			NavigableMap<Integer, GenomicRegion> head = openClustersByStop.headMap(referenceRegion.getRegion().getStart(), false);
			writeCluster(head.values());
			openClusters.keySet().removeAll(head.values());
			head.clear();
			
		}
		return re;
	}


	private NumericArray proto() {
		NumericArray re = buffer.createMemoryCopy();
		re.clear();
		return re;
	}

	private LineOrientedFile out;
	private PageFileWriter bout;
	
	

	private void writeCluster(Collection<GenomicRegion> regs) {
		MutableReferenceGenomicRegion<NumericArray> rgr = new MutableReferenceGenomicRegion<>();
		rgr.setReference(openClusters.getReference());
		for (GenomicRegion reg : regs)
			writeCluster(rgr.setRegion(reg).setData(openClusters.get(reg)));
	}
	
	private void writeCluster(
			MutableReferenceGenomicRegion<NumericArray> cluster) {
		try {
			
			if (cluster.getData().sum()==0) return;
			
			if (isCopy) {
				if (bout==null) {
					File main = new File(getId());
					bout = new PageFileWriter(File.createTempFile(main.getName(), ".tmp", main.getParentFile()).getPath());
				}
				cluster.serialize(bout);
			}
			else {
				if (out==null) {
					out = new LineOrientedFile(getId());
					out.startWriting();
					writeHeader(out);
				}
				
				out.write(cluster.toLocationString());
				if (genomic!=null) {
					out.writef("\t%s", genomic.getGenes().ei(cluster).map(r->r.getData()).concat(","));
				}
				for (int i=0; i<cluster.getData().length(); i++)
						out.writef("\t%s", cluster.getData().formatDecimals(i,decimals));
				out.writeLine();
			}
				
		} catch (IOException e) {
			throw new RuntimeException("Could not write clusters!",e);
		}
		
	}

	
	private void writeHeader(LineOrientedFile out) throws IOException {
		out.writef("Genomic position");
		if (genomic!=null)
			out.writef("\tGenes");
//		for (int i=0; i<inputs.length; i++) 
//			out.writef("\t%s",inputNames[i]);
		
		int l = buffer.length();
		if (program.getLabels()!=null && program.getLabels().length==l)
			for (int i=0; i<program.getLabels().length; i++) 
				out.writef("\t%s",program.getLabels()[i]);
		else {
			for (int i=0; i<l; i++) 
				out.writef("\t%d",i);
		}
		out.writeLine();
	}
	
	@Override
	public void end() {
		super.end();
		if (openClusters!=null)
			writeCluster(openClusters.keySet());
		try {
			if (out!=null)
				out.finishWriting();
			if (bout!=null) {
				bout.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write clusters!",e);
		}
	}
	
	public boolean dependsOnData() {
		return true;
	}
	
	private ExtendedIterator<MutableReferenceGenomicRegion<NumericArray>> iterateFile(boolean deleteOnEnd) throws IOException {
	
		if (buffer==null || bout==null) {
			// not a single feature entered this thread, so this could be!
			return EI.empty();
		}
		
		if (bout==null || !bout.isClosed() || !isCopy || buffer==null) throw new RuntimeException("Illegal operation: "+(bout==null)+" "+(bout!=null && !bout.isClosed())+" "+!isCopy+" "+(buffer==null));
		
		MutableReferenceGenomicRegion<NumericArray> mut = new MutableReferenceGenomicRegion<NumericArray>();
		mut.setData(buffer.createMemoryCopy().clear());
		
		ExtendedIterator<MutableReferenceGenomicRegion<NumericArray>> re = bout.read(false).iterator(()->mut);
		
//		ExtendedIterator<MutableReferenceGenomicRegion<NumericArray>> re = out.lineIterator().map(l->{
//			int tab = l.indexOf('\t');
//			return mut.parse(l.substring(0,tab), buffer.parse(StringUtils.split(l.substring(tab+1), '\t')));
//		});
		if (deleteOnEnd)
			re = re.hasNextAction(hasnext->{
				if (!hasnext)
					bout.getFile().delete();
				return hasnext;
			});
		return re;
	}
	
	

	@Override
	public void produceResults(GenomicRegionFeature<Void>[] o){
		if (o!=null && !program.isRunning()) { // when finished
			// otherwise i.e. without multi-threading the result is already written to the right file
			throw new RuntimeException("Multithreading not allowed!");
		}
	}
	
	
	@Override
	public GenomicRegionFeature<Void> copy() {
		ClusterSplicedReads re = new ClusterSplicedReads(getId());
		re.isCopy = true;
		re.copyProperties(this);
		re.setTolerance(tolerance);
		re.setDecimals(decimals);
		re.setDataToCounts(dataToCounts);
		re.genomic = genomic;
		return re;
	}

	
}

