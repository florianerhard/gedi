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

package gedi.core.genomic;


import gedi.app.Config;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.annotation.TranscriptToGene;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.sequence.CompositeSequenceProvider;
import gedi.core.sequence.SequenceProvider;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.FileUtils;
import gedi.util.datastructure.tree.Trie;
import gedi.util.functions.EI;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Function;

/**
 * An annotation gives an object for a given genomic position (example: Transcript annotation)
 * A mapping does the opposite; (example: transcript id to region)
 * A table maps such an object to another object (example: Transcript id to biotype)
 * @author erhard
 *
 */
public class Genomic implements SequenceProvider {
	
	public enum AnnotationType {
		Transcripts, UnionTranscripts, Genes
	}

	
	private String id;
	private CompositeSequenceProvider sequence = new CompositeSequenceProvider();
	private HashMap<String,Annotation<?>> annotations = new HashMap<String, Annotation<?>>();
	private HashMap<String,Mapping> mappings = new HashMap<String, Mapping>();
	private HashMap<String,GenomicMappingTable> mapTabs = new HashMap<String, GenomicMappingTable>();
	
	private LinkedHashMap<String,String> infos = new LinkedHashMap<>();
	
	private NameIndex nameIndex;
	
	
	public void merge(Genomic other) {
		for (SequenceProvider p : other.sequence.getProviders())
			this.sequence.add(p);
	
		for (String k : other.annotations.keySet()) {
			Annotation pres = annotations.get(k);
			if (pres==null) annotations.put(k, other.annotations.get(k));
			else pres.merge(other.annotations.get(k));
		}
		
		for (String k : other.mappings.keySet()) {
			Mapping pres = mappings.get(k);
			if (pres==null) mappings.put(k, other.mappings.get(k));
			else pres.merge(other.mappings.get(k));
		}
		
		for (String k : other.mapTabs.keySet()) {
			GenomicMappingTable pres = mapTabs.get(k);
			if (pres==null) mapTabs.put(k, other.mapTabs.get(k));
			else pres.merge(other.mapTabs.get(k));
		}
		
		if (nameIndex==null) nameIndex = other.nameIndex;
		else if (other.nameIndex!=null) nameIndex.merge(other.nameIndex);
	}
	
	public Trie<ReferenceGenomicRegion<?>> getNameIndex() {
		return nameIndex==null?null:nameIndex.getIndex();
	}
	
	
	public void add(SequenceProvider seq) {
		sequence.add(seq);
	}
	
	public void add(Mapping mapping) {
		mappings.put(mapping.getId(),mapping);
	}
	
	
	public void add(GenomicMappingTable table) {
		mapTabs.put(table.getId(),table);
	}
	
	public void add(NameIndex ni) {
		if (nameIndex==null)
			nameIndex = ni;
		else
			nameIndex.merge(ni);
	}
	
	public void add(Annotation<?> annotation) {
		annotations.put(annotation.getId(),annotation);
	}
	
	public void addInfo(String key, String value) {
		infos.put(key, value);
	}
	
	public LinkedHashMap<String, String> getInfos() {
		return infos;
	}
	public String getId() {
		return id;
	}
	
	public CharSequence reconstructRead(ReferenceGenomicRegion<? extends AlignedReadsData> r, int distinct) {
		return r.getData().genomeToRead(distinct,getSequence(r));
	}
	
	@SuppressWarnings("unchecked")
	public MemoryIntervalTreeStorage<Transcript> getTranscripts() {
		return (MemoryIntervalTreeStorage<Transcript>) annotations.get(AnnotationType.Transcripts.name()).get();
	}
	
	@SuppressWarnings("unchecked")
	public MemoryIntervalTreeStorage<String> getGenes() {
		if (!annotations.containsKey(AnnotationType.Genes.name()))
			add(new Annotation<String>(AnnotationType.Genes.name()).set(new TranscriptToGene(true).toGenes(getTranscripts())));
		return (MemoryIntervalTreeStorage<String>) annotations.get(AnnotationType.Genes.name()).get();
	}
	
