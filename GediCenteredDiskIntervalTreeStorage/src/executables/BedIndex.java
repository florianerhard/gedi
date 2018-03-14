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
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTree;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.processing.CombinedGenomicRegionProcessor;
import gedi.core.processing.FillStorageProcessor;
import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.OverlapMode;
import gedi.core.processing.sources.GeneProcessorSource;
import gedi.core.reference.ReferenceSequenceConversion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.sequence.FastaIndexSequenceProvider;
import gedi.core.sequence.SequenceProvider;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.longcollections.LongArrayList;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.io.text.tsv.formats.BedEntry;
import gedi.util.io.text.tsv.formats.GtfFileReader;
import gedi.util.mutable.MutableLong;
import gedi.util.mutable.MutableTriple;
import gedi.util.mutable.MutableTuple;
import gedi.util.nashorn.JSFunction;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class BedIndex {

	
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
	
	public static void start(String[] args) throws Exception {
		
		
		boolean progress = false;
		String input = null;
		boolean silent = false;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-p")) {
				progress=true;
			}
			else if (args[i].equals("-S")) {
				silent=true;
			}
			else if (args[i].equals("-D")){} 
			else if (!args[i].startsWith("-")) 
					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
		}
		
		if (i==args.length-1) 
			input = args[i++];
		if (i!=args.length) throw new UsageException("Unknown parameter "+args[i]);
		
		if (input==null) throw new UsageException("No input file!");
		
		

		LineIterator lit = new LineOrientedFile(input).lineIterator();
	
		MutableReferenceGenomicRegion<BedEntry> rgr = new MutableReferenceGenomicRegion<BedEntry>();
		MutableReferenceGenomicRegion<ScoreNameAnnotation> rgr2 = new MutableReferenceGenomicRegion<ScoreNameAnnotation>();
		
		MemoryIntervalTreeStorage<ScoreNameAnnotation> tmp = new MemoryIntervalTreeStorage<ScoreNameAnnotation>(ScoreNameAnnotation.class);
		
		Progress pro = progress?new ConsoleProgress(System.err):null;
		
		if (progress) pro.init();
		
		while (lit.hasNext()) {
			String[] f = StringUtils.split(lit.next(), '\t');
			
			BedEntry.parseValues(f).getReferenceGenomicRegion(rgr);
			ScoreNameAnnotation data = new ScoreNameAnnotation(rgr.getData().getName(), rgr.getData().getDoubleScore());
			
			if (rgr.getReference()==null) {
				if (!silent)
					System.out.println("Cannot parse location for "+StringUtils.concat("\t", f));
			}
			else {
				tmp.add(rgr.getReference(), rgr.getRegion(), data, (a,b)->{
					System.err.println("Multiregions: \n"+a+"\n"+b);
					return b;
				});
			}
			
			if (progress) pro.incrementProgress();	
		}
		
		new CenteredDiskIntervalTreeStorage<ScoreNameAnnotation>(StringUtils.removeFooter(input, ".bed")+".cit", ScoreNameAnnotation.class).fill(tmp);
		
		
		if (progress) pro.finish();
		
	}

	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("BedIndex <Options> <input>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -S\t\t\tSilent mode");
		System.err.println(" -p\t\t\tShow progress");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println();
		
	}
	
}
