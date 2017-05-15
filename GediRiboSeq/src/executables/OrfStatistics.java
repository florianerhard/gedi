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
import gedi.riboseq.inference.orf.Orf;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.oml.OmlNodeExecutor;
import gedi.util.oml.OmlReader;
import gedi.util.oml.PlaceholderInterceptor;
import gedi.util.oml.petrinet.GenomicRegionFeaturePipeline;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.xml.sax.InputSource;

public class OrfStatistics {

	private static final Logger log = Logger.getLogger( OrfStatistics.class.getName() );
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
		System.err.println("OrfStatistics <Options>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -i \t\t\tInteractive mode");
		System.err.println(" -r <orfs-file>\t\t\tFile containing orfs");
		System.err.println(" -o <prefix>\t\t\tPrefix for output files");
		System.err.println(" -g <genome1 genome2 ...>\t\t\tGenome names");
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
		
		GenomicRegionStorage<Orf> orfs = null;
		Genomic g = null;
		String prefix = null;
		boolean interactive = false;
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
			else if (args[i].equals("-i")) {
				interactive = true;
			}
			else if (args[i].equals("-r")) {
				Path p = Paths.get(checkParam(args,++i));
				orfs = (GenomicRegionStorage<Orf>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
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

		
		if (orfs==null) throw new UsageException("No orfs given!");
		if (prefix==null) throw new UsageException("No output prefix given!");

		
		HashMap<String, String> ph = new HashMap<String, String>();
		ph.put("prefix", prefix);
		ph.put("labels", EI.wrap(orfs.getMetaData().getEntry("conditions").asArray()).map(d->d.getEntry("name").asString()).concat(","));
		
		HashMap context = new HashMap();
		context.put("genomic", g);

		log.info("Loading pipeline");
		InputStream stream = OrfStatistics.class.getResourceAsStream("orfstats.oml");
		GenomicRegionFeaturePipeline pipe = (GenomicRegionFeaturePipeline)new OmlNodeExecutor().addInterceptor(new PlaceholderInterceptor().addPlaceHolders(ph)).execute(new OmlReader().parse(new InputSource(stream)),context);
		GenomicRegionFeatureProgram program = pipe.getProgram();
		
		program.setBenchmark(true);
		program.setThreads(nthreads);
		
		program.begin();
		orfs.ei()
			.progress(progress, (int)orfs.size(), r->r.toMutable().transformRegion(reg->reg.removeIntrons()).toLocationString())
			.forEachRemaining(program);
		program.end();
		
		
		program.printBenchmark();
		
		
	}
	
}
