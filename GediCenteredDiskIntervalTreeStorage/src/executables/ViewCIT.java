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

import gedi.app.Gedi;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.ScoreAnnotation;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.DigitalAlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.longcollections.LongArrayList;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.TriFunction;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.tsv.formats.BedEntry;
import gedi.util.nashorn.JSTriFunction;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class ViewCIT {

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
		if (index>=args.length) throw new UsageException("Missing argument for "+args[index-1]);
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
	private static int checkMultiParam(String[] args, int index, ArrayList<String> re) throws UsageException {
		while (index<args.length && !args[index].startsWith("-")) 
			re.add(args[index++]);
		return index-1;
	}
	public static <T> void start(String[] args) throws Exception {
		Gedi.startup();
		
		LineOrientedFile output = new LineOrientedFile(LineOrientedFile.STDOUT);
		
		Progress progress = new NoProgress();
		String query = null;
		String nameExpr = null;
		String scoreExpr = null;
		String filterExpr = null;
		String tExpr = null;
		CitOutputMode mode = null;
		boolean debug = false;
		boolean list = false;
				
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-l")) {
				list=true;
			}
			else if (args[i].equals("-p")) {
				progress = new ConsoleProgress(System.err);
			}
			else if (args[i].equals("-q")) {
				query=checkParam(args, ++i);
			}
			else if (args[i].equals("-m")) {
				mode = ParseUtils.parseEnumNameByPrefix(checkParam(args, ++i), true, CitOutputMode.class);
			}
			else if (args[i].equals("-name")) {
				nameExpr=checkParam(args, ++i);
			}
			else if (args[i].equals("-score")) {
				scoreExpr=checkParam(args, ++i);
			}
			else if (args[i].equals("-t")) {
				tExpr=checkParam(args, ++i);
			}
			else if (args[i].equals("-filter")) {
				filterExpr=checkParam(args, ++i);
			}
			else if (args[i].equals("-o")) {
				output = new LineOrientedFile(checkParam(args, ++i));
			}
			else if (args[i].equals("-D")){
				debug = true;
			} 
			else if (!args[i].startsWith("-")) 
					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
		}
		
		
		if (!new File(args[i]).exists()) {
			usage("File "+args[i]+" does not exist!");
			System.exit(1);
		}
		
		TriFunction<T, ReferenceSequence, GenomicRegion, Boolean> filter = filterExpr!=null?new JSTriFunction<T, ReferenceSequence, GenomicRegion, Boolean>(false,"function(d,ref,reg) "+filterExpr):null;
		
		CenteredDiskIntervalTreeStorage<T> storage = new CenteredDiskIntervalTreeStorage<T>(args[i]);
		
		if (debug)
			System.err.println(storage.getExtendedJson());
		
		if (list) {
			output.startWriting();
			output.writeLine(args[i]+": "+storage.getType().getName());
			output.writeLine(storage.getExtendedJson());
			ReferenceSequence[] refs = (ReferenceSequence[]) storage.getReferenceSequences().toArray(new ReferenceSequence[0]);
			Arrays.sort(refs);
			for (ReferenceSequence r : refs) {
				output.writef("%s%s\t%d\n",r.getName(),r.getStrand(),storage.size(r));
			}
			output.writeLine("----------------");
			output.writef("Total\t%d\n",storage.size());
			output.finishWriting();
			return;
		}
		
		if (mode==CitOutputMode.Cit && output.isPipe())
			throw new UsageException("For mode Cit, specify output file!");
		
		Class<T> type = storage.getType();
		if (tExpr!=null) {
			if (tExpr.equals("digital")) type = (Class<T>) DigitalAlignedReadsData.class;
		}
		
		LineWriter wr = mode!=CitOutputMode.Cit?output.write():null;
		CenteredDiskIntervalTreeStorage citout = mode==CitOutputMode.Cit?new CenteredDiskIntervalTreeStorage(output.getPath(), type):null;
		
		
		ExtendedIterator<ImmutableReferenceGenomicRegion<T>> it = storage.ei(query);
		if (!(progress instanceof NoProgress))
			it = it.progress(progress, (int) storage.size(), r->r.toMutable().transformRegion(reg->reg.removeIntrons()).toLocationString());
		if (filter!=null)
			it = it.filter(rgr->filter.apply(rgr.getData(), rgr.getReference(), rgr.getRegion()));
		if (tExpr!=null) {
			if (tExpr.equals("digital")) {
				it = it.map(rgr->{
					MutableReferenceGenomicRegion<T> re = rgr.toMutable().transformData(ard->(T)DigitalAlignedReadsData.fromAlignedReadsData((AlignedReadsData)ard, true));
					if (re.getData()==null) return null;
					return re.toImmutable();
				}).removeNulls();
			}
		}
		
		Consumer<ImmutableReferenceGenomicRegion<T>> sink = null;
		
		if (storage.getType()==LongArrayList.class && new File(StringUtils.removeFooter(args[0], ".cit")).exists() && (mode==null || mode==CitOutputMode.Indexed)) {
			PageFile ext = new PageFile(StringUtils.removeFooter(args[0], ".cit"));
			sink = rgr->{
				try {
					wr.writeLine(rgr.toLocationString());
					LongArrayList l = (LongArrayList) rgr.getData();
					for (int j=0; j<l.size(); j++) {
						ext.position(l.getLong(j));
					
						wr.writeLine(ext.readLine());
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				
			};
			it = it.endAction(()->{
				try {
					ext.close();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			});
		}
		else if (mode==CitOutputMode.Bed) {

			Gedi.startup(true);
			TriFunction<T, ReferenceSequence, GenomicRegion, Double> scorer = scoreExpr!=null?new JSTriFunction<T, ReferenceSequence, GenomicRegion, Double>(false,"function(d,ref,reg) "+scoreExpr):(d,ref,reg)->0.0;
			TriFunction<T, ReferenceSequence, GenomicRegion, String> namer = nameExpr!=null?new JSTriFunction<T, ReferenceSequence, GenomicRegion, String>(false,"function(d,ref,reg) "+nameExpr):(d,ref,reg)->".";
			String unameExpr = nameExpr;
			
			sink = rgr->{
				try {
					Object n = namer.apply(rgr.getData(), rgr.getReference(), rgr.getRegion());
					ScoreNameAnnotation sc = new ScoreNameAnnotation(
							StringUtils.toString(n),
							scorer.apply(rgr.getData(), rgr.getReference(), rgr.getRegion()));
					BedEntry be = new BedEntry(new ImmutableReferenceGenomicRegion<ScoreNameAnnotation>(rgr.getReference(), rgr.getRegion(), sc));
					if (rgr.getData() instanceof Transcript) {
						Transcript t = (Transcript) rgr.getData();
						if (t.isCoding()) {
							be.setThickStart(t.getCodingStart());
							be.setThickEnd(t.getCodingEnd());
						}
						if (unameExpr==null)
							be.setName(t.getTranscriptId());
					}
					wr.writeLine(be.toString());
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			};
			
		}
		else if (mode==CitOutputMode.BedGraph) {

			Gedi.startup(true);
			TriFunction<T, ReferenceSequence, GenomicRegion, Double> scorer = scoreExpr!=null?new JSTriFunction<T, ReferenceSequence, GenomicRegion, Double>(false,"function(d,ref,reg) "+scoreExpr):(d,ref,reg)->0.0;
			sink = rgr->{
				try {
					ScoreAnnotation sc = new ScoreAnnotation(
							scorer.apply(rgr.getData(), rgr.getReference(), rgr.getRegion()));
					for (int p=0; p<rgr.getRegion().getNumParts(); p++)
						wr.writef("%s\t%d\t%d\t%.1f\n",rgr.getReference().getName(),rgr.getRegion().getStart(p),rgr.getRegion().getEnd(p),sc.getScore());
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			};
			
		}
		else if (mode==CitOutputMode.Gtf) {

			Gedi.startup(true);
			sink = rgr->{
				try {
					if (!(rgr.getData() instanceof Transcript)) throw new RuntimeException("Gtf output only possible for Transcripts!");
					
					Transcript trans = (Transcript) rgr.getData();
					
					wr.writef("%s\tgedi\ttranscript\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\"; gene_biotype \"protein_coding\";\n",
							rgr.getReference().getName(),
							rgr.getRegion().getStart()+1,
							rgr.getRegion().getEnd(),
							rgr.getReference().getStrand().toString(),
							((Transcript)rgr.getData()).getGeneId(),
							((Transcript)rgr.getData()).getTranscriptId()
							);
					for (int p=0; p<rgr.getRegion().getNumParts(); p++) 
						wr.writef("%s\tgedi\texon\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\"; gene_biotype \"protein_coding\";\n",
								rgr.getReference().getName(),
								rgr.getRegion().getStart(p)+1,
								rgr.getRegion().getEnd(p),
								rgr.getReference().getStrand().toString(),
								trans.getGeneId(),
								trans.getTranscriptId()
								);
					
					if (trans.isCoding()) {
						MutableReferenceGenomicRegion<T> cds = trans.getCds(rgr);
						for (int p=0; p<cds.getRegion().getNumParts(); p++) 
							wr.writef("%s\tgedi\tCDS\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\"; gene_biotype \"protein_coding\";\n",
									cds.getReference().getName(),
									cds.getRegion().getStart(p)+1,
									cds.getRegion().getEnd(p),
									cds.getReference().getStrand().toString(),
									trans.getGeneId(),
									trans.getTranscriptId()
									);
						GenomicRegion start = cds.map(new ArrayGenomicRegion(0,3));
						GenomicRegion stop = cds.map(new ArrayGenomicRegion(cds.getRegion().getTotalLength()-3,cds.getRegion().getTotalLength()));
						wr.writef("%s\tgedi\tstart_codon\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\"; gene_biotype \"protein_coding\";\n",
								cds.getReference().getName(),
								start.getStart()+1,
								start.getEnd(),
								cds.getReference().getStrand().toString(),
								trans.getGeneId(),
								trans.getTranscriptId()
								);
						wr.writef("%s\tgedi\tstop_codon\t%d\t%d\t.\t%s\t.\tgene_id \"%s\"; transcript_id \"%s\"; gene_biotype \"protein_coding\";\n",
								cds.getReference().getName(),
								stop.getStart()+1,
								stop.getEnd(),
								cds.getReference().getStrand().toString(),
								trans.getGeneId(),
								trans.getTranscriptId()
								);
					}
					
					
					
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			};
			
		}
		else if (mode==CitOutputMode.Genepred) {

			sink = rgr->{
				try {
					Transcript t = (Transcript) rgr.getData();
					
					if (t.isCoding())
						wr.writef2("%s\t%s\t%s\t%s\t%d\t%d\t%d\t%d\t%d\t%s\t%s\n",
								t.getGeneId(),t.getTranscriptId(),rgr.getReference().getName(),rgr.getReference().getStrand(),
								rgr.getRegion().getStart(),rgr.getRegion().getEnd(),
								t.getCodingStart(),t.getCodingEnd(), rgr.getRegion().getNumParts(),
								StringUtils.concat(",", rgr.getRegion().getStarts()),
								StringUtils.concat(",", rgr.getRegion().getEnds())
								);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			};
			
		}
		else if (mode==CitOutputMode.Cit) {
			citout.fill(it);
		}
		else {
			TriFunction<T, ReferenceSequence, GenomicRegion, String> namer = nameExpr!=null?new JSTriFunction<T, ReferenceSequence, GenomicRegion, String>(false,"function(d,ref,reg) "+nameExpr):(d,ref,reg)->d.toString();
			
			sink = rgr->{
				try {
					Object n = namer.apply(rgr.getData(), rgr.getReference(), rgr.getRegion());
					wr.writeLine(rgr.toLocationString()+"\t"+StringUtils.toString(n));
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			};
		}
		
		if (sink!=null)
			it.forEachRemaining(sink);
		
		if (wr!=null)
			wr.close();
	}

	
	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("ViewCIT <Options> <file>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -l \t\tOnly list the number of elements per reference");
		System.err.println(" -q <location>\t\tOnly output elements overlapping the given query (i.e. whole reference or genomic region)");
		System.err.println(" -o <file>\t\tSpecify output file (Default: stdout)");
		System.err.println(" -m <mode>\t\toutput mode: Bed/Location/Cit/Genepred/Gtf (Default: location)");
		System.err.println(" -t <js/name>\t\tname of a transformer or javascript function body transform reference genomic region (variable d is current data, ref the reference, reg the region; either return a data object or a reference genomic region object!)");
		System.err.println(" -name <js>\t\tjavascript function body to generate name (variable d is current data, ref the reference, reg the region)");
		System.err.println(" -score <js>\t\tjavascript function body to generate score (variable d is current data, ref the reference, reg the region)");
		System.err.println(" -filter <js>\\t\tjavascript function body returning true for entries to use  (variable d is current data, ref the reference, reg the region)");
		System.err.println();
		System.err.println(" -p\t\t\tShow progress");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println();
		
	}
	
	
	private enum CitOutputMode {
		Location,Bed,Indexed,Cit,Genepred,Gtf,BedGraph
	}
	
}
