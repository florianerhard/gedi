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
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
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
import gedi.riboseq.inference.clustering.RiboClusterBuilder;
import gedi.riboseq.inference.clustering.RiboClusterInfo;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.riboseq.inference.orf.Orf;
import gedi.riboseq.inference.orf.OrfFinder;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.MemoryFloatArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;


public class InferOrfs {

	private static final Logger log = Logger.getLogger( InferOrfs.class.getName() );
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
		System.err.println(" -f <Read filter spec>\t\t\tUse only reads matching the filter (e.g. 28:30)");
		System.err.println(" -o <prefix>\t\t\tPrefix for output files");
		System.err.println(" -m <model-file>\t\t\tModel file from EstimateRiboModel & EstimateModelError");
		System.err.println(" -g <genomic-file ...>\t\t\tGenomic files");
		System.err.println(" -nthreads <n>\t\t\tNumber of threads (default: available cores)");
		System.err.println(" --noannot\t\t\tDo not prefer transcripts from the annotation to assemble ORFs.");
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
		int minTotalActivity = -1;
		int minRegionCount = 5;
		int minReadCount = 10;
		int minRi=-1;
		int minLength = -1;
		int nthreads = Runtime.getRuntime().availableProcessors();
		int chunk = 10;
		String test = null;
		boolean filterByGap = false;
		boolean annot = true;
		GenomicRegionStorage<?> keep = null;
		boolean onlyStandardChromosomes = false;
		boolean filterByInternal = true;
		
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
			else if (args[i].equals("-keep")) {
				Path p = Paths.get(checkParam(args,++i));
				keep = (GenomicRegionStorage<?>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
			}
			else if (args[i].equals("-chunk")) {
				chunk=checkIntParam(args, ++i);
			}
			else if (args[i].equals("-r")) {
				Path p = Paths.get(checkParam(args,++i));
				reads = (GenomicRegionStorage<AlignedReadsData>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p);
			}
			else if (args[i].equals("-ri")) {
				minRi = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-f")) {
				filter=RiboUtils.parseReadFilter(checkParam(args, ++i));
			}
			else if (args[i].equals("-m")) {
				model = RiboModel.fromFile(checkParam(args,++i),true);
			}
			else if (args[i].equals("-g")) {
				ArrayList<String> names = new ArrayList<>();
				i = checkMultiParam(args, ++i, names);
				g = Genomic.get(names);
			}
			else if (args[i].equals("-filterByGap")) {
				filterByGap = true;
			}
			else if (args[i].equals("-noFilterByInternal")) {
				filterByInternal = false;
			}
			else if (args[i].equals("-onlyStandardChromosomes")) {
				onlyStandardChromosomes = true;
			}
			else if (args[i].equals("-noannot")) {
				annot = false;
			}
			else if (args[i].equals("-minreg")) {
				minRegionCount = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-test")) {
				test = checkParam(args, ++i); // JN555585-:123474-124329
			}
			else if (args[i].equals("-minread")) {
				minReadCount = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-mintotal")) {
				minTotalActivity = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-minlength")) {
				minLength = checkIntParam(args, ++i);
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
		
		if (minRi<0) minRi = 1;
		
		MemoryIntervalTreeStorage<RiboClusterInfo> clusters;
		
		if (test==null) {
			RiboClusterBuilder clb = new RiboClusterBuilder(prefix, reads, filter, g.getTranscripts(), minRegionCount, minReadCount, progress, nthreads);
			clusters = clb.build();
			Genomic ug = g;
			clusters.getReferenceSequences().removeIf(r->!ug.getSequenceNames().contains(r.getName()));
		} else {
			clusters = new MemoryIntervalTreeStorage<>(RiboClusterInfo.class);
			clusters.add(ImmutableReferenceGenomicRegion.parse(test));
		}
		
		OutputCodonActivitiesProcessor codonOutProc = new OutputCodonActivitiesProcessor(prefix, numCond);
		
		OrfFinder v = new OrfFinder(g.getTranscripts(),reads, new CodonInference(model,g)
																	.setFilter(filter)
																	.setRegularization(model[0].getInferenceLamdba()));
		if (minLength>=0)
			v.setMinAaLength(minLength);
		if (minTotalActivity>=0)
			v.setMinOrfTotalActivity(minTotalActivity);
		v.setMinimalReproducibilityIndex(minRi);
		v.setFilterByGap(filterByGap);
		v.setFilterByInternal(filterByInternal);
		v.setAssembleAnnotationFirst(annot);
//		LineWriter codOut = new LineOrientedFile(prefix+".codons.data").write();
//		v.setCodonOut(codOut);
		
		AtomicInteger count = new AtomicInteger(0);
//		DiskGenomicNumericBuilder codonOut = new DiskGenomicNumericBuilder(prefix+".codons.rmq");
//		codonOut.setReferenceSorted(true);
		// cannot be done in parallel fashion!
		
//		DiskGenomicNumericBuilder[] perCondCodonOut = new DiskGenomicNumericBuilder[reads.getRandomRecord().getNumConditions()];
//		for (i=0; i<perCondCodonOut.length; i++) {
//			perCondCodonOut[i] = new DiskGenomicNumericBuilder(prefix+"."+i+".codons.rmq");
//			perCondCodonOut[i].setReferenceSorted(true);
			// cannot be done in parallel fashion!
//		}
		
		
		GenomicRegionStorage<?> ukeep = keep;
		Predicate<ReferenceGenomicRegion<?>> inkeep = keep==null?r->false: 
				r->{
			GenomicRegion rstop = r.map(new ArrayGenomicRegion(r.getRegion().getTotalLength()-3,r.getRegion().getTotalLength()));
			return ukeep.ei(r.getReference(),rstop)
						.filter(k->k.map(new ArrayGenomicRegion(k.getRegion().getTotalLength()-3,k.getRegion().getTotalLength()))
									.equals(rstop)
								)
						.count()>0;
		};
		
		
		LineWriter tab = new LineOrientedFile(prefix+".orfs.tsv").write();
		Orf.writeTableHeader(tab, conditions);
		progress.init();
		progress.setCount((int)clusters.size());
		Progress uprog = progress;
		ExtendedIterator<ImmutableReferenceGenomicRegion<Orf>> oit = (test==null?clusters.ei():clusters.ei(test))//"JN555585+:107973-109008")
			.iff(onlyStandardChromosomes, it->it.filter(rgr->rgr.getReference().isStandard()))
//			.progress(progress, (int)clusters.size(), r->r.toLocationString()+" n="+count.get())
			.parallelized(nthreads, chunk, ei->ei
				.sideEffect(cl->{
					synchronized (uprog) {
						uprog.setDescription(()->cl.toLocationString()+" n="+count.get()).incrementProgress();
					}
				})
				.map(cl->v.computeChunk(count.getAndIncrement(),cl.getReference(), cl.getRegion().getStart(), cl.getRegion().getEnd(), codonOutProc))
				.demultiplex(st->st.ei())
				.sideEffect(r->{
					try {
						synchronized (tab) {
							r.getData().writeTableLine(tab,r);
						}
					} catch (IOException e) {
						throw new RuntimeException("Could not write table!",e);
					}
				})
				.filter(r->r.getData().passesAllFilters() || r.getData().getOrfType().isAnnotated() || inkeep.test(r))
				);
//			.sideEffect(orf->{
//				try {
//					OrfWithCodons o = orf.getData();
//					LineWriter test = new LineOrientedFile("test.starts").write().writeLine("Position\tGenomic\tScore\tChangepoint\tType");
//					
//					double[][] accc = new double[numCond][orf.getRegion().getTotalLength()/3];
//					CodonType[] types = new CodonType[orf.getRegion().getTotalLength()/3];
//					for (Codon cc : o.getCodons()) {
//						int pos = orf.induce(cc).getStart()/3;
//						for (int c=0; c<numCond; c++)
//							accc[c][pos]+=cc.getActivity()[c];
//						types[pos] = cc.getType();
//					}
//					
//					for (int p=0; p<accc[0].length; p++) {
//						test.writef("%d\t%d\t%.4f\t%.4f\t%s\n", p, orf.map(p*3), 
//								emodel.computeStartScore(accc,p,accc[0].length-1),
//								emodel.computeChangePointScore(accc,p,accc[0].length-1),
//								types[p]);
//					}
//					
//					test.close();
//					System.out.println(orf);
//				} catch (Exception e) {
//					throw new RuntimeException(e);
//				}
//			})
		
//		codOut.close();
		
		GenomicRegionStorage out = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, prefix+".orfs").add(Class.class, Orf.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		
//		CenteredDiskIntervalTreeStorage<OrfInfo> out = new CenteredDiskIntervalTreeStorage<OrfInfo>(prefix+".orfs.cit", OrfInfo.class);
		out.fill(oit);
		if (reads.getMetaData()!=null && reads.getMetaData().isObject())
			out.setMetaData(reads.getMetaData());
		log.log(Level.INFO, "Finishing viewer indices");
		
		progress.finish();
		codonOutProc.finish();
		
//		codonOut.build();
//		for (i=0; i<perCondCodonOut.length; i++)
//			perCondCodonOut[i].build();
		
		tab.close();
	}
	

	private static class OutputCodonActivitiesProcessor implements Consumer<ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>>{
		
		private String prefix;
		private int numCond;
		private PageFileWriter out;
		
		public OutputCodonActivitiesProcessor(String prefix, int numCond) throws IOException {
			this.prefix = prefix;
			this.numCond = numCond;
			out = new PageFileWriter(prefix+".codon.bin");
		}
		
		
		public void accept(ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> codons) {
			
			synchronized (out) {
				try {
					FileUtils.writeReferenceSequence(out, codons.getReference());
					FileUtils.writeGenomicRegion(out, codons.getRegion());
					int n = 0;
					for (Codon c : codons.getData()) 
						if (c.getTotalActivity()>=0.01)
							n++;
					out.putCInt(n);
					
					for (Codon c : codons.getData()) {
						if (c.getTotalActivity()>=0.01) {
							FileUtils.writeGenomicRegion(out, c);
							for (int i=0; i<numCond; i++)
								out.putFloat(c.activity[i]);
						}
					}
				} catch (IOException e) {
					throw new RuntimeException("Could not write codon activities!",e);
				}
			}

		}
		
		public void finish() throws IOException, InterruptedException {
			CenteredDiskIntervalTreeStorage<MemoryFloatArray> cit = new CenteredDiskIntervalTreeStorage<>(prefix+".codons.cit",MemoryFloatArray.class);
			PageFile in = out.read(true);
			
//			IterateIntoSink<ImmutableReferenceGenomicRegion<MemoryFloatArray>> sink = new IterateIntoSink<>(it->cit.fill(it));
			
			ExtendedIterator<ImmutableReferenceGenomicRegion<MemoryFloatArray>> codit = in.ei().demultiplex(pf->{
				try {
					MutableReferenceGenomicRegion<MemoryFloatArray> cluster = new MutableReferenceGenomicRegion<MemoryFloatArray>()
							.setReference(FileUtils.readReferenceSequence(in))
							.setRegion(FileUtils.readGenomicRegion(in));
					int n = in.getCInt();
					
					LinkedList<ImmutableReferenceGenomicRegion<MemoryFloatArray>> l = new LinkedList<>();
					for (int i=0; i<n; i++) {
						GenomicRegion codReg = cluster.map(FileUtils.readGenomicRegion(in));
						MemoryFloatArray a = (MemoryFloatArray) NumericArray.createMemory(numCond, NumericArrayType.Float);
						for (int c=0; c<a.length(); c++)
							a.setFloat(c, in.getFloat());
						
						l.add(new ImmutableReferenceGenomicRegion<>(cluster.getReference(), codReg,a));
					}
					return EI.wrap(l.iterator());
				} catch (Exception e) {
					throw new RuntimeException("Could not read codons!",e);
				}
			});
			cit.fill(codit);
			in.close();
			
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
			
			new File(in.getPath()).delete();
		}
		
		private void setRmq(DiskGenomicNumericBuilder codon, ReferenceSequence ref, int genomic, int offset, double act, float[] buff) {
			buff[(genomic-offset+3)%3] = (float)act;
			codon.addValueEx(ref, genomic, buff);
			buff[(genomic-offset+3)%3] = 0;
		}
		
	}
}
