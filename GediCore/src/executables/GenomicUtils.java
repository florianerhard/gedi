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

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.script.ScriptException;

import gedi.app.Config;
import gedi.app.Gedi;
import gedi.app.extension.ExtensionContext;
import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.ParseUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.dynamic.impl.MapDynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.io.text.jhp.Jhp;
import gedi.util.io.text.jhp.JhpParameterException;
import gedi.util.job.pipeline.PipelineRunner;
import gedi.util.job.pipeline.PipelineRunnerExtensionPoint;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class GenomicUtils {

	
	private static final Logger log = Logger.getLogger( GenomicUtils.class.getName() );
	public static void main(String[] args) {
		try {
			start(args);
		} catch (UsageException e) {
			usage("An error occurred: "+e.getMessage(), e.additional);
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("An error occurred: "+e.getMessage());
			e.printStackTrace();
		}
	}
	

	private static void usage(String message, String additional) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("GenomicUtils <Options> -m <mode> -g <genomic> [<genomic>...]");
		System.out.println();
		System.out.println("Writes the transcripts from all genomics into a gtf file");
		System.err.println();
		System.err.println("	Options:");
		System.err.println("	-m 				What to do: ");
		System.err.println("						gtf: write gtf file of all transcripts to stdout");
		System.err.println("						star: create STAR index in current directory");
		System.err.println("	-g 				Genomic names");
		System.err.println();
		System.err.println("	-source 			What to write into the source colum for gtf output (default: gedi)");
		System.err.println("	-fixedNbases 		Nbases parameter for STAR index generation (compute otherwise as defined in the STAR manual)");
		System.err.println("	-p 				Output progress");
		System.err.println("");
		if (additional!=null) {
			System.err.println(additional);
			System.err.println("");
		}
	}
	
	
	private static class UsageException extends Exception {
		String additional = null;
		public UsageException(String msg) {
			super(msg);
		}
		public UsageException(String msg, String additional) {
			super(msg);
			this.additional = additional;
		}
	}
	

	private static int checkMultiParam(String[] args, int index, ArrayList<String> re) throws UsageException {
		while (index<args.length && !args[index].startsWith("-")) 
			re.add(args[index++]);
		return index-1;
	}
	private static String checkParam(String[] args, int index) throws UsageException {
		if (index>=args.length || args[index].startsWith("-")) throw new UsageException("Missing argument for "+args[index-1]);
		return args[index];
	}
	private static String[] checkPair(String[] args, int index) throws UsageException {
		int p = args[index].indexOf('=');
		if (!args[index].startsWith("--") || p==-1) throw new UsageException("Not an assignment parameter (--name=val): "+args[index]);
		return new String[] {args[index].substring(2, p),args[index].substring(p+1)};
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
	
	private static enum Mode {
		gtf,star
	}
	
	
	private static void start(String[] args) throws UsageException, IOException, InterruptedException {
		Gedi.startup(true);
		
		String source = "gedi";
		Genomic g = null;
		Progress progress = new NoProgress();
		Mode mode = null;
		int fixedNbases = -1;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null,null);
				System.exit(0);
			}
			else if (args[i].equals("-g")) {
				ArrayList<String> gnames = new ArrayList<>();
				i = checkMultiParam(args, ++i, gnames);
				g = Genomic.get(gnames);
			}
			else if (args[i].equals("-s")) {
				source = checkParam(args, ++i);
			}
			else if (args[i].equals("-m")) {
				mode = ParseUtils.parseEnumNameByPrefix(checkParam(args, ++i),true,Mode.class);
			}
			else if (args[i].equals("-m")) {
				fixedNbases = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-p")) {
				progress = new ConsoleProgress(System.err);
			}
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}
		
		if (g==null)
			throw new UsageException("No genome given!");
		if (mode==null)
			throw new UsageException("No mode given!");
		
		switch (mode) {
		case gtf:
			writeGtf(g, new LineOrientedFile().write(), progress, source);
			break;
		case star:
			createStar(g, progress, fixedNbases);
			break;
		}
		
		
		
	}


	private static void createStar(Genomic g, Progress progress, int fixedNbases) throws IOException, InterruptedException {
		File gtf = File.createTempFile("STAR", ".gtf");
		writeGtf(g, new LineOrientedFile(gtf.getPath()).write(), progress, "gedi");
		
		long len = 0;
		for (String ff : g.getGenomicFastaFiles().loop()) 
			len+=new File(ff).getTotalSpace();
		
		
		//STAR --runThreadN 24 --runMode genomeGenerate --genomeDir . --genomeFastaFiles Homo_sapiens.GRCh38.dna.primary_assembly.fa --sjdbGTFfile Homo_sapiens.GRCh38.86.gtf
		int nbases = (int)Math.ceil(Math.min(14, Math.log(len)/Math.log(2)/2-1));
		if (fixedNbases>-1)
			nbases = fixedNbases;
		
		String index = new File(g.getId().replace(',', '_')+".STAR-index").getPath();
		new File(index).mkdirs();
		progress.init().setDescription("Creating STAR index "+index);
		ArrayList<String> param = new ArrayList<>();
		param.addAll(Arrays.asList(
				"STAR","--runThreadN",Math.max(1, Runtime.getRuntime().availableProcessors()/2)+"",
				"--runMode","genomeGenerate","--genomeDir",
				index,"--genomeFastaFiles")
				);
		g.getGenomicFastaFiles().toCollection(param);
		param.addAll(Arrays.asList(
				"--genomeSAindexNbases",""+nbases,
				"--sjdbGTFfile",gtf.getPath()
				));
		
		ProcessBuilder pb = new ProcessBuilder(param);
		
		System.err.println("Calling "+EI.wrap(pb.command()).concat(" "));
		pb.redirectError(Redirect.INHERIT);
		pb.redirectOutput(Redirect.INHERIT);
		pb.start().waitFor();
		
		gtf.delete();
		progress.finish();
	}


	private static void writeGtf(Genomic g, LineWriter gtf, Progress progress, String source) throws IOException {
		MemoryIntervalTreeStorage<Transcript> trs = g.getTranscripts();
		progress.init().setDescription("Output GTF for "+g.getId()).setCount((int) trs.size());
		for (ImmutableReferenceGenomicRegion<Transcript> tr : trs.ei().loop()) {
			gtf.writef("%s\t%s\t%s\t%d\t%d\t.\t%s\t.\tgene_id \"%s\";\n", 
					tr.getReference().getName(),source,"gene",
					tr.getRegion().getStart()+1,tr.getRegion().getEnd(),
					tr.getReference().getStrand().getGff(),
					tr.getData().getGeneId());
			gtf.writef("%s\t%s\t%s\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\";\n", 
					tr.getReference().getName(),source,"transcript",
					tr.getRegion().getStart()+1,tr.getRegion().getEnd(),
					tr.getReference().getStrand().getGff(),
					tr.getData().getGeneId(),
					tr.getData().getTranscriptId());
			for (int p=0; p<tr.getRegion().getNumParts(); p++) {
				gtf.writef("%s\t%s\t%s\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\";\n", 
						tr.getReference().getName(),source,"exon",
						tr.getRegion().getStart(p)+1,tr.getRegion().getEnd(p),
						tr.getReference().getStrand().getGff(),
						tr.getData().getGeneId(),
						tr.getData().getTranscriptId());
			}
			if (tr.getData().isCoding()) {
				GenomicRegion cds = tr.getData().getCds(tr.getReference(), tr.getRegion());
				for (int p=0; p<cds.getNumParts(); p++) {
					gtf.writef("%s\t%s\t%s\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\";\n", 
							tr.getReference().getName(),source,"CDS",
							cds.getStart(p)+1,cds.getEnd(p),
							tr.getReference().getStrand().getGff(),
							tr.getData().getGeneId(),
							tr.getData().getTranscriptId());
				}
			}
			progress.incrementProgress();
		}
		gtf.close();
		progress.finish();
	}
}


