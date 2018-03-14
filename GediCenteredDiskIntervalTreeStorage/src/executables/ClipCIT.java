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
import java.util.Arrays;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.reads.AlignedReadClipper;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.StringUtils;
import gedi.util.functions.ExtendedIterator;
import gedi.util.userInteraction.progress.ConsoleProgress;


public class ClipCIT {

	private static int checkIntParam(String[] args, int index) {
		String re = checkParam(args, index);
		if (!StringUtils.isInt(re)) throw new RuntimeException("Must be an integer: "+args[index-1]);
		return Integer.parseInt(args[index]);
	}
	
	private static String checkParam(String[] args, int index)  {
		if (index>=args.length || args[index].startsWith("-")) throw new RuntimeException("Missing argument for "+args[index-1]);
		return args[index];
	}
	
	private static GenomicRegion checkRange(String[] args, int index)  {
		String r = checkParam(args, index);
		if (r.contains("-")) return GenomicRegion.parse(r);
		return new ArrayGenomicRegion(checkIntParam(args, index),Integer.MAX_VALUE);
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		if (args.length<3) {
			usage();
			System.exit(1);
		}
		
		boolean progress = false;
		String out = null;
		GenomicRegion first = null;
		
		
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-p"))
				progress = true;
			else if (args[i].equals("-c")) {
				first = checkRange(args, ++i);
			} 
			else {
				out = args[i++];
				args = Arrays.copyOfRange(args, i, args.length);
				i = args.length;
			}
		}
		if (out==null) {
			usage();
			System.exit(1);
		}
		
		if (args.length!=1) {
			usage();
			System.exit(1);
		}
			
		
		if (new File(out).equals(new File(args[0]))) {
			usage();
			System.exit(1);
		}
		
		CenteredDiskIntervalTreeStorage<AlignedReadsData> in =(CenteredDiskIntervalTreeStorage.load(args[0])); 
		CenteredDiskIntervalTreeStorage<AlignedReadsData> outCit = new CenteredDiskIntervalTreeStorage<>(out,in.getType(),in.isCompressed());
		outCit.setMetaData(in.getMetaData());
		
		AlignedReadClipper clipper = new AlignedReadClipper(first.getStart(), first.getEnd());

		ExtendedIterator<? extends ReferenceGenomicRegion<AlignedReadsData>> it = in.ei();
		if (progress)
			it = it.progress(new ConsoleProgress(System.err), (int)in.size(), a->a.toLocationString());
		it = it.map(a->clipper.apply(a));
		
		outCit.fill(it);
		
	}

	private static void usage() {
		System.out.println("ClipCIT [-p] [-c start[-end]] <output> <file1> \n\n -c is for both first and second read\n -p shows progress");
	}
	
}
