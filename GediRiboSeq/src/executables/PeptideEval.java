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
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.proteomics.digest.FullAfterAADigester;
import gedi.proteomics.digest.FullAfterAADigester.FullAfterAAPeptideIterator;
import gedi.proteomics.molecules.properties.mass.MassFactory;
import gedi.riboseq.inference.orf.Orf;
import gedi.riboseq.inference.orf.OrfType;
import gedi.util.ArrayUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.function.StepFunction;
import gedi.util.math.stat.counting.RollingStatistics;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class PeptideEval {

	private static final Logger log = Logger.getLogger( PeptideEval.class.getName() );
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
		System.err.println("PeptideEval Orf-cit Pep-cit");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -o <output>\t\t\tOutput file");
		System.err.println(" -min <mass>\t\t\tMinimal peptide mass");
		System.err.println(" -max <mass>\t\t\tMaximal peptide mass");
		System.err.println(" -e <enzyme>\t\t\tEither tryptic,undigested or a sequence of Amino acids (KR is equivalent to tryptic and the default)");
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
	private static String checkParam(String[] args, int index, String expected) throws UsageException {
		if (index>=args.length || args[index].startsWith("-")) throw new UsageException("Missing argument for "+expected);
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
		
		double maxMass = 5000;
		double minMass = 500;
		String out = null;
		String aminos = "KR";
		
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
			else if (args[i].equals("-min")) {
				minMass = checkDoubleParam(args,++i);
			}
			else if (args[i].equals("-max")) {
				maxMass = checkDoubleParam(args,++i);
			}
			else if (args[i].equals("-e")) {
				aminos = checkParam(args, ++i);
				if (aminos.equals("tryptic")) aminos = "KR";
				else if (aminos.equals("undigested")) aminos = "";
			}
			else if (args[i].equals("-D")) {
			}
			else if (!args[i].startsWith("-")) 
					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}

		Path p = Paths.get(checkParam(args,i, "Orf-cit"));
		GenomicRegionStorage<Orf> orfs = (GenomicRegionStorage<Orf>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
		
		p = Paths.get(checkParam(args,++i, "Pep-cit"));
		MemoryIntervalTreeStorage<Object> peps = ((GenomicRegionStorage<Object>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p)).toMemory();
		
		LineWriter o = new LineOrientedFile(out+".details.data").write().writeLine("Location\tOrf type\tActivity\tPeptide"); 
		
		FullAfterAADigester digest = new FullAfterAADigester(0,aminos.toCharArray());
		
		RollingStatistics frac = new RollingStatistics();
		HashMap<String,RollingStatistics> ffrac = new HashMap<String, RollingStatistics>();
		
		
		for (ReferenceGenomicRegion<Orf> orf : orfs.ei().progress(progress, (int) orfs.size(), rgr->rgr.toLocationStringRemovedIntrons()).loop()) {
			String dna = orf.getData().getCdsSequence();
			String prot = SequenceUtils.translate(dna);
			prot = prot.substring(0, prot.length()-1);
			
			int st = Math.max(0, orf.getData().getInferredStartPosition());
			
			int npeps = 0;
			int nhit = 0;
			
			
			FullAfterAAPeptideIterator pit = digest.iteratePeptides(prot);
			while (pit.hasNext()) {
				String pep = pit.next();
				double mass = MassFactory.getInstance().getPeptideMass(pep);
				if (mass>=minMass && mass<=maxMass) {
					ArrayGenomicRegion reg = new ArrayGenomicRegion((st+pit.getStartPosition())*3,(st+pit.getEndPosition())*3);
					boolean hit = !peps.getReferenceRegionsIntersecting(orf.getReference(),orf.map(reg)).isEmpty();
					if (hit)
						nhit++;
					npeps++;
					o.writef("%s\t%s\t%.5f\t%d\n", orf.toLocationString(),orf.getData().getOrfType(),orf.getData().getTmCov(),hit?1:0);
					
					if (orf.getData().getOrfType()==OrfType.CDS)
						frac.add(orf.getData().getTmCov(), hit?1:0);
					
					if (!orf.getData().getOrfType().isAnnotated())
						ffrac.computeIfAbsent("NotAnnot", x->new RollingStatistics()).add(orf.getData().getTmCov(), hit?1:0);
					
					if (!orf.getData().getOrfType().isAnnotated() && orf.getReference().getName().startsWith("JN"))
						ffrac.computeIfAbsent("NotAnnotVir", x->new RollingStatistics()).add(orf.getData().getTmCov(), hit?1:0);
				}
			}
			
			
		}
		
		o.close();
		
		
		o = new LineOrientedFile(out+".cds.fit").write().writeLine("Activity\tCDS"); 
		StepFunction fun = frac.computeEquiSize(1000, 100, NumericArrayFunction.Mean);
		
		for (i=0; i<fun.getKnotCount(); i++) {
			o.writef("%.5f\t%.5f\n",fun.getX(i),fun.getY(i));
		}
		
		o.close();
		
		
		for (String name : ffrac.keySet()) {
			o = new LineOrientedFile(out+"."+name+".fit").write().writeLine("Activity\t"+name); 
			fun = ffrac.get(name).computeEquiSize(50, 25, NumericArrayFunction.Mean);
			for (i=0; i<fun.getKnotCount(); i++)
				o.writef("%.5f\t%.5f\n",fun.getX(i),fun.getY(i));
			
			o.close();
		}
		
	}
	
	
}
