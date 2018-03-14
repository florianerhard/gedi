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
package executables;

import gedi.app.Config;
import gedi.app.Gedi;
import gedi.app.extension.ExtensionContext;
import gedi.core.data.annotation.Gff3Element;
import gedi.core.data.annotation.NameAttributeMapAnnotation;
import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.sequence.FastaIndexSequenceProvider;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.tree.Trie;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.StreamLineWriter;
import gedi.util.io.text.fasta.DefaultFastaHeaderParser;
import gedi.util.io.text.fasta.FastaEntry;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.io.text.genbank.GenbankFeature;
import gedi.util.io.text.genbank.GenbankFile;
import gedi.util.io.text.tsv.formats.Gff3FileReader;
import gedi.util.io.text.tsv.formats.GtfFileReader;
import gedi.util.mutable.MutablePair;
import gedi.util.orm.Orm;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IndexGenome {

	
	public static void main(String[] args) {
		try {
			start(args);
		} catch (UsageException e) {
			usage("An error occurred: "+e.getMessage());
			if (ArrayUtils.find(args, "-D")>=0)
				e.printStackTrace();
		} catch (Exception e) {
			System.err.println("An error occurred: "+e.getMessage());
			if (ArrayUtils.find(args, "-D")>=0)
				e.printStackTrace();
		}
	}
	
	private static class UsageException extends Exception {
		public UsageException(String msg) {
			super(msg);
		}
	}
	
	
	private static String checkParam(String[] args, int index) throws UsageException {
		if (index>=args.length || args[index].startsWith("-")) throw new UsageException("Missing argument for "+args[index-1]);
		return args[index];
	}
	
	private static int checkIntParam(String[] args, int index) throws UsageException {
		String re = checkParam(args, index);
		if (!StringUtils.isInt(re)) throw new UsageException("Must be an integer: "+args[index-1]);
		return Integer.parseInt(args[index]);
	}

	private static double checkDoubleParam(String[] args, int index) throws UsageException {
		String re = checkParam(args, index);
		if (!StringUtils.isNumeric(re)) throw new UsageException("Must be a double: "+args[index-1]);
		return Double.parseDouble(args[index]);
	}
	
	@SuppressWarnings("unchecked")
	public static void start(String[] args) throws Exception {
		
		Progress progress = new NoProgress();
		
		FastaFile seq = null;
		String annotPath = null;
		String name = null;
		String output = null;
		String folder = null;
		String gff = null;
		String genbank = null;
		String genbankLabel = "label";
		String[] genbankFeatures = new String[0];
		String genbankName = null;
		boolean ignoreMulti = true;
		boolean transcriptome = true;
		boolean bowtie = true;
		boolean star = true;
		boolean kallisto = true;
		int fixedNbases = -1;
		
		String ensemblOrg = null;
		String ensemblVer = null;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-p")) {
				progress=new ConsoleProgress(System.err);
			}
			else if (args[i].equals("-s")) {
				seq=new FastaFile(checkParam(args, ++i));
				seq.setFastaHeaderParser(new DefaultFastaHeaderParser(' '));
			}
			else if (args[i].equals("-a")) {
				annotPath=checkParam(args, ++i);
			}
			else if (args[i].equals("-gb")) {
				genbank=checkParam(args, ++i);
			}
			else if (args[i].equals("-organism")) {
				ensemblOrg=checkParam(args, ++i);
			}
			else if (args[i].equals("-version")) {
				ensemblVer=checkParam(args, ++i);
			}
			else if (args[i].equals("-gff")) {
				gff=checkParam(args, ++i);
			}
			else if (args[i].equals("-gblabel")) {
				genbankLabel=checkParam(args, ++i);
			}
			else if (args[i].equals("-gbname")) {
				genbankName=checkParam(args, ++i);
			}
			else if (args[i].equals("-gbfeatures")) {
				genbankFeatures = StringUtils.split(checkParam(args, ++i), ',');
			}
			else if (args[i].equals("-f")) {
				folder=checkParam(args, ++i);
			}
			else if (args[i].equals("-nobowtie")) {
				bowtie=false;
			}
			else if (args[i].equals("-nostar")) {
				star=false;
			}
			else if (args[i].equals("-nokallisto")) {
				kallisto=false;
			}
			else if (args[i].equals("-nbases")) {
				fixedNbases = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-ignoreMulti")) {
				ignoreMulti=true;
			}
			else if (args[i].equals("-n")) {
				name = checkParam(args,++i);
			}
			else if (args[i].equals("-o")) {
				output = checkParam(args,++i);
			}
			else if (args[i].equals("-D")){} 
			else throw new UsageException("Unknown parameter: "+args[i]);
		}
		
		
		if (genbank==null && seq==null && ensemblOrg==null) throw new UsageException("No fasta file given!");
		
		Gedi.startup();
		
		String prefix;
		String seqpath; 
		String annopath;
		String genetabpath;
		String transtabpath;
		String triepath;
		String annoStorageClass;
		
		if (genbank!=null || gff!=null) {

			String path = null;
			String acc = null;
			Supplier<String> seqSupp = null;
			// containing name and protein id
			HashMap<String, HashMap<String, ImmutableReferenceGenomicRegion<String[]>>> featureMap = new HashMap<>();
	
			String elabel = genbankLabel;
			String ename = genbankName==null?genbankLabel:genbankName;
			String[] features = EI.wrap(genbankFeatures).chain(EI.wrap("mRNA","CDS","gene")).set().toArray(new String[0]);
			
			
			if (genbank!=null) {
				path = genbank;
				GenbankFile file = new GenbankFile(genbank);
				acc = file.getAccession();
				seqSupp = ()->{
					try {
						return file.getSource();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				};
				GenomicRegionStorage<NameAttributeMapAnnotation> full = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, genbank+".full").add(Class.class, NameAttributeMapAnnotation.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
				ReferenceSequence ref = Chromosome.obtain(acc,Strand.Plus);
				if (!new File(path+".full.cit").exists()) {
					progress.init().setDescription("Indexing full annotation file in "+path);
					full.fill(file.featureIterator().map(f->
						new ImmutableReferenceGenomicRegion<NameAttributeMapAnnotation>(
								ref.toStrand(f.getPosition().getStrand()),
								f.getPosition().toGenomicRegion(),
								new NameAttributeMapAnnotation(f.getFeatureName(), f.toSimpleMap()))
					));
					progress.finish();
				}
				
				
				for (GenbankFeature f : file.featureIterator(features).loop()) {
					String key = f.getStringValue(elabel);
					key = key.replaceAll(" ", "_");
					if (key!=null) {
						ImmutableReferenceGenomicRegion<String[]> r = new ImmutableReferenceGenomicRegion<>(
								ref.toStrand(f.getPosition().getStrand()),
								f.getPosition().toGenomicRegion(), new String[] {key,f.getStringValue("protein_id")});
						HashMap<String, ImmutableReferenceGenomicRegion<String[]>> map = featureMap.computeIfAbsent(key, x->new HashMap<>());
						if (map.containsKey(f.getFeatureName()))
							throw new RuntimeException(f.getStringValue(elabel)+" contains more than one "+f.getFeatureName());
						map.put(f.getFeatureName(), r);
					}
				}
				
			} else {
				path = gff;
				acc = new LineOrientedFile(gff).lineIterator("#").map(s->StringUtils.splitField(s, '\t', 0)).unique(true).getUniqueResult("Multiple references defined in GFF", "GFF empty");
				HashMap<String,HashMap<String, String>> parentMap = new HashMap<>();
				ReferenceSequence ref = Chromosome.obtain(acc,Strand.Plus);
				
				HashSet<String> featureSet = new HashSet<>(Arrays.asList(features));
				
				for (String[] fields : new LineOrientedFile(gff).lineIterator("#").map(s->StringUtils.split(s, '\t')).loop()) {
					if (!featureSet.contains(fields[2]))
						continue;
					
					HashMap<String, String> amap = EI.split(fields[8], ';').index(s->StringUtils.splitField(s, '=', 0), s->StringUtils.splitField(s, '=', 1));
					String gedipos = fields[6]+":"+fields[3]+"-"+fields[4];
					amap.put("gedipos", gedipos);
					String fn = fields[2];
					
					Function<String,String> attr = k->{
						HashMap<String, String> mmap = amap;
						while (mmap!=null) {
							if (mmap.containsKey(k))
								return mmap.get(k);	
							if (mmap.containsKey("Parent"))
								mmap = parentMap.get(mmap.get("Parent"));
							else {
								HashMap<String, String> nmap = parentMap.get(mmap.get("gedipos"));
								if (nmap==mmap) {
									if (k.equals("gene"))
										return mmap.get("Name");
									return null;
								}
								mmap = nmap;
							}
						}
						return null;
					};
					
					ImmutableReferenceGenomicRegion<String[]> r = new ImmutableReferenceGenomicRegion<>(
							ref.toStrand(Strand.parse(fields[6])),
							new ArrayGenomicRegion(Integer.parseInt(fields[3])-1,Integer.parseInt(fields[4])),
							new String[] {attr.apply(ename),fn.equals("CDS") && attr.apply("Name")!=null?attr.apply("Name"):attr.apply("protein_id")});
					HashMap<String, ImmutableReferenceGenomicRegion<String[]>> map = featureMap.computeIfAbsent(attr.apply(elabel), x->new HashMap<>());
					if (map.containsKey(fn))
						throw new RuntimeException(attr.apply(ename)+" contains more than one "+fn);
					map.put(fn, r);
					
					parentMap.put(gedipos,amap);
					parentMap.put(amap.get("ID"),amap);
					
				}
			}
			
			
			ReferenceSequence ref = Chromosome.obtain(acc,Strand.Plus);
			
			
			if (name==null) name = FileUtils.getNameWithoutExtension(path);
			
			prefix = folder==null?path:new File(folder,FileUtils.getNameWithoutExtension(path)).toString();
			if (seq!=null && folder==null) prefix = FileUtils.getNameWithoutExtension(seq); 
			annopath = prefix+".index";
			triepath = prefix+".names";
			genetabpath = prefix+".genes.tab";
			transtabpath = prefix+".transcripts.tab";
			
			
			seqpath = seq==null?prefix+".fasta":seq.getPath();
			
			FastaFile ff = new FastaFile(seqpath);
			
			if (seq==null && seqSupp!=null) {
				progress.init().setDescription("Extracting sequence "+path);
				ff.startWriting();
				ff.writeEntry(new FastaEntry(acc,seqSupp.get().toUpperCase()));
				ff.finishWriting();
			} else {
				if (ff==null) throw new RuntimeException("No fasta file given!");
				String sequence = ff.entryIterator(false).getUniqueResult("Only a fasta file with a single entry is allowed here!","Fasta file empty!").getSequence();
				LineWriter wr = new LineOrientedFile(prefix+".fasta").write();
				wr.writeLine(new FastaEntry(ref.getName(),sequence).toString());
				wr.close();
				
				seqpath = prefix+".fasta";
				ff = new FastaFile(seqpath);
			}
			
			progress.init().setDescription("Indexing sequence "+path);
			ff.obtainDefaultIndex().create(ff);
			progress.finish();
			seqpath = ff.obtainDefaultIndex().getAbsolutePath();
			
			
			
			
//			HashMap<String, ImmutableReferenceGenomicRegion<String>> mrnas = file.featureIterator("mRNA").map(f->
//				new ImmutableReferenceGenomicRegion<String>(
//					ref.toStrand(f.getPosition().getStrand()),
//					f.getPosition().toGenomicRegion(), f.getStringValue(elabel))).indexAdapt(r->r.getData(),v->v,IndexGenome::adaptLabel);
//			HashMap<String, ImmutableReferenceGenomicRegion<String>> cdss = file.featureIterator("CDS").map(f->
//			new ImmutableReferenceGenomicRegion<String>(
//				ref.toStrand(f.getPosition().getStrand()),
//				f.getPosition().toGenomicRegion(), f.getStringValue(elabel))).indexAdapt(r->r.getData(),v->v,IndexGenome::adaptLabel);
			
			LineWriter geneout = new LineOrientedFile(genetabpath).write().writef("Gene ID\tGene Symbol\tBiotype\tSource\n");
			LineWriter transout = new LineOrientedFile(transtabpath).write().writef("Transcript ID\tProtein ID\tBiotype\tSource\n");
//			for (GenbankFeature f : file.featureIterator("CDS").loop()) {
//					geneout.writef("%s\t%s\t%s\tgenbank\n",f.getStringValue(elabel),f.getStringValue(ename),f.getFeatureName().equals("CDS")?"protein_coding":f.getFeatureName());
//					transout.writef("%s\t%s\t%s\tgenbank\n",f.getStringValue(elabel),f.getStringValue("protein_id"),f.getFeatureName().equals("CDS")?"protein_coding":f.getFeatureName());
//			}
			
			for (String label : featureMap.keySet()) {
				for (String feat : featureMap.get(label).keySet()) {
					if (!feat.equalsIgnoreCase("mRNA") && !feat.equalsIgnoreCase("gene")) {
						ImmutableReferenceGenomicRegion<String[]> r = featureMap.get(label).get(feat);
						geneout.writef("%s\t%s\t%s\tgenbank\n",label,r.getData()[0]==null?label:r.getData()[0],feat.equals("CDS")?"protein_coding":feat);
						transout.writef("%s\t%s\t%s\tgenbank\n",label,r.getData()[1]==null?"":r.getData()[1],feat.equals("CDS")?"protein_coding":feat);
					}
				}
			}
			
			geneout.close();
			transout.close();
			
			HashSet<String> genes = new HashSet<String>();
//			genes.addAll(mrnas.keySet());
//			genes.addAll(cdss.keySet());
			genes.addAll(featureMap.keySet());
			
			progress.init().setDescription("Indexing annotation file in "+path).setCount(genes.size());
			MemoryIntervalTreeStorage<Transcript> mem = new MemoryIntervalTreeStorage<Transcript>(Transcript.class);
			for (String gene : genes) {
				progress.incrementProgress();
//				ImmutableReferenceGenomicRegion<String> mrna = mrnas.get(gene);
//				ImmutableReferenceGenomicRegion<String> cds = cdss.get(gene);
				
				HashMap<String, ImmutableReferenceGenomicRegion<String[]>> map = featureMap.get(gene);
				ImmutableReferenceGenomicRegion<String[]> mrna = map.get("mRNA");
				if (mrna==null) mrna = map.get("gene");
				if (mrna==null && map.size()==1) mrna = map.values().iterator().next();
				ImmutableReferenceGenomicRegion<String[]> cds = map.get("CDS");
				
				if (mrna==null) {
					mem.add(cds.getReference(),cds.getRegion(),new Transcript(gene, gene, cds.getRegion().getStart(), cds.getRegion().getEnd()));
				} else if (cds==null) {
					mem.add(mrna.getReference(),mrna.getRegion(),new Transcript(gene, gene, -1,-1));
				} else {
					if (mrna==null || cds==null)
						throw new RuntimeException("Cannot determine gene "+gene);
					if (!mrna.getReference().equals(cds.getReference()))
						throw new RuntimeException("Inconsistent references for "+gene);
					if (!mrna.getRegion().containsUnspliced(cds.getRegion()))
						throw new RuntimeException("CDS not in mRNA for "+gene);
					
					mem.add(mrna.getReference(),mrna.getRegion(),new Transcript(gene, gene, cds.getRegion().getStart(),cds.getRegion().getEnd()));
				}
			}
			progress.finish();
			
			GenomicRegionStorage<Transcript> aaano = null;
			try {
				aaano = (GenomicRegionStorage<Transcript>) WorkspaceItemLoaderExtensionPoint.getInstance().get(Paths.get(annopath+".cit")).load(Paths.get(annopath+".cit"));
			} catch (Throwable e) {}
			if (aaano==null) {
				aaano = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, annopath).add(Class.class, Transcript.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
				aaano.fill(mem);
				annoStorageClass = aaano.getClass().getSimpleName();
			} else {
				annoStorageClass = aaano.getClass().getSimpleName();
			}
			
			progress.init().setDescription("Indexing annotation file in "+path).setCount(genes.size());
			Trie<ImmutableReferenceGenomicRegion<Void>> trie = new Trie<ImmutableReferenceGenomicRegion<Void>>();
			for (String gene : featureMap.keySet()) {
				ImmutableReferenceGenomicRegion<String[]> g = featureMap.get(gene).get("gene");
				if (g!=null)
					trie.put(gene, new ImmutableReferenceGenomicRegion<>(g.getReference(), g.getRegion()));
			}
//			for (GenbankFeature g : makeIt.apply(new String[] {"gene"}).loop()) {
//				ImmutableReferenceGenomicRegion<Void> rgr = new ImmutableReferenceGenomicRegion<Void>(ref, g.getPosition().toGenomicRegion());
//				String id = g.getStringValue("gene");
//				if (id!=null) trie.put(id, rgr);
//			}
			Orm.serialize(triepath, trie);
			progress.finish();
			
			annotPath = StringUtils.removeFooter(seqpath,"fi")+"gtf";
			if (!new File(annotPath).exists()) {
				progress.init().setDescription("Output GTF for "+path).setCount((int) aaano.size());
				LineWriter gtf = new LineOrientedFile(annotPath).write();
				for (ImmutableReferenceGenomicRegion<Transcript> tr : aaano.ei().loop()) {
					gtf.writef("%s\tGENBANK\t%s\t%d\t%d\t.\t%s\t.\tgene_id \"%s\";\n", 
							tr.getReference().getName(),"gene",
							tr.getRegion().getStart()+1,tr.getRegion().getEnd(),
							tr.getReference().getStrand().getGff(),
							tr.getData().getGeneId());
					gtf.writef("%s\tGENBANK\t%s\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\";\n", 
							tr.getReference().getName(),"transcript",
							tr.getRegion().getStart()+1,tr.getRegion().getEnd(),
							tr.getReference().getStrand().getGff(),
							tr.getData().getGeneId(),
							tr.getData().getTranscriptId());
					for (int p=0; p<tr.getRegion().getNumParts(); p++) {
						gtf.writef("%s\tGENBANK\t%s\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\";\n", 
								tr.getReference().getName(),"exon",
								tr.getRegion().getStart(p)+1,tr.getRegion().getEnd(p),
								tr.getReference().getStrand().getGff(),
								tr.getData().getGeneId(),
								tr.getData().getTranscriptId());
					}
					if (tr.getData().isCoding()) {
						GenomicRegion cds = tr.getData().getCds(tr.getReference(), tr.getRegion());
						for (int p=0; p<cds.getNumParts(); p++) {
							gtf.writef("%s\tGENBANK\t%s\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\";\n", 
									tr.getReference().getName(),"CDS",
									cds.getStart(p)+1,cds.getEnd(p),
									tr.getReference().getStrand().getGff(),
									tr.getData().getGeneId(),
									tr.getData().getTranscriptId());
						}
					}
				}
				gtf.close();
				
				progress.finish();
			}
			
			
		}
		else {
			
			if (annotPath==null || seq==null) {
				if (ensemblOrg!=null && ensemblVer!=null) {
					
					annotPath = ensemblOrg+"."+ensemblVer+".gtf";
					String fastaPath = ensemblOrg+"."+ensemblVer+".fasta";
					String uensemblVer = ensemblVer;
					try {
						if (!new File(annotPath).exists()) {
							progress.init().setDescription("Downloading gtf from ensembl: "+ensemblOrg+" "+ensemblVer);
							String path = "ftp://ftp.ensembl.org/pub/release-"+ensemblVer+"/gtf/"+ensemblOrg+"/";
							String file = EI.wrap(FileUtils.getFtpFolder(path)).filter(s->s.endsWith(uensemblVer+".gtf.gz")).first();
							FileUtils.downloadGunzip(new File(annotPath),path+file,progress);
							progress.finish();
						}
						
						if (!new File(fastaPath).exists()) {
							progress.init().setDescription("Downloading fasta from ensembl: "+ensemblOrg+" "+ensemblVer);
							String path = "ftp://ftp.ensembl.org/pub/release-"+ensemblVer+"/fasta/"+ensemblOrg+"/dna/";
							String file = EI.wrap(FileUtils.getFtpFolder(path)).filter(s->s.endsWith(".dna.primary_assembly.fa.gz")).first();
							if (file==null) {
								file = EI.wrap(FileUtils.getFtpFolder(path)).filter(s->s.endsWith(".dna.toplevel.fa.gz")).first();
								if (file!=null)
									System.err.println("Warning: Downloading toplevel version of genome. Check for patches and haplotypes!");
							}
							FileUtils.downloadGunzip(new File(fastaPath),path+file,progress);
							progress.finish();
						}
					} catch (Exception e) {
						throw new IOException("Could not download files from ensembl; check name and version!",e);
					}
					
					seq = new FastaFile(fastaPath);
					seq.setFastaHeaderParser(new DefaultFastaHeaderParser(' '));
					if (name==null)
						name = ensemblOrg+"."+ensemblVer;
				}
			}
			
			
			transcriptome = annotPath!=null;
			
			if (!transcriptome) {
				prefix = folder==null?seq.getAbsolutePath():new File(folder,FileUtils.getNameWithoutExtension(seq)).toString();
			}
			else 
				prefix = folder==null?annotPath:new File(folder,FileUtils.getNameWithoutExtension(annotPath)).toString();
			
			seqpath = folder==null?FileUtils.getFullNameWithoutExtension(seq)+".fi":new File(folder,FileUtils.getNameWithoutExtension(seq)+".fi").toString();
			if (!new File(seqpath).exists()) {
				progress.init().setDescription("Indexing fasta file in "+seqpath);
				new FastaIndexFile(seqpath).create(seq);
				progress.finish();
			}

			annopath = prefix+".index";
			triepath = prefix+".names";
			genetabpath = prefix+".genes.tab";
			transtabpath = prefix+".transcripts.tab";
			annoStorageClass = null;
			
			if (transcriptome) {
				
				GenomicRegionStorage<Transcript> aaano = null;
				try {
					aaano = (GenomicRegionStorage<Transcript>) WorkspaceItemLoaderExtensionPoint.getInstance().get(Paths.get(annopath+".cit")).load(Paths.get(annopath+".cit"));
				} catch (Throwable e) {}
				if (aaano==null) {
					GenomicRegionStorage<Transcript> cl = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, annopath).add(Class.class, Transcript.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
					FastaIndexSequenceProvider sss = new FastaIndexSequenceProvider(new FastaIndexFile(seqpath).open());
					
					progress.init().setDescription("Indexing annotation file in "+annopath);
					GtfFileReader gtf = new GtfFileReader(annotPath, "exon");
					gtf.setProgress(progress);
					gtf.setTableOutput(genetabpath,transtabpath);
					
					MemoryIntervalTreeStorage<Transcript> mem = ignoreMulti?gtf.readIntoMemoryTakeFirst(new StreamLineWriter(System.err)):gtf.readIntoMemoryThrowOnNonUnique();
					cl.fill(mem.ei().filter(rgr->sss.getSequenceNames().contains(rgr.getReference().getName())));
					progress.finish();
					annoStorageClass = cl.getClass().getSimpleName();
				} else {
					annoStorageClass = aaano.getClass().getSimpleName();
				}
				
				if (!new File(triepath).exists()) {
					FastaIndexSequenceProvider sss = new FastaIndexSequenceProvider(new FastaIndexFile(seqpath).open());
					progress.init().setDescription("Indexing names in "+annopath);
					Trie<ImmutableReferenceGenomicRegion<Void>> trie = new Trie<ImmutableReferenceGenomicRegion<Void>>();
					for (String[]a :new LineOrientedFile(annotPath).lineIterator("#").map(a->StringUtils.split(a, '\t')).filter(a->a[2].equals("gene")).loop()) {
						ImmutableReferenceGenomicRegion<Void> rgr = new ImmutableReferenceGenomicRegion<Void>(Chromosome.obtain(a[0],a[6]), new ArrayGenomicRegion(Integer.parseInt(a[3])-1,Integer.parseInt(a[4])));
						if (sss.getSequenceNames().contains(rgr.getReference().getName())) {
							String id = GtfFileReader.getGtfField("gene_id", a[8]);
							String n = GtfFileReader.getGtfField("gene_name", a[8]);
							if (id!=null) trie.put(id, rgr);
							if (n!=null) trie.put(n, rgr);
						}
					}
					Orm.serialize(triepath, trie);
					progress.finish();
				}
			}
		}
		
		if (name==null && annotPath==null) name = FileUtils.getNameWithoutExtension(seq);
		if (name==null) name = FileUtils.getNameWithoutExtension(annotPath);
		
		String path = Genomic.getGenomicPaths()[0].toString();
		String outfile = output!=null?output:path+"/"+name+".oml";
		LineWriter out = new LineOrientedFile(outfile).write();
		
		out.writef("<Genomic>\n\t<FastaIndexSequenceProvider file=\"%s\" />",new File(seqpath).getAbsolutePath());
		
		
		
		
		
		if (transcriptome) {
		
			out.writef("\n\n\t<Annotation name=\"Transcripts\">\n\t\t<%s file=\"%s\" />\n\t</Annotation>\n\t<NameIndex path=\"%s\" />\n",
					annoStorageClass,new File(annopath).getAbsolutePath(),new File(triepath).getAbsolutePath());
			
			out.writef("\t<GenomicMappingTable from=\"Transcripts\" >\n");
			out.writef("\t\t<Csv file=\"%s\" field=\"transcriptId,proteinId,biotype,source\" />\n",new File(transtabpath).getAbsolutePath());
			out.writef("\t</GenomicMappingTable>\n");
			out.writef("\t<GenomicMappingTable from=\"Genes\" >\n");
			out.writef("\t\t<Csv file=\"%s\" field=\"geneId,symbol,biotype,source\" />\n",new File(genetabpath).getAbsolutePath());
			out.writef("\t</GenomicMappingTable>\n");
			
			FastaFile tr = new FastaFile(prefix+".transcripts.fasta");
			if (!tr.exists()) {
				GenomicRegionStorage<Transcript> cl = (GenomicRegionStorage<Transcript>) WorkspaceItemLoaderExtensionPoint.getInstance().get(Paths.get(annopath+".cit")).load(Paths.get(annopath+".cit"));
				FastaIndexSequenceProvider sss = new FastaIndexSequenceProvider(new FastaIndexFile(seqpath).open());
			
				tr.startWriting();
				for (FastaEntry e : cl.ei().progress(progress, (int)cl.size(), rgr->rgr.toLocationStringRemovedIntrons()).map(rgr->new FastaEntry(rgr.getData().getTranscriptId(), sss.getSequence(rgr).toString())).loop()) 
					tr.writeEntry(e);
				tr.finishWriting();
			}
			progress.init().setDescription("Indexing fasta file in "+tr.getName());
			tr.obtainAndOpenDefaultIndex().close();
			progress.finish();
			
		}

		if (bowtie) {
			try {
				if (new ProcessBuilder().command("bowtie-build", "--version").start().waitFor()!=0) throw new IOException();
			} catch (IOException e) {
				System.err.println("bowtie-build cannot be invoked! Skipping bowtie index, you will not be able to use bowtie for this genome, but everything else will work!");
				bowtie = false;
			}
		}
		
		if (bowtie) {
			
			String indout = prefix+".genomic";
			if (!new File(indout+".1.ebwt").exists()) {
				progress.init().setDescription("Creating genomic bowtie index "+indout);
				ProcessBuilder pb = new ProcessBuilder(
						"bowtie-build",
						new FastaIndexFile(seqpath).open().getFastaFile().getPath(),
						indout
				);
				System.err.println("Calling "+EI.wrap(pb.command()).concat(" "));
				pb.redirectError(Redirect.INHERIT);
				pb.redirectOutput(Redirect.INHERIT);
				pb.start().waitFor();
				progress.finish();
			}
			out.write("\n\t<Info name=\"bowtie-genomic\" info=\""+new File(indout).getAbsolutePath()+"\" />\n");
			if (transcriptome) {
				indout = prefix+".transcriptomic";
				if (!new File(indout+".1.ebwt").exists()) {
						progress.init().setDescription("Creating genomic bowtie index "+indout);
					ProcessBuilder pb = new ProcessBuilder(
							"bowtie-build",
							prefix+".transcripts.fasta",
							indout
					);
					System.err.println("Calling "+EI.wrap(pb.command()).concat(" "));
					pb.redirectError(Redirect.INHERIT);
					pb.redirectOutput(Redirect.INHERIT);
					pb.start().waitFor();
					
					progress.finish();
				}
				out.write("\t<Info name=\"bowtie-transcriptomic\" info=\""+new File(indout).getAbsolutePath()+"\" />\n");
					
			}
		}
		
		
		if (star) {
			try {
				if (new ProcessBuilder().command("STAR", "--version").start().waitFor()!=0) throw new IOException();
			} catch (IOException e) {
				System.err.println("STAR cannot be invoked! Skipping STAR index, you will not be able to use STAR for this genome, but everything else will work!");
				star = false;
			}
		}
		
		if (star) {
			//STAR --runThreadN 24 --runMode genomeGenerate --genomeDir . --genomeFastaFiles Homo_sapiens.GRCh38.dna.primary_assembly.fa --sjdbGTFfile Homo_sapiens.GRCh38.86.gtf
			FastaIndexFile ff = new FastaIndexFile(seqpath).open();
			long len = EI.wrap(ff.getEntryNames()).map(n->new Long(ff.getLength(n))).reduce((a,b)->a+b);
			int nbases = (int)Math.ceil(Math.min(14, Math.log(len)/Math.log(2)/2-1));
			if (fixedNbases>-1)
				nbases = fixedNbases;
			
			String fasta =ff.getFastaFile().getPath();
			String index = new File(new File(fasta).getParentFile(),"STAR-index").getPath();
			if (!new File(index+"/SAindex").exists()) {
				new File(index).mkdirs();
				progress.init().setDescription("Creating STAR index "+index);
				ProcessBuilder pb = new ProcessBuilder(
						"STAR","--runThreadN",Math.max(1, Runtime.getRuntime().availableProcessors()/2)+"",
						"--runMode","genomeGenerate","--genomeDir",
						index,"--genomeFastaFiles",fasta,
						"--genomeSAindexNbases",""+nbases
				);
				if (annotPath!=null)
					pb.command().addAll(Arrays.asList("--sjdbGTFfile",annotPath));
				
				System.err.println("Calling "+EI.wrap(pb.command()).concat(" "));
				pb.redirectError(Redirect.INHERIT);
				pb.redirectOutput(Redirect.INHERIT);
				pb.start().waitFor();
				
				progress.finish();
			}
			out.write("\t<Info name=\"STAR\" info=\""+index+"\" />\n");
		}
		
		if (kallisto && transcriptome) {
			
			String indout = prefix+".kallisto";
			if (!new File(indout).exists()) {
					progress.init().setDescription("Creating kallisto index "+indout);
				ProcessBuilder pb = new ProcessBuilder(
						"kallisto","index","-i",indout,
						prefix+".transcripts.fasta"
				);
				System.err.println("Calling "+EI.wrap(pb.command()).concat(" "));
				pb.redirectError(Redirect.INHERIT);
				pb.redirectOutput(Redirect.INHERIT);
				pb.start().waitFor();
				
				progress.finish();
			}
			out.write("\t<Info name=\"kallisto-transcriptomic\" info=\""+new File(indout).getAbsolutePath()+"\" />\n");
		}
		
		
		out.write("</Genomic>\n");
		out.close();
		
		System.out.println("Written genomic info to "+outfile);
		
		
	}
	

	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("IndexGenome <Options>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -s <fasta-file>\t\tFasta file containing chromosomes");
		System.err.println(" -a <gtf-file>\t\tGtf file containing annotation");
		System.err.println(" -organism <name>\t\tOrganism name to download data from ensembl (e.g. homo_sapiens)");
		System.err.println(" -version <version>\t\tEnsembl version to download (e.g. 75)");
		System.err.println(" -gb <genbank-file>\t\tGenbank file containing annotation and sequence");
		System.err.println(" -gblabel <label>\t\tWhich genbank entry to take as gene and transcript label (default: label)");
		System.err.println(" -gbname <label>\t\tWhich genbank entry to take as gene name in the mapping table (default: same as gblabel)");
		System.err.println(" -gbfeatures <names>\t\tInclude other features as ncRNAs (comma separated, default: empty)");
		System.err.println(" -f <folder>\t\tOutput folder (Default: next to Fasta and Gtf / genbank)");
		System.err.println(" -n <name>\t\tName of the genome for later use (Default: file name of gtf/genbank-file)");
		System.err.println(" -o <file>\t\tSpecify output file (Default: ~/.gedi/genomic/${name}.oml)");
		System.err.println(" -nobowtie\t\t\tDo not create bowtie indices");
		System.err.println(" -nostar\t\t\tDo not create STAR indices");
		System.err.println(" -nbases <nbases>\t\t\tSpecify nbases parameter for STAR (instead of using the formula in the STAR manual)");
		
		System.err.println(" -p\t\t\tShow progress");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println();
		
	}
	
	private static Pattern uniPat = Pattern.compile("^(.*\\.)(\\d+)$");
	private static String adaptLabel(Integer ind, ImmutableReferenceGenomicRegion<String> data, String label) {
		Matcher matcher = uniPat.matcher(label);
		if (matcher.find()) 
			return matcher.group(1)+(Integer.parseInt(matcher.group(2))+1);
		return label+".1";
	}
	
}
