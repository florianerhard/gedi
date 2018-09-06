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
import gedi.core.data.annotation.ReferenceSequencesProvider;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.annotation.TranscriptToGene;
import gedi.core.data.annotation.TranscriptToMajorTranscript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.sequence.CompositeSequenceProvider;
import gedi.core.sequence.FastaIndexSequenceProvider;
import gedi.core.sequence.SequenceProvider;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.FileUtils;
import gedi.util.datastructure.tree.Trie;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.oml.Oml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
public class Genomic implements SequenceProvider, ReferenceSequencesProvider {
	
	public enum AnnotationType {
		Transcripts, UnionTranscripts, Genes, MajorTranscripts
	}

	
	private String id;
	private CompositeSequenceProvider sequence = new CompositeSequenceProvider();
	private HashMap<String,Annotation<?>> annotations = new HashMap<String, Annotation<?>>();
	private HashMap<String,Mapping> mappings = new HashMap<String, Mapping>();
	private HashMap<String,GenomicMappingTable> mapTabs = new HashMap<String, GenomicMappingTable>();
	
	private LinkedHashMap<String,String> infos = new LinkedHashMap<>();
	private HashMap<String,Genomic> referenceToOrigin;
	
	private NameIndex nameIndex;
	
	
	public ExtendedIterator<String> getGenomicFastaFiles() {
		return EI.wrap(sequence.getProviders())
				.cast(FastaIndexSequenceProvider.class)
				.unfold(fi->fi.getFiles().iterator())
				.map(f->f.getFastaFile().getPath());
	}
	
	public static Genomic merge(Genomic a, Genomic b) {
		HashSet<ReferenceSequence> ar = a.iterateReferenceSequences().set();
		HashSet<ReferenceSequence> br = b.iterateReferenceSequences().set();
		if (!Collections.disjoint(ar, br))
			throw new RuntimeException("Genomes are not disjoint!");

		Genomic re = new Genomic();
		re.referenceToOrigin = new HashMap<>();
		for (ReferenceSequence r : ar)
			re.referenceToOrigin.put(r.getName(), a.getOrigin(r));
		for (ReferenceSequence r : br)
			re.referenceToOrigin.put(r.getName(), b.getOrigin(r));
		
		for (SequenceProvider p : a.sequence.getProviders())
			re.sequence.add(p);
		for (SequenceProvider p : b.sequence.getProviders())
			re.sequence.add(p);
	
		for (String k : a.annotations.keySet()) {
			Annotation pres = re.annotations.get(k);
			if (pres==null) re.annotations.put(k, a.annotations.get(k));
			else pres.merge(a.annotations.get(k));
		}
		for (String k : b.annotations.keySet()) {
			Annotation pres = re.annotations.get(k);
			if (pres==null) re.annotations.put(k, b.annotations.get(k));
			else pres.merge(b.annotations.get(k));
		}
		
		for (String k : a.mappings.keySet()) {
			Mapping pres = re.mappings.get(k);
			if (pres==null) re.mappings.put(k, a.mappings.get(k));
			else pres.merge(a.mappings.get(k));
		}
		for (String k : b.mappings.keySet()) {
			Mapping pres = re.mappings.get(k);
			if (pres==null) re.mappings.put(k, b.mappings.get(k));
			else pres.merge(b.mappings.get(k));
		}
		
		for (String k : a.mapTabs.keySet()) {
			GenomicMappingTable pres = re.mapTabs.get(k);
			if (pres==null) re.mapTabs.put(k, a.mapTabs.get(k));
			else pres.merge(a.mapTabs.get(k));
		}
		for (String k : b.mapTabs.keySet()) {
			GenomicMappingTable pres = re.mapTabs.get(k);
			if (pres==null) re.mapTabs.put(k, b.mapTabs.get(k));
			else pres.merge(b.mapTabs.get(k));
		}
		
		if (a.nameIndex!=null) re.add(a.nameIndex);
		if (b.nameIndex!=null) re.add(b.nameIndex);
		return re;
	}
	
	public Genomic getOrigin(ReferenceSequence ref) {
		return getOrigin(ref.getName());
	}
	
	public Genomic getOrigin(String name) {
		if (referenceToOrigin==null) return this;
		return referenceToOrigin.get(name);
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
			nameIndex = ni.clone();
		else
			nameIndex.merge(ni);
	}
	
	@Override
	public String toString() {
		return id;
	}
	
