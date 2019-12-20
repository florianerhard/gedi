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
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.riboseq.analysis.MajorIsoform;
import gedi.riboseq.codonprocessor.CodonProcessor;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.orf.Orf;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.SimpleInterval;
import gedi.util.functions.EI;
import gedi.util.io.text.fasta.FastaLikeFile;
import gedi.util.oml.OmlNodeExecutor;
import gedi.util.oml.OmlReader;
import gedi.util.oml.PlaceholderInterceptor;
import gedi.util.oml.petrinet.GenomicRegionFeaturePipeline;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.InputSource;

public class CodonStatistics {

	private static final Logger log = Logger.getLogger( CodonStatistics.class.getName() );
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
		System.err.println("CodonStatistics <Options>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -f <folder>\t\t\tPrice output folder");
		System.err.println(" -o <prefix>\t\t\tPrefix for output files");
		System.err.println(" -g <genome1 genome2 ...>\t\t\tGenome names");
		System.err.println(" -opt \t\t\tUse opt if it's there!");
		System.err.println(" -nthreads <n>\t\t\tNumber of threads (default: available cores)");
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
		
		String f = null;
		Genomic g = null;
		String prefix = null;
		double rnathres = -10;
		boolean opt = false;
		int nthreads = Runtime.getRuntime().availableProcessors();

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
			else if (args[i].equals("-opt")) {
				opt = true;
			}
			else if (args[i].equals("-f")) {
				f = checkParam(args, ++i);
			}
			else if (args[i].equals("-nthreads")) {
				nthreads=checkIntParam(args, ++i);
			}
			else if (args[i].equals("-o")) {
				prefix = checkParam(args,++i);
			}
			else if (args[i].equals("-g")) {
				ArrayList<String> names = new ArrayList<>();
				i = checkMultiParam(args, ++i, names);
				g = Genomic.get(names);
			}
			else if (args[i].equals("-D")) {
			}
//			else if (!args[i].startsWith("-")) 
//					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}

		
		if (f==null) throw new UsageException("No price folder given!");
		if (prefix==null) throw new UsageException("No output prefix given!");

		boolean uopt = opt;
		int trim = opt?".opt.codons.cit".length():".codons.cit".length();
		String codFile = EI.wrap(new File(f).list()).filter(p->p.endsWith(".codons.cit") && p.endsWith(".opt.codons.cit")==uopt).getUniqueResult("More than one codons.cit in folder!", "No codons.cit file found!");
		String pref = new File(codFile.substring(0,codFile.length()-trim)).getPath();
		
		Path codPath = Paths.get(f,codFile);
		GenomicRegionStorage<NumericArray> ccit = (GenomicRegionStorage<NumericArray>) WorkspaceItemLoaderExtensionPoint.getInstance().get(codPath).load(codPath);
		
		Path orfPath = Paths.get(f,pref+".orfs.cit");
		GenomicRegionStorage<PriceOrf> ocit = (GenomicRegionStorage<PriceOrf>) WorkspaceItemLoaderExtensionPoint.getInstance().get(orfPath).load(orfPath);
		
		Path majorPath = Paths.get(f,pref+".majorisoform.cit");
		GenomicRegionStorage<MajorIsoform> mcit = (GenomicRegionStorage<MajorIsoform>) WorkspaceItemLoaderExtensionPoint.getInstance().get(majorPath).load(majorPath);
		
		HashMap<String,ArrayList<SimpleInterval>> localStruc = readRNALfold(new File(f,pref+".majorisoform.RNALfold"),rnathres);
		
		Genomic genomic = g;
		String uprefix = prefix;
		
		CodonProcessor proc = ccit.ei()
			.progress(progress, (int)ccit.size(), r->r.toLocationString())
			.filter(c->!c.getReference().isMitochondrial())
			.parallelized(nthreads, 1024, ()->new CodonProcessor(genomic,ocit,mcit,localStruc).addDefaultOutputs(uprefix),(ei,p)->ei.map(codon->p.apply(codon)))
			.drainStates()
			.reduce((a,b)->a.merge(b));
		
		proc.counters()
			.progress(progress, -1, o->o.toString())
			.parallelized(nthreads, 1, ei->ei.map(counter->{
				counter.outputs().forEachRemaining(o->o.createOutput(ocit.getMetaDataConditions()));
				return 1;
			}))
			.drain();
		
	}

	private static HashMap<String, ArrayList<SimpleInterval>> readRNALfold(File file, double threshold) throws IOException {
		HashMap<String,ArrayList<SimpleInterval>> re = new HashMap<String, ArrayList<SimpleInterval>>();
		
		Pattern lsp = Pattern.compile("([.()]+)\\s+\\(\\s*(.*?)\\)\\s+(\\d+)");
		
		ArrayList<SimpleInterval> list = null;
		for (String l : EI.lines(file).loop()) {
			if (l.startsWith(">")) 
				re.put(l.substring(1), list = new ArrayList<SimpleInterval>());
			else {
				Matcher m = lsp.matcher(l);
				if (m.find()) {
					double en = Double.parseDouble(m.group(2));
					int pos = Integer.parseInt(m.group(3))-1;
					int len = m.group(1).length();
					if (en<threshold)
						list.add(new SimpleInterval(pos, pos+len-1));
				}
			}
		}
		
		return re;
		
	}
	
}
