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

package gedi.centeredDiskIntervalTree;


import gedi.app.extension.GlobalInfoProvider;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.FunctorUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.randomaccess.ConcurrentPageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Does still not support paired end reads (MissingInformationIntronInformation)
 * @author erhard
 *
 * @param <D>
 */
public class CenteredDiskIntervalTreeStorage<D>  implements GenomicRegionStorage<D> {
	
	public static final String MAGIC = "CDITS";
	public static final String EXT_MAGIC = "ECDIT";
	
	private String path;
	
	private ConcurrentPageFile file;
	
	private LinkedHashMap<ReferenceSequence,CenteredDiskIntervalTree<D>> pages;
	private DynamicObject meta;
	private String extendedJson;
	
	private boolean compression;
	
	public CenteredDiskIntervalTreeStorage(String file, Class<D> dataClass) throws IOException {
		this(file,dataClass,false);
	}
	public CenteredDiskIntervalTreeStorage(String file, Class<D> dataClass, boolean compression) throws IOException {
		if (!file.endsWith(".cit"))
			file = file+".cit";
		this.path = file;
		this.dataClass = dataClass;
		this.compression = compression;
		if (new File(path).exists()) {
			readHeader();
		}
	}
	
	public String getPath() {
		return path;
	}
	
	public void printStats() {
		for (ReferenceSequence ref : pages.keySet()) {
			System.out.println(ref+"\t"+pages.get(ref).getBytes());
		}
	}
	
