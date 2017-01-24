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

package gedi.atac;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.ScoreAnnotation;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.numeric.GenomicNumericProvider;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.processing.CombinedGenomicRegionProcessor;
import gedi.core.processing.FillStorageProcessor;
import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.OverlapMode;
import gedi.core.processing.sources.GeneProcessorSource;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.ReferenceSequenceConversion;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.formats.BedEntry;
import gedi.util.io.text.tsv.formats.GtfFileReader;
import gedi.util.mutable.MutableTriple;
import gedi.util.userInteraction.progress.ConsoleProgress;

public class AtacPeakFinder {

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
	
	private enum StrandMode {
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
		
		
		boolean progress = false;
		boolean chrY = false;
		
		double factor = 2;
		double peakFactor = 5;
		double bw = 20;
		
		
		int i;
		for (i=0; i<args.length-2; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-p")) {
				progress=true;
			}
			else if (args[i].equals("-y")) {
				chrY=true;
			}
			else if (args[i].equals("-f")) {
				factor = checkDoubleParam(args,++i);
			}
			else if (args[i].equals("-a")) {
				peakFactor = checkDoubleParam(args,++i);
			}
			else if (args[i].equals("-b")) {
				bw = checkDoubleParam(args,++i);
			}
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}
		
		if (i+2!=args.length) throw new UsageException("Input and/or output not given!");
		
		CenteredDiskIntervalTreeStorage<AlignedReadsData> storage = new CenteredDiskIntervalTreeStorage<AlignedReadsData>(args[i++]);
		
		
		TreeSet<ReferenceSequence> chromosomes = new TreeSet<ReferenceSequence>();
		for (ReferenceSequence ref : storage.getReferenceSequences())
			if (chrY || !ref.getName().equals("chrY"))
				chromosomes.add(ref.toStrandIndependent());
		
		
		PeakCalling caller = new PeakCalling(storage);
		caller.setFactor(factor);
		caller.setPeakFactor(peakFactor);
		caller.setBandwidth(bw);
		if (progress) caller.setProgress(new ConsoleProgress());
		
		MemoryIntervalTreeStorage<ScoreAnnotation> re = caller.call(chromosomes);
		
		CenteredDiskIntervalTreeStorage<ScoreAnnotation> out = new CenteredDiskIntervalTreeStorage<ScoreAnnotation>(args[i++],ScoreAnnotation.class);
		out.fill(re);
		
		String quantiFile = StringUtils.removeFooter(out.getPath(),".cit")+".quantification.csv";
		writeQuantification(new LineOrientedFile(quantiFile), re, storage);
	}

	private static void writeQuantification(LineOrientedFile out, MemoryIntervalTreeStorage<ScoreAnnotation> peaks, CenteredDiskIntervalTreeStorage<AlignedReadsData> reads) throws IOException {
		int cond = reads.getRandomRecord().getNumConditions();
		
		out.delete();
		
		for (ReferenceSequence ref : peaks.getReferenceSequences()) {
			
			
			HashMap<ImmutableReferenceGenomicRegion<ScoreAnnotation>,int[]> detail = new HashMap<ImmutableReferenceGenomicRegion<ScoreAnnotation>,int[]>();
			
			Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>> adder = new Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>>() {
				
				@Override
				public void accept(
						MutableReferenceGenomicRegion<? extends AlignedReadsData> mrgr) {
					
					int f = GenomicRegionPosition.Start.position(ref,mrgr.getRegion(),4);
					int b = GenomicRegionPosition.Stop.position(ref,mrgr.getRegion(),-4);
					
					for (int p : new int[] {f,b}) 
						StreamSupport.stream(
								peaks.iterateIntersectingMutableReferenceGenomicRegions(
										ref,p,p+1)
										,false)
								.forEach(peak->{
									int[] c = detail.computeIfAbsent(peak.toImmutable(), x->new int[cond]);
									for (int i=0; i<c.length; i++)
										c[i] += mrgr.getData().getTotalCountForConditionFloor(i, ReadCountMode.Weight);
								});
					
					
				}
				
			};
			reads.iterateMutableReferenceGenomicRegions(ref).forEachRemaining(adder);
			reads.iterateMutableReferenceGenomicRegions(ref.toPlusStrand()).forEachRemaining(adder);
			reads.iterateMutableReferenceGenomicRegions(ref.toMinusStrand()).forEachRemaining(adder);
			
			if (out.exists())
				out.startAppending();
			else {
				DynamicObject meta = reads.getMetaData();
				out.startWriting();
				out.write("Peak\tScore");
				for (int i=0; i<cond; i++) {
					DynamicObject name = meta.get(".conditions["+i+"].name");
					if (name.isString())
						out.writef("\t%s",name.asString());
					else
						out.writef("\t%d",i);
				}
				out.writeLine();
			}
			
			for (ImmutableReferenceGenomicRegion<ScoreAnnotation> peak : detail.keySet()) {
				int[] count = detail.get(peak);
				out.writef("%s\t%s", peak.toLocationString(),peak.getData().getScore());
				for (int c=0; c<cond; c++) 
					out.writef("\t%d",count[c]);
				out.writeLine();
			}
			out.finishWriting();
			
		}
		
	}

	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("AtacPeakFinder <Options> <CIT> <OUT>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -a <factor>\t\tFactor for peaks (default: 5)");
		System.err.println(" -f <factor>t\tFactor for peaks (default: 2)");
		System.err.println(" -b <width>\t\tKernel bandwidth (default: 20)");
		System.err.println(" -y \t\t\tAlso find peaks on chrY (default: do not)");
		System.err.println(" -p\t\t\tShow progress");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println();
		
	}
	
	
}
