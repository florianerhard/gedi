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

package gedi.lfc;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.processing.CombinedGenomicRegionProcessor;
import gedi.core.processing.FillStorageProcessor;
import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.OverlapMode;
import gedi.core.processing.ProcessorContext;
import gedi.core.processing.ReadsWriterProcessor;
import gedi.core.processing.sources.GeneProcessorSource;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.ReferenceSequenceConversion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.lfc.downsampling.DigitalDownsampling;
import gedi.lfc.downsampling.LogDownsampling;
import gedi.lfc.downsampling.LogscDownsampling;
import gedi.lfc.downsampling.MaxDownsampling;
import gedi.lfc.downsampling.MinDownsampling;
import gedi.lfc.downsampling.NoDownsampling;
import gedi.lfc.downsampling.SquareRootDownsampling;
import gedi.lfc.full.ProcessGenome;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.datastructure.tree.redblacktree.SimpleInterval;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.formats.GtfFileReader;
import gedi.util.mutable.MutableMonad;
import gedi.util.mutable.MutableTriple;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Consumer;

public class Count {
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
	
	public enum StrandMode {
		Yes,No,Reverse
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
	
	public static void start(String[] args) throws Exception {
		
		ArrayList<Downsampling> ds = new ArrayList<Downsampling>(Arrays.asList(new LogscDownsampling()));
		
		StrandMode strandspecific = StrandMode.No;
		LineOrientedFile output = new LineOrientedFile(LineOrientedFile.STDOUT);
		HashSet<String> restrict = null;
		int minaqual = 0;
		
		HashMap<String,Downsampling> downSampling = new HashMap<String,Downsampling>();
		downSampling.put("dig", new DigitalDownsampling());
		downSampling.put("log", new LogDownsampling());
		downSampling.put("logsc", new LogscDownsampling());
		downSampling.put("max", new MaxDownsampling());
		downSampling.put("min", new MinDownsampling());
		downSampling.put("no", new NoDownsampling());
		downSampling.put("sqrt", new SquareRootDownsampling());
		
		boolean progress = false;
		boolean removeMultiMapping = false;
		boolean removeOverlapMultiMapping = false;
		boolean ignoreProperPair = false;
		double credi = 0.05;
		boolean outputall = false;
		
		String gtfFeatureType = "exon";
		String citOut = null;
		OverlapMode mode = OverlapMode.Contained;
		
		int minCond = -1;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-p")) {
				progress=true;
			}
			else if (args[i].equals("-d")) {
				ds.clear();
				for (String dsn : StringUtils.split(checkParam(args,++i), ',')) {
					if (dsn.equalsIgnoreCase("all"))
						ds.addAll(downSampling.values());
					else {
						Downsampling d = downSampling.get(dsn.toLowerCase());
						if (d==null) throw new UsageException("Downsampling type "+dsn+" unknown!");
						ds.add(d);
					}
				}
			}
			else if (args[i].equals("-r")) {
				File f = new File(args[++i]);
				if (f.exists())
					restrict = new HashSet<>(Arrays.asList(FileUtils.readAllLines(f)));
				else
					restrict = new HashSet<>(Arrays.asList(StringUtils.split(args[i], ',')));
			}
			else if (args[i].equals("-s")) {
				strandspecific = ParseUtils.parseEnumNameByPrefix(checkParam(args,++i), true, StrandMode.class);
			}
			else if (args[i].equals("-o")) {
				output = new LineOrientedFile(checkParam(args,++i));
			}
			else if (args[i].equals("-b")) {
				removeMultiMapping = true;
			}
			else if (args[i].equals("-v")) {
				removeOverlapMultiMapping = true;
			}
			else if (args[i].equals("-a")) {
				minaqual = checkIntParam(args,++i);
			}
			else if (args[i].equals("-A")) {
				outputall = true;
			}
			else if (args[i].equals("-C")) {
				minCond = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-i")) {
				ignoreProperPair = true;
			}
			else if (args[i].equals("-t")) {
				gtfFeatureType = checkParam(args, ++i);
			}
			else if (args[i].equals("-m")) {
				mode = ParseUtils.parseEnumNameByPrefix(checkParam(args, ++i),true,OverlapMode.class);
				if (mode==null) throw new UsageException("Mode "+args[i]+" unknown!");
			}
			else if (args[i].equals("-c")) {
				credi = 1-checkDoubleParam(args, ++i);
			}
			else if (args[i].equals("-nolfc")) {
				credi = Double.NaN;
			}
			else if (args[i].equals("-i")) {
				System.err.println("Warning: -i not supported!");
			}
			else if (args[i].equals("-cit")) {
				citOut = checkParam(args, ++i);
			}
			else if (args[i].equals("-D")) {
			}
			else if (!args[i].startsWith("-")) 
					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}
		