	/**
	 * Equivalent to the constructor, but IOExceptions are wrapped into a RuntimeException
	 * @param file
	 * @return
	 */
	public static <T> CenteredDiskIntervalTreeStorage<T> load(String file) {
		try {
			return new CenteredDiskIntervalTreeStorage<T>(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public CenteredDiskIntervalTreeStorage(String file) throws IOException {
		if (!file.endsWith(".cit"))
			file = file+".cit";
		this.path = file;
		if (new File(path).exists()) {
			readHeader();
		} else {
			throw new IOException("Cannot open "+file+". To create CIT, use the other constructor!");
		}
	}
	
	private void readHeader() throws IOException {
		this.file = new ConcurrentPageFile(path);
		pages = new LinkedHashMap<ReferenceSequence, CenteredDiskIntervalTree<D>>();
		file.position(0);
		String mag = file.getAsciiChars(5);
		if (!mag.equals(MAGIC) && !mag.equals(EXT_MAGIC)) 
			throw new RuntimeException("Wrong file format!");
		
		boolean extended = mag.equals(EXT_MAGIC);
		
		int refs = file.getInt();
		long minStart = Long.MAX_VALUE;
		for (int i=0; i<refs; i++) {
			Chromosome chr = Chromosome.read(file);
			long start = file.getLong();
			minStart = Math.min(minStart,start);
			CenteredDiskIntervalTree<D> tree = new CenteredDiskIntervalTree<D>(null,file,start,file.getLong());
//			tree.checkList();
			pages.put(chr,tree);
		}
		// new version: save data class here!
		if (file.position()<minStart) {
			String dc = file.getString();
			if (dc.equals("gedi.core.data.annotation.MemoryTranscript"))
				dc = "gedi.core.data.annotation.Transcript";
			try {
				if (dataClass==null)
					dataClass = (Class<D>) Class.forName(dc);
//				System.out.println(dataClass);
			} catch (ClassNotFoundException e) {
				throw new IOException("Could not load CIT, class "+dc+" unknown!");
			}
		}
		
		if (extended) {
			extendedJson = file.getString();
			if (extendedJson.length()>0)
				file.getContext().setGlobalInfo(DynamicObject.parseJson(extendedJson));
		}
		
		for (CenteredDiskIntervalTree<D> tree : pages.values())
			tree.setSupplier(getSupplier());
	}

	private Supplier<D> supplier;
	private Class<D> dataClass;
	
	
	@Override
	public DynamicObject getMetaData() {
		if (meta==null) {
			File m = new File(path+".metadata.json");
			if (m.exists())
				try {
					meta = DynamicObject.parseJson(FileUtils.readAllText(m));
				} catch (IOException e) {
					throw new RuntimeException("Could not read metadata file "+m,e);
				}
			else
				 meta = DynamicObject.getEmpty();
		}
		return meta;
	}
	
	@Override
	public void setMetaData(DynamicObject meta) {
		this.meta = meta;
		try {
			if (!meta.isNull())
				FileUtils.writeAllText(meta.toJson(),new File(path+".metadata.json"));
		} catch (IOException e) {
			throw new RuntimeException("Could not write metadata file "+path+".metadata.json",e);
		}
	}
	
	
	@Override
	public Class<D> getType() {
		return dataClass;
	}
	
	public void setSupplier(Supplier<D> supplier) {
		this.supplier = supplier;
	}
	
	public Supplier<D> getSupplier() {
		if (supplier==null && getType()==null)
			throw new RuntimeException("Cannot determine data class!");
		if (supplier==null)
			supplier = FunctorUtils.newInstanceSupplier(getType());
		return supplier;
	}

	public boolean exists() {
		return pages!=null;
	}
	
	public void close() throws IOException {
		file.close();
	}
	
	private boolean forceUnsortedFilling = false;
	public void setForceUnsortedFilling(boolean forceUnsortedFilling) {
		this.forceUnsortedFilling = forceUnsortedFilling;
	}
	
	
	public void mapChromosomes(Function<ReferenceSequence,ReferenceSequence> mapping) throws IOException {
		PageFileWriter out = new PageFileWriter(file.getPath()+".rename");
		
		out.putAsciiChars(EXT_MAGIC);
		out.putInt(pages.size());
		long minCurrentStart = Long.MAX_VALUE;
		for (ReferenceSequence ref : pages.keySet()) {
			ReferenceSequence refm = mapping.apply(ref);
			if (refm==null) refm = ref;
			Chromosome.write(refm.toChromosome(),out);
//			Chromosome.write(Chromosome.obtain(mapping.containsKey(ref.getName())?mapping.get(ref.getName()):ref.getName(),ref.getStrand()),out);
			CenteredDiskIntervalTree<D>  all = pages.get(ref);
			minCurrentStart = Math.min(minCurrentStart,all.getStart());
			out.putLong(0);
			out.putLong(0);// placeholder
		}
		out.putString(dataClass.getName()); // new!
		out.putString(extendedJson); // newnew!
		
		
		long offset = out.position()-minCurrentStart;
		
		out.position(0);
		out.putAsciiChars(EXT_MAGIC);
		out.putInt(pages.size());
		for (ReferenceSequence ref : pages.keySet()) {
			ReferenceSequence refm = mapping.apply(ref);
			if (refm==null) refm = ref;
			Chromosome.write(refm.toChromosome(),out);
//			Chromosome.write(Chromosome.obtain(mapping.containsKey(ref.getName())?mapping.get(ref.getName()):ref.getName(),ref.getStrand()),out);
			CenteredDiskIntervalTree<D> all = pages.get(ref);
			minCurrentStart = Math.min(minCurrentStart,all.getStart());
			out.putLong(all.getStart()+offset);
			out.putLong(all.getEnd()+offset);
		}
		
		out.putString(dataClass.getName()); // new!
		out.putString(extendedJson); // newnew!
		
		for (ReferenceSequence ref : pages.keySet()) {
			CenteredDiskIntervalTree<D>  all = pages.get(ref);
			file.position(all.getStart());
			while (file.position()<all.getEnd()) 
				out.put(file.get());
		}
		
		out.close();
		new File(out.getPath()).renameTo(new File(file.getPath()));
		file = new ConcurrentPageFile(path);
		
		readHeader();
	}
	
	@Override
	public void fill(Iterator<? extends ReferenceGenomicRegion<D>> it, final Progress progress)  {
		if (file!=null) throw new RuntimeException("File "+file+" already exists!");
		
		DynamicObject globalInfo = null;
		
		try {
		
			HashMap<ReferenceSequence,InternalCenteredDiskIntervalTreeBuilder<D>> references = new HashMap<ReferenceSequence,InternalCenteredDiskIntervalTreeBuilder<D>>(); // reference -> builder
			
//			System.out.println("Building cit");
			int re=0;
			ReferenceSequence last = null;
			int count = 0;
			
					
			while (it.hasNext()) {
				ReferenceGenomicRegion<D> rgr = it.next();
				if (globalInfo==null) {
					if (rgr.getData() instanceof GlobalInfoProvider)
						globalInfo = ((GlobalInfoProvider)rgr.getData()).getGlobalInfo();
					else 
						globalInfo = DynamicObject.getEmpty();
					globalInfo = globalInfo.merge(DynamicObject.from("compress", compression));
				}
				
				
				if (rgr.getReference().equals(last)) count++;
				else if (last!=null) {
					if (count>1000) {
//						System.out.println("Writing temp file for "+last);
						references.get(last).toDisk();
					}
					count=0;
				}
				
				last = rgr.getReference();
				
				InternalCenteredDiskIntervalTreeBuilder<D> builder = references.get(rgr.getReference());
				if (builder==null) references.put(rgr.getReference(), builder = new InternalCenteredDiskIntervalTreeBuilder<D>(new File(path).getAbsoluteFile().getParent(),new File(path).getName()+"."+rgr.getReference().toPlusMinusString(),globalInfo));
				builder.add(rgr.getRegion(), rgr.getData());
				re++;
//				if (++re%10000==0) {
//					System.out.println(re+" regions");
//				}
			}
			
			ReferenceSequence[] refs = references.keySet().toArray(new ReferenceSequence[0]);
			Arrays.sort(refs);
			
			PageFileWriter out = new PageFileWriter(path);
			out.putAsciiChars(EXT_MAGIC);
			out.putInt(refs.length);
			for (int i=0; i<refs.length; i++) {
				Chromosome.write(Chromosome.obtain(refs[i].getName(),refs[i].getStrand()),out);
				out.putLong(0);
				out.putLong(0);// placeholder
			}
			out.putString(dataClass.getName()); // new!
			if (globalInfo==null)
				globalInfo = DynamicObject.getEmpty();
			out.putString(globalInfo.toJson()); // newnew!
			
			if (progress!=null)
				progress.init().setCount(re);
			
			long[] offset = new long[refs.length+1];
			for (int i=0; i<refs.length; i++) {
//				System.out.println("Output "+refs[i]);
				ReferenceSequence rr = refs[i];
				if (progress!=null)
					progress.setDescription(()->"Writing "+rr);
				
				offset[i] = out.position();
				InternalCenteredDiskIntervalTreeBuilder<D> builder = references.get(refs[i]);
				builder.build(out);
//				System.out.println("Finished "+refs[i]+" @"+out.position());
			}
			offset[refs.length] = out.position();
			
			if (progress!=null)
				progress.finish();
			
			out.position(EXT_MAGIC.length()+Integer.BYTES);
			for (int i=0; i<refs.length; i++) {
				Chromosome.write(Chromosome.obtain(refs[i].getName(),refs[i].getStrand()),out);
				out.putLong(offset[i]);
				out.putLong(offset[i+1]);
			}
			out.close();
//			System.out.println("Finished!");
			file = new ConcurrentPageFile(path);
			readHeader();
			
		} catch (IOException e) {
			throw new RuntimeException("Could not write storage!",e);
		}
	}
	
	@Override
	public <O> void fill(GenomicRegionStorage<O> storage, Function<MutableReferenceGenomicRegion<O>,MutableReferenceGenomicRegion<D>> mapper, final Progress progress)  {
		if (file!=null) throw new RuntimeException("File "+file+" already exists!");
		
		DynamicObject globalInfo = storage.getRandomRecord() instanceof GlobalInfoProvider ? ((GlobalInfoProvider)storage.getRandomRecord()).getGlobalInfo(): DynamicObject.getEmpty();
		globalInfo = globalInfo.merge(DynamicObject.from("compress", compression));
		
		try {
		
			PageFileWriter out = new PageFileWriter(path);
			out.putAsciiChars(EXT_MAGIC);
			ReferenceSequence[] refs = storage.getReferenceSequences().toArray(new ReferenceSequence[0]);
			ReferenceSequence[] mappedRefs = new ReferenceSequence[refs.length];
			out.putInt(refs.length);
			for (int i=0; i<refs.length; i++) {
				try {
					mappedRefs[i] = mapper.apply(new MutableReferenceGenomicRegion().setReference(refs[i])).getReference();
				} catch (Throwable e) {
					mappedRefs[i] = refs[i];
				}
				Chromosome.write(Chromosome.obtain(mappedRefs[i].getName(),mappedRefs[i].getStrand()),out);
				out.putLong(0);
				out.putLong(0);// placeholder
			}
			
			out.putString(dataClass.getName()); // new!
			out.putString(globalInfo.toJson()); // newnew!
			
//			ConsoleProgress pr = new ConsoleProgress();
			
			if (progress!=null)
				progress.init();
			
			long[] offset = new long[refs.length+1];
			int[] re = {0};
			for (int i=0; i<refs.length; i++) {
//				System.out.println("Starting "+refs[i]);
				if (progress!=null)
					progress.setDescriptionf("Processing %s", mappedRefs[i]);
				offset[i] = out.position();
				InternalCenteredDiskIntervalTreeBuilder<D> builder = new InternalCenteredDiskIntervalTreeBuilder<D>(new File(path).getAbsoluteFile().getParent(),new File(path).getName(), globalInfo);
//				ReferenceSequence ref = refs[i];
//				storage.iterateGenomicRegions(refs[i]).forEachRemaining(region->{
//				if (refs[i].getName().equals("chr1")){
					storage.iterateMutableReferenceGenomicRegions(refs[i]).forEachRemaining(region->{
					try {
						MutableReferenceGenomicRegion<D> r2 = mapper.apply(region);
						if (r2!=null) {
							builder.add(r2.getRegion(), r2.getData());
							re[0]++;
							if (progress!=null)
								progress.incrementProgress();
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					});
//				}
				
//				pr.out.printf("Writing CIT for %s\n", mappedRefs[i]);
				builder.build(out);
//				System.out.println("Finished "+refs[i]+" @"+re[0]);
			}
			if (progress!=null)
				progress.finish();
			offset[refs.length] = out.position();
			
			out.position(MAGIC.length()+Integer.BYTES);
			for (int i=0; i<refs.length; i++) {
				Chromosome.write(Chromosome.obtain(mappedRefs[i].getName(),mappedRefs[i].getStrand()),out);
				out.putLong(offset[i]);
				out.putLong(offset[i+1]);
			}
			out.close();
			file = new ConcurrentPageFile(path);
			readHeader();
			
			if (!storage.getMetaData().isNull())
				new LineOrientedFile(path+".metadata.json").writeAllText(storage.getMetaData().toJson());
			
			
			
		} catch (IOException e) {
			throw new RuntimeException("Could write storage!",e);
		}
	}
		
	

	@Override
	public Set<ReferenceSequence> getReferenceSequences() {
		if (pages==null) {
			return Collections.emptySet();
		}
		return pages.keySet();
	}

	
	public <C extends Collection<GenomicRegion>> C getIntersectingRegions(ReferenceSequence reference, int start, int stop, C re) throws IOException {
		return pages.get(reference).getIntersectingRegions(start, stop, re);
	}

	public <C extends Map<GenomicRegion,D>> C getIntersectingRegions(ReferenceSequence reference, int start, int stop, C re) throws IOException {
		return pages.get(reference).getIntersectingRegions(start, stop, re);
	}
	
	public <C extends Collection<GenomicRegion>> C getIntersectingRegions(ReferenceSequence reference, GenomicRegion region, C re) throws IOException {
		return pages.get(reference).getIntersectingRegions(region, re);
	}

	public <C extends Map<GenomicRegion,D>> C getIntersectingRegions(ReferenceSequence reference, GenomicRegion region, C re) throws IOException {
		return pages.get(reference).getIntersectingRegions(region, re);
	}
	
	public <C extends Collection<GenomicRegion>> C getContainedConsistentRegions(ReferenceSequence reference, int start, int stop, C re) throws IOException {
		return pages.get(reference).getContainedConsistentRegions(start, stop, re);
	}

	public <C extends Map<GenomicRegion,D>> C getContainedConsistentRegions(ReferenceSequence reference, int start, int stop, C re) throws IOException {
		return pages.get(reference).getContainedConsistentRegions(start, stop, re);
	}
	
	public <C extends Collection<GenomicRegion>> C getContainedConsistentRegions(ReferenceSequence reference, GenomicRegion region, C re) throws IOException {
		return pages.get(reference).getContainedConsistentRegions(region, re);
	}

	public <C extends Map<GenomicRegion,D>> C getContainedConsistentRegions(ReferenceSequence reference, GenomicRegion region, C re) throws IOException {
		return pages.get(reference).getContainedConsistentRegions(region, re);
	}
	

	@Override
	public Spliterator<MutableReferenceGenomicRegion<D>> iterateMutableReferenceGenomicRegions(
			ReferenceSequence ref) {
		if (!pages.containsKey(ref)) return Spliterators.emptySpliterator();
		return pages.get(ref).spliterator(ref);
	}

	@Override
	public Spliterator<MutableReferenceGenomicRegion<D>> iterateIntersectingMutableReferenceGenomicRegions(
			ReferenceSequence ref, GenomicRegion region) {
		if (!pages.containsKey(ref)) return Spliterators.emptySpliterator();
		return pages.get(ref).iterateIntersectingRegions(ref,region);
	}

	@Override
	public boolean add(ReferenceSequence reference, GenomicRegion region, D data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(ReferenceSequence reference, GenomicRegion region) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(ReferenceSequence ref, GenomicRegion region) {
		if (!pages.containsKey(ref)) return false;
		try {
			return pages.get(ref).contains(region);
		} catch (IOException e) {
			throw new RuntimeException("Could not query tree!",e);
		}
	}
	
	

	@Override
	public D getData(ReferenceSequence ref, GenomicRegion region) {
		if (!pages.containsKey(ref)) return null;
		try {
			return pages.get(ref).getData(region);
		} catch (IOException e) {
			throw new RuntimeException("Could not query tree!",e);
		}
	}

	@Override
	public long size(ReferenceSequence reference) {
		return pages.containsKey(reference)?pages.get(reference).size():0;
	}

	@Override
	public void clear() {
		try {
			close();
			new File(getPath()).delete();
		} catch (IOException e) {
			throw new RuntimeException("Could not delete CIT!",e);
		}
	}

	@Override
	public String getName() {
		return FileUtils.getNameWithoutExtension(getPath());
	}

	@Override
	public String toString() {
		return getPath();
	}
	
	
}