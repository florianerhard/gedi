package gedi.lfc;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.processing.CombinedGenomicRegionProcessor;
import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.OverlapMode;
import gedi.core.processing.sources.GeneProcessorSource;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequenceConversion;
import gedi.core.region.GenomicRegionStorage;
import gedi.lfc.downsampling.DigitalDownsampling;
import gedi.lfc.downsampling.LogDownsampling;
import gedi.lfc.downsampling.LogscDownsampling;
import gedi.lfc.downsampling.MaxDownsampling;
import gedi.lfc.downsampling.MinDownsampling;
import gedi.lfc.downsampling.NoDownsampling;
import gedi.lfc.downsampling.SquareRootDownsampling;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.formats.GtfFileReader;
import gedi.util.userInteraction.progress.ConsoleProgress;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.StreamSupport;

public class Lfc {
	public static void main(String[] args) {
		try {
			start(args);
		} catch (Exception e) {
			System.err.println("An error occured: "+e.getMessage());
		}
	}
	
	public static void start(String[] args) throws Exception {
		
		ContrastMapping before = new ContrastMapping();
		ContrastMapping after = new ContrastMapping();
		GenomicRegionStorage<Transcript> annotation = null;
		Downsampling ds = new LogscDownsampling();
		String name = null;
		boolean strandspecific = false;
		LineOrientedFile output = new LineOrientedFile(LineOrientedFile.STDOUT);
		HashSet<String> restrict = null;
		
		ArrayList<String> bamfiles = new ArrayList<String>();
		
		
		HashMap<String,Downsampling> downSampling = new HashMap<String,Downsampling>();
		downSampling.put("dig", new DigitalDownsampling());
		downSampling.put("log", new LogDownsampling());
		downSampling.put("logsc", new LogscDownsampling());
		downSampling.put("max", new MaxDownsampling());
		downSampling.put("min", new MinDownsampling());
		downSampling.put("no", new NoDownsampling());
		downSampling.put("sqrt", new SquareRootDownsampling());
		
		boolean progress = false;
		boolean all = false;
		
		for (int i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-p")) {
				progress=true;
			}
			else if (args[i].equals("-c1")) {
				before.addMapping(bamfiles.size(), bamfiles.size());
				after.addMapping(bamfiles.size(), bamfiles.size());
				bamfiles.add(args[++i]);
			}
			else if (args[i].equals("-c2")) {
				before.addMapping(bamfiles.size(), bamfiles.size());
				after.addMapping(bamfiles.size(), bamfiles.size());
				bamfiles.add(args[++i]);
			}
			else if (args[i].equals("-c")) {
				HashMap<String,Integer> beforeIndex = new HashMap<String, Integer>();
				HashMap<String,Integer> afterIndex = new HashMap<String, Integer>();
				
				Iterator<String> it = new LineOrientedFile(args[++i]).lineIterator("#");
				name = StringUtils.removeFooter(args[i],".mapping");
				while (it.hasNext()) {
					String[] f = StringUtils.split(it.next(), '\t');
					before.addMapping(bamfiles.size(), beforeIndex.computeIfAbsent(f[1]+"\t"+f[2], k->beforeIndex.size()));
					after.addMapping(before.getMappedIndex(bamfiles.size()), afterIndex.computeIfAbsent(f[1], l->afterIndex.size()),f[1]);
					
					bamfiles.add(f[0]);
				}
			}
			else if (args[i].equals("-d")) {
				ds = downSampling.get(args[++i].toLowerCase());
			}
			else if (args[i].equals("-r")) {
				restrict = new HashSet<>(Arrays.asList(FileUtils.readAllLines(new File(args[++i]))));
			}
			else if (args[i].equals("-e")) {
				annotation = new CenteredDiskIntervalTreeStorage<Transcript>(args[++i]);
			}
			else if (args[i].equals("-g")) {
				annotation = new GtfFileReader(args[++i],"exon").readIntoMemoryTakeFirst();
			}
			else if (args[i].equals("-s")) {
				strandspecific = true;
			}
			else if (args[i].equals("-o")) {
				output = new LineOrientedFile(args[++i]);
			}
			else if (args[i].equals("-all")) {
				all = true;
			}
		}
		
		
		
		if (after.getNumMergedConditions()<2 || ds==null || annotation==null) {
			if (after.getNumMergedConditions()<2)
				usage("Only a single condition found; either -c1/-c2 or -c has to be given; check your mapping file!");
			else if (ds==null)
				usage("Given downsampling mode unknown!");
			else
				usage("No annotation file given!");
			return;
		}

		
		BamGenomicRegionStorage bams = new BamGenomicRegionStorage(strandspecific, bamfiles.toArray(new String[0]));

		bams.setIgnoreVariations(true);
		
		
		GenomicRegionProcessor pr; 
		if (all) {
			CombinedGenomicRegionProcessor cpr = new CombinedGenomicRegionProcessor();
			for (String k : downSampling.keySet())
				cpr.add(new LfcAlignedReadsProcessor(before, after, downSampling.get(k), new LineOrientedFile(name+"_"+k+".lfc")));
			pr = cpr;
		}
		else
			pr = new LfcAlignedReadsProcessor(before, after, ds, output);
		
		GeneProcessorSource p = new GeneProcessorSource();
		p.setOverlapMode(OverlapMode.Contained);
		if (progress)
			p.setProgress(new ConsoleProgress());
		if (restrict!=null)
			p.setRestrict(restrict);
		p.process(bams, ReferenceSequenceConversion.none, StreamSupport.stream(annotation.iterateMutableReferenceGenomicRegions(),false), pr);
		
	}

	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("java -jar lfc.jar -g <gtf> [-c1 <bam> -c2 <bam>] [-c <mapping>] [-d <downsampling>] [-s] [-r <file>] [-o <output>] [-h]");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -g\t\tGtf file containing gene annotations");
		System.err.println(" -c1/-c2\tSpecify two bam files directly (either -c1/-c2 or -c has to be given)");
		System.err.println(" -c\t\tSpecify a mapping file (tab-separated: bam-file condition pool; see example.mapping)");
		System.err.println(" -d\t\tDownsamling mode: One of no,dig,log,logsc,max,min,sqrt (Default is logsc)");
		System.err.println(" -s\t\tsequencing was strand specific");
		System.err.println(" -o\t\tSpecify output file (Default is stdout)");
		System.err.println(" -r\t\tFile containing gene ids (one per line) that should considered (Default: don't restrict analysis)");
		System.err.println(" -all\t\tPerform all downsampling procedures, producing multiple output files.");
		System.err.println(" -p\t\tShow progress");
		System.err.println(" -h\t\tShow this message");
		System.err.println();
		
	}
	
	
}
 