		MutableTriple<GenomicRegionStorage<AlignedReadsData>,ContrastMapping,ContrastMapping> storage = parseStorage(strandspecific,minaqual,removeMultiMapping,args,i, ignoreProperPair, args.length-1);
		GenomicRegionStorage<Transcript> annotation = parseAnnotation(args[args.length-1],gtfFeatureType);
		
		GenomicRegionProcessor pr; 
		if (ds.size()>1) {
			if (output.isPipe()) throw new UsageException("Output must be a file name for multiple downsampling modes!");
			String pref = output.getPath().lastIndexOf('.')>=0?output.getPath().substring(0, output.getPath().lastIndexOf('.')):"";
			String suff = output.getPath().substring(1+output.getPath().lastIndexOf('.'));
			CombinedGenomicRegionProcessor cpr = new CombinedGenomicRegionProcessor();
			for (String k : downSampling.keySet())
				cpr.add(new LfcAlignedReadsProcessor(storage.Item2, storage.Item3, downSampling.get(k), new LineOrientedFile(pref+"."+k+"."+suff))
						.setCredible(credi)
						.setAllreads(outputall)
						.setMinConditionsWithReads(minCond));
			pr = cpr;
		}
		else
			pr = new LfcAlignedReadsProcessor(storage.Item2, storage.Item3, ds.get(0), output)
					.setCredible(credi)
					.setAllreads(outputall)
					.setTranscripts(annotation)
					.setMinConditionsWithReads(minCond);
		
		
		ReferenceSequenceConversion conv = ReferenceSequenceConversion.none;
		if (strandspecific==StrandMode.Reverse) conv = ReferenceSequenceConversion.toOpposite;
		
//		if (restrict!=null) {
			GeneProcessorSource p = new GeneProcessorSource();
			p.setOverlapMode(mode);
			if (removeOverlapMultiMapping)
				p.setRemoveMultiMappingReads(true, strandspecific!=StrandMode.No);
			
			if (progress)
				p.setProgress(new ConsoleProgress());
			
			if (citOut!=null) {
				CombinedGenomicRegionProcessor cpr = pr instanceof CombinedGenomicRegionProcessor?(CombinedGenomicRegionProcessor)pr:new CombinedGenomicRegionProcessor(pr);
				// TODO write metadata file
							
				CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> cit = new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>(citOut, DefaultAlignedReadsData.class);
				cpr.add(new FillStorageProcessor(cit));
				pr = cpr;
			}
			
	//		if (outputall!=null) {
	//			CombinedGenomicRegionProcessor cpr = pr instanceof CombinedGenomicRegionProcessor?(CombinedGenomicRegionProcessor)pr:new CombinedGenomicRegionProcessor(pr);
	//						
	//			cpr.add(new ReadsWriterProcessor(outputall));
	//			pr = cpr;
	//		}
			
