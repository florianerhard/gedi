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
import gedi.core.data.annotation.NameAttributeMapAnnotation;
import gedi.core.data.annotation.Transcript;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
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
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.StreamLineWriter;
import gedi.util.io.text.fasta.DefaultFastaHeaderParser;
import gedi.util.io.text.fasta.FastaEntry;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.io.text.genbank.GenbankFeature;
import gedi.util.io.text.genbank.GenbankFile;
import gedi.util.io.text.tsv.formats.GtfFileReader;
import gedi.util.orm.Orm;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
		String genbank = null;
		String genbankLabel = "label";
		boolean ignoreMulti = false;
		boolean transcriptome = true;
		boolean bowtie = true;
		
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
			else if (args[i].equals("-gblabel")) {
				genbankLabel=checkParam(args, ++i);
			}
			else if (args[i].equals("-f")) {
				folder=checkParam(args, ++i);
			}
			else if (args[i].equals("-nobowtie")) {
				bowtie=false;
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
		
		
		if (genbank==null && seq==null) throw new UsageException("No fasta file given!");
		
		Gedi.startup();
		
		String prefix;
		String seqpath; 
		String annopath;
		String genetabpath;
		String transtabpath;
		String triepath;
		String annoStorageClass;
		
		if (genbank!=null) {
			GenbankFile file = new GenbankFile(genbank);
			
			if (name==null) name = FileUtils.getNameWithoutExtension(genbank);
			
			prefix = folder==null?genbank:new File(folder,FileUtils.getNameWithoutExtension(genbank)).toString();
			annopath = prefix+".index";
			triepath = prefix+".names";
			genetabpath = prefix+".genes.tab";
			transtabpath = prefix+".transcripts.tab";
			
			
			seqpath = prefix+".fasta";
			
			
			progress.init().setDescription("Indexing sequence "+genbank);
			FastaFile ff = new FastaFile(seqpath);
			ff.startWriting();
			ff.writeEntry(new FastaEntry(file.getAccession(),file.getSource().toUpperCase()));
			ff.finishWriting();
			ff.obtainDefaultIndex().create(ff);
			progress.finish();
			seqpath = ff.obtainDefaultIndex().getAbsolutePath();
			
			GenomicRegionStorage<NameAttributeMapAnnotation> full = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, genbank+".full").add(Class.class, NameAttributeMapAnnotation.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
			ReferenceSequence ref = Chromosome.obtain(file.getAccession(),Strand.Plus);
			if (!new File(genbank+".full.cit").exists()) {
				progress.init().setDescription("Indexing full annotation file in "+genbank);
				full.fill(file.featureIterator().map(f->
					new ImmutableReferenceGenomicRegion<NameAttributeMapAnnotation>(
							ref.toStrand(f.getPosition().getStrand()),
							f.getPosition().toGenomicRegion(),
							new NameAttributeMapAnnotation(f.getFeatureName(), f.toSimpleMap()))
				));
				progress.finish();
			}
			
			
			String elabel = genbankLabel;
			
			HashMap<String, ImmutableReferenceGenomicRegion<String>> mrnas = file.featureIterator("mRNA").map(f->
				new ImmutableReferenceGenomicRegion<String>(
					ref.toStrand(f.getPosition().getStrand()),
					f.getPosition().toGenomicRegion(), f.getStringValue(elabel))).indexAdapt(r->r.getData(),v->v,IndexGenome::adaptLabel);
			HashMap<String, ImmutableReferenceGenomicRegion<String>> cdss = file.featureIterator("CDS").map(f->
			new ImmutableReferenceGenomicRegion<String>(
				ref.toStrand(f.getPosition().getStrand()),
				f.getPosition().toGenomicRegion(), f.getStringValue(elabel))).indexAdapt(r->r.getData(),v->v,IndexGenome::adaptLabel);
			
			LineWriter geneout = new LineOrientedFile(genetabpath).write().writef("Gene ID\tGene Symbol\tBiotype\tSource\n");
			LineWriter transout = new LineOrientedFile(transtabpath).write().writef("Transcript ID\tProtein ID\tBiotype\tSource\n");
			
			for (GenbankFeature f : file.featureIterator("CDS").loop()) {
				geneout.writef("%s\t%s\t%s\tgenbank\n",f.getStringValue(elabel),f.getStringValue(elabel),"protein_coding");
				transout.writef("%s\t%s\t%s\tgenbank\n",f.getStringValue(elabel),f.getStringValue("protein_id"),"protein_coding");
			}
			
			geneout.close();
			transout.close();
			
			HashSet<String> genes = new HashSet<String>();
			genes.addAll(mrnas.keySet());
			genes.addAll(cdss.keySet());
			
			progress.init().setDescription("Indexing annotation file in "+genbank).setCount(genes.size());
			MemoryIntervalTreeStorage<Transcript> mem = new MemoryIntervalTreeStorage<Transcript>(Transcript.class);
			for (String gene : genes) {
				progress.incrementProgress();
				ImmutableReferenceGenomicRegion<String> mrna = mrnas.get(gene);
				ImmutableReferenceGenomicRegion<String> cds = cdss.get(gene);
				if (mrna==null) {
					mem.add(cds.getReference(),cds.getRegion(),new Transcript(gene, gene, cds.getRegion().getStart(), cds.getRegion().getEnd()));
				} else if (cds==null) {
					mem.add(mrna.getReference(),mrna.getRegion(),new Transcript(gene, gene, -1,-1));
				} else {
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
				GenomicRegionStorage<Transcript> transcripts = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, annopath).add(Class.class, Transcript.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
				transcripts.fill(mem);
				annoStorageClass = transcripts.getClass().getSimpleName();
			} else {
				annoStorageClass = aaano.getClass().getSimpleName();
			}
			
			progress.init().setDescription("Indexing annotation file in "+genbank).setCount(genes.size());
			Trie<ImmutableReferenceGenomicRegion<Void>> trie = new Trie<ImmutableReferenceGenomicRegion<Void>>();
			for (GenbankFeature g : file.featureIterator("gene").loop()) {
				ImmutableReferenceGenomicRegion<Void> rgr = new ImmutableReferenceGenomicRegion<Void>(ref, g.getPosition().toGenomicRegion());
				String id = g.getStringValue("gene");
				if (id!=null) trie.put(id, rgr);
			}
			Orm.serialize(triepath, trie);
			progress.finish();
			
			
		}
		else {
			
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
						ImmutableReferenceGenomicRegion<Void> rgr = new ImmutableReferenceGenomicRegion<Void>(Chromosome.obtain(a[0],a[6]), new ArrayGenomicRegion(Integer.parseInt(a[3]),Integer.parseInt(a[4])));
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
		LineWriter out = new LineOrientedFile(output!=null?output:Config.getInstance().getConfigFolder()+"/genomic/"+name+".oml").write();
		
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
			
			FastaFile tr = new FastaFile(FileUtils.getFullNameWithoutExtension(seqpath)+".transcripts.fasta");
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
				System.err.println("bowtie-build cannot be invoked! Skipping bowtie index!");
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
				pb.redirectError(Redirect.INHERIT);
				pb.redirectOutput(Redirect.INHERIT);
				pb.start().waitFor();
				progress.finish();
			}
			out.write("\t<Info name=\"bowtie-genomic\" info=\""+new File(indout).getAbsolutePath()+"\" />\n");
			if (transcriptome) {
				indout = prefix+".transcriptomic";
				if (!new File(indout+".1.ebwt").exists()) {
						progress.init().setDescription("Creating genomic bowtie index "+indout);
					ProcessBuilder pb = new ProcessBuilder(
							"bowtie-build",
							FileUtils.getFullNameWithoutExtension(seqpath)+".transcripts.fasta",
							indout
					);

					pb.redirectError(Redirect.INHERIT);
					pb.redirectOutput(Redirect.INHERIT);
					pb.start().waitFor();
					
					progress.finish();
				}
				out.write("\t<Info name=\"bowtie-transcriptomic\" info=\""+new File(indout).getAbsolutePath()+"\" />\n");
					
			}
		}
		
		
		out.write("</Genomic>");
		out.close();
		
		
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
		System.err.println(" -ignoreMulti\t\tIgnore identical transcripts in Gtf file");
		System.err.println(" -gb <genbank-file>\t\tGenbank file containing annotation and sequence");
		System.err.println(" -gblabel <label>\t\tWhich genbank entry to take as gene and transcript label (default: label)");
		System.err.println(" -f <folder>\t\tOutput folder (Default: next to Fasta and Gtf / genbank)");
		System.err.println(" -n <name>\t\tName of the genome for later use (Default: file name of gtf/genbank-file)");
		System.err.println(" -o <file>\t\tSpecify output file (Default: ~/.gedi/genomic/${name}.oml)");
		System.err.println(" -nobowtie\t\t\tDo not create bowtie indices");
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
