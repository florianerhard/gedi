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

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.core.region.feature.features.AbstractFeature;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.PeekIterator;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;



@GenomicRegionFeatureDescription(toType=Void.class)
public class ClusterReads extends AbstractFeature<Void> {

	private int tolerance = 0;
	private boolean isCopy = false;
	private BiFunction<Object,NumericArray,NumericArray> dataToCounts;
	private int decimals = 2;
	
	
	public ClusterReads(String file) {
		minValues = maxValues = 0;
		minInputs = maxInputs = 0;
		setFile(file);
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

	MutableReferenceGenomicRegion<NumericArray> currentCluster = null;
	private NumericArray buffer;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void accept_internal(Set<Void> values) {
		buffer = dataToCounts==null?program.dataToCounts(referenceRegion.getData(), buffer):dataToCounts.apply(referenceRegion.getData(), buffer);

		checkCluster(referenceRegion);
		currentCluster.getData().add(buffer);
		
		buffer.clear();
		
	}
	
	
	private void checkCluster(
			ReferenceGenomicRegion<?> referenceRegion) {
		if (currentCluster==null)
			// first cluster
			currentCluster = new MutableReferenceGenomicRegion<NumericArray>().set(referenceRegion.getReference(),new ArrayGenomicRegion(referenceRegion.getRegion().getStart(),referenceRegion.getRegion().getEnd()),proto());
		else if (!currentCluster.getReference().equals(referenceRegion.getReference())) {
			// first cluster on this chromosome
			writeCluster(currentCluster);
			currentCluster.setReference(referenceRegion.getReference());
			currentCluster.setRegion(new ArrayGenomicRegion(referenceRegion.getRegion().getStart(),referenceRegion.getRegion().getEnd()));
			currentCluster.getData().clear();
		} else if (overlaps(currentCluster.getRegion(),referenceRegion.getRegion(),tolerance)) {
			// extend cluster
			if (referenceRegion.getRegion().getEnd()>currentCluster.getRegion().getEnd())
				((ArrayGenomicRegion)currentCluster.getRegion()).getCoords()[1] = referenceRegion.getRegion().getEnd();
		} else {
			// next cluster
			writeCluster(currentCluster);
			currentCluster.setReference(referenceRegion.getReference());
			currentCluster.setRegion(new ArrayGenomicRegion(referenceRegion.getRegion().getStart(),referenceRegion.getRegion().getEnd()));
			currentCluster.getData().clear();
		}
	}

	private NumericArray proto() {
		NumericArray re = buffer.createMemoryCopy();
		re.clear();
		return re;
	}

	private LineOrientedFile out;
	private PageFileWriter bout;
	
	private void writeCluster(
			MutableReferenceGenomicRegion<NumericArray> cluster) {
		try {
			
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
		for (int i=0; i<inputs.length; i++) 
			out.writef("\t%s",inputNames[i]);
		
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
		if (currentCluster!=null)
			writeCluster(currentCluster);
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
	
	private static boolean overlaps(
			GenomicRegion cl,
			GenomicRegion o,
			int t) {
		
		return Math.max(cl.getStart(), o.getStart())<Math.min(cl.getEnd(),o.getEnd())+t;
	}

	public boolean dependsOnData() {
		return true;
	}
	
	private ExtendedIterator<MutableReferenceGenomicRegion<NumericArray>> iterateFile(boolean deleteOnEnd) throws IOException {
	
		if (buffer==null) {
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
	
	

	@SuppressWarnings("unchecked")
	@Override
	public void produceResults(GenomicRegionFeature<Void>[] o){
		if (o!=null && !program.isRunning()) { // when finished
			// otherwise i.e. without multi-threading the result is already written to the right file
			
			try {
				Iterator<ImmutableReferenceGenomicRegion<NumericArray>>[] its = new Iterator[o.length];
				for (int i=0; i<its.length; i++)
						its[i] = ((ClusterReads)o[i]).iterateFile(false).map(m->m.toImmutable(n->n.createMemoryCopy()));

				PeekIterator<ImmutableReferenceGenomicRegion<NumericArray>> it = FunctorUtils.mergeIterator(its, FunctorUtils.naturalComparator()).peeking();
				buffer = it.peek().getData().createMemoryCopy();
				while (it.hasNext()) {
					ImmutableReferenceGenomicRegion<NumericArray> n = it.next();
					checkCluster(n);
					currentCluster.getData().add(n.getData());
				}

				if (currentCluster!=null)
					writeCluster(currentCluster);
				
				out.finishWriting();
				
				for (int i=0; i<o.length; i++)
					((ClusterReads)o[i]).deleteTmp();
				
			} catch (IOException e) {
				throw new RuntimeException("Could not write clusters!",e);
			}
			
		}
	}
	
	
	private void deleteTmp() {
		if (!isCopy)
			throw new RuntimeException();
		if (bout!=null)
			bout.getFile().delete();
	}

	@Override
	public GenomicRegionFeature<Void> copy() {
		ClusterReads re = new ClusterReads(getId());
		re.isCopy = true;
		re.copyProperties(this);
		re.setTolerance(tolerance);
		re.setDecimals(decimals);
		re.setDataToCounts(dataToCounts);
		return re;
	}

	
}