			if (restrict!=null)
				p.setRestrict(restrict);
			p.process(storage.Item1, conv, annotation, pr);
//		}
//		else {
//			new ProcessGenome(pr, progress, removeMultiMapping, mode, strandspecific, restrict).process(storage.Item1, annotation);
//		}
		
	}

	

	
	private static GenomicRegionStorage<Transcript> parseAnnotation(
			String arg, String gtfFeatureType) throws IOException, UsageException {
		
		try {
			if (!new File(arg).exists()) {
				// try parse as region string
				MemoryIntervalTreeStorage<Transcript> re = new MemoryIntervalTreeStorage<Transcript>(Transcript.class);
				int n = 0;
				for (String a : StringUtils.split(arg, ','))
					re.add(new MutableReferenceGenomicRegion<Transcript>().parse(a,new Transcript("User"+n, "User"+(n++), -1, -1)));
				return re;
			}
		} catch(Exception e){}
		
		String type = arg.substring(arg.lastIndexOf('.')+1).toLowerCase();
		if (type.equals("gtf")) {
			MemoryIntervalTreeStorage<Transcript> re;
			if (gtfFeatureType.equalsIgnoreCase("all"))
				re = new GtfFileReader(arg).readIntoMemoryTakeFirst();
			else
				re = new GtfFileReader(arg,gtfFeatureType).readIntoMemoryTakeFirst();
			if (re.size()==0) throw new UsageException("Annotation empty; try using another GTF feature type (parameter -t)!");
			return re;
		}
		if (type.equals("cit")) 
			return  new CenteredDiskIntervalTreeStorage<Transcript>(arg);
		if (type.equals("pos")) {
			MemoryIntervalTreeStorage<Transcript> re = new MemoryIntervalTreeStorage<Transcript>(Transcript.class);
			new LineOrientedFile(arg).lineIterator().map(s->StringUtils.split(s,'\t')).forEachRemaining(f->re.add(new MutableReferenceGenomicRegion<Transcript>().parse(f[1],new Transcript(f[0], f[0], -1, -1))));
			return re;
		}
		
		throw new UsageException("Unknown annotation file: "+arg+" Type: "+type);
		
	}

	private static MutableTriple<GenomicRegionStorage<AlignedReadsData>,ContrastMapping,ContrastMapping> parseStorage(StrandMode mode, int minaqual, boolean removeMultimapping,
			String[] args, int i, boolean ignoreProperPair, int j) throws IOException, UsageException {
		
		
		if (j<=i || i>=args.length) throw new UsageException("No alignment files and annotation file given!");
		
		
		ContrastMapping before = new ContrastMapping();
		ContrastMapping after = new ContrastMapping();
		
		
		String type = args[i].substring(args[i].lastIndexOf('.')+1).toLowerCase();
		if (type.equals("mapping")) {
			if (i+1!=j) throw new UsageException("Only a single mapping file allowed in mode 1!");
			// mode 1: bam mapping files
			
			HashMap<String,Integer> beforeIndex = new HashMap<String, Integer>();
			HashMap<String,Integer> afterIndex = new HashMap<String, Integer>();
			ArrayList<String> files = new ArrayList<String>();
			
			Iterator<String> it = new LineOrientedFile(args[i]).lineIterator("#");
			while (it.hasNext()) {
				String[] f = StringUtils.split(it.next(), '\t');
				if (f.length==3) {
					before.addMapping(files.size(), beforeIndex.computeIfAbsent(f[1]+"\t"+f[2], k->beforeIndex.size()));
					after.addMapping(before.getMappedIndex(files.size()), afterIndex.computeIfAbsent(f[1], l->afterIndex.size()),f[1]);
				} else if (f.length==2) {
					before.addMapping(files.size(), beforeIndex.computeIfAbsent(f[1], k->beforeIndex.size()));
					after.addMapping(before.getMappedIndex(files.size()), afterIndex.computeIfAbsent(f[1], l->afterIndex.size()),f[1]);
				} else throw new UsageException("Mapping file has neither 2 nor 3 columns!");
				files.add(f[0]);
			}
			
			BamGenomicRegionStorage bams = new BamGenomicRegionStorage(mode!=StrandMode.No, files.toArray(new String[0]));
			bams.setIgnoreProperPair(ignoreProperPair);
			bams.setIgnoreVariations(true);
			if (removeMultimapping)
				bams.setRemoveMultiMapping(true);
			if (minaqual>0)
				bams.setMinimalAlignmentQuality(minaqual);
			return new MutableTriple<GenomicRegionStorage<AlignedReadsData>, ContrastMapping, ContrastMapping>(bams,before,after);
		}
		
		if (type.equals("bam")) {
			// mode 2: multiple bam files
			ArrayList<String> files = new ArrayList<String>();
			for (int index=i; index<j; index++) {
				before.addMapping(files.size(), files.size());
				after.addMapping(files.size(), files.size(),args[index]);
				files.add(args[index]);
			}
			BamGenomicRegionStorage bams = new BamGenomicRegionStorage(mode!=StrandMode.No, files.toArray(new String[0]));
			bams.setIgnoreProperPair(ignoreProperPair);
			bams.setIgnoreVariations(true);
			if (removeMultimapping)
				bams.setRemoveMultiMapping(true);
			if (minaqual>0)
				bams.setMinimalAlignmentQuality(minaqual);
			return new MutableTriple<GenomicRegionStorage<AlignedReadsData>, ContrastMapping, ContrastMapping>(bams,before,after);
		}
		
		if (type.equals("cit")) {
			// mode 3: a cit file
			CenteredDiskIntervalTreeStorage<AlignedReadsData> cit = new CenteredDiskIntervalTreeStorage<AlignedReadsData>(args[i]);
			
			if (i+1==j) {
				AlignedReadsData ard = cit.exists()?cit.getRandomRecord():null;
				if (ard==null) throw new UsageException("CIT is empty: "+args[i]);
				
				for (int index=0; index<ard.getNumConditions(); index++) {
					before.addMapping(index,index);
					after.addMapping(index,index);
				}
				return new MutableTriple<GenomicRegionStorage<AlignedReadsData>, ContrastMapping, ContrastMapping>(cit,before,after);
			}
			if (i+2==j) {
				HashMap<String,Integer> beforeIndex = new HashMap<String, Integer>();
				HashMap<String,Integer> afterIndex = new HashMap<String, Integer>();
				
				Iterator<String> it = new LineOrientedFile(args[i+1]).lineIterator("#");
				while (it.hasNext()) {
					String[] f = StringUtils.split(it.next(), '\t');
					int index = Integer.parseInt(f[0]); 
					before.addMapping(index, beforeIndex.computeIfAbsent(f[1]+"\t"+f[2], k->beforeIndex.size()));
					after.addMapping(before.getMappedIndex(index), afterIndex.computeIfAbsent(f[1], l->afterIndex.size()),f[1]);
					
				}
				
				return new MutableTriple<GenomicRegionStorage<AlignedReadsData>, ContrastMapping, ContrastMapping>(cit,before,after);
			}
			
			
		}
		
		throw new UsageException("Unknown alignment file: "+args[i]);
		
	}

	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("java -jar lfc.jar <Options> <Alignment> <Annotation>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -b \t\t\tIgnore ambigous (multimapping) reads from bam files");
		System.err.println(" -v \t\t\tIgnore reads mapping at overlaps of genes");
		System.err.println(" -d <modes>\t\tDownsamling modes: Comma separated list of no,dig,log,logsc,max,min,sqrt (Use 'all' to include all; Default: logsc)");
		System.err.println(" -s <mode>\t\tsequencing was strand specific? (Yes/No/Reverse; Default: No)");
		System.err.println(" -o <file>\t\tSpecify output file (Default: stdout)");
		System.err.println(" -r <file>\t\tList of or file containing gene ids (one per line) that should considered (Default: don't restrict analysis)");
		System.err.println(" -m <mode>\t\tOverlap mode, one of Contained,ContainedUnspliced,Intersected (Default: Contained)");
		System.err.println(" -a <qual>\t\tMinimal alignment quality to use from bam files (Default: 0)");
		System.err.println(" -i \t\t\tIgnore proper pair flag in BAM file");
		System.err.println(" -t <feature>\t\tFeature type to use from gtf file; use 'all' for all features at once (Default: exon)");
		System.err.println(" -c <quantile>\t\tCompute symmetric log fold change credible intervals of given quantile (Default: 0.95)");
		System.err.println(" -nolfc\t\t\tDo not compute fold changes, count reads!");
		System.err.println(" -cit <file>\t\tWrite CIT file");
		System.err.println(" -A \t\t\tReport all reads instead of summarized read counts");
		System.err.println(" -C <count>\t\tConsider only locations, where at least <count> conditions have reads");
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println(" -p\t\t\tShow progress");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println();
		System.err.println("Alignment:");
		System.err.println("One of three modes:");
		System.err.println(" 1.: A bam mapping file (tab separated: bam-file condition pool; see example.mapping");
		System.err.println(" 2.: A list of bam files");
		System.err.println(" 3.: A CIT file and an optional cit mapping file (tab separated: index-in-cit condition pool)");
		System.err.println();
		System.err.println("Annotation:");
		System.out.println(" Either a gtf file or a cit file containing transcripts");
		System.err.println();
		
	}
	
	
}
 