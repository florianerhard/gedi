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
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.sequence.SequenceProvider;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.cleavage.RiboModel.CodonFeature;
import gedi.riboseq.cleavage.RiboModel.LfcParameters;
import gedi.riboseq.cleavage.SimpleCodonModel;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.riboseq.inference.codon.ReadsXCodonMatrix;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.FunctorUtils.DemultiplexIterator;
import gedi.util.FunctorUtils.ToIntMappedIterator;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.MemoryFloatArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.tree.Trie;
import gedi.util.datastructure.tree.Trie.AhoCorasickResult;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.BiIntConsumer;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.IterateIntoSink;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.function.PiecewiseLinearFunction;
import gedi.util.math.function.StepFunction;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.math.stat.classification.CompleteRocAnalysis;
import gedi.util.math.stat.counting.RollingStatistics;
import gedi.util.math.stat.descriptive.MeanVarianceOnline;
import gedi.util.math.stat.kernel.GaussianKernel;
import gedi.util.math.stat.kernel.PreparedIntKernel;
import gedi.util.mutable.MutableDouble;
import gedi.util.mutable.MutableMonad;
import gedi.util.mutable.MutablePair;
import gedi.util.r.R;
import gedi.util.r.RConnect;
import gedi.util.r.RRunner;
import gedi.util.sequence.Alphabet;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.logging.Logger;

import jdistlib.math.spline.SmoothSpline;
import jdistlib.math.spline.SmoothSplineResult;
import libsvm.Svm;
import libsvm.Svm_model;
import libsvm.Svm_node;
import libsvm.Svm_parameter;
import libsvm.Svm_problem;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RserveException;

import cern.colt.bitvector.BitVector;

public class EstimateModelError {

	private static final Logger log = Logger.getLogger( EstimateModelError.class.getName() );
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
		System.err.println("EstimateModelError <Options>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -r <mapping-file>\t\t\tRead mappings");
		System.err.println(" -f <Read filter spec>\t\t\tUse only reads matching the filter (e.g. 28:30)");
		System.err.println(" -delta <delta>\t\t\tSpecify regularization parameter");
		System.err.println(" -t <pair1 pair2 ...>\t\t\tSpecifications treatment pairs (ltm or harr to chx) to detect startcodon (e.g.: 8hpi_harr/8hpi or 7/5)");
		System.err.println(" -m <model-file>\t\t\tModel file from EstimateRiboModel");
		System.err.println(" -g <genomic-file ...>\t\t\tGenomic files");
		System.err.println(" -nthreads <n>\t\t\tNumber of threads (default: available cores)");
		System.err.println(" -o <prefix>\t\t\tPrefix for output files");
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


	private static String checkParam(String[] args, int index) throws UsageException {
		if (index>=args.length || args[index].startsWith("-")) throw new UsageException("Missing argument for "+args[index-1]);
		return args[index];
	}

	private static int checkIntParam(String[] args, int index) throws UsageException {
		String re = checkParam(args, index);
		if (!StringUtils.isInt(re)) throw new UsageException("Must be an integer: "+args[index-1]);
		return Integer.parseInt(args[index]);
	}
	private static int checkMultiParam(String[] args, int index, ArrayList<String> re) throws UsageException {
		while (index<args.length && !args[index].startsWith("-")) 
			re.add(args[index++]);
		return index-1;
	}
	private static double checkDoubleParam(String[] args, int index) throws UsageException {
		if (index>=args.length) throw new UsageException("Missing argument for "+args[index-1]);
		String re = args[index];
		if (!StringUtils.isNumeric(re)) throw new UsageException("Must be a double: "+args[index-1]);
		return Double.parseDouble(args[index]);
	}

