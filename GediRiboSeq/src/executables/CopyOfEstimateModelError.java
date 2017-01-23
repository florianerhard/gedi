//package executables;
//
//import gedi.app.Gedi;
//import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
//import gedi.core.data.annotation.Transcript;
//import gedi.core.data.reads.AlignedReadsData;
//import gedi.core.data.reads.ReadCountMode;
//import gedi.core.reference.ReferenceSequence;
//import gedi.core.region.ArrayGenomicRegion;
//import gedi.core.region.GenomicRegion;
//import gedi.core.region.ImmutableReferenceGenomicRegion;
//import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
//import gedi.core.sequence.FastaIndexSequenceProvider;
//import gedi.riboseq.cleavage.RiboModel;
//import gedi.riboseq.inference.codon.Codon;
//import gedi.riboseq.inference.codon.CodonInference;
//import gedi.riboseq.utils.RiboUtils;
//import gedi.util.ArrayUtils;
//import gedi.util.FunctorUtils.ToIntMappedIterator;
//import gedi.util.SequenceUtils;
//import gedi.util.StringUtils;
//import gedi.util.datastructure.array.NumericArray;
//import gedi.util.datastructure.array.NumericArray.NumericArrayType;
//import gedi.util.datastructure.array.functions.NumericArrayFunction;
//import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
//import gedi.util.datastructure.tree.Trie;
//import gedi.util.datastructure.tree.Trie.AhoCorasickResult;
//import gedi.util.datastructure.tree.redblacktree.IntervalTree;
//import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
//import gedi.util.functions.BiIntConsumer;
//import gedi.util.functions.EI;
//import gedi.util.io.randomaccess.PageFile;
//import gedi.util.io.randomaccess.PageFileWriter;
//import gedi.util.io.text.LineOrientedFile;
//import gedi.util.io.text.LineWriter;
//import gedi.util.math.function.PiecewiseLinearFunction;
//import gedi.util.math.function.StepFunction;
//import gedi.util.math.stat.counting.RollingStatistics;
//import gedi.util.mutable.MutableMonad;
//import gedi.util.mutable.MutablePair;
//import gedi.util.r.RConnect;
//import gedi.util.sequence.Alphabet;
//import gedi.util.userInteraction.progress.ConsoleProgress;
//import gedi.util.userInteraction.progress.NoProgress;
//import gedi.util.userInteraction.progress.Progress;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map.Entry;
//import java.util.logging.Logger;
//
//import jdistlib.math.spline.SmoothSpline;
//import jdistlib.math.spline.SmoothSplineResult;
//
//public class CopyOfEstimateModelError {
//
//	private static final Logger log = Logger.getLogger( CopyOfEstimateModelError.class.getName() );
//	public static void main(String[] args) {
//		try {
//			start(args);
//		} catch (UsageException e) {
//			usage("An error occurred: "+e.getMessage());
//			if (ArrayUtils.find(args, "-D")>=0)
//				e.printStackTrace();
//		} catch (Exception e) {
//			System.err.println("An error occurred: "+e.getMessage());
//			if (ArrayUtils.find(args, "-D")>=0)
//				e.printStackTrace();
//		}
//	}
//	
//	private static void usage(String message) {
//		System.err.println();
//		if (message!=null){
//			System.err.println(message);
//			System.err.println();
//		}
//		System.err.println("EstimateModelError <Options>");
//		System.err.println();
//		System.err.println("Options:");
//		System.err.println(" -r <cit-file>\t\t\tCit file containing mappings");
//		System.err.println(" -o <prefix>\t\t\tPrefix for output files");
//		System.err.println(" -m <model-file>\t\t\tModel file from EstimateRiboModel");
//		System.err.println(" -a <cit-file>\t\t\tTranscript annotation file");
//		System.err.println(" -g <fasta-index-file>\t\t\tGenome sequence index");
//		System.err.println();
//		System.err.println(" -D\t\t\tOutput debugging information");
//		System.err.println(" -p\t\t\tShow progress");
//		System.err.println(" -h\t\t\tShow this message");
//		System.err.println();
//		
//	}
//	
//	
//	private static class UsageException extends Exception {
//		public UsageException(String msg) {
//			super(msg);
//		}
//	}
//	
//	
//	private static String checkParam(String[] args, int index) throws UsageException {
//		if (index>=args.length || args[index].startsWith("-")) throw new UsageException("Missing argument for "+args[index-1]);
//		return args[index];
//	}
//	
//	private static int checkIntParam(String[] args, int index) throws UsageException {
//		String re = checkParam(args, index);
//		if (!StringUtils.isInt(re)) throw new UsageException("Must be an integer: "+args[index-1]);
//		return Integer.parseInt(args[index]);
//	}
//
//	private static double checkDoubleParam(String[] args, int index) throws UsageException {
//		String re = checkParam(args, index);
//		if (!StringUtils.isNumeric(re)) throw new UsageException("Must be a double: "+args[index-1]);
//		return Double.parseDouble(args[index]);
//	}
//	
//	public static void start(String[] args) throws Exception {
//		
//		CenteredDiskIntervalTreeStorage<AlignedReadsData> reads = null;
//		MemoryIntervalTreeStorage<Transcript> annotation = null;
//		FastaIndexSequenceProvider sequence = null;
//		RiboModel model = null;
//		String prefix = null;
//		double trim = 0.1;
//		double minGapActivity = 25;
//		int gapHalfBin = 100;
//		int gapStep = 10;
//		double threshold = 1E-2;
//		int internalHalfBin = 1000; 
//		int internalStep = 100;
//		
//		Progress progress = new NoProgress();
//		
//		int i;
//		for (i=0; i<args.length; i++) {
//			
//			if (args[i].equals("-h")) {
//				usage(null);
//				return;
//			}
//			else if (args[i].equals("-p")) {
//				progress=new ConsoleProgress(System.err);
//			}
//			else if (args[i].equals("-r")) {
//				reads = new CenteredDiskIntervalTreeStorage<AlignedReadsData>(checkParam(args,++i));
//			}
//			else if (args[i].equals("-m")) {
//				model = RiboModel.fromFile(checkParam(args,++i));
//			}
//			else if (args[i].equals("-g")) {
//				sequence = new FastaIndexSequenceProvider(checkParam(args, ++i));
//			}
//			else if (args[i].equals("-trim")) {
//				trim = checkDoubleParam(args, ++i);
//			}
//			else if (args[i].equals("-gapMinAct")) {
//				minGapActivity = checkDoubleParam(args, ++i);
//			}
//			else if (args[i].equals("-o")) {
//				prefix = checkParam(args,++i);
//			}
//			else if (args[i].equals("-a")) {
//				annotation = new CenteredDiskIntervalTreeStorage<Transcript>(checkParam(args,++i)).toMemory();
//			}
//			else if (args[i].equals("-D")) {
//			}
////			else if (!args[i].startsWith("-")) 
////					break;
//			else throw new UsageException("Unknown parameter: "+args[i]);
//			
//		}
//
//		
//		if (reads==null) throw new UsageException("No reads given!");
//		if (annotation==null) throw new UsageException("No annotation file given!");
//		if (prefix==null) throw new UsageException("No output prefix given!");
//		if (sequence==null) throw new UsageException("No genome index given!");
//		if (model==null) throw new UsageException("No model given!");
//
//		Gedi.startup(false);
//		
//		
//		RollingStatistics[] internalQuantiles = new RollingStatistics[] {new RollingStatistics(),new RollingStatistics(),new RollingStatistics()};
//		RollingStatistics gapsBeta = new RollingStatistics();
//		RollingStatistics startRolling = new RollingStatistics();
//		RollingStatistics stopRolling = new RollingStatistics();
//		
//		
//		if (!new File(prefix+".bin").exists()) {
//			
//			log.info("Identifying annotated non-translated regions in ORFs");
//			MemoryIntervalTreeStorage<IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>> nontrans = inferNontrans(annotation,sequence,progress);
//			
//			
//			log.info("Collecting codon activities for non-translated regions in ORFs as well as for their ORF codons and determine gaps in the coverage of the major transcripts");
//			CodonInference inference = new CodonInference(model);
//	
//			LineWriter internal = new LineOrientedFile(prefix+".internal.data").write().writeLine("Location\tFrame\tSum\tNonTrans");
//			PageFileWriter internalOrfs = new PageFileWriter(prefix+".decoy.orfs");
//			
//			LineWriter gaps = new LineOrientedFile(prefix+".bfits.data").write().writeLine("Gene\tCds\tLength\tSum\tMean coverage\tGaps\tStart\tStop");
//			
//			double signal = 0;
//			double noise = 0;
//			HashMap<String,double[]> perReadCounter = new HashMap<String, double[]>();
//			
//			// now loop through all ~genes, infer codons based on the reads and the model and record codons from nontrans in comparison to the overlapping cds codons
//			for (ImmutableReferenceGenomicRegion<IntervalTree<GenomicRegion, ImmutableReferenceGenomicRegion<String>>> gene : 
//							nontrans.ei()
//							.progress(progress, (int)nontrans.size(), r->r.toLocationString()).loop()
//							) {
//				
//				
//				// internal error model
//				ImmutableReferenceGenomicRegion<Void> codonCoord = new ImmutableReferenceGenomicRegion<Void>(gene.getReference(), gene.getRegion().removeIntrons());
//				ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons = inference.inferCodons(reads, codonCoord.getReference(), codonCoord.getRegion().getStart(), codonCoord.getRegion().getEnd());
//	
//				Iterator<Entry<GenomicRegion, ImmutableReferenceGenomicRegion<String>>> nontransIt = gene.getData().entrySet().iterator();
//				while (nontransIt.hasNext()) {
//					Entry<GenomicRegion, ImmutableReferenceGenomicRegion<String>> nt = nontransIt.next();
//					GenomicRegion ntreg = codonCoord.induce(nt.getKey());
//	
//					GenomicRegion codReg = ntreg.extendFront(1).extendBack(1); // additional entriy before and one after the ntreg
//					double[] cod = new double[codReg.getTotalLength()-2]; // one entry for each of the codons in codReg
//					double[] gof = new double[codReg.getTotalLength()-2]; // one entry for each of the codons in codReg
//					
//					for (Codon cc : codons.getData().getIntervalsIntersecting(codReg.getStart(), codReg.getStop(), new ArrayList<>())) 
//						if (codReg.containsUnspliced(cc)) {
//							ArrayGenomicRegion cci = codReg.induce(cc);
//							cod[cci.getStart()] = cc.getTotalActivity();
//							gof[cci.getStart()] = cc.getGoodness();
//						}
//					
//					NumericArray.wrap(cod).serialize(internalOrfs);
//					
//					for (int p=1; p<cod.length; p+=3) {
//						int am = max(cod,p);
//						double max = cod[am];
//						if (max>=1) {
//							double sum = cod[p-1]+cod[p]+cod[p+1];
//							int genomic = codonCoord.map(codReg.map(p));
//							int frame = nt.getValue().induce(genomic)%3;
//							internal.writef("%s:%d\t%d\t%.6f\t%.6g\n",gene.getReference(),genomic,frame,sum,cod[p]);
//	//						if (cod[p]<0.99*max)
//							internalQuantiles[frame].add(sum, cod[p]/sum);
//						}
//					}
//				}
//				
//				// gaps model
//				// find major transcript of this ~gene
//				FastaIndexSequenceProvider usequence = sequence;
//				GenomicRegion major = null;
//				double act = 0;
//				for (GenomicRegion cds : annotation.ei(gene)
//						.filter(r->r.getData().isCoding()) // only coding
//						.map(r->r.getData().getCds(r.getReference(),r.getRegion())) // map to cds
//						.filter(r->SequenceUtils.isOrf(usequence.getSequence(gene.getReference(), r).toString(),false)) // keep only the ORFs
//						.map(r->gene.induce(r)) // to the gene coordinate system
//						.map(r->r.extendBack(-3)) // cut the stop codon
//						.loop()
//						) {
//					double cact = 0;
//					for (Codon cc : codons.getData().getIntervalsIntersecting(cds.getStart(), cds.getStop(), new ArrayList<>())) {
//						if (cds.containsUnspliced(cc) && cds.induce(cc).getStart()%3==0) {
//							cact+=cc.getTotalActivity();
//						}
//					}
//					if (cact>act) {
//						major = cds;
//						act = cact;
//					}
//				}
//				
//				if (major!=null && act>=minGapActivity) {
//					double[] ccc = new double[major.getTotalLength()/3];
//					for (Codon cc : codons.getData().getIntervalsIntersecting(major.getStart(), major.getStop(), new ArrayList<>())) {
//						if (major.containsUnspliced(cc) && major.induce(cc).getStart()%3==0) {
//							ccc[major.induce(cc).getStart()/3]=cc.getTotalActivity();
//						}
//					}
//					if (ArrayUtils.sum(ccc)/ccc.length>1) {
//						for (Codon cc : codons.getData().getIntervalsIntersecting(major.getStart(), major.getStop(), new ArrayList<>())) {
//							if (major.containsUnspliced(cc)){
//								if (major.induce(cc).getStart()%3==0) {
//									signal+=cc.getTotalActivity();
//								} else {
//									noise+=cc.getTotalActivity();
//								}
//							}
//						}
//						
//						for (ImmutableReferenceGenomicRegion<AlignedReadsData> read : reads.ei(new ImmutableReferenceGenomicRegion<Void>(gene.getReference(), gene.map(major))).filter(r->gene.getRegion().containsUnspliced(r.getRegion())).loop() ) {
//							GenomicRegion reg = gene.induce(read.getRegion());
//							if (major.contains(reg.getStart()))
//								for (int d=0; d<read.getData().getDistinctSequences(); d++) {
//									double c = read.getData().getTotalCountForDistinct(d, ReadCountMode.Weight);
//									String key = (RiboUtils.hasLeadingMismatch(read.getData(), d)?"-":"")+read.getRegion().getTotalLength();
//									int frame = major.induceMaybeOutside(reg.getStart())%3;
//									if (frame<0) frame = (3-frame)%3;
//									perReadCounter.computeIfAbsent(key, s->new double[3])[frame]+=c;
//								}
//						}
//					}
//					
//					double[] unsorted = ccc.clone();
//					Arrays.sort(ccc);
//					int ngaps = 0;
//					for (;ngaps<ccc.length && ccc[ngaps]<=threshold; ngaps++);
//					
//					double tmCov = NumericArrayFunction.trimmedMean(trim).applyAsDouble(NumericArray.wrap(ccc,ngaps,ccc.length));
//					
//	//				double medCov = new Median().evaluate(ccc, ngaps, ccc.length-ngaps);
//					gaps.writef("%s\t%s\t%d\t%.2f\t%.2f\t%d\t%.2f\t%.2f\n", gene.toLocationString(), major.toRegionString(), 
//							ccc.length, ArrayUtils.sum(ccc),tmCov, 
//							ngaps, unsorted[0], unsorted[unsorted.length-1]);
//					gapsBeta.add(tmCov, ngaps/(double)ccc.length);
//					startRolling.add(tmCov, Math.log((unsorted[0]+threshold)/tmCov)/Math.log(2));
//					stopRolling.add(tmCov, Math.log((unsorted[unsorted.length-1]+threshold)/tmCov)/Math.log(2));
//				}
//				
//			
//				
//				
//				
//			}
//			internal.close();
//			gaps.close();
//			internalOrfs.close();
//			
//			LineWriter signalToNoise = new LineOrientedFile(prefix+".signaltonoise.data").write().writeLine("Type\tSignal\tNoise");
//			signalToNoise.writef("Codon\t%.2f\t%.2f\n", signal, noise);
//			for (String k : perReadCounter.keySet()) {
//				double[] c = perReadCounter.get(k);
//				int m = ArrayUtils.argmax(c);
//				double s = ArrayUtils.sum(c);
//				signalToNoise.writef("%s\t%.2f\t%.2f\n", k, c[m], s-c[m]);
//			}
//			signalToNoise.close();
//			
//			PageFileWriter f = new PageFileWriter(prefix+".bin");
//			internalQuantiles[1].serialize(f);
//			internalQuantiles[2].serialize(f);
//			gapsBeta.serialize(f);
//			startRolling.serialize(f);
//			stopRolling.serialize(f);
//			
//			f.close();
//		} else {
//			
//			log.info("Taking data from "+prefix+".bin");
//			
//			PageFile f = new PageFile(prefix+".bin");
//			internalQuantiles[1].deserialize(f);
//			internalQuantiles[2].deserialize(f);
//			gapsBeta.deserialize(f);
//			startRolling.deserialize(f);
//			stopRolling.deserialize(f);
//			f.close();
//		}
//		
//		internalQuantiles[1].setProgress(progress);
//		internalQuantiles[2].setProgress(progress);
//		gapsBeta.setProgress(progress);
//		startRolling.setProgress(progress);
//		stopRolling.setProgress(progress);
//		
//		// compute internal model
//		int halfbinsize = internalHalfBin;
//		int step = internalStep;
//		double[] quantiles = {0.90,0.91,0.92,0.93,0.94,0.95,0.96,0.97,0.98,0.99};
//		NumericArrayFunction[] functions = new NumericArrayFunction[quantiles.length];
//		for (i=0; i<functions.length; i++) 
//			functions[i] = NumericArrayFunction.quantile(quantiles[i]);
//		
//		PiecewiseLinearFunction[][] internalFit = new PiecewiseLinearFunction[3][quantiles.length];
//		
//		LineWriter txt = new LineOrientedFile(prefix+".internal.model.txt").write().writeLine("Frame\tSum\tQuantile\tValue\tSmooth");
//		for (i=1; i<3; i++) {
//			log.info("Computing internal data for frame "+i);
//			StepFunction[] steps = internalQuantiles[i].computeEquiSize(halfbinsize,step,functions);
//			for (int s=0; s<steps.length; s++) {
//				
//				double[] lx = steps[s].getX().clone();
//				for (int l=0; l<lx.length; l++)
//					lx[l] = Math.log10(Math.round(lx[l]*1E6)*1E-6);
//				
//				log.info("Fitting spline for quantile "+quantiles[s]);
//				SmoothSplineResult spl = SmoothSpline.fitDFMatch(lx, steps[s].getY(), 10);
//				
//				internalFit[i][s] = new PiecewiseLinearFunction(steps[s].getX(), EI.wrap(steps[s].getX()).mapToDouble(x->Math.min(1, SmoothSpline.predict(spl, Math.log10(x), 0))).toDoubleArray());
//				
//				for (int m=0; m<steps[s].getKnotCount(); m++) 
//					txt.writef("%d\t%.6f\t%.2f\t%.6g\t%.6g\n", i, steps[s].getX(m), quantiles[s], steps[s].getY(m),
////							SmoothSpline.predict(spl, Math.log10(steps[s].getX(m)), 0),
//							internalFit[i][s].applyAsDouble(steps[s].getX(m)));
//				
//				
//			}
//		}
//		txt.close();
//		model.setInternal(quantiles, internalFit[1], internalFit[2]);
//		
//		
//		// compute gap model
////		halfbinsize = gapHalfBin;
////		step = gapStep;
////		
////		DoubleArrayList cov = new DoubleArrayList();
////		DoubleArrayList mean = new DoubleArrayList();
////		DoubleArrayList var = new DoubleArrayList();
////		
////		log.info("Computing gap data");
////		coverageBeta.iterateEquiSize(halfbinsize, step).forEachRemaining(rb->{
////			cov.add(Math.round(rb.getCovariate()*1E6)*1E-6);
////			estimateBeta(rb.getValues(),mean,var);	
////		});
////		txt = new LineOrientedFile(prefix+".gaps.model.txt").write().writeLine("Coverage\tmean\tvar\tmean.Smooth\tvar.Smooth");
////			
////		log.info("Fitting spline for gap means");
////		SmoothSplineResult spla = SmoothSpline.fitDFMatch(cov.toDoubleArray(), mean.toDoubleArray(), 20);
////		log.info("Fitting spline for gap variances");
////		SmoothSplineResult splb = SmoothSpline.fitDFMatch(cov.toDoubleArray(), var.toDoubleArray(), 10);
////		
////		for (int m=0; m<cov.size(); m++) 
////			txt.writef("%.6f\t%.6g\t%.6g\t%.6g\t%.6g\n", cov.getDouble(m), mean.getDouble(m), var.getDouble(m), //0.,0.);
////					SmoothSpline.predict(spla, cov.getDouble(m), 0),SmoothSpline.predict(splb, cov.getDouble(m), 0));
////		txt.close();
////		
////		
////		PiecewiseLinearFunction plfa = new PiecewiseLinearFunction(cov.toDoubleArray(), EI.wrap(cov.toDoubleArray()).mapToDouble(x->SmoothSpline.predict(spla, x, 0)).toDoubleArray());
////		PiecewiseLinearFunction plfb = new PiecewiseLinearFunction(cov.toDoubleArray(), EI.wrap(cov.toDoubleArray()).mapToDouble(x->SmoothSpline.predict(splb, x, 0)).toDoubleArray());
//		
//		model.setRolling(trim,threshold);
//		
//		MutablePair<PiecewiseLinearFunction, PiecewiseLinearFunction> momentsModel = fitRollingMeanVar(gapsBeta, gapHalfBin, gapStep, prefix, "Gaps");
//		model.setGap(momentsModel.Item1, momentsModel.Item2);
//		
//		momentsModel = fitRollingMeanVar(startRolling, gapHalfBin, gapStep, prefix, "Start");
//		model.setStart(momentsModel.Item1, momentsModel.Item2);
//		
//		momentsModel = fitRollingMeanVar(stopRolling, gapHalfBin, gapStep, prefix, "Stop");
//		model.setStop(momentsModel.Item1, momentsModel.Item2);
//		
//		
//		
//		log.info("Computing pvalues for decoy ORFs");
//		LineWriter pvals = new LineOrientedFile(prefix+".internal.pvalues").write().writeLine("Sum\tpval");
//		PageFile internalOrfs = new PageFile(prefix+".decoy.orfs");
//		NumericArray buff = NumericArray.createMemory(0, NumericArrayType.Double);
//		while (!internalOrfs.eof()) {
//			buff.deserialize(internalOrfs);
//			double ip = model.computeErrorOrf(buff.toDoubleArray(), 1, buff.length()-1);
//			pvals.writef("%.1f\t%.5g\n",NumericArrayFunction.Sum.applyAsDouble(buff),ip);
//		}
//		pvals.close();
//		
//		log.info("Computing scores for major isoforms");
//		LineWriter scores = new LineOrientedFile(prefix+".major.scores").write().writeLine("Gene\tGap\tStart\tStop");
//		// "Gene\tCds\tLength\tSum\tMean coverage\tGaps\tStart\tStop"
//		for (String[] f : new LineOrientedFile(prefix+".bfits.data").lineIterator().skip(1).map(s->StringUtils.split(s,'\t')).loop()) {
//			double gap = model.computeGapPval(Integer.parseInt(f[2]), Double.parseDouble(f[4]), Integer.parseInt(f[5]));
//			double start = model.computeStartPval(Double.parseDouble(f[4]), Double.parseDouble(f[6]));
//			double stop = model.computeStopPval(Double.parseDouble(f[4]), Double.parseDouble(f[6]));
//			scores.writef("%s\t%.5g\t%.5g\t%.5g\n", f[0],gap,start,stop);
//		}
//		scores.close();
//		
//		
//		PageFileWriter pf = new PageFileWriter(prefix+".model");
//		model.serialize(pf);
//		pf.close();
//		
//		log.info("Running R script for plotting");
//		RConnect.R().set("prefix", prefix);
//		RConnect.R().run(CopyOfEstimateModelError.class.getResource("error_eval.R"));
//		
//		
//	}
//	
//	
//	private static MutablePair<PiecewiseLinearFunction, PiecewiseLinearFunction> fitRollingMeanVar(RollingStatistics rolling, int halfbinsize, int step, String prefix, String name) throws IOException {
//		
//		DoubleArrayList cov = new DoubleArrayList();
//		DoubleArrayList mean = new DoubleArrayList();
//		DoubleArrayList var = new DoubleArrayList();
//		
//		log.info("Computing "+name+" data");
//		rolling.iterateEquiSize(halfbinsize, step).forEachRemaining(rb->{
//			cov.add(Math.round(rb.getCovariate()*1E6)*1E-6);
//			fitMoments(rb.getValues(),mean,var);	
//		});
//		LineWriter txt = new LineOrientedFile(prefix+"."+name+".model.txt").write().writeLine("Coverage\tmean\tvar\tmean.Smooth\tvar.Smooth");
//			
//		log.info("Fitting spline for "+name+" means");
//		SmoothSplineResult spla = SmoothSpline.fitDFMatch(cov.toDoubleArray(), mean.toDoubleArray(), 20);
//		log.info("Fitting spline for "+name+" variances");
//		SmoothSplineResult splb = SmoothSpline.fitDFMatch(cov.toDoubleArray(), var.toDoubleArray(), 10);
//		
//		for (int m=0; m<cov.size(); m++) 
//			txt.writef("%.6f\t%.6g\t%.6g\t%.6g\t%.6g\n", cov.getDouble(m), mean.getDouble(m), var.getDouble(m), //0.,0.);
//					SmoothSpline.predict(spla, cov.getDouble(m), 0),SmoothSpline.predict(splb, cov.getDouble(m), 0));
//		txt.close();
//		
//		
//		PiecewiseLinearFunction plfa = new PiecewiseLinearFunction(cov.toDoubleArray(), EI.wrap(cov.toDoubleArray()).mapToDouble(x->SmoothSpline.predict(spla, x, 0)).toDoubleArray());
//		PiecewiseLinearFunction plfb = new PiecewiseLinearFunction(cov.toDoubleArray(), EI.wrap(cov.toDoubleArray()).mapToDouble(x->SmoothSpline.predict(splb, x, 0)).toDoubleArray());
//
//		return new MutablePair<PiecewiseLinearFunction, PiecewiseLinearFunction>(plfa, plfb);
//	}
//	
//	private static void fitMoments(NumericArray d, DoubleArrayList mean, DoubleArrayList var) {
//		double m = NumericArrayFunction.Mean.applyAsDouble(d);
//		double v = NumericArrayFunction.Variance.applyAsDouble(d);
////		double c = m*(1-m)/v-1;
////		alpha.add(m*c);
////		beta.add((1-m)*c);
//		mean.add(m);
//		var.add(v);
//	}
//	
//
//	private static int max(double[] cod, int i) {
//		return ArrayUtils.argmax(cod, i-1, i+2);
//	}
//
//	/**
//	 * 
//	 * 
//	 * Storage contains regions of clusters of overlapping transcripts (~ a gene); the data is a interval tree containing the nontrans regions
//	 * with their cds associated
//	 * 
//	 * @param annotation
//	 * @param sequence
//	 * @param progress
//	 * @return
//	 */
//	private static MemoryIntervalTreeStorage<IntervalTree<GenomicRegion, ImmutableReferenceGenomicRegion<String>>> inferNontrans(
//			MemoryIntervalTreeStorage<Transcript> annotation,
//			FastaIndexSequenceProvider sequence, Progress progress) {
//		
//		MemoryIntervalTreeStorage<IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>> re = new MemoryIntervalTreeStorage<IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>>((Class)IntervalTree.class);
//		
//		for (ReferenceSequence ref : annotation.getReferenceSequences()){
//			annotation.getTree(ref)
//				.groupIterator()
//				.map(r->new ArrayGenomicRegion(new IntervalTree<GenomicRegion,Transcript>(r,ref)))
//				.forEachRemaining(reg->re.add(ref, reg, new IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>(ref)));
//		}
//		
//		
//		List<ImmutableReferenceGenomicRegion<IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>>> singleton = new MutableMonad<ImmutableReferenceGenomicRegion<IntervalTree<GenomicRegion,ImmutableReferenceGenomicRegion<String>>>>().asMonadList();  
//		for (ImmutableReferenceGenomicRegion<Transcript> t : 
//							annotation.ei()
//								.progress(progress, (int)annotation.size(), r->r.getData().getTranscriptId())
//								.filter(r->r.getData().isCoding()).loop()
//								) {
//			
//			re.getReferenceRegionsIntersecting(t.getReference(), t.getRegion(), singleton);
//			IntervalTree<GenomicRegion, ImmutableReferenceGenomicRegion<String>> set = singleton.get(0).getData();
//			
//			ImmutableReferenceGenomicRegion<String> cds = new ImmutableReferenceGenomicRegion<String>(t.getReference(), t.getData().getCds(t.getReference(), t.getRegion()),t.getData().getTranscriptId());
//			String seq = sequence.getSequence(cds).toString();
//			
//			identifyNonTrans(seq,(s,e)->set.put(cds.map(new ArrayGenomicRegion(s,e)), cds));
////			System.out.println(seq);
////			System.out.println(StringUtils.packAscii(cds,set.iterator()));
//			
//			singleton.clear();
//			
//		}
//		
////		System.out.println(re.ei().mapToInt(r->r.getData().toGenomicRegion(gr->gr).getTotalLength()).collect((a,b)->a+b));
//		
//		
//		return re;
//	}
//
//
//	static Trie<String> nonTransStart = new Trie<String>();
//	static Trie<String> nonTransEnd = new Trie<String>();
//	
//	static {
//		nonTransStart.put("TAA", "TAA");
//		nonTransStart.put("TAG", "TAG");
//		nonTransStart.put("TGA", "TGA");
//		
//		nonTransEnd.putAll(nonTransStart);
//		StringUtils.iterateHammingHull("ATG",Alphabet.getDna()).forEachRemaining(s->nonTransEnd.put(s,s));
//	}
//	private static void identifyNonTrans(String seq,
//			BiIntConsumer re) {
//		
//		ToIntMappedIterator<AhoCorasickResult<String>> st = nonTransStart.iterateAhoCorasick(seq).mapToInt(r->r.getStart()+3);
//		while (st.hasNext()) {
//			int s = st.nextInt();
//			nonTransEnd.iterateAhoCorasick(seq.substring(s)).mapToInt(r->r.getStart()).filter(i->i%3==0).head(1).filter(i->i>0).forEachRemaining(e->re.accept(s, s+e));
//		}
//		
//	}
//	
//}
