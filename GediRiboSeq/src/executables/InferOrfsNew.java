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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import gedi.app.Gedi;
import gedi.app.extension.ExtensionContext;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.cleavage.SimpleCodonModel;
import gedi.riboseq.inference.clustering.RiboClusterBuilder;
import gedi.riboseq.inference.clustering.RiboClusterInfo;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.riboseq.inference.orf.NoiseModel;
import gedi.riboseq.inference.orf.NoiseModel.SingleNoiseModel;
import gedi.riboseq.inference.orf.OrfInference;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.riboseq.inference.orf.StartCodonScorePredictor;
import gedi.riboseq.inference.orf.StartCodonTraining;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.MemoryFloatArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.DoubleRanking;
import gedi.util.math.stat.NumericSample;
import gedi.util.math.stat.classification.CompleteRocAnalysis;
import gedi.util.math.stat.testing.MultipleTestingCorrection;
import gedi.util.mutable.MutableInteger;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class InferOrfsNew {

	private static final Logger log = Logger.getLogger( InferOrfsNew.class.getName() );
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
		System.err.println("InferOrfs <Options>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -r <cit-file>\t\t\tCit file containing read mappings");
		System.err.println(" -o <prefix>\t\t\tPrefix for output files");
		System.err.println(" -m <model-file>\t\t\tModel file from EstimateRiboModel");
		System.err.println(" -delta <delta>\t\t\tSpecify regularization parameter");
		System.err.println(" -t <pair1 pair2 ...>\t\t\tSpecifications treatment pairs (ltm or harr to chx) to detect startcodon (e.g.: 8hpi_harr/8hpi or 7/5)");
		System.err.println(" -g <genomic-file ...>\t\t\tGenomic files");
		System.err.println(" -continue\t\t\tDo not start over if temp files from intermediate steps are present, and keep all temp files");
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
	
	
	private static String checkParam(String[] args, int index) throws UsageException {
		if (index>=args.length || args[index].startsWith("-")) throw new UsageException("Missing argument for "+args[index-1]);
		return args[index];
	}
	
	
	private static int checkMultiParam(String[] args, int index, ArrayList<String> re) throws UsageException {
		while (index<args.length && !args[index].startsWith("-")) 
			re.add(args[index++]);
		return index-1;
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void start(String[] args) throws Exception {
		Gedi.startup(true);
		
		GenomicRegionStorage<AlignedReadsData> reads = null;
		Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter = null;
		Genomic g = null;
		RiboModel[] model = null;
		String prefix = null;
		int nthreads = Runtime.getRuntime().availableProcessors();
		int chunk = 10;
		String test = null;
		double fdr = 0.1;
		double delta = 0;
		int trainingExamples = 1000;
		boolean cont = false;
		SimpleCodonModel simpleModel = null;
		boolean novelTranscripts = false;
		MemoryIntervalTreeStorage<Void> introns = new MemoryIntervalTreeStorage<>(Void.class);
		ArrayList<String> checkOrfs = new ArrayList<>();
		boolean removeAnno = true;
		
//		String[] startcodonpairs = new String[0];
		
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
//			else if (args[i].equals("-t")) {
//				ArrayList<String> li = new ArrayList<>();
//				i = checkMultiParam(args, ++i, li);
//				startcodonpairs = li.toArray(new String[0]);
//			}
			else if (args[i].equals("-chunk")) {
				chunk=checkIntParam(args, ++i);
			}
			else if (args[i].equals("-r")) {
				Path p = Paths.get(checkParam(args,++i));
				reads = (GenomicRegionStorage<AlignedReadsData>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
			}
			else if (args[i].equals("-s")) {
				simpleModel = new SimpleCodonModel(StringUtils.split(checkParam(args,++i),' '));
			}
			else if (args[i].equals("-f")) {
				filter=RiboUtils.parseReadFilter(checkParam(args, ++i));
			}
			else if (args[i].equals("-fdr")) {
				fdr=checkDoubleParam(args, ++i);
			}
			else if (args[i].equals("-m")) {
				model = RiboModel.fromFile(checkParam(args,++i),false);
			}
			else if (args[i].equals("-delta")) {
				delta = checkDoubleParam(args, ++i);
			}
			else if (args[i].equals("-continue")) {
				cont = true;
			}
			else if (args[i].equals("-novel")) {
				novelTranscripts = true;
			}
			else if (args[i].equals("-dontRemoveAnno")) {
				removeAnno = false;
			}
			else if (args[i].equals("-introns")) {
				Path p = Paths.get(checkParam(args,++i));
				GenomicRegionStorage<Void> ti = (GenomicRegionStorage<Void>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
				introns.fill(ti);
			}
			else if (args[i].equals("-check")) {
				checkOrfs.add(checkParam(args,++i));
			}
			else if (args[i].equals("-checkAnno")) {
				checkOrfs.add("Annotation");
			}
			else if (args[i].equals("-g")) {
				ArrayList<String> names = new ArrayList<>();
				i = checkMultiParam(args, ++i, names);
				g = Genomic.get(names);
			}
			else if (args[i].equals("-test")) {
				test = checkParam(args, ++i); // JN555585-:123474-124329
			}
			else if (args[i].equals("-o")) {
				prefix = checkParam(args,++i);
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
		if (model==null) throw new UsageException("No model given!");

		
		// TODO parameters
		
		int numCond = reads.getRandomRecord().getNumConditions();
		String[] conditions = new String[numCond];
		if (reads.getMetaData().isNull()) {
			for (int c=0; c<conditions.length; c++)
				conditions[c] = c+"";
		} else {
			for (int c=0; c<conditions.length; c++) {
				conditions[c] = reads.getMetaData().getEntry("conditions").getEntry(c).getEntry("name").asString();
				if ("null".equals(conditions[c])) conditions[c] = c+"";
			}
		}
		HashMap<String, Integer> condIndex = ArrayUtils.createIndexMap(conditions);

//		int[][] startCodonPairs = new int[startcodonpairs.length][2];
//		for (int c=0; c<startcodonpairs.length; c++) {
//			String[] p = StringUtils.split(startcodonpairs[c], '/');
//			if (p.length!=2) throw new UsageException("Wrong syntax in start codon treatment pair: "+startcodonpairs[c]);
//			for (int ind=0; ind<2; ind++) {
//				if (condIndex.containsKey(p[ind]))
//					startCodonPairs[c][ind] = condIndex.get(p[ind]);
//				else if (StringUtils.isInt(p[ind]))
//					startCodonPairs[c][ind] = Integer.parseInt(p[ind]);
//				else
//					throw new UsageException("Condition "+p[ind]+" unknown for treatment pair!");
//			}
//		}
		
		
		MemoryIntervalTreeStorage<RiboClusterInfo> clusters;
		
		if (test==null) {
			RiboClusterBuilder clb = new RiboClusterBuilder(prefix, reads, filter, g.getTranscripts(), 1, 5, progress, nthreads);
			clb.setContinueMode(cont);
			clusters = clb.build();
			Genomic ug = g;
			clusters.getReferenceSequences().removeIf(r->!ug.getSequenceNames().contains(r.getName()));
		} else {
			clusters = new MemoryIntervalTreeStorage<>(RiboClusterInfo.class);
			ReferenceGenomicRegion<?> rgr = g.getNameIndex().get(test);
			if (rgr==null)
				rgr = ImmutableReferenceGenomicRegion.parse(test);
			else
				rgr = rgr.toMutable().transformRegion(r->r.extendFront(1)).toImmutable();// workaround for IndexGenome bug
			test = rgr.toLocationString();
			
			clusters.add(rgr.getReference(),rgr.getRegion(),null);
			nthreads = 0;
		}
		
		if (simpleModel!=null)
			for (RiboModel m : model) 
				m.setSimple(simpleModel);
		
		OrfInference v = new OrfInference(g,reads);
		if (introns.size()>0)
			v.addSpliceJunctions(introns.ei());
		if (checkOrfs.size()>0) {
			for (String name : checkOrfs) {
				if (name=="Annotation") 
					v.addCheckAnnotation();
				else {
					Path p = Paths.get(name);
					GenomicRegionStorage<Void> ti = (GenomicRegionStorage<Void>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
					v.addCheckOrfs(FileUtils.getNameWithoutExtension(name),ti.ei());
				}
			}
			
		}
		
		v.setAllowNovelTranscripts(novelTranscripts);
		v.setRemoveAnno(removeAnno);
		
		if (test!=null)
			progress = new NoProgress();
		
		Thread viewerIndexer = null;
		
		if (!cont || !new File(prefix+".codons.bin").exists() || test!=null) {
			
			CodonInference ci = new CodonInference(model,g)
			.setFilter(filter)
//			.setRho(rho)
			.setRegularization(delta);
			
			log.log(Level.INFO, "Codon inference");
			AtomicInteger count = new AtomicInteger(0);
			progress.init();
			progress.setCount((int)clusters.size());
			Progress uprog = progress;
			ExtendedIterator<ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>> pit = (test==null?clusters.ei():clusters.ei(test))//"JN555585+:107973-109008")
				.parallelized(nthreads, chunk, ei->ei
					.sideEffect(cl->{
						synchronized (uprog) {
							uprog.setDescription(()->cl.toLocationString()+" n="+count.get()).incrementProgress();
						}
					})
					.map(cl->v.codonInference(ci,count.getAndIncrement(),cl.getReference(), cl.getRegion().getStart(), cl.getRegion().getEnd()))
					.removeNulls()
					);
			PageFileWriter tmp = new PageFileWriter(prefix+".codons.bin");
			while (pit.hasNext()) {
				ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> n = pit.next();
				FileUtils.writeReferenceSequence(tmp, n.getReference());
				FileUtils.writeGenomicRegion(tmp, n.getRegion());
				tmp.putCInt(n.getData().size());
				for (Codon c : n.getData()) {
					FileUtils.writeGenomicRegion(tmp, c);
					FileUtils.writeDoubleArray(tmp, c.activity);
				}
			}
			tmp.close();
			progress.finish();
			
			
			if (test==null) {
				log.log(Level.INFO, "Finishing viewer indices (background)");
				String uprefix = prefix;
				int unthreads = nthreads;
				int uchunk = chunk;
				viewerIndexer = new Thread(()->{
					try {
						if (new File(uprefix+".codons.cit").exists())
							new File(uprefix+".codons.cit").delete();
						
						CenteredDiskIntervalTreeStorage<MemoryFloatArray> cit = new CenteredDiskIntervalTreeStorage<>(uprefix+".codons.cit",MemoryFloatArray.class);
						processCodons(uprefix+".codons.bin", new NoProgress(), null, unthreads, uchunk, MemoryFloatArray.class, 
								ei->codonsToArray(ei),
								cit::fill);
						writeViewerIndices(uprefix, cit);
						
					} catch (IOException e) {
						throw new RuntimeException("Could not finish viewer indices!",e);
					}
				});
				viewerIndexer.setName("ViewerIndices");
				viewerIndexer.start();
			}
				
			
		} else {
			log.log(Level.INFO, "Using saved codons "+prefix+".codons.bin");
		}
		
		if (!cont) new File(prefix+".clusters.cit").delete();

		
		if (!cont || !new File(prefix+".start.model").exists()) {
			log.log(Level.INFO, "Train start prediction");
		
			StartCodonTraining startPred = new StartCodonTraining(trainingExamples);
			processCodonsSink(prefix+".codons.bin", progress, null, nthreads, chunk, PriceOrf.class, 
				ei->ei.unfold(o->v.findAnnotated(false,o).iterator()).removeNulls().filter(o->o.getData().getTotalActivityFromPredicted()>=25 && o.getData().getOrfAaLength()>=10),
				n->startPred.add(n)
				);
			
//			startPred.writeDirichletModel(50,50,1000);
//			if (true) return;
			
//			log.log(Level.INFO, "Considering "+startPred.getNumExamples()+" ORFs with meanActivity>"+startPred.getMinMean()+" with Isoform fraction>="+thres);
			log.log(Level.INFO, "Considering "+startPred.getNumExamples()+" ORFs");
			
//			log.log(Level.INFO, "CV");
//			CompleteRocAnalysis roc = startPred.crossValidation(5,true,false);
//			FileUtils.writeAllText(roc.toString(), new File(prefix+".start.startscore.cv"));
//			log.log(Level.INFO, "Start: 5-fold CV AUROC="+roc.getAucFprTpr()+" AUPR="+roc.getAucPpvTpr());
//			
//			roc = startPred.crossValidation(5,false,true);
//			FileUtils.writeAllText(roc.toString(), new File(prefix+".start.rangescore.cv"));
//			log.log(Level.INFO, "Range: 5-fold CV AUROC="+roc.getAucFprTpr()+" AUPR="+roc.getAucPpvTpr());
//			
//			roc = startPred.crossValidation(5,true,true);
//			FileUtils.writeAllText(roc.toString(), new File(prefix+".start.bothscores.cv"));
//			log.log(Level.INFO, "Both: 5-fold CV AUROC="+roc.getAucFprTpr()+" AUPR="+roc.getAucPpvTpr());
			
			StartCodonScorePredictor predictor = startPred.train();
			v.setStartCodonPredictor(predictor);
			
//			log.log(Level.INFO, "Evaluate ranks in all CDS");
//			double[] bounds = {0.3,1,Double.POSITIVE_INFINITY};
//			NumericSample[] ranks = {new NumericSample(),new NumericSample(),new NumericSample()};
//			
//			LineWriter startDetails = new LineOrientedFile(prefix+".start.ranks").write();
//			startDetails.write("Transcript\tGeom.mean\tScore\tRank\tLocation\n");
//			processCodonsSink(prefix+".codons.bin", progress, null, nthreads, chunk, PriceOrf.class, 
//					ei->ei.unfold(o->v.findAnnotated(false,o).iterator()).removeNulls().filter(o->o.getData().getTotalActivityFromPredicted()>=25 && o.getData().getOrfAaLength()>=10),
//					n->{
//						double[] scores = v.computeStartScores(n.getData(), null,true);
//						double ann = scores[n.getData().getPredictedStartAminoAcid()];
//						DoubleRanking ranking = new DoubleRanking(scores);
//						ranking.sort(false);
//						double rank = ranking.getCurrentRank(n.getData().getPredictedStartAminoAcid())/(double)scores.length;
//						double sinh = n.getData().getGeomMean(n.getData().getPredictedStartAminoAcid());
//						
//						if (!Double.isNaN(ann)) {
//							for (int r=0; r<bounds.length; r++) 
//								if (sinh<bounds[r]) {
//									ranks[r].add(rank);
//									break;
//								}
//							startDetails.writef2("%s\t%.2f\t%.2f\t%.2f\t%s\n",
//									n.getData().getTranscript(),
//									sinh,
//									ann,
//									rank,
//									n.toLocationString()
//									);
//						}
//					}
//					);
//			
//			startDetails.close();
//			
//			for (int r=0; r<bounds.length; r++) 
//				log.log(Level.INFO, "Activity<"+bounds[r]+" AUECDF="+ranks[r].ecdf().integral(0,1));
			
			
			
			if (cont) {
				PageFileWriter fm = new PageFileWriter(prefix+".start.model");
				predictor.serialize(fm);
				fm.close();
			}
			
		} else {
			log.log(Level.INFO, "Using saved start prediction model "+prefix+".start.model");
			PageFile fm = new PageFile(prefix+".start.model");
			StartCodonScorePredictor predictor = new StartCodonScorePredictor(); 
			predictor.deserialize(fm);
			fm.close();
			v.setStartCodonPredictor(predictor);
		}
		
		if (!cont || !new File(prefix+".noise.model").exists()) {
			log.log(Level.INFO, "Calibrate noise model");
		
			ArrayList<SingleNoiseModel> total = new ArrayList<>();
			processCodonsSink(prefix+".codons.bin", progress, null, nthreads, chunk, SingleNoiseModel.class, 
				ei->ei.map(o->new ImmutableReferenceGenomicRegion<SingleNoiseModel>(o.getReference(),o.getRegion(),v.computeNoise(o))),
				n->{
					if (n.getData()!=null)
						total.add(n.getData());
				});
			
			Collections.sort(total);
			NoiseModel noise = new NoiseModel(total.toArray(new SingleNoiseModel[0]));
			
			if (cont) {
				PageFileWriter fm = new PageFileWriter(prefix+".noise.model");
				noise.serialize(fm);
				fm.close();
			}
			
			v.setNoiseModel(noise);
			
		} else {
			log.log(Level.INFO, "Using saved noise model "+prefix+".noise.model");
			PageFile fm = new PageFile(prefix+".noise.model");
			NoiseModel m = new NoiseModel();
			m.deserialize(fm);
			
			v.setNoiseModel(m);
			fm.close();
		}
		
		log.log(Level.INFO, "Infer ORFs");
		LineWriter tab = new LineOrientedFile(prefix+".orfs.tsv").write();
		PriceOrf.writeTableHeader(tab, conditions);
		
		PageFileWriter tmp = new PageFileWriter(prefix+".orfs.bin");
		DoubleArrayList pvals = new DoubleArrayList();
		processCodonsSink(prefix+".codons.bin", progress, ()->"Cache: "+StringUtils.toString(v.getNoiseModel().getCacheSize()),nthreads, chunk, PriceOrf.class, 
				ei->ei.demultiplex(o->v.inferOrfs(o).ei()),
				n->{
					try {
//							if (n.getData().getExpP()>v.getTestThreshold() && n.getData().getAbortiveP()>v.getTestThreshold()) {
								n.toMutable().serialize(tmp);
								pvals.add(n.getData().getCombinedP());
//							}
							n.getData().writeTableLine(tab, n);
					} catch (IOException e) {
						throw new RuntimeException("Could not write ORFs!",e);
					}
					
				});
		tab.close();
		
		
		log.log(Level.INFO, "Found "+pvals.size()+" ORFs");
		log.log(Level.INFO, "Multiple testing correction and filtering");
		double[] corr = MultipleTestingCorrection.benjaminiHochberg(pvals.toDoubleArray());
		PageFile in = tmp.read(true);
		in.getContext().add(Class.class, PriceOrf.class);
		MutableInteger index = new MutableInteger();
		
		GenomicRegionStorage out = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, prefix+".tmp.orfs").add(Class.class, PriceOrf.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		double ufdr = fdr;
		out.fill(
				in.ei().map(pf->{
					try {
						MutableReferenceGenomicRegion<PriceOrf> orf = new MutableReferenceGenomicRegion<>();
						orf.deserialize(in);
						v.setCorrectedPvalue(orf,corr[index.N++]);
						return orf;
					} catch (IOException e) {
						throw new RuntimeException("Could not read temporary orfs!",e);
					}
					})
				.progress(progress, corr.length, r->r.toLocationStringRemovedIntrons())
				.filter(o->o.getData().getCombinedP()<ufdr)
				.iff(checkOrfs.size()>0, ei->ei.sideEffect(o->v.setDetected(o)))
				);
		in.close();
		new File(in.getPath()).delete();
		
		
		// TODO TPM (OPM?) ausrechnen und an tsv anhaengen
		
		log.log(Level.INFO, "Remaining after multiple testing correction: "+out.size()+" ORFs");
		log.log(Level.INFO, "Reassign codons");
		GenomicRegionStorage<PriceOrf> out2 = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, prefix+".orfs").add(Class.class, PriceOrf.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		processCodons(prefix+".codons.bin", progress, null, nthreads, chunk, PriceOrf.class, 
				ei->ei.demultiplex(o->v.redistributeCodons(o, out.ei(o))),
				out2::fill);
		out.clear();
		
		
		if (reads.getMetaData()!=null && reads.getMetaData().isObject())
			out2.setMetaData(reads.getMetaData());
		
		if (viewerIndexer!=null)
			viewerIndexer.join();
		
		if (!cont) new File(prefix+".codons.bin").delete();

		if (checkOrfs.size()>0) {
			CenteredDiskIntervalTreeStorage<MemoryFloatArray> cod = new CenteredDiskIntervalTreeStorage<>(prefix+".codons.cit");
			ToDoubleFunction<ImmutableReferenceGenomicRegion<?>> total = r->cod.ei(r).mapToDouble(c->c.getData().sum()).sum();
			
			for (String name : v.getCheckOrfNames()) 
				v.iterateChecked(name).map(r->String.format("%s\t%s\t%s\t%s\t%.0f\t%d",
						r.getReference()+":"+r.map(r.getRegion().getTotalLength()-1),
						r.toLocationString(),
						r.getData().Item1.toString(),
						r.getData().Item2.toString(),
						total.applyAsDouble(r),
						r.getRegion().getTotalLength()/3-1
						)).print("Stopid\tLocation\tData\tStatus\tExpression\taaLength",prefix+"."+name+".checked.tsv");
			
			out2.ei().map(r->String.format("%s\t%s\t%s\t%s\t%.0f\t%d",
					r.getReference()+":"+r.map(r.getRegion().getTotalLength()-1),
					r.toLocationString(),
					"",
					"Detected",
					total.applyAsDouble(r),
					r.getRegion().getTotalLength()/3-1
					)).print("Stopid\tLocation\tData\tStatus\tExpression\taaLength",prefix+".PRICE.checked.tsv");
		}
		
		
	}
	
	private static ExtendedIterator<ImmutableReferenceGenomicRegion<MemoryFloatArray>> codonsToArray(ExtendedIterator<ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>> ei) {
		
		return ei.unfold(r->EI.wrap(r.getData()).<ImmutableReferenceGenomicRegion<MemoryFloatArray>>map(codon->{
					GenomicRegion codReg = r.map(codon);
					MemoryFloatArray a = (MemoryFloatArray) NumericArray.createMemory(codon.activity.length, NumericArrayType.Float);
					for (int c=0; c<a.length(); c++)
						a.setFloat(c, (float) codon.activity[c]);
					
					return new ImmutableReferenceGenomicRegion<MemoryFloatArray>(r.getReference(), codReg,a);
				}
				)
		);
	}

	private static <A> void processCodonsSink(String file, Progress progress, Supplier<String> extraProgress, int nthreads, int chunk, Class<A> aclass, 
			Function<ExtendedIterator<ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>>,ExtendedIterator<ImmutableReferenceGenomicRegion<A>>> parallel, 
			Consumer<ImmutableReferenceGenomicRegion<A>> sink) throws IOException {
	
		processCodons(file, progress, extraProgress, nthreads, chunk, aclass, parallel, oit->oit.forEachRemaining(sink));
		
	}
	
	private static <A> void processCodons(String file, Progress progress, Supplier<String> extraProgress, int nthreads, int chunk, Class<A> aclass, 
			Function<ExtendedIterator<ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>>,ExtendedIterator<ImmutableReferenceGenomicRegion<A>>> parallel, 
			Consumer<ExtendedIterator<ImmutableReferenceGenomicRegion<A>>> sink) throws IOException {
		
		PageFile prof = new PageFile(file);
		Progress uprog = progress;
		progress.init();
		ExtendedIterator<ImmutableReferenceGenomicRegion<A>> oit = prof.ei().map(pr->{
					try {
						ReferenceSequence ref = FileUtils.readReferenceSequence(pr);
						GenomicRegion reg = FileUtils.readGenomicRegion(pr);
						int n = pr.getCInt();
						IntervalTreeSet<Codon> codons = new IntervalTreeSet<Codon>(null);
						for (int i=0; i<n;i++) {
							codons.add(new Codon(FileUtils.readGenomicRegion(pr),FileUtils.readDoubleArray(pr)));
						}
						
						return new ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>(ref,reg,codons);
					} catch (IOException e) {
						throw new RuntimeException("Could not read temporary profiles!",e);
					}
					})
		.parallelized(nthreads, chunk, ei->parallel.apply(ei)
				.sideEffect(r->{
					synchronized (uprog) {
						
						
						if (extraProgress!=null)
							uprog.setDescription(()->r.toLocationStringRemovedIntrons()+" mem="+StringUtils.getShortHumanReadableMemory(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())
									+" "+extraProgress.get()).incrementProgress();
						else
							uprog.setDescription(()->r.toLocationStringRemovedIntrons()).incrementProgress();
					}
				})
				);
		
		sink.accept(oit);
		
		progress.finish();
		
	}


		
	public static void writeViewerIndices(String prefix, CenteredDiskIntervalTreeStorage<MemoryFloatArray> cit) throws IOException {
		
		int numCond = cit.getRandomRecord().length();
		
		DiskGenomicNumericBuilder codonOut = new DiskGenomicNumericBuilder(prefix+".codons.rmq");
		codonOut.setReferenceSorted(true);
		
		DiskGenomicNumericBuilder[] perCondCodonOut = new DiskGenomicNumericBuilder[numCond];
		for (int i=0; i<perCondCodonOut.length; i++) {
			perCondCodonOut[i] = new DiskGenomicNumericBuilder(prefix+"."+i+".codons.rmq");
			perCondCodonOut[i].setReferenceSorted(true);
		}
		
		float[] data = new float[3];
		for (ImmutableReferenceGenomicRegion<MemoryFloatArray> c : cit.ei().loop()) {
			double sum = c.getData().evaluate(NumericArrayFunction.Sum);
			setRmq(codonOut, c.getReference(), 
					c.getRegion().map(0),0,
					sum, data);
			setRmq(codonOut, c.getReference(), 
					c.getRegion().map(1),1,
					sum, data);
			setRmq(codonOut, c.getReference(), 
					c.getRegion().map(2),2,
					sum, data);
		}
		codonOut.build();
		for (int i=0; i<perCondCodonOut.length; i++) {
			for (ImmutableReferenceGenomicRegion<MemoryFloatArray> c : cit.ei().loop()) {
				double v = c.getData().getFloat(i);
				if (v>=0.01) {
					setRmq(perCondCodonOut[i], c.getReference(), 
							c.getRegion().map(0),0,
							v, data);
					setRmq(perCondCodonOut[i], c.getReference(), 
							c.getRegion().map(1),1,
							v, data);
					setRmq(perCondCodonOut[i], c.getReference(), 
							c.getRegion().map(2),2,
							v, data);
				}
			}
			perCondCodonOut[i].build();
		}
		
	}
	
	private static void setRmq(DiskGenomicNumericBuilder codon, ReferenceSequence ref, int genomic, int offset, double act, float[] buff) {
		buff[(genomic-offset+3)%3] = (float)act;
		codon.addValueEx(ref, genomic, buff);
		buff[(genomic-offset+3)%3] = 0;
	}
		
}