	public static void start(String[] args) throws Exception {
		Gedi.startup(true);

		GenomicRegionStorage<AlignedReadsData> reads = null;
		Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter = null;
		Genomic g = null;
		RiboModel[] models = null;
		String prefix = null;
		double trim = 0.1;
		double minGapActivity = 25;
		int gapHalfBin = 100;
		int gapStep = 10;
		double threshold = 1E-2;
		int upstreamStartPrediction = 7;
		int downstreamStartPrediction = 7;
		double cutoffStartPrediction = 6;
		double fprStartPrediction = 0.01;
		double lambda = Double.NaN;
		int lambdaM = 1000;
		int lambdaO = 100;
		int lambdaF = 1;
		
		Boolean useAllCodons = null;
		boolean summarize = false;
		boolean fixedNegatives = false;
		boolean startsvm = false;
		
		boolean ribotaper = false;
		
		int internalHalfBin = 1000; 
		int internalStep = 100;
		String[] startcodonpairs = new String[0];
		SimpleCodonModel simpleModel = new SimpleCodonModel(StringUtils.split("28->12 29->12 29L->13 30L->13",' '));
		SimpleCodonModel overrideSimpleModel = null;
		int nthreads = Runtime.getRuntime().availableProcessors();
		MemoryIntervalTreeStorage<Transcript> predefinedMajor = null;
		
		IterateIntoSink<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>> simulate = null;

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
			else if (args[i].equals("-ribotaper")) {
				ribotaper = true;
			}
			else if (args[i].equals("-f")) {
				filter=RiboUtils.parseReadFilter(checkParam(args, ++i));
			}
			else if (args[i].equals("-r")) {
				Path p = Paths.get(checkParam(args,++i));
				reads = (GenomicRegionStorage<AlignedReadsData>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
			}
			else if (args[i].equals("-c")) {
				GenomicRegionStorage sim = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, checkParam(args,++i)).add(Class.class, DefaultAlignedReadsData.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
				//				CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> sim = new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>(checkParam(args,++i),DefaultAlignedReadsData.class);
				simulate = new IterateIntoSink<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>>(sim::fill);
			}
			else if (args[i].equals("-s")) {
				simpleModel = new SimpleCodonModel(StringUtils.split(checkParam(args,++i),' '));
			}
			else if (args[i].equals("-override")) {
				overrideSimpleModel = new SimpleCodonModel(StringUtils.split(checkParam(args,++i),' '));
			}
			else if (args[i].equals("-major")) {
				predefinedMajor = new MemoryIntervalTreeStorage<Transcript>(Transcript.class);
				predefinedMajor.fill(new LineOrientedFile(checkParam(args, ++i)).lineIterator().skip(1)
											.map(l->StringUtils.splitField(l,'\t',0))
											.unique(false)
											.map(s->ImmutableReferenceGenomicRegion.<Transcript>parse(s))
						);
			}
			else if (args[i].equals("-t")) {
				ArrayList<String> li = new ArrayList<>();
				i = checkMultiParam(args, ++i, li);
				startcodonpairs = li.toArray(new String[0]);
			}
			else if (args[i].equals("-m")) {
				models = RiboModel.fromFile(checkParam(args,++i),false);
			}
			else if (args[i].equals("-trim")) {
				trim = checkDoubleParam(args, ++i);
			}
			else if (args[i].equals("-delta") || args[i].equals("-lambda")) {
				lambda = checkDoubleParam(args, ++i);
			}
			else if (args[i].equals("-gapMinAct")) {
				minGapActivity = checkDoubleParam(args, ++i);
			}
			else if(args[i].equals("-svmallcodons")) {
				useAllCodons = checkIntParam(args, ++i)==1;
				startsvm = true;
			}
			else if(args[i].equals("-svmfixednegatives")) {
				fixedNegatives = checkIntParam(args, ++i)==1;
				startsvm = true;
			}
			else if(args[i].equals("-svmsummarize")) {
				summarize = checkIntParam(args, ++i)==1;
				startsvm = true;
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
		if (g==null) throw new UsageException("No genomic file given!");
		if (prefix==null) throw new UsageException("No output prefix given!");
		if (models==null) throw new UsageException("No model given!");

		Dataset dataset = new Dataset(reads,models,g, filter, overrideSimpleModel, predefinedMajor, startcodonpairs);
		
		if (Double.isNaN(lambda)) {
			log.info("Determine delta");
			lambda = determineLambda(prefix, dataset, progress,lambdaM,lambdaO,lambdaF);
			log.info("delta = "+lambda);
		}			
		
		StartStopProcessor startstopProcessor = new StartStopProcessor(prefix, dataset, startcodonpairs, threshold, trim);
		InternalProcessor internalProcessor = new InternalProcessor(prefix, dataset, internalHalfBin, internalStep);
		SimulateProcessor simulateProcessor = new SimulateProcessor(prefix,dataset,simulate,0);
		GofProcessor gofProcessor = new GofProcessor(prefix, dataset);
		GapsProcessor gapsProcessor = new GapsProcessor(prefix, dataset, threshold, trim, gapHalfBin, gapStep);
		SignalToNoiseProcessor signalToNoiseProcessor = new SignalToNoiseProcessor(prefix, dataset, simpleModel);
		AroundStartProcessor aroundStartProcessor = new AroundStartProcessor(prefix, dataset, simpleModel);
		
		if (useAllCodons==null) useAllCodons = overrideSimpleModel!=null;
		
		startsvm |=dataset.startPairs.length==0;
		
		SvmStartStopProcessor svmProcessor = !startsvm?null:new SvmStartStopProcessor(prefix, dataset,upstreamStartPrediction,downstreamStartPrediction,cutoffStartPrediction,startcodonpairs,fprStartPrediction,useAllCodons,summarize,fixedNegatives);
		SvmLfcProcessor lfcProcessor = new SvmLfcProcessor(prefix, dataset);
		RibotaperProcessor ribotaperProcessor = ribotaper?new RibotaperProcessor(prefix):null;
		
		
		double[] totalPost = dataset.models[0].computeReadLength();
		log.info("Computing valid read lengths");
		LineWriter len = new LineOrientedFile(prefix+".readlengths").write().writeLine("Length\tTotal posterior\tUse");
		for (int l=1; l<totalPost.length; l++) {
			len.writef("%d\t%.5g\t%d\n", l,totalPost[l],dataset.models[0].isValidReadLength(l)?1:0);
		}
		len.close();
		
		//		if (!new File(prefix+".bin").exists()) {
		log.info("Identifying annotated non-translated regions in ORFs");
		MemoryIntervalTreeStorage<IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>> nontrans = inferNontrans(dataset.genomic.getTranscripts(),dataset.genomic,progress);

		CodonInference inference = new CodonInference(dataset.models);
		inference.setFilter(filter);
		inference.setRegularization(lambda);
		

		log.info("Processing chunks with "+nthreads+" threads.");
		// now loop through all ~genes, infer codons based on the reads and the model and record codons from nontrans in comparison to the overlapping cds codons
		double eminGapActivity = minGapActivity;
		nontrans.ei().progress(progress, (int)nontrans.size(), r->r.toLocationString())
			.parallelized(nthreads,10,it->it.<Integer>map(gene->{
				try {
					Inferred inferred = infer(gene,inference,dataset);	
					internalProcessor.process(inferred);
					
					if (inferred.major.major!=null) {
						if (inferred.major.act>=eminGapActivity || dataset.predefinedMajor!=null) {
							inferred.loadCodons();
							if (inferred.getCoverage()>1 || dataset.predefinedMajor!=null) {
								startstopProcessor.process(inferred);
								if (svmProcessor!=null)
									svmProcessor.process(inferred);
								lfcProcessor.process(inferred);
								signalToNoiseProcessor.process(inferred);
								aroundStartProcessor.process(inferred);
								
								simulateProcessor.process(inferred);
								if (ribotaperProcessor!=null)
									ribotaperProcessor.process(inferred);
							}
							gapsProcessor.process(inferred);
							gofProcessor.process(inferred);
						}
					}
					return 1;
				} catch (Throwable e) {
					throw new RuntimeException("Could not process "+gene,e);
				}
			})).drain();
		if (ribotaperProcessor!=null)		
			ribotaperProcessor.finish();
		
		log.info("Writing statistics");
		aroundStartProcessor.finish();
		simulateProcessor.finish();
		signalToNoiseProcessor.finish();
		
		log.info("Running LFC SVM");
		lfcProcessor.finish();
		if (lfcProcessor.model!=null)
			dataset.models[0].setLfcSvm(new RiboModel.LfcParameters(lfcProcessor.model, dataset.startCodonPairs, lfcProcessor.cutoff, lfcProcessor.lfcpseudo, lfcProcessor.lfcReferenceStart, lfcProcessor.lfcReferenceEnd));
		
		if (svmProcessor!=null) {
			log.info("Running Start-codon SVM");
			svmProcessor.finish();
			dataset.models[0].setStartSvm(svmProcessor.model, svmProcessor.sum, svmProcessor.getConditions(), svmProcessor.upstream, svmProcessor.downstream, svmProcessor.probCutoff);
		}
		
		log.info("Writing start stop estimates");
		startstopProcessor.finish();
		
		log.info("Writing internal estimates");
		internalProcessor.finish(progress);
		dataset.models[0].setRolling(trim,threshold);
		
		gapsProcessor.finish(progress);
		gofProcessor.finish(progress);

		
		
		log.info("Computing scores for major isoforms");
		LineWriter scores = new LineOrientedFile(prefix+".major.scores").write().writeLine("Gene\tMean\tGap\tLength\tThreshold\tPval");
		// "Gene\tCds\tLength\tSum\tMean coverage\tGaps\tStart\tStop"
		for (String[] f : new LineOrientedFile(prefix+".bfits.data").lineIterator().skip(1).map(s->StringUtils.split(s,'\t')).loop()) {
//			double gap = dataset.model.computeGapPval(Integer.parseInt(f[2]), Double.parseDouble(f[4]), Integer.parseInt(f[5]));
			double pval = dataset.models[0].getGapPvalue(Integer.parseInt(f[2]), Double.parseDouble(f[3])/Integer.parseInt(f[2]), Integer.parseInt(f[5]));
			scores.writef("%s\t%.5f\t%d\t%d\t%.4f\t%.4g\n", f[0],Double.parseDouble(f[3])/Integer.parseInt(f[2]), Integer.parseInt(f[5]), Integer.parseInt(f[2]),
					dataset.models[0].getGapThreshold(Double.parseDouble(f[3])/Integer.parseInt(f[2])),
					pval);
		}
		scores.close();


		PageFileWriter pf = new PageFileWriter(prefix+".model");
		for (RiboModel model : dataset.models)
			model.serialize(pf);
		pf.close();
		
		
		
		log.info("Running R scripts for plotting");
		RRunner r = new RRunner(prefix+".plot.R");
		r.set("prefix",prefix);
		r.addSource(EstimateModelError.class.getResourceAsStream("signaltonoise_eval.R"));
		r.addSource(EstimateModelError.class.getResourceAsStream("startcodon_eval.R"));
		r.addSource(EstimateModelError.class.getResourceAsStream("error_eval.R"));
		r.run(false);
		
//		RConnect.R().set("prefix", prefix);
//		RConnect.R().run(EstimateModelError.class.getResource("error_eval.R"));
//		RConnect.R().run(EstimateModelError.class.getResource("signaltonoise_eval.R"));
//		RConnect.R().run(EstimateModelError.class.getResource("startcodon_eval.R"));



	}

	
	private static double determineLambda(String prefix, Dataset ds, Progress progress, int m, int o, int f) throws IOException {
		
		int R = 1000;
		ReferenceSequence ref = Chromosome.obtain("chr1+");
		
		RandomNumbers rnd = new RandomNumbers();
		
		char[] seq = new char[12+100];
		for (int i=0; i<seq.length; i++)
			seq[i] = SequenceUtils.nucleotides[rnd.getUnif(0, 4)];

		progress.init().setCount(R);
		double s = 0;
		for (int r=0; r<R; r++) {
			
			IntervalTree<GenomicRegion, DefaultAlignedReadsData> simu = new IntervalTree<GenomicRegion, DefaultAlignedReadsData>(null);
			for (int p=0; p<=9; p+=3) 
				for (int i=0; i<m; i++)
					ds.models[0].generateRead(simu , p, ss->seq[ss+50]);
			
			for (int i=0; i<o; i++)
				ds.models[0].generateRead(simu , f+3, ss->seq[ss+50]);

			s -= getLambda(ds,f+3,EI.wrap(simu.keySet()).map(reg->new ImmutableReferenceGenomicRegion<>(ref,reg,simu.get(reg))));
			double us = s;
			double ur = r;
			progress.incrementProgress().setDescription(()->"Lambda = "+(us/(ur+1)));
		}
		progress.finish();
		
		s /= R;
		LineWriter data = new LineOrientedFile(prefix+".lambda.data").write().writeLine("Major count\tOff frame count\tOff frame pos\tLambda");
		data.writef2("%d\t%df\t%d\t%.5f\n",m,o,f,s);
		data.close();
		
		ds.models[0].setInferenceLamdba(s);
		
		
		return s;
	}
	
	private static double getLambda(Dataset ds, int p, Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>> reads) {
		ReadsXCodonMatrix m = new ReadsXCodonMatrix(ds.models[0], -1);
		int readcount = m.addAll(reads);
		m.finishReads();
		
		if (readcount==0) throw new RuntimeException("No reads");
		
		int cond = m.checkConditions();
		if (cond==-2) // no reads at all
			throw new RuntimeException("No reads");
		
		if (cond<0) throw new RuntimeException("Inconsistent conditions!");
		double threshold = 1E-2;
		int maxIter = 1000;
		
		int iters = 0;
		double lastDifference;
		do  {
			m.computeExpectedReadsPerCodon();
			m.computePriorReadProbabilities();
			m.computeExpectedCodonPerRead();
			iters++;
			lastDifference = m.computeExpectedCodons();
		} while (lastDifference>threshold && iters<maxIter);
	
		for (Codon c : m.getCodons())
			if (c.getStart()==p) return m.regularize3(c);
		throw new RuntimeException("No codon");
		
	}

	private static MutablePair<PiecewiseLinearFunction, PiecewiseLinearFunction> fitRollingMeanVar(RollingStatistics rolling, int halfbinsize, int step, String prefix, String name) throws IOException {

		DoubleArrayList cov = new DoubleArrayList();
		DoubleArrayList mean = new DoubleArrayList();
		DoubleArrayList var = new DoubleArrayList();

		log.info("Computing "+name+" data");
		rolling.iterateEquiSize(halfbinsize, step).forEachRemaining(rb->{
			cov.add(Math.round(rb.getCovariate()*1E6)*1E-6);
			fitMoments(rb.getValues(),mean,var);	
		});
		LineWriter txt = new LineOrientedFile(prefix+"."+name+".model.txt").write().writeLine("Coverage\tmean\tvar\tmean.Smooth\tvar.Smooth");

		log.info("Fitting spline for "+name+" means");
		SmoothSplineResult spla = SmoothSpline.fitDFMatch(cov.toDoubleArray(), mean.toDoubleArray(), 20);
		log.info("Fitting spline for "+name+" variances");
		SmoothSplineResult splb = SmoothSpline.fitDFMatch(cov.toDoubleArray(), var.toDoubleArray(), 10);

		for (int m=0; m<cov.size(); m++) 
			txt.writef("%.6f\t%.6g\t%.6g\t%.6g\t%.6g\n", cov.getDouble(m), mean.getDouble(m), var.getDouble(m), //0.,0.);
					SmoothSpline.predict(spla, cov.getDouble(m), 0),SmoothSpline.predict(splb, cov.getDouble(m), 0));
		txt.close();


		PiecewiseLinearFunction plfa = new PiecewiseLinearFunction(cov.toDoubleArray(), EI.wrap(cov.toDoubleArray()).mapToDouble(x->SmoothSpline.predict(spla, x, 0)).toDoubleArray());
		PiecewiseLinearFunction plfb = new PiecewiseLinearFunction(cov.toDoubleArray(), EI.wrap(cov.toDoubleArray()).mapToDouble(x->SmoothSpline.predict(splb, x, 0)).toDoubleArray());

		return new MutablePair<PiecewiseLinearFunction, PiecewiseLinearFunction>(plfa, plfb);
	}

	private static void fitMoments(NumericArray d, DoubleArrayList mean, DoubleArrayList var) {
		double m = NumericArrayFunction.Mean.applyAsDouble(d);
		double v = NumericArrayFunction.Variance.applyAsDouble(d);
		//		double c = m*(1-m)/v-1;
		//		alpha.add(m*c);
		//		beta.add((1-m)*c);
		mean.add(m);
		var.add(v);
	}


	private static int max(double[] cod, int i) {
		return ArrayUtils.argmax(cod, i-1, i+2);
	}

	/**
	 * 
	 * 
	 * Storage contains regions of clusters of overlapping transcripts (~ a gene); the data is a interval tree containing the nontrans regions
	 * with their cds associated
	 * 
	 * @param annotation
	 * @param sequence
	 * @param progress
	 * @return
	 */
	private static MemoryIntervalTreeStorage<IntervalTree<GenomicRegion, ImmutableReferenceGenomicRegion<String>>> inferNontrans(
			MemoryIntervalTreeStorage<Transcript> annotation,
			SequenceProvider sequence, Progress progress) {

		MemoryIntervalTreeStorage<IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>> re = new MemoryIntervalTreeStorage<IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>>((Class)IntervalTree.class);

		for (ReferenceSequence ref : annotation.getReferenceSequences()){
			annotation.getTree(ref)
			.groupIterator(100)
			.map(r->new ArrayGenomicRegion(new IntervalTree<GenomicRegion,Transcript>(r,ref)))
			.forEachRemaining(reg->re.add(ref, reg.extendFront(50).extendBack(50), new IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>(ref)));
		}


		List<ImmutableReferenceGenomicRegion<IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>>> singleton = new MutableMonad<ImmutableReferenceGenomicRegion<IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>>>().asMonadList();  
		for (ImmutableReferenceGenomicRegion<Transcript> t : 
			annotation.ei()
			.progress(progress, (int)annotation.size(), r->r.getData().getTranscriptId())
			.filter(r->r.getData().isCoding()).loop()
				) {

			re.getReferenceRegionsIntersecting(t.getReference(), t.getRegion(), singleton);
			IntervalTree<GenomicRegion, ImmutableReferenceGenomicRegion<String>> set = singleton.get(0).getData();

			ImmutableReferenceGenomicRegion<String> cds = new ImmutableReferenceGenomicRegion<String>(t.getReference(), t.getData().getCds(t.getReference(), t.getRegion()),t.getData().getTranscriptId());
			String seq = sequence.getSequence(cds).toString();

			identifyNonTrans(seq,(s,e)->set.put(cds.map(new ArrayGenomicRegion(s,e)), cds));
			//			System.out.println(seq);
			//			System.out.println(StringUtils.packAscii(cds,set.iterator()));

			singleton.clear();

		}

		//		System.out.println(re.ei().mapToInt(r->r.getData().toGenomicRegion(gr->gr).getTotalLength()).collect((a,b)->a+b));


		return re;
	}


	static Trie<String> nonTransStart = new Trie<String>();
	static Trie<String> nonTransEnd = new Trie<String>();

	static {
		nonTransStart.put("TAA", "TAA");
		nonTransStart.put("TAG", "TAG");
		nonTransStart.put("TGA", "TGA");

		nonTransEnd.putAll(nonTransStart);
		StringUtils.iterateHammingHull("ATG",Alphabet.getDna()).forEachRemaining(s->nonTransEnd.put(s,s));
	}
	private static void identifyNonTrans(String seq,
			BiIntConsumer re) {

		ToIntMappedIterator<AhoCorasickResult<String>> st = nonTransStart.iterateAhoCorasick(seq).mapToInt(r->r.getStart()+3);
		while (st.hasNext()) {
			int s = st.nextInt();
			nonTransEnd.iterateAhoCorasick(seq.substring(s)).mapToInt(r->r.getStart()).filter(i->i%3==0).head(1).filter(i->i>0).forEachRemaining(e->re.accept(s, s+e));
		}

	}

	
	private static class Inferred {

		ImmutableReferenceGenomicRegion<IntervalTree<GenomicRegion, ImmutableReferenceGenomicRegion<String>>> gene;
		ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons;
		MajorIsoform major;
		ToDoubleFunction<Collection<Codon>> gofComp;
		
		public Inferred(Dataset dataset,
				ImmutableReferenceGenomicRegion<IntervalTree<GenomicRegion, ImmutableReferenceGenomicRegion<String>>> gene,
				ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons, ToDoubleFunction<Collection<Codon>> gofComp) {
			this.gene = gene;
			this.codons = codons;
			this.gofComp = gofComp;
			major = new MajorIsoform(dataset, gene, codons);
		}

		public double getCoverage() {
			return ArrayUtils.sum(major.ccc)/major.ccc.length;
		}

		public void loadCodons() {
			major.loadCodons(codons);
		}
		
	}

	private static Inferred infer(
			ImmutableReferenceGenomicRegion<IntervalTree<GenomicRegion, ImmutableReferenceGenomicRegion<String>>> gene,
			CodonInference inference, Dataset dataset) {
		ImmutableReferenceGenomicRegion<Void> codonCoord = new ImmutableReferenceGenomicRegion<Void>(gene.getReference(), gene.getRegion().removeIntrons());
		MutableMonad<ToDoubleFunction<Collection<Codon>>> gofComp = new MutableMonad<ToDoubleFunction<Collection<Codon>>>();
		ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons = inference.inferCodons(dataset.reads, codonCoord.getReference(), codonCoord.getRegion().getStart(), codonCoord.getRegion().getEnd(),gofComp);
		return new Inferred(dataset,gene,codons, gofComp.Item);
	}
	
	private static double[] getRobustNoiseEstimate(double[] infVec, double[] quants) {
		NumericArray buf = NumericArray.createMemory(infVec.length/3*2, NumericArrayType.Double);
		int index = 0;
		for (int i=0; i<infVec.length; i+=3) {
			buf.setDouble(index++, infVec[i+1]);
			buf.setDouble(index++, infVec[i+2]);
		}
		if (index!=buf.length()) throw new RuntimeException();
		buf.sort().cumSum();

		double[] re = new double[quants.length];
		for (int i=0; i<quants.length; i++) {
			int p = (int) (index*(1-quants[i]));
			re[i] = (p>0?buf.getDouble(p-1):0)/p*index;
		}
		return re;
	}

	
	private static class MajorIsoform {

		Dataset dataset;
		
		ImmutableReferenceGenomicRegion<Transcript> trans = null;
		GenomicRegion major = null; // does not include stop codon
		double act = -1;
		
		
		double tsig;
		double tnoi;
		double csig;
		double cnoi;
		double[] infVec;
		double[][] cinfVec;
		double[][] upstreamcinfVec;

		double[] untreatedccc; // does not include stop codon
		double[] ccc; // does not include stop codon
		double[][] accc;  // does not include stop codon
		double[][] upstream;  // does not include stop codon
		int lenUpstream;
		GenomicRegion upstreamPart;
		
		public MajorIsoform(Dataset dataset, ImmutableReferenceGenomicRegion<IntervalTree<GenomicRegion, ImmutableReferenceGenomicRegion<String>>> gene, ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
			this.dataset = dataset;
			this.lenUpstream = 50;
			
			Iterable<ImmutableReferenceGenomicRegion<Transcript>> itrbl;
			
			if (dataset.predefinedMajor==null)
				itrbl = dataset.genomic.getTranscripts().ei(gene)
						.filter(r->r.getData().isCoding()) // only coding
						.loop();
			else {
				ImmutableReferenceGenomicRegion<Transcript>[] tr = dataset.genomic.getTranscripts().ei(gene)
					.filter(r->r.getData().isCoding()) // only coding
					.filter(r->dataset.predefinedMajor.contains(r.getReference(), r.getRegion()))
					.<ImmutableReferenceGenomicRegion<Transcript>>toArray((Class)ImmutableReferenceGenomicRegion.class);
				if (tr.length==0) 
					itrbl = EI.<ImmutableReferenceGenomicRegion<Transcript>>empty().loop();
				else if (tr.length==1)
					itrbl = EI.singleton(tr[0]).loop();
				else
					throw new RuntimeException("Two transcripts of cluster "+gene.toLocationString()+" in given major: "+StringUtils.toString(tr));
			}
			
			
			for (ImmutableReferenceGenomicRegion<Transcript> tr : itrbl) {

				GenomicRegion cds = tr.getData().getCds(tr.getReference(),tr.getRegion()); // map to cds
				if (!SequenceUtils.isOrf(dataset.genomic.getSequence(gene.getReference(), cds).toString(),false)) // keep only the ORFs
					continue;
				cds=codons.induce(cds); // to the gene coordinate system
				cds = cds.extendBack(-3); // cut the stop codon

				double cact = 0;
				for (Codon cc : codons.getData().getIntervalsIntersecting(cds.getStart(), cds.getStop(), new ArrayList<>())) {
					if (cds.containsUnspliced(cc) && cds.induce(cc).getStart()%3==0) {
						cact+=cc.getTotalActivity();
					}
				}
				if (cact>act) {
					major = cds;
					act = cact;
					trans = tr;
					
					GenomicRegion trInGene = codons.induce(tr.getRegion());
					int startInTr = trInGene.induce(cds.getStart());
					upstreamPart = trInGene.mapMaybeOutside(new ArrayGenomicRegion(startInTr-lenUpstream*3,startInTr+2));
				}
			}
			
		}

		public void loadCodons(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
			tsig = 0;
			tnoi = 0;
			csig = 0;
			cnoi = 0;
			infVec = new double[major.getTotalLength()];
			cinfVec = new double[dataset.numCond][major.getTotalLength()];
			upstreamcinfVec = new double[dataset.numCond][lenUpstream*3];
			
			ccc = new double[major.getTotalLength()/3];
			untreatedccc = new double[major.getTotalLength()/3];
			accc = new double[dataset.numCond][major.getTotalLength()/3];
			upstream = new double[dataset.numCond][lenUpstream];
			
			
			for (Codon cc : codons.getData().getIntervalsIntersecting(major.getStart(), major.getStop(), new ArrayList<>())) {
				if (major.containsUnspliced(cc)) { 
					if (major.induce(cc).getStart()%3==0) {
						tsig+=cc.getTotalActivity();
						if (cc.getTotalActivity()>=1)
							csig++;

						int pos = major.induce(cc).getStart()/3;
						ccc[pos]+=cc.getTotalActivity();
						for (int c=0; c<dataset.numCond; c++) {
							accc[c][pos]+=cc.getActivity()[c];
							if (dataset.isUntreated(c))
								untreatedccc[pos]+=cc.getActivity()[c];
						}
					}
					else {
						tnoi+=cc.getTotalActivity();
						if (cc.getTotalActivity()>=1)
							cnoi++;
					}
					int pos = major.induce(cc).getStart();
					infVec[pos]+=cc.getTotalActivity();
					for (int c=0; c<dataset.numCond; c++)
						cinfVec[c][pos]+=cc.getActivity()[c];
				}
				else if (upstreamPart.containsUnspliced(cc)) {
					if (upstreamPart.induce(cc).getStart()%3==0) {
						int pos = upstreamPart.induce(cc).getStart()/3;
						for (int c=0; c<dataset.numCond; c++)
							upstream[c][pos]+=cc.getActivity()[c];
					}
					int pos = upstreamPart.induce(cc).getStart();
					for (int c=0; c<dataset.numCond; c++)
						upstreamcinfVec[c][pos]+=cc.getActivity()[c];
				}
			}
		}

	}
	
	private static class Dataset {

		MemoryIntervalTreeStorage<Transcript> predefinedMajor;
		
		GenomicRegionStorage<AlignedReadsData> reads;
		RiboModel[] models;
		Genomic genomic;
		int numCond;
		String[] conditions;
		HashMap<String, Integer> condIndex;
		Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter;

		private String[] startPairs;
		private int[][] startCodonPairs;
		private BitVector untreated;
		
		public Dataset(GenomicRegionStorage<AlignedReadsData> reads,
				RiboModel[] models, Genomic g, Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter, 
				SimpleCodonModel overrideSimpleModel, MemoryIntervalTreeStorage<Transcript> predefinedMajor, String[] startPairs) throws UsageException {
			this.reads = reads;
			this.models = models;
			this.genomic = g;
			this.filter = filter;
			this.predefinedMajor = predefinedMajor;
			
			for (RiboModel model : models)
				model.setSimple(overrideSimpleModel);
			
			numCond = reads.getRandomRecord().getNumConditions();
			conditions = new String[numCond];
			if (reads.getMetaData().isNull()) {
				for (int c=0; c<conditions.length; c++)
					conditions[c] = c+"";
			} else {
				for (int c=0; c<conditions.length; c++) {
					conditions[c] = reads.getMetaData().getEntry("conditions").getEntry(c).getEntry("name").asString();
					if ("null".equals(conditions[c])) conditions[c] = c+"";
				}
			}
			condIndex = ArrayUtils.createIndexMap(conditions);
			
			this.untreated = new BitVector(numCond);
			this.untreated.not();
			this.startPairs = startPairs;
			startCodonPairs = new int[startPairs.length][2];
			for (int c=0; c<startPairs.length; c++) {
				String[] p = StringUtils.split(startPairs[c], '/');
				if (p.length!=2) throw new UsageException("Wrong syntax in start codon treatment pair: "+startPairs[c]);
				for (int ind=0; ind<2; ind++) {
					if (condIndex.containsKey(p[ind]))
						startCodonPairs[c][ind] = condIndex.get(p[ind]);
					else if (StringUtils.isInt(p[ind]))
						startCodonPairs[c][ind] = Integer.parseInt(p[ind]);
					else
						throw new UsageException("Condition "+p[ind]+" unknown for treatment pair!");
				}
				
				if (condIndex.containsKey(p[0]))
					untreated.putQuick(condIndex.get(p[0]), false);
				else if (StringUtils.isInt(p[0]))
					untreated.putQuick(Integer.parseInt(p[0]), false);
			}
		}

		public boolean isUntreated(int c) {
			return untreated.getQuick(c);
		}
	}
	
	
	private static class AroundStartProcessor {
		
		private int aroundStartUpstream = 50;
		private int aroundStartDownstream = 300;
		
		double[] aroundStart;
		double[] aroundStartSimple;
		
		private String prefix;
		private Dataset dataset;
		private SimpleCodonModel simpleModel;
		
		public AroundStartProcessor(String prefix, Dataset dataset, SimpleCodonModel simpleModel) {
			aroundStart = new double[aroundStartDownstream+aroundStartUpstream];
			aroundStartSimple = new double[aroundStartDownstream+aroundStartUpstream];
			this.prefix = prefix;
			this.dataset = dataset;
			this.simpleModel = simpleModel;
		}
		
		
		public void process(Inferred inferred) throws IOException {
			if (inferred.major.major.getTotalLength()<aroundStartDownstream)
				return;
			
			ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons = inferred.codons;
			
			
			double[] tass = new double[aroundStartSimple.length];
			ArrayGenomicRegion introns = inferred.major.major.invert();
			for (ImmutableReferenceGenomicRegion<AlignedReadsData> read : dataset.reads.ei(codons).filter(r->codons.contains(r)).loop() ) {
				GenomicRegion reg = codons.induce(read.getRegion());
				for (int d=0; d<read.getData().getDistinctSequences(); d++) {
					int pos = simpleModel.getPosition(read, d);
					int start = reg.getStart()+pos;
					
					if (pos!=-1 && !introns.contains(start)) {
						start = inferred.major.major.induceMaybeOutside(start);
						if (start>=-aroundStartUpstream && start<aroundStartDownstream)
							for (int c=0; c<inferred.major.upstreamcinfVec.length; c++)
								if (dataset.isUntreated(c))
									tass[start+aroundStartUpstream]+=read.getData().getCount(d, c, ReadCountMode.Weight);
					}
				}
			}
			
			double st = 0;
			for (int c=0; c<inferred.major.upstreamcinfVec.length; c++)
				if (dataset.isUntreated(c))
					st+=inferred.major.cinfVec[c][0];
			
			if (tass[aroundStartUpstream]>1 && st>1) {
			
				synchronized (aroundStart) {
					ArrayUtils.mult(tass, 1/(tass[aroundStartUpstream]));
					ArrayUtils.add(aroundStartSimple, tass);
		
					
					for (int i=0; i<aroundStartUpstream; i++)
						for (int c=0; c<inferred.major.upstreamcinfVec.length; c++)
							if (dataset.isUntreated(c))
								aroundStart[i]+=inferred.major.upstreamcinfVec[c][inferred.major.upstreamcinfVec[c].length-aroundStartUpstream+i]/st;
					
					for (int i=0; i<aroundStartDownstream; i++) {
						for (int c=0; c<inferred.major.upstreamcinfVec.length; c++)
							if (dataset.isUntreated(c))
								aroundStart[i+aroundStartUpstream]+=inferred.major.cinfVec[c][i]/st;
					}	
				}
			}

		}
		
		public void finish() throws IOException {
			LineWriter out = new LineOrientedFile(prefix+".aroundStart.data").write().writeLine("Type\tPosition\tReads");
			for (int i=0; i<aroundStart.length; i++)
				out.writef("PRICE\t%d\t%.2f\n", i-aroundStartUpstream, aroundStart[i]);
			for (int i=0; i<aroundStartSimple.length; i++)
				out.writef("Simple\t%d\t%.2f\n", i-aroundStartUpstream, aroundStartSimple[i]);
			out.close();
		}
		
	}
	
	private static class SignalToNoiseProcessor {
		
		double[] robnoi = ArrayUtils.seq(0, 0.25, 0.001);
		double[] sndata;
		double[] cndata;
		double[][] robdata;
		String prefix;
		Dataset dataset;
		
		SimpleCodonModel simpleModel;
		HashMap<String, double[]> perReadCounter;
		HashMap<String, double[]> perReadCounterUni;
		LineWriter signalToNoiseDetails;
	
		
		
		public SignalToNoiseProcessor(String prefix, Dataset dataset, SimpleCodonModel simpleModel) throws IOException {
			sndata = new double[simpleModel==null?2:4];
			cndata = new double[simpleModel==null?2:4];
			robdata = simpleModel==null?new double[1][robnoi.length]:new double[2][robnoi.length];
			this.prefix = prefix;
			this.dataset = dataset;
			this.simpleModel = simpleModel;
			perReadCounter = new HashMap<String, double[]>();
			perReadCounterUni = new HashMap<String, double[]>();
			signalToNoiseDetails = new LineOrientedFile(prefix+".signaltonoise.details.data").write(true).writeLine("Location\tSignal\tNoise\tSimple.Signal\tSimple.Noise\tReads");
			
		}


		public void process(Inferred inferred) throws IOException {
			
			MajorIsoform mi = inferred.major;
			ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons = inferred.codons;
			GenomicRegion major = mi.major;
			ImmutableReferenceGenomicRegion<Transcript> trans = mi.trans;
			double tsig = mi.tsig;
			double tnoi = mi.tnoi;
			double[] infVec = mi.infVec;
			
			double[] trobnoi = getRobustNoiseEstimate(infVec, robnoi);
			synchronized (this) {
				sndata[0]+=tsig;
				sndata[1]+=tnoi;
				cndata[0]+=mi.csig;
				cndata[1]+=mi.cnoi;
				for (int j=0; j<trobnoi.length; j++) robdata[0][j]+=trobnoi[j];
			}
			
			double readc = 0;
			double ssig = 0;
			double snoi = 0;
			double csig = 0;
			double cnoi = 0;
			infVec = new double[major.getTotalLength()];
			ExtendedIterator<ImmutableReferenceGenomicRegion<AlignedReadsData>> it = dataset.reads.ei(codons);
			if (dataset.filter!=null)
				it = it.filter(dataset.filter);

			for (ImmutableReferenceGenomicRegion<AlignedReadsData> read : it.filter(r->codons.contains(r)).loop() ) {
				GenomicRegion reg = codons.induce(read.getRegion());
				// extract putative codon part: center +/- 2 i.e.5 bp
				ArrayGenomicRegion pcod = reg.map(new ArrayGenomicRegion(reg.getTotalLength()/2-2,reg.getTotalLength()/2+3));
				if (major.intersects(pcod)) //major.contains(reg.getStart()))
					for (int d=0; d<read.getData().getDistinctSequences(); d++) {
						double c = read.getData().getTotalCountForDistinct(d, ReadCountMode.Weight);
						boolean lm = RiboUtils.hasLeadingMismatch(read.getData(), d);
						int l = read.getRegion().getTotalLength();
						if (lm&&!RiboUtils.isLeadingMismatchInsideGenomicRegion(read.getData(), d))
							l++;
						String key = (lm?"-":"")+l;
						Integer frame = null;
						for (int s = 0; s<reg.getTotalLength(); s+=3) {
							int m = reg.map(s);
							if (major.contains(m)){
								frame = major.induceMaybeOutside(m)%3;
								break;
							}
						}

						if (frame!=null) {
							if (frame<0) frame = (3-frame)%3;
							synchronized (perReadCounter) {
								perReadCounter.computeIfAbsent(key, s->new double[3])[frame]+=c;
								if (c>=1)
									perReadCounterUni.computeIfAbsent(key, s->new double[3])[frame]++;
							}
							readc+=c;
						}

						if (simpleModel!=null) {
							int simplePos = simpleModel.getPosition(read,d);
							if (simplePos>=0) {
								simplePos = reg.map(simplePos);
								if (major.contains(simplePos)) {
									if (major.induce(simplePos)%3==0) {
										ssig+=c;
										if (c>=1)
											csig++;
									} else {
										snoi+=c;
										if (c>=1)
											cnoi++;
									}
									infVec[major.induce(simplePos)]+=c;
								}
							}
						}
					}
			}

			double[] srobnoi = getRobustNoiseEstimate(infVec, robnoi);
			if (simpleModel!=null) {
				synchronized (this) {
					sndata[2]+=ssig;
					sndata[3]+=snoi;
					cndata[2]+=csig;
					cndata[3]+=cnoi;
					for (int j=0; j<srobnoi.length; j++) robdata[1][j]+=srobnoi[j];
				}
			}

			synchronized (signalToNoiseDetails) {
				signalToNoiseDetails.writef("%s\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n", trans.toLocationString(), tsig, tnoi,ssig, snoi,readc);	
			}
			

		}
		
		public void finish() throws IOException {
			signalToNoiseDetails.close();
			LineWriter signalToNoise = new LineOrientedFile(prefix+".signaltonoise.data").write().writeLine("Type\tSignal\tNoise");
			signalToNoise.writef("Codon\t%.2f\t%.2f\n", sndata[0], sndata[1]);
			if (simpleModel!=null)
				signalToNoise.writef("Simple\t%.2f\t%.2f\n", sndata[2], sndata[3]);
			for (String k : perReadCounter.keySet()) {
				double[] c = perReadCounter.get(k);
				int m = ArrayUtils.argmax(c);
				double s = ArrayUtils.sum(c);
				signalToNoise.writef("%s\t%.2f\t%.2f\n", k, c[m], s-c[m]);
			}
			signalToNoise.close();
			
			
			signalToNoise = new LineOrientedFile(prefix+".codonsignaltonoise.data").write().writeLine("Type\tSignal\tNoise");
			signalToNoise.writef("Codon\t%.2f\t%.2f\n", cndata[0], cndata[1]);
			if (simpleModel!=null)
				signalToNoise.writef("Simple\t%.2f\t%.2f\n", cndata[2], cndata[3]);
			for (String k : perReadCounterUni.keySet()) {
				double[] c = perReadCounterUni.get(k);
				int m = ArrayUtils.argmax(c);
				double s = ArrayUtils.sum(c);
				signalToNoise.writef("%s\t%.2f\t%.2f\n", k, c[m], s-c[m]);
			}
			signalToNoise.close();

			LineWriter robNoise = new LineOrientedFile(prefix+".robustnoise.data").write().writeLine("Type\tTrim\tNoise");
			String[] type = {"Codon","Simple"};
			for (int t=0; t<robdata.length; t++) {
				for (int q=0; q<robnoi.length; q++) {
					robNoise.writef("%s\t%.4f\t%.2f\n", type[t],robnoi[q],robdata[t][q]);
				}
			}
			robNoise.close();
		}

		
	}
	
	private static class InternalProcessor {
		String prefix;
		Dataset dataset;
		RollingStatistics[] internalQuantiles;
		LineWriter internal;
		
		int internalHalfBin;
		int internalStep;
		
		int totalbglength = 0;
		int totalPresent = 0;

		public InternalProcessor(String prefix, Dataset dataset, int internalHalfBin, int internalStep) throws IOException {
			this.prefix = prefix;
			this.dataset = dataset;
			this.internalHalfBin = internalHalfBin;
			this.internalStep = internalStep;
			
			internalQuantiles = new RollingStatistics[] {new RollingStatistics(),new RollingStatistics(),new RollingStatistics()};
			internal = new LineOrientedFile(prefix+".internal.data").write().writeLine("Location\tFrame\tSum\tNonTrans");
		}

		
		public void process(Inferred inferred) throws IOException {
			
			ImmutableReferenceGenomicRegion<IntervalTree<GenomicRegion, ImmutableReferenceGenomicRegion<String>>> gene = inferred.gene;
			ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons = inferred.codons;
			
			Iterator<Entry<GenomicRegion, ImmutableReferenceGenomicRegion<String>>> nontransIt = gene.getData().entrySet().iterator();
			while (nontransIt.hasNext()) {
				Entry<GenomicRegion, ImmutableReferenceGenomicRegion<String>> nt = nontransIt.next();
				GenomicRegion ntreg = codons.induce(nt.getKey());

				GenomicRegion codReg = ntreg.extendFront(1).extendBack(1); // additional entriy before and one after the ntreg
				double[] cod = new double[codReg.getTotalLength()-2]; // one entry for each of the codons in codReg

				for (Codon cc : codons.getData().getIntervalsIntersecting(codReg.getStart(), codReg.getStop(), new ArrayList<>())) 
					if (codReg.containsUnspliced(cc)) {
						ArrayGenomicRegion cci = codReg.induce(cc);
						cod[cci.getStart()] = cc.getTotalActivity();
					}

				for (int p=1; p<cod.length; p+=3) {
					int am = max(cod,p);
					double max = cod[am];
					if (max>=1) {
						
						totalbglength++;
						if (cod[p]>=1)
							totalPresent++;
						
						double sum = cod[p-1]+cod[p]+cod[p+1];
						int genomic = codons.map(codReg.map(p));
						int frame = nt.getValue().induce(genomic)%3;
						//						if (cod[p]<0.99*max)
						synchronized (internalQuantiles[frame]) {
							internal.writef("%s:%d\t%d\t%.6f\t%.6g\n",gene.getReference(),genomic,frame,sum,cod[p]);
							internalQuantiles[frame].add(sum, cod[p]/sum);
						}
					}
				}
			}
		}

		
		public void finish(Progress progress) throws IOException {
			internal.close();
			
			internalQuantiles[1].setProgress(progress);
			internalQuantiles[2].setProgress(progress);
			
			// compute internal model
			int halfbinsize = internalHalfBin;
			int step = internalStep;
			double[] quantiles = {0.90,0.91,0.92,0.93,0.94,0.95,0.96,0.97,0.98,0.99};
			NumericArrayFunction[] functions = new NumericArrayFunction[quantiles.length];
			for (int i=0; i<functions.length; i++) 
				functions[i] = NumericArrayFunction.quantile(quantiles[i]);

			PiecewiseLinearFunction[][] internalFit = new PiecewiseLinearFunction[3][quantiles.length];

			LineWriter txt = new LineOrientedFile(prefix+".internal.model.txt").write().writeLine("Frame\tSum\tQuantile\tValue\tSmooth");
			for (int i=1; i<3; i++) {
				log.info("Computing internal data for frame "+i);
				StepFunction[] steps = internalQuantiles[i].computeEquiSize(halfbinsize,step,functions);
				for (int s=0; s<steps.length; s++) {

					double[] lx = steps[s].getX().clone();
					for (int l=0; l<lx.length; l++)
						lx[l] = Math.log10(Math.round(lx[l]*1E6)*1E-6);

					if (steps[s].getKnotCount()>1) {
						log.info("Fitting spline for quantile "+quantiles[s]);
						SmoothSplineResult spl = SmoothSpline.fitDFMatch(lx, steps[s].getY(), 10);
	
						internalFit[i][s] = new PiecewiseLinearFunction(steps[s].getX(), EI.wrap(steps[s].getX()).mapToDouble(x->Math.min(1, SmoothSpline.predict(spl, Math.log10(x), 0))).toDoubleArray());
					} else {
						internalFit[i][s] = new PiecewiseLinearFunction(steps[s].getX(), steps[s].getY());
					}
					for (int m=0; m<steps[s].getKnotCount(); m++) 
						txt.writef("%d\t%.6f\t%.2f\t%.6g\t%.6g\n", i, steps[s].getX(m), quantiles[s], steps[s].getY(m),
								//							SmoothSpline.predict(spl, Math.log10(steps[s].getX(m)), 0),
								internalFit[i][s].applyAsDouble(steps[s].getX(m)));


				}
			}
			txt.close();
			dataset.models[0].setInternal(quantiles, internalFit[1], internalFit[2]);
			dataset.models[0].setCodonProbInBackground(totalPresent/(double)totalbglength);
			
		}
		
	}
	
	private static class SimulateProcessor {

		int flank = 50;
		
		IterateIntoSink<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>> simulate;
		LineWriter simuDetails;

		Dataset dataset;
		int modelIndex;
		
		public SimulateProcessor(String prefix, Dataset dataset,
				IterateIntoSink<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>> simulate, int modelIndex) {
			this.simulate = simulate;
			this.dataset = dataset;
			this.modelIndex = modelIndex;
			if (simulate!=null)
				simuDetails = simulate!=null?new LineOrientedFile(prefix+".simulation.details.data").write():null;
		}



		public void process(Inferred inferred) throws IOException {
			
			MajorIsoform mi = inferred.major;
			ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons = inferred.codons;
			if (simulate!=null) {
				
				GenomicRegion major = mi.major;
				double[] ccc = mi.ccc;
				ImmutableReferenceGenomicRegion<Transcript> trans = mi.trans;
				double tsig = mi.tsig;
				
				// for simulation
				MutableReferenceGenomicRegion<?> ecodonCoord = codons.toMutable().alterRegion(r->r.extendBack(flank).extendFront(flank));
				GenomicRegion emajor = major.translate(flank);
				CharSequence seq = dataset.genomic.getSequence(ecodonCoord);
				IntervalTree<GenomicRegion,DefaultAlignedReadsData> simReads = new IntervalTree<GenomicRegion,DefaultAlignedReadsData>(ecodonCoord.getReference().toPlusStrand());
				int r1 = 0;
				for (int p=0; p<ccc.length; p++) {
					int majorP = p*3;
					for (int n=0; n<ccc[p]; n++) 
						if (dataset.models[modelIndex].generateRead(simReads,majorP,pos->seq.charAt(emajor.mapMaybeOutside(pos))))
							r1++;
				}
				MutableDouble r2 = new MutableDouble();
				for (ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> r : simReads.ei().map(r->ecodonCoord.map(r.toMutable().alterRegion(x->emajor.mapMaybeOutside(x)))).loop()) {
					try {
						simulate.put(r);
						r2.N+=r.getData().getTotalCountOverall(ReadCountMode.Weight);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				synchronized (simuDetails) {
					simuDetails.writef("%s\t%.2f\t%.2f\t%d\t%.2f\n", trans.toLocationString(), tsig, ArrayUtils.sum(ccc), r1, r2.N);	
				}
			}
		}
		
		public void finish() throws IOException, InterruptedException {
			if (simulate!=null)
				simuDetails.close();
			if (simulate!=null)
				simulate.finish();
		}
		
	}
	
private static class GapsProcessor {
		
		RollingStatistics gaps99;
//		RollingStatistics gapsBeta;
		LineWriter gaps;
		LineWriter gaps2;
		double threshold;
		double trim;
		String prefix;
		Dataset dataset;
		int gapHalfBin;
		int gapStep;
		
		public GapsProcessor(String prefix, Dataset dataset, double threshold, double trim, int gapHalfBin,int gapStep) throws IOException {
//			gapsBeta = new RollingStatistics();
			gaps99 = new RollingStatistics();
			gaps = new LineOrientedFile(prefix+".bfits.data").write().writeLine("Gene\tCds\tLength\tSum\tMean coverage\tGaps\tStart\tStop");
			gaps2 = new LineOrientedFile(prefix+".bfits2.data").write().writeLine("Gene\tCds\tLength\tSum\tMean coverage\tGaps\tStart\tStop");
			this.threshold = threshold;
			this.trim = trim;
			this.prefix = prefix;
			this.dataset = dataset;
			this.gapHalfBin = gapHalfBin;
			this.gapStep = gapStep;
		}

		public void process(Inferred inferred) throws IOException {
			
			MajorIsoform mi = inferred.major;
			
			double[] ccc = mi.ccc;
			double[] utccc = mi.untreatedccc;
			
			
			double[] unsorted = ccc.clone();
			Arrays.sort(ccc);
			int ngaps = 0;
			for (;ngaps<ccc.length && ccc[ngaps]<=threshold; ngaps++);

			double tmcov2;
			if (utccc.length<10)
				tmcov2 = ArrayUtils.sum(utccc, 1, utccc.length)/(utccc.length-1);
			else
				tmcov2 = ArrayUtils.sum(utccc, 10, utccc.length)/(utccc.length-10); 
			
			double[] unsorted2 = utccc.clone();
			Arrays.sort(utccc);
			int ngaps2 = 0;
			for (;ngaps2<utccc.length && utccc[ngaps2]<=threshold; ngaps2++);

			
			double tmCov = NumericArrayFunction.trimmedMean(trim).applyAsDouble(NumericArray.wrap(ccc,ngaps,ccc.length));

			double sum = ArrayUtils.sum(ccc);
			synchronized (gaps99) {
				gaps.writef("%s\t%s\t%d\t%.2f\t%.2f\t%d\t%.2f\t%.2f\n", mi.trans.toLocationString(), mi.major.toRegionString(), 
						ccc.length, ArrayUtils.sum(ccc),tmCov, 
						ngaps, unsorted[0], unsorted[unsorted.length-1]);
				
				gaps2.writef("%s\t%s\t%d\t%.2f\t%.2f\t%d\t%.2f\t%.2f\n", mi.trans.toLocationString(), mi.major.toRegionString(), 
						ccc.length, ArrayUtils.sum(utccc),tmcov2, 
						ngaps2, unsorted2[0], unsorted2[unsorted2.length-1]);
				
				gaps99.add(Math.log10(sum/ccc.length), ngaps/(double)ccc.length);
			}
			
			//				double medCov = new Median().evaluate(ccc, ngaps, ccc.length-ngaps);
//			synchronized (gapsBeta) {
//				
//				gapsBeta.add(tmCov, ngaps/(double)ccc.length);
//			}
		}
	
		public void finish(Progress progress) throws IOException {
			gaps.close();
			gaps2.close();
//			gapsBeta.setProgress(progress);
//			MutablePair<PiecewiseLinearFunction, PiecewiseLinearFunction> momentsModel = fitRollingMeanVar(gapsBeta, gapHalfBin, gapStep, prefix, "Gaps");
//			dataset.model.setGap(momentsModel.Item1, momentsModel.Item2);

			
			StepFunction median = gaps99.computeBinCovariate(gaps99.getCovariateRange()/100, 0, NumericArrayFunction.Median);
			StepFunction mad = gaps99.computeBinCovariate(gaps99.getCovariateRange()/100, 0, NumericArrayFunction.Mad);

			double[] med5mad = new double[median.getKnotCount()];
			for (int i=0; i<med5mad.length; i++)
				med5mad[i] = median.getY(i)+5*mad.getY(i);
			
			log.info("Fitting spline for gaps Median and Mad");
			SmoothSplineResult spl = SmoothSpline.fitDFMatch(median.getX(), med5mad,10);

			PiecewiseLinearFunction gap99Fit = new PiecewiseLinearFunction(median.getX(), 
					EI.wrap(median.getX()).mapToDouble(x->Math.min(1, Math.max(0,SmoothSpline.predict(spl, Math.round(x*1E6)*1E-6, 0)))).toDoubleArray());

			LineWriter txt = new LineOrientedFile(prefix+".gaps.model.txt").write().writeLine("Sum\tValue\tSmooth");

			for (int m=0; m<median.getKnotCount(); m++) 
				txt.writef("%.6f\t%.6g\t%.6g\n", median.getX(m), med5mad[m],
						gap99Fit.applyAsDouble(median.getX(m)));
			txt.close();
			
			dataset.models[0].setGap(gap99Fit);
			
		}

	}

	private static class GofProcessor {
		
		RollingStatistics gof99;
		RollingStatistics gofCod;
		LineWriter gof;
		LineWriter gofc;
		String prefix;
		Dataset dataset;
		
		public GofProcessor(String prefix, Dataset dataset) throws IOException {
	//		gapsBeta = new RollingStatistics();
			gof99 = new RollingStatistics();
			gofCod = new RollingStatistics();
			gof = new LineOrientedFile(prefix+".gof.data").write().writeLine("Activity\tGOF");
			gofc = new LineOrientedFile(prefix+".gofcod.data").write().writeLine("Activity\tGOF");
			this.prefix = prefix;
			this.dataset = dataset;
		}
	
		public void process(Inferred inferred) throws IOException {
			
			double total = 0;
			ArrayList<Codon> cods = new ArrayList<Codon>();
			DoubleArrayList a = new DoubleArrayList();
			DoubleArrayList b = new DoubleArrayList();
			MajorIsoform mi = inferred.major;
			for (Codon c : inferred.codons.getData()) {
				if (mi.major.containsUnspliced(c) && mi.major.induce(c.getStart()) % 3 == 0) {
					cods.add(new Codon(inferred.codons.map(c),c));
					total+=c.getTotalActivity();
					if (c.getTotalActivity()>=1) {
						a.add(Math.log10(c.getTotalActivity()));
						b.add(Math.log10(c.getGoodness()/c.getTotalActivity()));
					}
				}
			}

			double gofv = inferred.gofComp==null?0:inferred.gofComp.applyAsDouble(cods);
			
			synchronized (gof) {
				gof.writef("%.4f\t%.4f\n",total, gofv/total);
				for (int i=0; i<a.size(); i++)
					gofc.writef("%.4f\t%.8f\n",Math.pow(10, a.getDouble(i)), Math.pow(10,b.getDouble(i))); 
				gof99.add(Math.log10(total),Math.log10(gofv/total));
				gofCod.addAll(a.toDoubleArray(), b.toDoubleArray());
			}

			
		}
	
		public void finish(Progress progress) throws IOException {
			gof.close();
			gofc.close();
			
			
			StepFunction median = gof99.computeBinCovariate(gof99.getCovariateRange()/100, 0, NumericArrayFunction.Median);
			StepFunction mad = gof99.computeBinCovariate(gof99.getCovariateRange()/100, 0, NumericArrayFunction.Mad);
			StepFunction count = gof99.computeBinCovariate(gof99.getCovariateRange()/100, 0, NumericArrayFunction.Count);
			
			double[] med5mad = new double[median.getKnotCount()];
			for (int i=0; i<med5mad.length; i++)
				med5mad[i] = i==0||count.getY(i)>50?median.getY(i)+5*mad.getY(i):med5mad[i-1];
			
			log.info("Fitting spline for gaps Median and Mad");
			SmoothSplineResult spl = SmoothSpline.fitDFMatch(median.getX(), med5mad,10);
	
			PiecewiseLinearFunction gof99Fit = new PiecewiseLinearFunction(median.getX(), 
					EI.wrap(median.getX()).mapToDouble(x->SmoothSpline.predict(spl, x, 0)).toDoubleArray());
	
			LineWriter txt = new LineOrientedFile(prefix+".gof.model.txt").write().writeLine("Sum\tValue\tSmooth");
	
			for (int m=0; m<median.getKnotCount(); m++) 
				txt.writef("%.6f\t%.6g\t%.6g\n", median.getX(m), med5mad[m],
						gof99Fit.applyAsDouble(median.getX(m)));
			txt.close();
			
			
			StepFunction p99 = gofCod.computeBinCovariate(gofCod.getCovariateRange()/100, 0, NumericArrayFunction.quantile(0.99));
			count = gofCod.computeBinCovariate(gofCod.getCovariateRange()/100, 0, NumericArrayFunction.Count);
			
			double[] outlier = new double[p99.getKnotCount()];
			for (int i=0; i<outlier.length; i++)
				outlier[i] = i==0||count.getY(i)>50?p99.getY(i):outlier[i-1];
			
			log.info("Fitting spline for codon Gof ");
			SmoothSplineResult splc = SmoothSpline.fitDFMatch(p99.getX(), outlier,10);
	
			PiecewiseLinearFunction gofCodFit = new PiecewiseLinearFunction(p99.getX(), 
					EI.wrap(p99.getX()).mapToDouble(x->SmoothSpline.predict(splc, x, 0)).toDoubleArray());
	
			txt = new LineOrientedFile(prefix+".gofcod.model.txt").write().writeLine("Sum\tValue\tSmooth");
	
			for (int m=0; m<p99.getKnotCount(); m++) 
				txt.writef("%.6f\t%.6g\t%.6g\n", p99.getX(m), outlier[m],
						gofCodFit.applyAsDouble(p99.getX(m)));
			txt.close();
			
			dataset.models[0].setGof(gof99Fit, gofCodFit);
			
		}
	
	}
	
	
	private static class StartStopProcessor {
		
		private static final int changePointBandwidth = 5;
		private static final PreparedIntKernel changePointKernel = new GaussianKernel(changePointBandwidth).prepare();
		
		Dataset dataset;
		MeanVarianceOnline[] startCollector;
		MeanVarianceOnline[] stopCollector;
		MeanVarianceOnline[] startPairsCollector;
		MeanVarianceOnline changePoint;
		int[][] startCodonPairs;
		LineWriter startStopDetails;
		LineWriter startPairsDetails;
		LineWriter changePointDetails;
		
		double threshold;
		double trim;
		String prefix;
		String[] startcodonpairs;
		double[] tmsum;
		

		public StartStopProcessor(String prefix, Dataset dataset, String[] startcodonpairs, double threshold, double trim) throws UsageException, IOException {
			this.prefix = prefix;
			this.dataset = dataset;
			this.startcodonpairs = startcodonpairs;
			this.threshold = threshold;
			this.trim = trim;
			
			startCollector = new MeanVarianceOnline[dataset.numCond];
			stopCollector = new MeanVarianceOnline[dataset.numCond];
			for (int c=0; c<dataset.numCond; c++) {
				startCollector[c] = new MeanVarianceOnline();
				stopCollector[c] = new MeanVarianceOnline();
			}
			tmsum = new double[dataset.numCond];
			
			changePoint = new MeanVarianceOnline();
			startCodonPairs = new int[startcodonpairs.length][2];
			startPairsCollector = new MeanVarianceOnline[startCodonPairs.length];
			for (int c=0; c<startCodonPairs.length; c++) {
				String[] p = StringUtils.split(startcodonpairs[c], '/');
				if (p.length!=2) throw new UsageException("Wrong syntax in start codon treatment pair: "+startcodonpairs[c]);
				for (int ind=0; ind<2; ind++) {
					if (dataset.condIndex.containsKey(p[ind]))
						startCodonPairs[c][ind] = dataset.condIndex.get(p[ind]);
					else if (StringUtils.isInt(p[ind]))
						startCodonPairs[c][ind] = Integer.parseInt(p[ind]);
					else
						throw new UsageException("Condition "+p[ind]+" unknown for treatment pair!");
				}
				startPairsCollector[c] = new MeanVarianceOnline();
			}

			startStopDetails = new LineOrientedFile(prefix+".startstop.details.data").write(true).writeLine("Location\tCondition\tStart\tStop\ttmCov");
			startPairsDetails = new LineOrientedFile(prefix+".startpairs.details.data").write(true).writeLine("Location\tPos\tActivities");
			changePointDetails = new LineOrientedFile(prefix+".changepoint.details.data").write(true).writeLine("Location\tUpstream\tDownstream");

		}

		public void process(Inferred inferred) throws IOException {
			
			MajorIsoform mi = inferred.major;
			ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons = inferred.codons;
			
			GenomicRegion major = mi.major;
			double[][] accc = mi.accc;
			ImmutableReferenceGenomicRegion<Transcript> trans = mi.trans;
			
			double[] starts = new double[dataset.numCond];
			double[] decoy = new double[dataset.numCond];
			double[] stops = new double[dataset.numCond];
			double[] tmCov = new double[dataset.numCond];
			
			
			//Location\tCondition\tStart\tStop\ttmCov
			synchronized (this) {
				for (int c=0; c<dataset.numCond; c++) {
					starts[c] = accc[c][0];
					stops[c] = accc[c][accc[c].length-1];
//					int ngaps = 0;
//					for (;ngaps<accc[c].length && accc[c][ngaps]<=threshold; ngaps++);
					tmCov[c] = NumericArrayFunction.trimmedMean(trim).applyAsDouble(NumericArray.wrap(accc[c]));//,ngaps,accc[c].length));
					if (!Double.isNaN(tmCov[c]))
						tmsum[c]+=tmCov[c];
					
					if (starts[c]>threshold && tmCov[c]>threshold)
						startCollector[c].add(Math.log((starts[c]+threshold)/tmCov[c])/Math.log(2));
					if (stops[c]>threshold && tmCov[c]>threshold)
						stopCollector[c].add(Math.log((stops[c]+threshold)/tmCov[c])/Math.log(2));
					startStopDetails.writef("%s\t%s\t%.2f\t%.2f\t%.2f\n", trans.toLocationString(), dataset.conditions[c], starts[c], stops[c], tmCov[c]);
				}
	
				startPairsDetails.writef("%s\t0\t%s\t%s\n",trans.toLocationString(), StringUtils.concat(",", starts, "%.2f"), StringUtils.concat(",", tmCov, "%.2f"));
				
				for (int d=0; d<10; d++) {
					int pos = RandomNumbers.getGlobal().getUnif(1, accc[0].length-1);
					for (int c=0; c<dataset.numCond; c++) 
						decoy[c] = accc[c][pos];
					startPairsDetails.writef("%s\t%d\t%s\t%s\n",trans.toLocationString(), pos, StringUtils.concat(",", decoy, "%.2f"), StringUtils.concat(",", tmCov, "%.2f"));
				}
				
				for (int pa=0; pa<startCodonPairs.length; pa++) {
					int[] p = startCodonPairs[pa];
					double sc = Math.log(starts[p[0]]+threshold)-Math.log(starts[p[1]]+threshold);
					if (starts[p[0]]>threshold && starts[p[1]]>threshold)
						startPairsCollector[pa].add(sc/Math.log(2));
				}
			}
			
			
			double chpLeft = 0;
			double chpRight = 0;
			
			ArrayGenomicRegion upstream = new ArrayGenomicRegion(major.getStart()-changePointKernel.getMaxAffectedIndex(0)*3-3, major.getStart());
			ArrayGenomicRegion downstream = new ArrayGenomicRegion(major.getStart(),changePointKernel.getMaxAffectedIndex(0)*3+3+major.getStart());
			for (Codon cc : codons.getData().getIntervalsIntersecting(upstream.getStart(), downstream.getStop(), new ArrayList<>())) {
				if (upstream.containsUnspliced(cc) && upstream.induce(cc).getStart()%3==0) {
					chpLeft+=cc.getTotalActivity()*changePointKernel.applyAsDouble((upstream.getTotalLength()-upstream.induce(cc).getEnd())/3);
				}
				
				else if (downstream.containsUnspliced(cc) && downstream.induce(cc).getStart()%3==0) {
					chpRight+=cc.getTotalActivity()*changePointKernel.applyAsDouble(downstream.induce(cc).getStart()/3);
				}
			}
			
			synchronized (changePoint) {
				changePoint.add(Math.log((chpRight+threshold)/(chpLeft+threshold))/Math.log(2));
				changePointDetails.writef("%s\t%.2f\t%.2f\n", trans.toLocationString(), chpLeft, chpRight);
			}
			
			
		}
		
		
		public void finish() throws IOException {
			startPairsDetails.close();
			changePointDetails.close();
			startStopDetails.close();
			
			LineWriter scores = new LineOrientedFile(prefix+".startstop.estimateData").write().writeLine("Name\tmean\tsd");
			CodonFeature[] features = new CodonFeature[dataset.numCond*2+startCodonPairs.length];
			int ind = 0;
			for (int c=0; c<dataset.numCond; c++) {
				features[ind++] = new CodonFeature(c, c, startCollector[c].getMean(), startCollector[c].getStandardDeviation(),true);
				scores.writef("%s Start\t%.3f\t%.3f\n", dataset.conditions[c], startCollector[c].getMean(), startCollector[c].getStandardDeviation());
				features[ind++] = new CodonFeature(c, c, stopCollector[c].getMean(), stopCollector[c].getStandardDeviation(),false);
				scores.writef("%s Stop\t%.3f\t%.3f\n", dataset.conditions[c], stopCollector[c].getMean(), stopCollector[c].getStandardDeviation());
			}
			for (int c=0; c<startPairsCollector.length; c++) {
				double tmmean = Math.log(tmsum[startCodonPairs[c][0]]/tmsum[startCodonPairs[c][1]])/Math.log(2);
						
				features[ind++] = new CodonFeature(startCodonPairs[c][0], startCodonPairs[c][1], startPairsCollector[c].getMean()-tmmean, startPairsCollector[c].getStandardDeviation(),true);
				scores.writef("%s\t%.3f\t%.3f\n", startcodonpairs[c], startPairsCollector[c].getMean()-tmmean, startPairsCollector[c].getStandardDeviation());
			}
			scores.close();
			log.info("Writing change point estimates");
			scores = new LineOrientedFile(prefix+".changepoint.estimateData").write().writeLine("mean\tsd");
			scores.writef("%.3f\t%.3f\n", changePoint.getMean(), changePoint.getStandardDeviation());
			scores.close();
			
			
			
			
			dataset.models[0].setStartCodon(features, tmsum);
			dataset.models[0].setChangePoint(changePoint.getMean(), changePoint.getStandardDeviation(), changePointBandwidth);
			
			LineWriter eval = new LineOrientedFile(prefix+".startpairs.eval.data").write().writeLine("Location\tPos\tPair\tLod");
			DoubleArrayList labels = new DoubleArrayList();
			for (String[] a : new LineOrientedFile(prefix+".startpairs.details.data").lineIterator().skip(1).map(a->StringUtils.split(a,'\t')).loop()) {
				double[] act = StringUtils.parseDouble(StringUtils.split(a[2], ','));
				double[] tmcov = StringUtils.parseDouble(StringUtils.split(a[3], ','));
				double[] comb = new double[3];
				
				for (int f=0; f<features.length; f++) {
					if (features[f].start) {
						double sc = features[f].compute(act, tmcov, tmsum, threshold);
						
						comb[0]+=sc;
						if (f<dataset.numCond*2)
							comb[1]+=sc;
						else 
							comb[2]+=sc;
						eval.writef("%s\t%s\t%s\t%.5f\n", a[0],a[1],f>=dataset.numCond*2?startcodonpairs[f-dataset.numCond*2]:dataset.conditions[f/2], sc);
					}
				}
				
				eval.writef("%s\t%s\t%s\t%.5f\n", a[0],a[1],"Combined all", comb[0]);
				eval.writef("%s\t%s\t%s\t%.5f\n", a[0],a[1],"Combined within", comb[1]);
				eval.writef("%s\t%s\t%s\t%.5f\n", a[0],a[1],"Combined diff", comb[2]);
			}
			
			
			eval.close();
			
			
		}
	}
	
	
	
	private static class SvmStartStopProcessor {
		
		Dataset dataset;
		LineWriter featureVectors;
		int upstream;
		int downstream;
		double fprStartPrediction;
		
		String prefix;
		double[] sum;
		String[] startPairs;
		int[][] startCodonPairs;
		private double startCutoff;
		private Svm_model model;
		private double probCutoff;
		
		private boolean useAllCodons;
		private boolean summarize;
		private boolean fixedNegatives;
		
		
		private ArrayList<String> loc = new ArrayList<String>();
		private int perConditionVectorLength;

		public SvmStartStopProcessor(String prefix, Dataset dataset, 
				int upstream, int downstream, double startCutoff, 
				String[] startPairs, double fprStartPrediction,
				boolean useAllCodons,boolean summarize,boolean fixedNegatives) throws UsageException, IOException {
			this.prefix = prefix;
			this.dataset = dataset;
			this.upstream = upstream;
			this.downstream = downstream;
			this.startCutoff = startCutoff;
			this.fprStartPrediction = fprStartPrediction;
			this.useAllCodons = useAllCodons;
			this.summarize = summarize;
			this.fixedNegatives = fixedNegatives;
			
			perConditionVectorLength = summarize?summa.length:upstream+downstream+1;
			
			sum = new double[dataset.numCond];
			
			log.info("SVM runs with (all codons="+useAllCodons+" summarize="+summarize+" fixedNegatives="+fixedNegatives+")");
			
			
			featureVectors = new LineOrientedFile(prefix+".startstop.svmfeatures.data").write(true).writef("Class");
			if (summarize) {
				for (int ca=0; ca<summa.length; ca++) 
					for (int c=0; c<dataset.numCond; c++) {
						featureVectors.writef("\t%d.%s", ca, dataset.conditions[c]);
					}
			} else {
				for (int p=-upstream; p<=downstream; p++) 
					for (int c=0; c<dataset.numCond; c++) 
						featureVectors.writef("\t%d.%s", p, dataset.conditions[c]);
			}
			featureVectors.writeLine();
			
			if (startPairs.length==0) {
				startPairs = new String[(dataset.conditions.length+1)/2];
				for (int i=0; i<startPairs.length; i++) {
					startPairs[i] = dataset.conditions[i*2]+"/"+dataset.conditions[Math.min(i*2+1,dataset.conditions.length-1)];
				}
			}
			this.startPairs = startPairs;
			startCodonPairs = new int[startPairs.length][2];
			for (int c=0; c<startPairs.length; c++) {
				String[] p = StringUtils.split(startPairs[c], '/');
				if (p.length!=2) throw new UsageException("Wrong syntax in start codon treatment pair: "+startPairs[c]);
				for (int ind=0; ind<2; ind++) {
					if (dataset.condIndex.containsKey(p[ind]))
						startCodonPairs[c][ind] = dataset.condIndex.get(p[ind]);
					else if (StringUtils.isInt(p[ind]))
						startCodonPairs[c][ind] = Integer.parseInt(p[ind]);
					else
						throw new UsageException("Condition "+p[ind]+" unknown for treatment pair!");
				}
			}
			
		}

		public void process(Inferred inferred) throws IOException {
			
			MajorIsoform mi = inferred.major;
			
			double[][] accc = mi.accc;
			double[][] uccc = mi.upstream;
			
			double[][] concat = new double[accc.length][];
			if (!useAllCodons) {
				for (int c=0; c<accc.length; c++) 
					concat[c] = ArrayUtils.concat(uccc[c],accc[c]);
			} else {
				for (int c=0; c<accc.length; c++) {
					concat[c] = new double[uccc[c].length+accc[c].length];
					concat[c][0] = mi.upstreamcinfVec[c][0]+mi.upstreamcinfVec[c][1];
					for (int p=1; p<uccc[c].length; p++) {
						concat[c][p] = mi.upstreamcinfVec[c][p*3-1]+mi.upstreamcinfVec[c][p*3]+mi.upstreamcinfVec[c][p*3+1];
					}
					concat[c][uccc[c].length] = mi.upstreamcinfVec[c][uccc[c].length*3-1]+mi.cinfVec[c][0]+mi.cinfVec[c][1];
					for (int p=1; p<accc[c].length; p++) {
						concat[c][uccc[c].length+p] = mi.cinfVec[c][p*3-1]+mi.cinfVec[c][p*3]+mi.cinfVec[c][p*3+1];
					}
				}
			}
			
			int start = uccc[0].length;
			
			if (accc[0].length>2*(downstream+this.upstream)) {
				synchronized (this) {
					if (fixedNegatives && concat[0].length<=start+150+12)
						return;
					
					loc.add(mi.trans.toLocationString());
					writeFeatureVector(true,start,concat);
					
					
					for (int d=0; d<10; d++) {
						int pos;
						if (fixedNegatives)
							pos = start+fixedNegativePositions[d];
						else if (summarize)
							pos = RandomNumbers.getGlobal().getUnif(start+downstream+this.upstream, concat[0].length-13);
						else
							pos = RandomNumbers.getGlobal().getUnif(start+12+this.upstream, concat[0].length-12);
						writeFeatureVector(false,pos,concat);
					}
					
					for (int c=0; c<sum.length; c++)
						sum[c]+=ArrayUtils.sum(accc[c]);
					
				}
			}
		}
		
		private static int[] fixedNegativePositions = {-6, -3, 3, 9, 18, 30, 60, 90, 120, 150};
		public static int[][] summa = {{-3, -2},{-1}, {0}, {1}, {2, 3}, {4, 5, 6}, {7, 8, 9}, {10, 11, 12}};
		private static int summa0pos = 2;
		
		private void writeFeatureVector(boolean cls, int start, double[][] concat) throws IOException {
			featureVectors.writef("%d", cls?1:0);
			if (summarize) {
				for (int ca=0; ca<summa.length; ca++) 
					for (int c=0; c<dataset.numCond; c++) {
						double s = 0;
						for (int p:summa[ca]) 
							s+=concat[c][start+p];
						featureVectors.writef("\t%.3f", s);
					}
			} else {
				for (int p=start-upstream; p<=start+downstream; p++) 
					for (int c=0; c<dataset.numCond; c++) 
						featureVectors.writef("\t%.3f", concat[c][p]);
			}
			featureVectors.writeLine();
		}
		

		public void finish() throws IOException {
			featureVectors.close();
			
			ArrayUtils.mult(sum, 1/ArrayUtils.max(sum));
			
			String[] libsvmDataName = new String[startPairs.length+1];
			LineOrientedFile[] libsvmDataFile = new LineOrientedFile[startPairs.length+1];
			LineWriter[] libsvmData = new LineWriter[startPairs.length+1];
			for (int i=0; i<libsvmData.length; i++){
				libsvmDataName[i] = (i<startPairs.length?startPairs[i].replace('/', '_'):"combined");
				libsvmDataFile[i] = new LineOrientedFile(prefix+".startstop.svmfeatures.normalized."+libsvmDataName[i]+".libsvm");
				libsvmData[i] = libsvmDataFile[i].write(true);
			}
			
			boolean[] posExceedsCutoff = new boolean[startCodonPairs.length];
			
			LineWriter filterValue = new LineOrientedFile(prefix+".startstop.svmfeatures.filtervalues.data").write(true).writeLine("Location\t"+StringUtils.concat("\t", startPairs));
			int exa = 0;
			
			int upstream = summarize?summa0pos:this.upstream;
			
			featureVectors = new LineOrientedFile(prefix+".startstop.svmfeatures.normalized.data").write(true);
			for (double[] vec : new LineOrientedFile(prefix+".startstop.svmfeatures.data")
													.lineIterator()
													.skip(1,h->featureVectors.writeLine2(h))
													.map(a->StringUtils.split(a,'\t'))
													.map(a->StringUtils.parseDouble(a))
													.loop()) {
				
				if (vec[0]>0) {
					// positive example
					
					filterValue.writef(loc.get(exa/11));
					for (int i=0; i<startCodonPairs.length; i++) {
						filterValue.writef("\t%.2f", vec[upstream*sum.length+startCodonPairs[i][0]+1]);
						posExceedsCutoff[i] = vec[upstream*sum.length+startCodonPairs[i][0]+1]>=startCutoff;
					}
					filterValue.writeLine();
				}
				exa++;
						
				double s = 0;
				for (int vi=1; vi<vec.length; vi++) {
					vec[vi] /= sum[(vi-1)%sum.length];
					s+=vec[vi];
				}
				for (int vi=1; vi<vec.length; vi++)
					vec[vi]/=s;
				
				featureVectors.writef("%.0f",vec[0]); //class
				for (int vi=1; vi<vec.length; vi++) 
					featureVectors.writef("\t%.8f",vec[vi]);
				featureVectors.writeLine();
				
				for (int i=0; i<libsvmData.length; i++) {
					if (i<libsvmData.length-1) {
						if (posExceedsCutoff[i] || dataset.predefinedMajor!=null) {
							libsvmData[i].writef("%.0f",vec[0]); //class
							for (int l=0; l<perConditionVectorLength; l++)
								for (int p=0; p<startCodonPairs[i].length; p++) {
									int ocomp = (l)*sum.length+startCodonPairs[i][p]+1;
									int ncomp = (l)*2+p+1;
									if (vec[ocomp]>0)
										libsvmData[i].writef("\t%d:%.8f",ncomp,vec[ocomp]);
								}
							libsvmData[i].writeLine();
						}
					}
					else {
						if (ArrayUtils.count(posExceedsCutoff,true)>0 || dataset.predefinedMajor!=null) {
							StringBuffer line = new StringBuffer();
							libsvmData[i].writef("%.0f",vec[0]); //class
							for (int l=0; l<perConditionVectorLength; l++)
								for (int st=0; st<startCodonPairs.length; st++)
									for (int p=0; p<startCodonPairs[st].length; p++) {
										int ocomp = (l)*sum.length+startCodonPairs[st][p]+1;
										int ncomp = (l)*2*startCodonPairs.length+st*2+p+1;
										if (vec[ocomp]>0) {
											libsvmData[i].writef("\t%d:%.8f",ncomp,vec[ocomp]);
											line.append(String.format("\t%d:%.8f",ncomp,vec[ocomp]));
										}
									}
							libsvmData[i].writeLine();
							
						}
					}
					
				}
				
				
			}
			featureVectors.close();
			for (int i=0; i<libsvmData.length; i++) 
				libsvmData[i].close();
			
			
			Svm_parameter param = new Svm_parameter();
			param.degree = 3;
			param.coef0 = 0;
			param.nu = 0.5;
			param.cache_size = 100;
			param.eps = 1e-3;
			param.p = 0.1;
			param.shrinking = 1;
			param.nr_weight = 0;
			param.weight_label = new int[0];
			param.weight = new double[0];
			param.svm_type = Svm_parameter.C_SVC;
			param.kernel_type = Svm_parameter.RBF;
			param.C = 100;
			param.gamma = 0.5;
			param.probability = 1;
			
			LineWriter cvout = new LineOrientedFile(prefix+".startstop.svmfeatures.cv.eval").write(true).writeLine("Location\tPos\tPair\tLod");
			Svm.svm_set_print_string_function(s->{});
			
			
			for (int i=0; i<libsvmDataFile.length; i++) {
				Svm_problem prob = readProblem(libsvmDataFile[i].lineIterator());
				double[] result = new double[prob.l];
				
				Svm.svm_cross_validation(prob, param, 5, result,1);
				
				
			
				for (int ex=0; ex<result.length; ex++)
					cvout.writef("%s\t%d\tSVM %s\t%.5f\n",loc.get(ex/11),prob.y[ex]>0?0:1,libsvmDataName[i],result[ex]);
				
				if (i==libsvmDataFile.length-1) {
					model = Svm.svm_train(prob, param);
					CompleteRocAnalysis roc = new CompleteRocAnalysis();
					for (int ex=0; ex<result.length; ex++) 
						roc.addScore(result[ex], prob.y[ex]>0);
					probCutoff = roc.getCutoffForFpr(fprStartPrediction);
					System.out.println("SVM cutoff :"+probCutoff);
					int ind = roc.getIndexForCutoff(probCutoff);
					System.out.println("TP: "+roc.getTp(ind));
					System.out.println("TN: "+roc.getTn(ind));
					System.out.println("FP: "+roc.getFp(ind));
					System.out.println("FN: "+roc.getFn(ind));
					
					System.out.println("FDR: "+roc.getFdr(ind));
					System.out.println("Sens: "+roc.getTpr(ind));
				}
				
			}
			cvout.close();
		}
		

		public int[] getConditions() {
			int[] re = new int[startCodonPairs.length*2];
			for (int i=0; i<re.length; i++)
				re[i] = startCodonPairs[i/2][i%2];
			return re;
		}
	}
	
	private static class SvmLfcProcessor {
		
		Dataset dataset;
		String prefix;
		
		Svm_model model;
		
		private double lfcpseudo = 6;
		private int lfcReferenceStart = 10;
		private int lfcReferenceEnd = 300;
		
		private LineWriter lfc;
		double cutoff;
		private double desiredtpr = 0.01;

		LfcParameters param;
		
		public SvmLfcProcessor(String prefix, Dataset dataset) throws UsageException, IOException {
			this.prefix = prefix;
			this.dataset = dataset;
			
			this.lfc = new LineOrientedFile(prefix+".start.lfc.data").write().writeLine("Location\tPos\tPair\tLod");
		}

		public void process(Inferred inferred) throws IOException {

			if (param==null) param = new LfcParameters(null, dataset.startCodonPairs, cutoff, lfcpseudo, lfcReferenceStart, lfcReferenceEnd);
			
			MajorIsoform mi = inferred.major;
			
			double[][] accc = mi.accc;
			double[][] uccc = mi.upstream;
			
			double[][] concat = new double[accc.length][];
			for (int c=0; c<accc.length; c++) 
				concat[c] = ArrayUtils.concat(uccc[c],accc[c]);
			
			int start = uccc[0].length;
			
			double[][] lfcData = param.prepareLfc(concat);
			synchronized (this) {
				writeLfc(mi.trans.toLocationString(),true,0,start,lfcData);
				
				
				for (int d=0; d<10; d++) {
					int pos = RandomNumbers.getGlobal().getUnif(start+lfcReferenceStart, concat[0].length-lfcReferenceStart);
					writeLfc(mi.trans.toLocationString(),false,pos-start,pos,lfcData);
				}
			}
		}
		
		
		
		
		
		private void writeLfc(String loc, boolean cls, int cds, int pos, double[][] lfcData) throws IOException {
			for (int i=0; i<dataset.startPairs.length; i++) {
				lfc.writef("%s\t%d\t%s\t%.5f\n",loc,cds,dataset.startPairs[i],lfcData[i][pos]);
			}
		}

		public void finish() throws IOException {
			lfc.close();
			
			if (dataset.startPairs.length>0) {
				
				Svm_parameter param = new Svm_parameter();
				param.degree = 3;
				param.coef0 = 0;
				param.nu = 0.5;
				param.cache_size = 100;
				param.eps = 1e-3;
				param.p = 0.1;
				param.shrinking = 1;
				param.nr_weight = 0;
				param.weight_label = new int[0];
				param.weight = new double[0];
				param.svm_type = Svm_parameter.C_SVC;
				param.kernel_type = Svm_parameter.RBF;
				param.C = 100;
				param.gamma = 0.5;
				param.probability = 1;
				
				LineWriter cvout = new LineOrientedFile(prefix+".start.lfc.cv.eval").write(true).writeLine("Location\tPos\tPair\tLod");
				Svm.svm_set_print_string_function(s->{});
				
				ArrayList<String> loc = new ArrayList<String>();
				DoubleArrayList y = new  DoubleArrayList();
				ArrayList<Svm_node[]> x = new ArrayList<Svm_node[]>();
				
				new LineOrientedFile(prefix+".start.lfc.data").lineIterator()
						.skip(1)
						.map(l->StringUtils.split(l,'\t'))
						.multiplexUnsorted((a,b)->a[0].equals(b[0]),String[].class)
						.forEachRemaining(a->{
							assert a.length==dataset.startPairs.length*11;
							for (int i=0; i<a.length; i+=dataset.startPairs.length) {
								Svm_node[] xi = new Svm_node[dataset.startPairs.length];
								for (int k=0; k<xi.length; k++) {
									xi[k] = new Svm_node();
									xi[k].index = k;
									xi[k].value = Double.parseDouble(a[i+k][3]);
								}
								x.add(xi);
								y.add(i==0?1:0);
							}
							loc.add(a[0][0]);
						});
				
				Svm_problem prob = new Svm_problem();
				prob.x = x.toArray(new Svm_node[0][]);
				prob.y = y.toDoubleArray();
				prob.l = y.size();
				
				LineWriter libsvmout = new LineOrientedFile(prefix+".start.lfc.libsvm").write(true);
				writeProblem(libsvmout, prob);
				libsvmout.close();
				
				double[] result = new double[prob.l];
				
				Svm.svm_cross_validation(prob, param, 5, result,1);
				
				
			
				for (int ex=0; ex<result.length; ex++)
					cvout.writef("%s\t%d\tSVM %s\t%.5f\n",loc.get(ex/11),prob.y[ex]>0?0:1,"LFC complete",result[ex]);
				
				model = Svm.svm_train(prob, param);
				CompleteRocAnalysis roc = new CompleteRocAnalysis();
				for (int ex=0; ex<result.length; ex++) 
					roc.addScore(result[ex], prob.y[ex]>0);
				System.out.println("AUC: "+roc.getAucFprTpr());
				cutoff = roc.getCutoffForTpr(desiredtpr);
				
				cvout.close();
			}
			
		}
		
	}
	
	
	public static Svm_problem readProblem(LineIterator lit) {
		DoubleArrayList y = new  DoubleArrayList();
		ArrayList<Svm_node[]> x = new ArrayList<Svm_node[]>();
		for (String[] f : lit.map(s->StringUtils.split(s,'\t')).loop()) {
			y.add(Double.parseDouble(f[0]));
			Svm_node[] nodes = new Svm_node[f.length-1];
			for (int n=0; n<nodes.length; n++) {
				nodes[n] = new Svm_node();
				nodes[n].index = Integer.parseInt(StringUtils.splitField(f[n+1], ':', 0));
				nodes[n].value = Double.parseDouble(StringUtils.splitField(f[n+1], ':', 1));
			}
			
			x.add(nodes);
		}
		Svm_problem prob = new Svm_problem();
		prob.x = x.toArray(new Svm_node[0][]);
		prob.y = y.toDoubleArray();
		prob.l = y.size();
		return prob;
	}
	
	public static void writeProblem(LineWriter out, Svm_problem prob) throws IOException {
		for (int i=0; i<prob.l; i++) {
			out.writef(""+prob.y[i]);
			for (Svm_node n : prob.x[i])
				out.writef("\t%d:%s",n.index,n.value+"");
			out.writeLine();
		}
	}
	
	private static class RibotaperProcessor {
		
		LineWriter ribotaperout;
		private R r;
		

		public RibotaperProcessor(String prefix) throws UsageException, IOException, RserveException {
			
			ribotaperout = new LineOrientedFile(prefix+".ribotaper.data").write(true).writef("Location\tTotal\tinframe\tp.value\n");
			r = RConnect.R();
			if (!r.requirePackage("multitaper")) 
				throw new UsageException("Cannot compute ribotaper pvalues, multitaper package is missing!");
			r.run(getClass().getResource("/scripts/ribotaper.R"));
			log.info("Established R connection for ribotaper scoring");
		}

		public void process(Inferred inferred) throws IOException, REngineException {
			
			MajorIsoform mi = inferred.major;
			
			double[] data = mi.infVec;
			double inframe = 0;
			for (int i=0; i<data.length; i+=3)
				inframe+=data[i];
			double total = EI.wrap(data).sum();
		
			synchronized (this) {
				try {
					r.assign("x", data);
					double pval = r.eval("ribotaper(x)$pval").asDouble();
					ribotaperout.writef("%s\t%.1f\t%.1f\t%.5g\n", mi.trans.toLocationString(),total,inframe,pval);
				} catch (REXPMismatchException e) {
					throw new RuntimeException("Could not read result of ribotaper!",e);
				}
			}
			
		}
		

		public void finish() throws IOException {
			ribotaperout.close();
			r.close();
		}
	}
	
}
