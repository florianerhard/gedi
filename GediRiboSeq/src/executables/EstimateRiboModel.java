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
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.AlignedReadsDataToFeatureProgram;
import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.core.region.feature.features.AbsolutePosition;
import gedi.core.region.feature.features.AnnotationFeature;
import gedi.core.region.feature.output.FeatureStatisticOutput;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.riboseq.cleavage.CleavageModelEstimator;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.formats.Csv;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class EstimateRiboModel {

	private static final Logger log = Logger.getLogger( EstimateRiboModel.class.getName() );
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
		System.err.println("EstimateRiboModel <Options>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -r <reads-file>\t\t\tFile containing read mappings");
		System.err.println(" -f <Read filter spec>\t\t\tUse only reads matching the filter (e.g. 28:30)");
		System.err.println(" -o <prefix>\t\t\tPrefix for output files");
		System.err.println(" -maxpos <position>\t\t\tPosition of maximal upstream probability (default: estimate from annotated start codons)");
		System.err.println(" -g <genome1 genome2 ...>\t\t\tGenome names");
		System.err.println(" -maxiter <number>\t\t\tMaximal number of iterations per EM repeat (default: 1000)");
		System.err.println(" -repeats <number>\t\t\tMaximal number of EM repeats (default: 1000000)");
		System.err.println(" -nthreads <number>\t\t\tNumber of threads to run the EM algorithm (default: Number of available processors)");
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
		
		GenomicRegionStorage<AlignedReadsData> reads = null;
		Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter = null;
		Genomic g = null;
		int[] maxpos = null;
		String prefix = null;
		int maxiter = 1000;
		int repeats = 100000;
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
			else if (args[i].equals("-nthreads")) {
				nthreads=checkIntParam(args, ++i);
			}
			else if (args[i].equals("-r")) {
				Path p = Paths.get(checkParam(args,++i));
				reads = (GenomicRegionStorage<AlignedReadsData>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
			}
			else if (args[i].equals("-f")) {
				filter=RiboUtils.parseReadFilter(checkParam(args, ++i));
			}
			else if (args[i].equals("-maxiter")) {
				maxiter = checkIntParam(args,++i);
			}
			else if (args[i].equals("-maxpos")) {
				maxpos = new int[] {checkIntParam(args,++i)};
			}
			else if (args[i].equals("-repeats")) {
				repeats = checkIntParam(args,++i);
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

		
		if (reads==null) throw new UsageException("No reads given!");
		if (g==null) throw new UsageException("No genome given!");
		if (prefix==null) throw new UsageException("No output prefix given!");

		
		if (maxpos==null) {
			log.info("Estimating maxpos...");
			GenomicRegionFeatureProgram<AlignedReadsData> program = new GenomicRegionFeatureProgram<AlignedReadsData>();
			program.setThreads(nthreads);
			
			AnnotationFeature<Transcript> a = new AnnotationFeature<Transcript>(false);
			a.addTranscripts(g);
			a.setId("transcript");
			program.add(a);
			
			AbsolutePosition p = new AbsolutePosition();
			p.setReportFurtherDownstream(false);
			p.setReportFurtherUpstream(false);
			p.setAnnotationPosition(GenomicRegionPosition.StartCodon);
			p.setId("Position");
			program.add(p,"transcript");
			
			FeatureStatisticOutput t = new FeatureStatisticOutput(prefix+".maxPos.estimateData");
			t.addCondition(new String[] {"transcript"}, "[U]");
			t.addCondition(new String[] {"Position"}, "[U]");
			program.add(t,"Position");
			
			new AlignedReadsDataToFeatureProgram(program).setProgress(progress).processStorage(reads);

			DataFrame df = Csv.toDataFrame(prefix+".maxPos.estimateData", true, 0, null);
			int[] pos = df.getIntegerColumn(0).getRaw().toIntArray();
			double[] sum = null;
			double[][] mat = new double[df.columns()-1][];
			for (int c=1; c<df.columns(); c++) {
				mat[c-1] = ArrayUtils.restrict(df.getDoubleColumn(c).getRaw().toDoubleArray(),ind->pos[ind]<0);
				sum = ArrayUtils.add(sum, mat[c-1]);
			}
			int[] posr = ArrayUtils.restrict(pos,ind->pos[ind]<0);

			maxpos = new int[mat.length+1];
			for (int c=0; c<mat.length; c++) {
				maxpos[c] = -posr[ArrayUtils.argmax(mat[c])];
				String name = reads.getMetaData()!=null?reads.getMetaData().getEntry("conditions").getEntry(c).getEntry("name").asString():null;
				if (name==null || name.length()==0) name = i+"";
				log.info("maxpos("+name+") = "+maxpos[c]);
			}
			maxpos[maxpos.length-1] = -posr[ArrayUtils.argmax(sum)];
			log.info("maxpos(merged) = "+maxpos[maxpos.length-1]);
			
		}
		
		log.info("Initalizing EM algorithm with maxiter="+maxiter+"; repeats="+repeats);
		CleavageModelEstimator em = new CleavageModelEstimator(g.getTranscripts(),reads,filter);
		em.setMaxiter(maxiter);
		em.setRepeats(repeats);
		em.setProgress(progress);
		
		if (new LineOrientedFile(prefix+".conditions.estimateData").exists()) {
			log.info("Reading estimate data from file "+prefix+".conditions.estimateData");
			em.readEstimateData(new LineOrientedFile(prefix+".conditions.estimateData"));
		}
		else {
			log.info("Collecting estimate data from file "+prefix+".conditions.estimateData");
			em.collectEstimateData(new LineOrientedFile(prefix+".conditions.summary"));
			em.writeEstimateData(new LineOrientedFile(prefix+".conditions.estimateData"));
		}
		
		log.info("Inferring per condition models");
		PageFileWriter model = new PageFileWriter(prefix+".conditions.model");
		int cond = reads.getRandomRecord().getNumConditions();
		RiboModel[] models = new RiboModel[cond];
		double[] total = em.getTotal();
		double totalll = 0;
		for (i=0; i<cond; i++) {
			String name = reads.getMetaData()!=null?reads.getMetaData().getEntry("conditions").getEntry(i).getEntry("name").asString():null;
			if (name==null || name.length()==0) name = i+"";
			
			int mp = maxpos.length==1?maxpos[0]:maxpos[i];
			em.setMaxPos(mp);
			log.info("Using maxpos="+mp);
			log.info("Estimate parameters for "+prefix+"."+name);
			double ll = em.estimateBoth(i,nthreads);
			log.info(String.format("LL=%.6g",ll));
			em.plotProbabilities(prefix+"."+name,prefix+"."+name+".png");
			models[i] = em.getModel();
			new LineOrientedFile(prefix+"."+name+".model.csv").writeAllText(models[i].toTable());
			models[i].serialize(model);
			totalll +=ll;
		}
		model.close();
		log.info(String.format("Total LL=%.6g",totalll));
		
		log.info("Inferring merged model");
		model = new PageFileWriter(prefix+".merged.model");
		em.setMerge(true);
		String name = "merged";
			
		if (new LineOrientedFile(prefix+"."+name+".estimateData").exists()) {
			log.info("Reading estimate data from file "+prefix+"."+name+".estimateData");
			em.readEstimateData(new LineOrientedFile(prefix+"."+name+".estimateData"));
		}
		else {
			log.info("Collecting estimate data from file "+prefix+"."+name+".estimateData");
			em.collectEstimateData(new LineOrientedFile(prefix+"."+name+".summary"));
			em.writeEstimateData(new LineOrientedFile(prefix+"."+name+".estimateData"));
		}
		
		em.setMaxPos(maxpos[maxpos.length-1]);
		log.info("Using maxpos="+maxpos[maxpos.length-1]);
		
		log.info("Estimate parameters for "+prefix+"."+name);
		double ll = em.estimateBoth(0,nthreads);
		log.info(String.format("LL=%.6g",ll));
		em.plotProbabilities(prefix+"."+name,prefix+"."+name+".png");

//		RiboModel mm = RiboModel.merge(models,total);
//		double mmll = em.computeLL(0, mm.getPl(),mm.getPr(),mm.getU());
//		log.info(String.format("Averaged model: LL=%.6g",mmll));
		
//		RiboModel m = mmll>ll?mm:em.getModel();
		RiboModel m = em.getModel();
		new LineOrientedFile(prefix+"."+name+".model.csv").writeAllText(m.toTable());
		m.serialize(model);
		model.close();
		log.info("Finished estimating ribo models");
		
	}
	
	private static class Fluent<T> {
		T o;

		public Fluent(T o) {
			this.o = o;
		}

		public Fluent<T> a(Consumer<T> c) {
			c.accept(o);
			return this;
		}
		
		public T get() {
			return o;
		}
	}
	
}
