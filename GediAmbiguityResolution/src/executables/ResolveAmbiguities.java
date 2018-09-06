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

import gedi.ambiguity.clustering.ClusterInfo;
import gedi.ambiguity.clustering.ReadClusterBuilder;
import gedi.app.Gedi;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.commandline.GediCommandline;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.FunctorUtils;
import gedi.util.LogUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializer;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.formats.Bed;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.mutable.MutableInteger;
import gedi.util.r.RConnect;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPInteger;


public class ResolveAmbiguities {
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
		System.err.println("ResolveAmbiguities <Options>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -r <cit-file>\t\t\tCit file containing mappings");
		System.err.println(" -d <bed-file>\t\t\tBed file containing genomic regions where read mappings will be discarded");
		System.err.println(" -minreg <count>\t\t\tminimal number of regions to keep a cluster (Default: 5)");
		System.err.println(" -minread <count>\t\t\tminimal number of reads to keep a cluster (Default: 10)");
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

	private static double checkDoubleParam(String[] args, int index) throws UsageException {
		String re = checkParam(args, index);
		if (!StringUtils.isNumeric(re)) throw new UsageException("Must be a double: "+args[index-1]);
		return Double.parseDouble(args[index]);
	}
	
	public static void start(String[] args) throws Exception {
		Gedi.startup(true);
		LogUtils.config();
		
		CenteredDiskIntervalTreeStorage<AlignedReadsData> reads = null;
		Genomic genomic = Genomic.getEmpty();
		
		double uniques = 30;
		double factor = 0.1;
		int minRegionCount = 5;
		int minReadCount = 10;
		int context = 100;
		LineOrientedFile out = null;
		String pdf = null;
		String outcit = null;
		
		MemoryIntervalTreeStorage<Void> discard = new MemoryIntervalTreeStorage(Void.class);;
		
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
			else if (args[i].equals("-r")) {
				reads = new CenteredDiskIntervalTreeStorage<AlignedReadsData>(checkParam(args,++i));
			}
			else if (args[i].equals("-d")) {
				discard.fill(Bed.<Void>iterateEntries(checkParam(args,++i),a->null));
			}
			else if (args[i].equals("-s")) {
				out = new LineOrientedFile(checkParam(args,++i));
			}
			else if (args[i].equals("-o")) {
				outcit = checkParam(args,++i);
			}
			else if (args[i].equals("-pdf")) {
				pdf = checkParam(args,++i);
			}
			else if (args[i].equals("-minreg")) {
				minRegionCount = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-factor")) {
				factor = checkDoubleParam(args, ++i);
			}
			else if (args[i].equals("-uniques")) {
				uniques = checkDoubleParam(args, ++i);
			}
			else if (args[i].equals("-context")) {
				context = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-minread")) {
				minReadCount = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-g")) {
				while (i+1<args.length && !args[i+1].startsWith("-")) {
					Genomic g = Genomic.get(checkParam(args,++i));
					genomic=Genomic.merge(genomic,g);
				}
			}
			else if (args[i].equals("-D")) {
			}
//			else if (!args[i].startsWith("-")) 
//					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}

		
		if (reads==null) throw new UsageException("No reads given!");
		if (out==null) throw new UsageException("No statistics file given!");
		if (outcit==null) throw new UsageException("No output file given!");

		boolean inisout = new File(outcit).equals(new File(reads.getPath()));
		HashMap<Integer,ImmutableReferenceGenomicRegion<Double>[]> weights = new HashMap<Integer, ImmutableReferenceGenomicRegion<Double>[]>();
		
		
		GediCommandline cmd = new GediCommandline();
		cmd.addParam("reads",reads);
		cmd.setExitOperation('n');
		
		out.startWriting();
		out.writeLine("Id\tReads\tMultiplicity\tnCluster\tremaining");
		//\tRead.sequence\tRead.Entropy.mono\tRead.entropy.di
		LineOrientedFile uOut = out;
		int ucontext = context;
		String updf = pdf;
		double uuniques = uniques;
		double ufactor = factor;
		
		if (pdf!=null)
			RConnect.R().startPDF(pdf,14,14);

		// Step 1: Build clusters
		MemoryIntervalTreeStorage<ClusterInfo> clusters = new ReadClusterBuilder(reads,genomic.getTranscripts(),minRegionCount,minReadCount,context,false,progress).build(reads.getPath()+".clusters");
		new File(reads.getPath()+".clusters").delete();
		
		Genomic ugenomic = genomic;
		
		MutableInteger counter = new MutableInteger();
		reads.ei()
			.progress(progress, (int)reads.size(), r->r.toLocationString())
			.<ImmutableReferenceGenomicRegion<int[]>>demultiplex(r->EI.seq(0, r.getData().getDistinctSequences())
											.filter(d->r.getData().getMultiplicity(d)>1)
											.map(d->new ImmutableReferenceGenomicRegion<int[]>(
													r.getReference(), 
													r.getRegion(), 
													new int[] {d,r.getData().getId(d), r.getData().getTotalCountForDistinctInt(d, ReadCountMode.All)}
													))
										)
			.sideEffect(r->counter.N++)
			.sort(new AmbiSerializer(),(a,b)->Integer.compare(getId(a),getId(b)))
			.progress(progress, counter, r->getId(r)+" Mapsize="+weights.size()+" Mem="+StringUtils.getHumanReadableMemory(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()))
			.multiplex((a,b)->Integer.compare(getId(a),getId(b)), ImmutableReferenceGenomicRegion.class)
			.forEachRemaining(r->rescue(cmd,uOut,updf!=null?a->RandomNumbers.getGlobal().getBool(0.001):a->false,ugenomic,
					uuniques,ufactor,weights::put,discard,
					clusters,ucontext,r));
					
		if (pdf!=null)
			RConnect.R().finishPDF();
		
		out.finishWriting();
		
		// now create the new cit
		AlignedReadsDataFactory fac = new AlignedReadsDataFactory(reads.getRandomRecord().getNumConditions());
		new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>(inisout?outcit+".tmp.cit":outcit, DefaultAlignedReadsData.class).fill(
				reads.ei()
				.progress(progress, (int)reads.size(), r->r.toLocationString())
				.map(r->{
					fac.start();
					for (int d=0; d<r.getData().getDistinctSequences(); d++) {
						if (r.getData().getMultiplicity(d)>1) {
							ImmutableReferenceGenomicRegion<Double>[] w = weights.get(r.getData().getId(d));
//							int remaining = 0;
//							for (ImmutableReferenceGenomicRegion<Double> p : w)
//								if (p.getData()>0) 
//									remaining++;
							
							int index = Arrays.binarySearch(w, r);
//							if (index<0) 
//								throw new RuntimeException();
							if (index>=0 && w[index].getData().floatValue()>0)
								fac.add(r.getData(), d).setMultiplicity(w.length).setWeight(w[index].getData().floatValue());
						}
						else
							fac.add(r.getData(), d).setWeight(1);
					}
					if (fac.getDistinctSequences()>0)
						return new ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>(r.getReference(), r.getRegion(),fac.create());
					return null;
				}).removeNulls()
				);
		
		if (inisout)
			new File(outcit+".tmp.cit").renameTo(new File(outcit));
		else if (!reads.getMetaData().isNull()) {
			FileUtils.writeAllText(reads.getMetaData().toJson(), new File(outcit+".metadata.json"));
		}
		
		// Step 2: Build ambiguity graph
//		AmbiguityGraph graph = new AmbiguityGraphBuilder(clusters,amb,progress).build();
//		System.out.println("Vertices\tTotal read mappings\tEdges");
//		// Graph statistics: size of each connected component
//		for (IntSet cc : new ConnectedComponentsAlgorithm().compute(ReflectionUtils.get(graph, "backingGrph"))) {
//			Set<ReferenceGenomicRegion<ClusterInfo>> vs = ReflectionUtils.invoke(graph, "i2v", cc);
//			int regs = 0;
//			HashSet<Integer> edges = new HashSet<Integer>();
//			for (ReferenceGenomicRegion<ClusterInfo> c : vs) { 
//				regs+=c.getData().getRegionCount();
//				edges.addAll(graph.getIncidentEdges(c));
//			}Applying STAR
//			System.out.printf("%d\t%d\t%d\n",cc.size(),regs,edges.size());
//		}
		
		
		
	}