	@SuppressWarnings("unchecked")
	public MemoryIntervalTreeStorage<String> getUnionTranscripts() {
		if (!annotations.containsKey(AnnotationType.UnionTranscripts.name()))
			add(new Annotation<String>(AnnotationType.UnionTranscripts.name()).set(new TranscriptToGene(false).toGenes(getTranscripts())));
		return (MemoryIntervalTreeStorage<String>) annotations.get(AnnotationType.UnionTranscripts.name()).get();
	}

	@SuppressWarnings("unchecked")
	public <T> MemoryIntervalTreeStorage<T> getAnnotation(String id) {
		return ((Annotation<T>)annotations.get(id)).get();
	}
	
	public <F,T> Function<F,ReferenceGenomicRegion<T>> getMapping(String id) {
		if (!mappings.containsKey(id)) 
			mappings.put(id, new Mapping(this,id));
			
		return mappings.get(id).get();
	}
	
	
	public Function<String,String> getTable(String id, String from, String to) {
		GenomicMappingTable tab = mapTabs.get(id);
		if (tab==null) return null;
		
		return tab.get(from,to);
	}
	
	
	/**
	 * Shorthand for getMapping("Transcripts.transcriptId"), i.e. gets a function to get the transcript region for a given transcript id
	 * @param id
	 * @param value
	 * @return
	 */
	public Function<String,String> getTranscriptTable(String targetProperty) {
		return getTranscriptTable("transcriptId",targetProperty);
	}
	
	
	public Function<String,String> getTranscriptTable(String from, String to) {
		return this.getTable(AnnotationType.Transcripts.name(),from,to);
	}

	
	public Function<String,String> getGeneTable(String targetProperty) {
		return getGeneTable("geneId",targetProperty);
	}
	
	public Function<String,String> getGeneTable(String from, String to) {
		return this.getTable(AnnotationType.Genes.name(),from,to);
	}

	/**
	 * Null safe way of mapping, i.e. if the mapping function
	 * @param id
	 * @param value
	 * @return
	 */
	public <F,T> ReferenceGenomicRegion<T> map(String id, F value) {
		return this.<F,T>getMapping(id).apply(value);
	}
	
	/**
	 * Shorthand for getMapping("Transcripts.transcriptId"), i.e. gets a function to get the transcript region for a given transcript id
	 * @param id
	 * @param value
	 * @return
	 */
	public Function<String,ReferenceGenomicRegion<Transcript>> getTranscriptMapping() {
		return this.getMapping(AnnotationType.Transcripts.name()+".transcriptId");
	}
	
	/**
	 * Shorthand for getMapping("UnionTranscripts.geneId"), i.e. gets a function to get the union transcript region for a given gene id
	 * @param id
	 * @param value
	 * @return
	 */
	public Function<String,ReferenceGenomicRegion<Transcript>> getUnionTranscriptMapping() {
		return this.getMapping(AnnotationType.UnionTranscripts.name()+".geneId");
	}
	/**
	 * Shorthand for getMapping("Genes.geneId"), i.e. gets a function to get the gene region for a given gene id
	 * @param id
	 * @param value
	 * @return
	 */
	public Function<String,ReferenceGenomicRegion<Transcript>> getGeneMapping() {
		return this.getMapping(AnnotationType.Genes.name()+".geneId");
	}
	
	
	/**
	 * Shorthand for map("Transcripts.transcriptId",ref.getReference().getName()).map(ref) (but null safe, i.e. if the Transcript is not present, null is returned.
	 * refs getReference() must point to a transcript id!
	 * @param ref
	 * @return
	 */
	public <T> ReferenceGenomicRegion<T> transcriptToGenome(ReferenceGenomicRegion<T> ref) {
		ReferenceGenomicRegion<String> mapped = map("Transcripts.transcriptId",ref.getReference().getName());
		if (mapped==null) return null;
		return mapped.map(ref);
	}
	
	
	@Override
	public int getLength(String name) {
		return sequence.getLength(name);
	}
	
