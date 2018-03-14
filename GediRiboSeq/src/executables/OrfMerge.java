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
import gedi.app.extension.ExtensionContext;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.riboseq.inference.orf.Orf;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.IterateIntoSink;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cern.colt.bitvector.BitVector;

public class OrfMerge {

	private static final Logger log = Logger.getLogger( OrfMerge.class.getName() );
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
	
	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("OrfStatistics <Options> cit1 cit2 ...");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -o <output>\t\t\tOutput file");
		System.err.println(" -n <regex>\t\t\tPrepend group 1 of this pattern (matched against the given file names) to the metadata 'name' field");
		System.err.println();
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println(" -p\t\t\tShow progress");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println();
		
	}
	
	
	private static class UsageException extends Exception {
		public UsageException(String msg) {
			super(msg);
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
		Gedi.startup(true);
		
		String out = null;
		Pattern fileToName = Pattern.compile("/([^/]*)\\.[^\\.]*$");

		Progress progress = new NoProgress();
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-p")) {
				progress=new ConsoleProgress(System.err);
			}
			else if (args[i].equals("-o")) {
				out = checkParam(args,++i);
			}
			else if (args[i].equals("-n")) {
				fileToName = Pattern.compile(checkParam(args,++i));
			}
			else if (args[i].equals("-D")) {
			}
			else if (!args[i].startsWith("-")) 
					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}

		
		if (out==null) throw new UsageException("No output file given!");

		HashSet<ReferenceSequence> refs = new HashSet<ReferenceSequence>();
		GenomicRegionStorage<Orf>[] in = new GenomicRegionStorage[args.length-i];
		DynamicObject[] metas = new DynamicObject[in.length];
		for (; i<args.length; i++) {
			Path p = Paths.get(args[i]);
			in[in.length-args.length+i] = (GenomicRegionStorage<Orf>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
			refs.addAll(in[in.length-args.length+i].getReferenceSequences());
			
			String prefix = FileUtils.getNameWithoutExtension(args[i]);
			Matcher matcher = fileToName.matcher(args[i]);
			if (matcher.find() && matcher.groupCount()==1) 
				prefix = matcher.group(1);
			
			metas[in.length-args.length+i] = prepend(prefix+".",in[in.length-args.length+i].getMetaData());
		}
		
		int[] selectedBuffer = new int[in.length];
		int[] testBuffer = new int[in.length];
		
		GenomicRegionStorage<Orf> outp = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, out).add(Class.class, Orf.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		IterateIntoSink<ImmutableReferenceGenomicRegion<Orf>> outSink = new IterateIntoSink<ImmutableReferenceGenomicRegion<Orf>>(outp::fill);
		outp.setMetaData(DynamicObject.merge(metas));
		
		for (ReferenceSequence r : refs) {
			Comparator<GenomicRegion> endComp = r.getStrand().equals(Strand.Minus)?((a,b)->Integer.compare(a.getStart(),b.getStart())):((a,b)->Integer.compare(a.getEnd(),b.getEnd()));
			
			ExtendedIterator<GenomicRegion[]>[] its = new ExtendedIterator[in.length];
			for (i=0; i<its.length; i++)
				its[i] = in[i].ei(r).map(rgr->rgr.getRegion()).sort(endComp).multiplex(endComp, GenomicRegion.class);
			cluster:for (GenomicRegion[][] cluster : FunctorUtils.parallellIterator(its, (a,b)->endComp.compare(a[0], b[0]),GenomicRegion[].class).progress(progress, -1, x->r.toString()).loop()) {
				
				for (int j = 0; j < cluster.length; j++) {
					if (cluster[j]==null) continue cluster; 
				}
				
				
				BitVector[] used = new BitVector[cluster.length];
				for (int j = 0; j < used.length; j++) 
					used[j] = new BitVector(cluster[j].length);
				
				boolean somethingleft;
				do {
					int max = -1;
					Arrays.fill(testBuffer, 0);
					do {
						if (!anyUsed(used,testBuffer)) {
							int over = computeOverlap(cluster,testBuffer);
							if (over>max) {
								max = over;
								System.arraycopy(testBuffer, 0, selectedBuffer, 0, testBuffer.length);
							}
						}
					} while (ArrayUtils.increment(testBuffer, j->cluster[j].length));
					
					if (max==-1) break;
					
					merge(outSink,r,cluster,selectedBuffer,in);
					somethingleft = use(used,selectedBuffer);
				} while (somethingleft);
						
			}
			
		}
		
		
		outSink.finish();
		
		
		
		
	}

	private static DynamicObject prepend(String prefix, DynamicObject meta) {
		return DynamicObject.parseJson(meta.toJson().replaceAll("\"name\":\"(.*?)\"", "\"name\":\""+prefix+"$1\""));
	}

	private static boolean use(BitVector[] used, int[] indices) {
		for (int i=0; i<indices.length; i++) {
			used[i].putQuick(indices[i],true);
			if (used[i].size()==used[i].cardinality())
				return false;
		}
		return true;
	}

	private static boolean anyUsed(BitVector[] used, int[] indices) {
		for (int i=0; i<indices.length; i++)
			if (used[i].getQuick(indices[i]))
				return true;
		return false;
	}

	private static int computeOverlap(GenomicRegion[][] cluster,
			int[] indices) {
		GenomicRegion inter = cluster[0][indices[0]];
		for (int i=1; i<cluster.length; i++) {
			inter = inter.intersect(cluster[i][indices[i]]);
		}
		for (int i=0; i<cluster.length; i++)
			if (!inter.isIntronConsistent(cluster[i][indices[i]]))
				return -1;

		return inter.getTotalLength();
	}

	private static void merge(IterateIntoSink<ImmutableReferenceGenomicRegion<Orf>> out, ReferenceSequence ref, GenomicRegion[][] cluster, int[] indices, GenomicRegionStorage<Orf>[] storages) throws InterruptedException {
		Orf[] orfs = new Orf[indices.length];
		GenomicRegion inter = cluster[0][indices[0]];
		for (int i=0; i<orfs.length; i++) {
			orfs[i] = storages[i].getData(ref, cluster[i][indices[i]]);
			inter = inter.intersect(orfs[i].getStartToStop(ref, cluster[i][indices[i]]));
		}
		
		Orf re = orfs[0].merge(orfs[1], 
				new ImmutableReferenceGenomicRegion<Void>(ref, cluster[0][indices[0]]).induce(inter).getStart()/3,
				new ImmutableReferenceGenomicRegion<Void>(ref, cluster[1][indices[1]]).induce(inter).getStart()/3);
		for (int a=2; a<indices.length; a++)
			re = re.merge(re, 0, new ImmutableReferenceGenomicRegion<Void>(ref, cluster[a][indices[a]]).induce(inter).getStart()/3);
		
		out.put(new ImmutableReferenceGenomicRegion<Orf>(ref, inter, re));
	}
	
}