	@Override
	public ExtendedIterator<ReferenceSequence> iterateReferenceSequences() {
		return getTranscripts().iterateReferenceSequences();
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
	
//	public CharSequence reconstructRead(ReferenceGenomicRegion<? extends AlignedReadsData> r, int distinct) {
//		return r.getData().genomeToRead(distinct,getSequence(r));
//	}

	@SuppressWarnings("unchecked")
	public MemoryIntervalTreeStorage<Transcript> getMajorTranscripts() {
		if (!annotations.containsKey(AnnotationType.MajorTranscripts.name()))
			add(new Annotation<Transcript>(AnnotationType.MajorTranscripts.name()).set(new TranscriptToMajorTranscript().apply(getTranscripts())));
		return (MemoryIntervalTreeStorage<Transcript>) annotations.get(AnnotationType.MajorTranscripts.name()).get();
	}

	public boolean hasTranscripts() {
		return annotations.get(AnnotationType.Transcripts.name())!=null;
	}

	
	
	@SuppressWarnings("unchecked")
	public MemoryIntervalTreeStorage<Transcript> getTranscripts() {
		Annotation<?> a = annotations.get(AnnotationType.Transcripts.name());
		if (a==null) return new MemoryIntervalTreeStorage<>(Transcript.class);
		return (MemoryIntervalTreeStorage<Transcript>) a.get();
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
		if (id.equals(AnnotationType.Transcripts.name()))
			getTranscripts();
		if (id.equals(AnnotationType.UnionTranscripts.name()))
			getUnionTranscripts();
		if (id.equals(AnnotationType.Genes.name()))
			getGenes();
		return ((Annotation<T>)annotations.get(id)).get();
	}
	
	public <F,T> Function<F,ReferenceGenomicRegion<T>> getMapping(String id) {
		if (!mappings.containsKey(id)) 
			mappings.put(id, new Mapping(this,id));
			
		return mappings.get(id).get();
	}
	
	public Collection<String> getTables() {
		return mapTabs.keySet();
	}
	
	public Collection<String> getTableColumns(String id) {
		return mapTabs.get(id).getColumns();
	}
	
	public Collection<String> getTableColumns(String id, String from) {
		return mapTabs.get(id).getTargetColumns(from);
	}
	
	
	public Function<String,String> getTable(String id, String from, String to) {
		GenomicMappingTable tab = mapTabs.get(id);
		if (tab==null) return null;
		
		return tab.get(from,to);
	}
	
	public Collection<String> getTranscriptTableColumns() {
		return mapTabs.get(AnnotationType.Transcripts.name()).getColumns();
	}
	
	public Collection<String> getTranscriptTableColumns(String from) {
		return mapTabs.get(AnnotationType.Transcripts.name()).getTargetColumns(from);
	}
	
	public Collection<String> getGeneTableColumns() {
		return mapTabs.get(AnnotationType.Genes.name()).getColumns();
	}
	
	public Collection<String> getGeneTableColumns(String from) {
		return mapTabs.get(AnnotationType.Genes.name()).getTargetColumns(from);
	}
	
	/**
	 * Shorthand for getTranscriptTable("Transcripts.transcriptId"), i.e. gets a function to get the transcript region for a given transcript id
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
		return this.getMapping(AnnotationType.Genes.name());
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
		return merge(EI.wrap(genomics));
	}
	public static synchronized Genomic merge(Genomic... genomics) {
		return merge(EI.wrap(genomics));
	}
	
	
	public static synchronized Genomic all() {
		return get(EI.wrap(Paths.get(Config.getInstance().getConfigFolder(),"genomic").toFile().list()).filter(f->f.endsWith(".oml")).map(FileUtils::getNameWithoutExtension));
	}
	
	public static synchronized Genomic get(Iterable<String> names) {
		return get(names.iterator());
	}
	public static synchronized Genomic get(String... names) {
		return get(EI.wrap(names));
	}
	public static synchronized Genomic get(Iterator<String> names) {
		if (!names.hasNext()) return null;
		
		String[] a = EI.wrap(names).toArray(String.class);
		String name = EI.wrap(a).concat(",");
		if (!cache.containsKey(name)) {
			Genomic g = new Genomic();
			for (int i=0; i<a.length; i++) 
				g = Genomic.merge(g,get(a[i],false,true));
			g.id = name;
			cache.put(name, g);
		}
		return cache.get(name);
	}
	public static synchronized Genomic merge(Iterator<Genomic> genomic) {
		if (!genomic.hasNext()) return null;
		Genomic g = new Genomic();
		StringBuilder sb = new StringBuilder();
		while (genomic.hasNext()) {
			Genomic gen = genomic.next();
			if (sb.length()>0) sb.append(",");
			sb.append(gen.id);
			g = merge(g,gen);
		}
		g.id = sb.toString();
		cache.put(g.id, g);
		return g;
	}
	
	public static Path[] getGenomicPaths() {
		return EI.wrap(Config.getInstance().getConfig().getEntry("genomic").asArray())
				.map(d->Paths.get(d.asString(null)))
				.chain(EI.singleton(Paths.get(Config.getInstance().getConfigFolder(),"genomic")))
				.toArray(Path.class);
	}

	public static synchronized Genomic get(String name) {
		return get(name,true,true);
	}
	public static synchronized Genomic check(String name) {
		return get(name,true,true);
	}
	private static synchronized Genomic get(String name, boolean caching, boolean throwOnNotExisting) {
		if (!caching || !cache.containsKey(name)) {
			Throwable a = null,b = null;
			Genomic g = null;
			
			Path path = Paths.get(name);
			if (path!=null && path.toFile().exists()) {
				try {
					g = Oml.create(path.toString());
					g.id = name;
					if (caching)
						cache.put(name, g);
					return g;
				} catch (Throwable e) {
					b=e;
				}
			}
			Path[] folders = getGenomicPaths();
			outer:for (Path folder : folders)
				if (folder.toFile().exists())
					for (String p : folder.toFile().list()) {
						if (p.startsWith(name+".")) {
							path = folder.resolve(p);
							
							if (path==null || !path.toFile().exists()) {
								continue;
							}
							
							try {
								g = Oml.create(path.toString());
								g.id = name;
								if (caching)
									cache.put(name, g);
								break outer;
							} catch (Throwable e) {
								b=e;
								continue;
							}
						}
					}
			
			if (g==null) {
				if (throwOnNotExisting){
					if (b!=null) throw new RuntimeException("Could not load genomic "+name+"!",b);
					if (a!=null) throw new RuntimeException("Could not load genomic "+name+"!",a);
					throw new RuntimeException("Genomic name "+name+" does not exisit in config/genomic!");
				}
				return null;
			}
			return g;
		}
		return cache.get(name);
	}

	
}