	private static int getId(ImmutableReferenceGenomicRegion<int[]> a) {
		return a.getData()[1];
	}
	
	
	/**
	 * Zero step:
	 *  remove all that do not hit a cluster (due to cluster filtering)
	 * 
	 * First step (only if unique count in one possibility is > uniqueFactor * totalcount):
	 *  remove all with unique count < fraction * max(uniqueFactor)
	 *  
	 * Second step:
	 *  remove all where mingraph < fraction * max(mingraph)
	 *  
	 * Third step (only if mediancontext > uniqueFactor*totalcount for one; remove everything otherwise):
	 *  remove all where mediancontext < fraction *  max(mediancontext)
	 *  
	 * Fourth step:
	 *  weigh all remaining by mediancontext
	 * 
	 * 
	 * @param cmd
	 * @param out
	 * @param plotit
	 * @param genomic
	 * @param clusters
	 * @param context
	 * @param a
	 */
	private static void rescue(GediCommandline cmd, LineOrientedFile out, IntPredicate plotit, Genomic genomic, double uniques, double factor,
			BiConsumer<Integer,ImmutableReferenceGenomicRegion<Double>[]> weightMap,
			MemoryIntervalTreeStorage<Void> discard,
			MemoryIntervalTreeStorage<ClusterInfo> clusters, int context, 
			ImmutableReferenceGenomicRegion<int[]>[] a) {
	
//		
//		String seq = a[0].getData().Item2.genomeToRead(a[0].getData().Item1, genomic.getSequence(a[0])).toString();
//		double En = Entropy.compute(seq, 1);
//		double Ed = Entropy.compute(seq, 2);
		
		HashSet<ReferenceGenomicRegion<ClusterInfo>> cl = new HashSet<ReferenceGenomicRegion<ClusterInfo>>();
		int[] unique = new int[a.length];
		double[][] graphs = new double[a.length][];
		double[][] contexts = new double[a.length][];
		double[] mingraph = new double[a.length];
		double[] medianContext = new double[a.length];
		String[] names = new String[a.length];
		double[] w = new double[a.length];
		Arrays.fill(mingraph, -1);
		
//		if (getId(a[0])==563589) {
//			System.out.println();
//		}
//		System.out.println(getId(a[0])+" "+a.length);
		for (int i=0; i<a.length; i++) {
			ImmutableReferenceGenomicRegion<int[]> r = a[i];
			ImmutableReferenceGenomicRegion<ClusterInfo> clu = clusters.ei(r.getReference(), r.getRegion()).getUniqueResult(true,false);
			if (clu!=null) { // clu can be null due to cluster filtering
				w[i] = 1;
				cl.add(clu);
				names[i] = a[i].toLocationString();
				unique[i] = clu.getData().getTotalUniqueMappingReadCount();
				graphs[i] = clu.getData().getNeighborData(clu.getRegion().induce(r.getRegion()));
				contexts[i] = clu.getData().getContextData(clu.getRegion().induce(r.getRegion()), context);
				if (clu.getReference().getStrand()==Strand.Minus) {
					ArrayUtils.reverse(graphs[i]);
					ArrayUtils.reverse(contexts[i]);
				}
				mingraph[i] = ArrayUtils.min(graphs[i]);
				medianContext[i] = ArrayUtils.median(contexts[i].clone());
				
				
				
			}
		}
		
		if (discard!=null)
			for (int i=0; i<a.length; i++) {
				MutableReferenceGenomicRegion<int[]> r = a[i].toMutable().transformReference(rf->rf.toStrandIndependent());
				if (discard.ei(r).filter(d->d.contains(r)).count()>0)
					w[i] = 0;
			}
		
		for (int i=0; i<a.length; i++) {
			for (int j=0; j<i; j++) {
				// this assumes that the complete clusters are equal; choose one of them (the smallest one)
				if (w[j]>0 && w[i]>0 && ArrayUtils.equals(graphs[j],graphs[i],1E-14) && ArrayUtils.equals(contexts[j], contexts[i], 1E-14)) {
					if (a[j].compareTo(a[i])<0)
						w[j] = 0;
					else 
						w[i] = 0;
				}
			}
		}
		
		// apply filtering steps
		double maxunique = max(unique,w);
		if (maxunique>=uniques) {
			for (int i=0; i<unique.length; i++) {
				if (unique[i]<factor*maxunique)
					w[i]=0;
			}
		}
		
		double maxMinGraph = max(mingraph,w);
		for (int i=0; i<mingraph.length; i++) {
			if (mingraph[i]<factor*maxMinGraph)
				w[i]=0;
		}
		
		
		double maxMedianContext = max(medianContext,w);
		if (maxMedianContext>=0.01 * a[0].getData()[2]) {
			for (int i=0; i<medianContext.length; i++) {
				if (medianContext[i]<factor*maxMedianContext)
					w[i]=0;
			}
		} else {
			Arrays.fill(w,0);
		}
		
		
		double sum = 0;
		for (int i=0; i<w.length; i++) {
			w[i]*=medianContext[i];
			sum+=w[i];
		}
		if (sum==0)
			sum=1;
		int remaining = 0;
		for (int i=0; i<w.length; i++) {
			w[i]/=sum;
			if (w[i]>0)
				remaining++;
		}
		
		// build array
		ImmutableReferenceGenomicRegion<Double>[] re = new ImmutableReferenceGenomicRegion[remaining];
		int index = 0;
		for (int i=0; i<w.length; i++)
			if (w[i]>0)
				re[index++] = new ImmutableReferenceGenomicRegion<Double>(a[i].getReference(), a[i].getRegion(),w[i]);
		Arrays.sort(re);
		if (ArrayUtils.unique(re, FunctorUtils.naturalComparator())!=re.length)
			throw new RuntimeException(StringUtils.toString(re));
		weightMap.accept(getId(a[0]), re);
		
		
		if (cl.size()>1 && plotit.test(getId(a[0]))) {
			try {
				w = ArrayUtils.restrict(w, i->mingraph[i]>-1);
				unique = ArrayUtils.restrict(unique, i->mingraph[i]>-1);
				names = ArrayUtils.restrict(names, i->mingraph[i]>-1);
				graphs = ArrayUtils.restrict(graphs, i->mingraph[i]>-1);
				contexts = ArrayUtils.restrict(contexts, i->mingraph[i]>-1);
				
				RConnect.R().assign("id", new REXPInteger(getId(a[0])));
				RConnect.R().assign("w", w);
				RConnect.R().assign("names", names);
				RConnect.R().assign("graphs", REXP.createDoubleMatrix(graphs));
				RConnect.R().assign("contexts", REXP.createDoubleMatrix(contexts));
				RConnect.R().assign("unique", unique);
				RConnect.R().evalUnchecked(new LineIterator(ResolveAmbiguities.class.getResourceAsStream("plotambiguity.R")).concat("\n"));
				
				
				
			} catch (Exception e) {
				try {
					LineOrientedFile r = new LineOrientedFile("Rdebug.R");
					r.startWriting();
					r.writeLine("id<-c("+getId(a[0])+")");
					r.writeLine("names<-c("+EI.wrap(names).map(s->"\""+s+"\"").concat(",")+")");
					r.writeLine("graphs<-matrix(c("+EI.wrap(graphs).demultiplex(EI::wrap).concat(",")+"),nrow="+graphs.length+")");
					r.writeLine("contexts<-matrix(c("+EI.wrap(contexts).demultiplex(EI::wrap).concat(",")+"),nrow="+contexts.length+")");
					r.writeLine("unique<-c("+EI.wrap(unique).concat(",")+")");
					r.writeLine("w<-c("+EI.wrap(w).concat(",")+")");
					r.finishWriting();
					
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				throw new RuntimeException("Cannot plot!",e);
			}
		}
		

		try {
			out.writef("%d\t%d\t%d\t%d\t%d\n",
					getId(a[0]),
					a[0].getData()[2],
					a.length, 
					cl.size(), 
					remaining
					);
		} catch (IOException e1) {
		}
		
	}

	private static double max(double[] a, double[] w) {
		double re = Double.NEGATIVE_INFINITY;
		for (int i=0; i<a.length; i++)
			re = Math.max(re, a[i]*w[i]);
		return re;
	}
	
	private static double max(int[] a, double[] w) {
		double re = Double.NEGATIVE_INFINITY;
		for (int i=0; i<a.length; i++)
			re = Math.max(re, a[i]*w[i]);
		return re;
	}
	
	
	private static class AmbiSerializer implements BinarySerializer<ImmutableReferenceGenomicRegion<int[]>> {

		private HashMap<ReferenceSequence,Short> refMap = new HashMap<ReferenceSequence, Short>();
		private ArrayList<ReferenceSequence> revMap = new ArrayList<ReferenceSequence>();
		
		@Override
		public Class<ImmutableReferenceGenomicRegion<int[]>> getType() {
			return (Class)ImmutableReferenceGenomicRegion.class;
		}

		@Override
		public void serialize(
				BinaryWriter out,
				ImmutableReferenceGenomicRegion<int[]> object)
				throws IOException {
			short rid = refMap.computeIfAbsent(object.getReference(), r->(short)refMap.size());
			if (rid>=revMap.size()) revMap.add(object.getReference());
			
			out.putCShort(rid);
			FileUtils.writeGenomicRegion(out, object.getRegion());
			for (int i=0; i<3; i++)
				if (object.getData()[i]<0) {
					System.err.println("Fatal error, cannot continue: No read ids!");
					System.exit(1);
				}
				else
					out.putCInt(object.getData()[i]);
		}

		@Override
		public ImmutableReferenceGenomicRegion<int[]> deserialize(
				BinaryReader in) throws IOException {
			int rid = in.getCShort();
			ArrayGenomicRegion reg = FileUtils.readGenomicRegion(in);
			int[] d = {in.getCInt(),in.getCInt(),in.getCInt()};
			
			return new ImmutableReferenceGenomicRegion<int[]>(
					revMap.get(rid),
					reg,
					d
					);
		}

		@Override
		public void serializeConfig(BinaryWriter out) throws IOException {
		}

		@Override
		public void deserializeConfig(BinaryReader in) throws IOException {
		}
		
	}
	
}