	public CharSequence getSequence(String loc) {
		return getSequence(ImmutableReferenceGenomicRegion.parse(loc));
	}

	@Override
	public Set<String> getSequenceNames() {
		return sequence.getSequenceNames();
	}

	@Override
	public CharSequence getPlusSequence(String name, GenomicRegion region) {
		return sequence.getPlusSequence(name, region);
	}

	@Override
	public char getPlusSequence(String name, int pos) {
		return sequence.getPlusSequence(name, pos);
	}
	
	private static Genomic empty = new Genomic();
	public static Genomic getEmpty() {
		return empty;
	}
	

	private static HashMap<String,Genomic> cache = new HashMap<String, Genomic>();
	public static Genomic reload(String name) {
		cache.remove(name);
		return get(name);
	}
	
	public static synchronized Genomic merge(Collection<Genomic> genomics) {
		if (genomics.size()==0) return null;
		Iterator<Genomic> it = genomics.iterator();
		Genomic g = it.next();
		while (it.hasNext())
			g.merge(it.next());
		return g;
	}
	public static synchronized Genomic merge(Genomic... genomics) {
		if (genomics.length==0) return null;
		Genomic g = genomics[0];
		for (int i=1; i<genomics.length; i++)
			g.merge(genomics[i]);
		return g;
	}
	
	
	public static synchronized Genomic all() {
		return get(EI.wrap(Paths.get(Config.getInstance().getConfigFolder(),"genomic").toFile().list()).filter(f->f.endsWith(".oml")).map(FileUtils::getNameWithoutExtension));
	}
	
	public static synchronized Genomic get(Iterable<String> names) {
		return get(names.iterator());
	}
	public static synchronized Genomic get(Iterator<String> names) {
		if (!names.hasNext()) return null;
		Genomic g = get(names.next());
		while (names.hasNext())
			g.merge(get(names.next()));
		return g;
	}
	public static synchronized Genomic get(String... names) {
		if (names.length==0) return null;
		Genomic g = get(names[0]);
		for (int i=1; i<names.length; i++)
			g.merge(get(names[i]));
		return g;
	}
	public static synchronized Genomic get(String name) {
		if (!cache.containsKey(name)) {
			Throwable a = null,b = null;
			
			Path path = Paths.get(name);
			if (path!=null && path.toFile().exists()) {
				WorkspaceItemLoader<Genomic> loader = WorkspaceItemLoaderExtensionPoint.getInstance().get(path);
				if (loader==null) {
					a = new RuntimeException("No loader for "+path.toFile().getName()+" available!");
				} else {
					
					try {
						Genomic g = loader.load(path);
						cache.put(name, g);
						return g;
					} catch (Throwable e) {
						b=e;
					}
				}
			}
			
			Path parent = Paths.get(Config.getInstance().getConfigFolder(),"genomic");
			if (parent.toFile().exists())
				for (String p : parent.toFile().list()) {
					if (p.startsWith(name+".")) {
						path = parent.resolve(p);
						
						if (path==null || !path.toFile().exists()) {
							continue;
						}
						WorkspaceItemLoader<Genomic> loader = WorkspaceItemLoaderExtensionPoint.getInstance().get(path);
						if (loader==null) {
							a = new RuntimeException("No loader for "+path.toFile().getName()+" available!");
							continue;
						}
						
						try {
							Genomic g = loader.load(path);
							g.id = name;
							cache.put(name, g);
							break;
						} catch (Throwable e) {
							b=e;
							continue;
						}
					}
				}
			
			if (!cache.containsKey(name)) {
				if (b!=null) throw new RuntimeException("Could not load genomic "+name+"!",b);
				if (a!=null) throw new RuntimeException("Could not load genomic "+name+"!",a);
				throw new RuntimeException("Genomic name "+name+" does not exisit in config/genomic!");
			}
		}
		return cache.get(name);
	}

	
}
