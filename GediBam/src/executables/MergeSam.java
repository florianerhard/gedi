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
import gedi.bam.tools.SamMismatchCorrectionBarcodeAnnotator;
import gedi.bam.tools.SamToRegion;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.Trie;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializer;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.jhp.Jhp;
import gedi.util.io.text.tsv.formats.CsvReaderFactory;
import gedi.util.mutable.MutableDouble;
import gedi.util.mutable.MutableInteger;
import gedi.util.mutable.MutableMonad;
import gedi.util.nashorn.JS;
import gedi.util.oml.OmlNodeExecutor;
import gedi.util.oml.OmlReader;
import gedi.util.oml.PlaceholderInterceptor;
import gedi.util.oml.petrinet.GenomicRegionFeaturePipeline;
import gedi.util.orm.Orm;
import gedi.util.userInteraction.log.ErrorProtokoll;
import gedi.util.userInteraction.log.LogErrorProtokoll;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class MergeSam {


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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void start(String[] args) throws Exception {
		
		Gedi.startup(true);
		
		
		Progress progress = new NoProgress();
		ErrorProtokoll errors = new LogErrorProtokoll();

		String table = null;
		String bcFile = null;
		String bcJson = null;
		String[] genomic = null;
		int bcOffset = -1;
		int filterPrio = 1;
		String output = null;
		String oml = null;
		String conditionsFile = null;
		boolean check = false;
		boolean correctMc = false;
		HashMap<String, Object> param = JS.parseParameter(args, false);
		String prioPipeline = null;
		boolean removeNonStandard = true;
		boolean unmapped = false;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-p")) {
				progress=new ConsoleProgress();
			}
			else if (args[i].equals("-nonstandard")) {
				removeNonStandard = false;
			}
			else if (args[i].equals("-prio")) {
				prioPipeline = checkParam(args, ++i);
			}
			else if (args[i].equals("-t")) {
				table = checkParam(args,++i);
			}
			else if (args[i].equals("-genomic")) {
				ArrayList<String> gnames = new ArrayList<>();
				i = checkMultiParam(args, ++i, gnames);
				genomic = gnames.toArray(new String[0]);
			}
			else if (args[i].equals("-filter")) {
				filterPrio = checkIntParam(args,++i);
			}
			else if (args[i].equals("-bcoffset")) {
				bcOffset = checkIntParam(args,++i);
			}
			else if (args[i].equals("-bcfile")) {
				bcFile = checkParam(args,++i);
			}
			else if (args[i].equals("-bcjson")) {
				bcJson = checkParam(args,++i);
			}
			else if (args[i].equals("-c")) {
				conditionsFile = checkParam(args,++i);
			}
			else if (args[i].equals("-check")) {
				check = true;
			}
			else if (args[i].equals("-chrM")) {
				correctMc = true;
			}
			else if (args[i].equals("-oml")) {
				oml = checkParam(args,++i);
			}
			else if (args[i].equals("-o")) {
				output = checkParam(args,++i);
			}
			else if (args[i].equals("-unmapped")) {
				unmapped = true;
			}
			// other parameters may be replacements for the table!
		}
		boolean correctM = correctMc; // just for using it within lambda
		
		if (table==null) throw new UsageException("No input file!");
		if (output==null) throw new UsageException("No output file!");
		
		String src = new LineOrientedFile(table).readAllText();
		src = new Jhp(new JS().addParam(args, false)).apply(src);

		MergeObject[] mo = new CsvReaderFactory().createReader(Paths.get(table).getFileName().toString(), new LineIterator(src, "#"))
					.iterateObjects(MergeObject.class).toArray(MergeObject.class);
		
		HashMap<String, Genomic> genomics = EI.wrap(mo).map(m->m.Genome).unique(false).index(g->g, g->Genomic.get(g));
		EI.wrap(mo).forEachRemaining(m->m.g=genomics.get(m.Genome));
				
		Genomic g = Genomic.merge(genomics.values());
		SamMismatchCorrectionBarcodeAnnotator bca = null;
		
		if (bcFile!=null) { 
			
			if (bcJson!=null) {
				DynamicObject barcodes = DynamicObject.parseJson(FileUtils.readAllText(new File(bcJson)));
				String[] bcs = EI.wrap(barcodes.getEntry("condition").asArray()).map(d->d.getEntry("barcode").asString()).toArray(String.class);
				
				bcOffset = barcodes.getEntry("offset").asInt();
				
				bca = new SamMismatchCorrectionBarcodeAnnotator(bcFile, bcOffset, bcs);
			}
			
			else if (bcOffset>=0 && conditionsFile!=null) {
				bca = new SamMismatchCorrectionBarcodeAnnotator(bcFile, bcOffset, conditionsFile);
			}
			
			if (bca!=null) {
				bca.setTotalOut(new LineOrientedFile(FileUtils.getExtensionSibling(output,".barcodecorrection.tsv")).write());
				if (oml!=null) {
					PlaceholderInterceptor pi = new PlaceholderInterceptor();
					pi.addPlaceHolders(param);
	//				if (param.containsKey("run")) pi.addPlaceHolder("run",(String) param.get("run"));
					pi.addPlaceHolder("conditions",conditionsFile);
					pi.addPlaceHolder("folder", new File(output).getAbsoluteFile().getParent());
					bca.startPrograms(
							new OmlNodeExecutor().addInterceptor(new PlaceholderInterceptor(pi).addPlaceHolder("mode","collapsed")).<GenomicRegionFeaturePipeline>execute(new OmlReader().parse(new File(oml))).getProgram(),
							new OmlNodeExecutor().addInterceptor(new PlaceholderInterceptor(pi).addPlaceHolder("mode","corrected")).<GenomicRegionFeaturePipeline>execute(new OmlReader().parse(new File(oml))).getProgram(),
							new OmlNodeExecutor().addInterceptor(new PlaceholderInterceptor(pi).addPlaceHolder("mode","read")).<GenomicRegionFeaturePipeline>execute(new OmlReader().parse(new File(oml))).getProgram()
						);
				}
			}
		}
		
		
		HashMap<String,Object> context = new HashMap<String, Object>();
		context.put("genomic", g);
		
		GenomicRegionFeatureProgram uprioPipeline = prioPipeline==null?null:new OmlNodeExecutor().<GenomicRegionFeaturePipeline>execute(new OmlReader().parse(new File(prioPipeline)),context).getProgram();
		if (uprioPipeline!=null) {
			uprioPipeline.setThreads(0);
			uprioPipeline.begin();
		}
		
		Trie<String> genotrie = new Trie<String>();
		for (String gg : genomic)
			genotrie.put(gg, gg);
		
		boolean uremoveNonStandard = removeNonStandard;
		ExtendedIterator<ReferenceGenomicRegion<MergeData>>[] iterators = 
				EI.wrap(mo).map(m->m.iterator()
						.iff(uremoveNonStandard, ei->ei.filter(rgr->rgr.getReference().isStandard() || !genotrie.getByPrefix(rgr.getReference().getName()).isEmpty()))
						).toArray(new ExtendedIterator[0]);
		
		LineWriter unmappedOut = unmapped?new LineOrientedFile(FileUtils.getFullNameWithoutExtension(output)+".unmapped.fasta").write():null;
		
		MutableMonad<NumericArray> total = new MutableMonad<>();
		int ufilterPrio = filterPrio;
		SamMismatchCorrectionBarcodeAnnotator ubca = bca;
		Comparator<ReferenceGenomicRegion<MergeData>> byId = (a,b)->Integer.compare(a.getData().getId(), b.getData().getId());
		ExtendedIterator<ReferenceGenomicRegion<AlignedReadsData>> it = EI.merge(byId,iterators)
			.iff(correctM,ite->ite.map(r->correctChrM(g,errors,r)).removeNulls())
			.iff(check, ite->ite.parallelizedSideEffect(a->checkSequence(g,errors,a, correctM),1000))
			.progress(progress, -1, r->"Read id: "+r.getData().getId()+" Mem="+StringUtils.getHumanReadableMemory(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()))
			.multiplex(byId, ReferenceGenomicRegion.class)
				.iff(unmappedOut!=null, ite->ite.sideEffect(a->{
					if (EI.wrap(a).filter(r->!r.getReference().equals(Chromosome.UNMAPPED)).count()==0) {
						unmappedOut.writeLine2(a[0].getData().getFasta());					
					}
				}))
				.map(r->retainMinMismatchByPriority(ufilterPrio, r,uprioPipeline))
				.sideEffect(MergeSam::annotateMultiplicity)
			.demultiplex(EI::wrap)
			.sort(new MergeDataSerializer())
			.progress(progress, -1, r->"Location: "+r.toLocationString())
			.multiplex(MergeSam::compareWoUnmapped, MergeSam::mergeRegions)
			.iff(bca!=null,ite->ite.map(ubca::transform).removeNulls())
			
			.filter(r->!r.getReference().equals(Chromosome.UNMAPPED) && r.getRegion().getTotalLength()>=18)
			.sideEffect(r->{
				total.Item = r.getData().addTotalCountsForConditions(total.Item, ReadCountMode.Weight);
			})
			;

		if (unmapped)
			unmappedOut.close();
		
		GenomicRegionStorage p = GenomicRegionStorageExtensionPoint.getInstance().get(DefaultAlignedReadsData.class,output, GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		p.fill(it);
	
		if (bca!=null) {
			bca.finishPrograms();
			bca.finish();
		}

		if (uprioPipeline!=null) {
			uprioPipeline.end();
		}
		
		String genomicJson = genomic==null?"":("\"genomic\": ["+EI.wrap(genomic).map(s->"\""+s+"\"").concat(",")+"],\n");
		
		if (conditionsFile!=null) {
			DynamicObject d = DynamicObject.from(new CsvReaderFactory().createReader(conditionsFile).iterateMap(true).map(m->{m.remove("barcode"); return m;}).toArray());
			DynamicObject t = DynamicObject.parseJson("["+EI.wrap(total.Item.toDoubleArray()).map(tx-> "{\"total\": "+tx+"}").concat(",")+"]");
			d = d.cascade(t);
			String meta = "{"+genomicJson+"\"conditions\":"+d.toJson().replaceAll("\"condition\"","\"name\"")+"}";
			FileUtils.writeAllText(meta, new File(output+".metadata.json"));
		} else if (bcJson!=null) {
			DynamicObject barcodes = DynamicObject.parseJson(FileUtils.readAllText(new File(bcJson)));
			DynamicObject d = barcodes.getEntry("condition");
			DynamicObject t = DynamicObject.parseJson("["+EI.wrap(total.Item.toDoubleArray()).map(tx-> "{\"total\": "+tx+"}").concat(",")+"]");
			d = d.cascade(t);
			String meta = "{"+genomicJson+"\"conditions\":"+d.toJson().replaceAll("\"condition\"","\"name\"")+"}";
			FileUtils.writeAllText(meta, new File(output+".metadata.json"));
		} else {
			String meta = "{"+genomicJson+"\"conditions\":[{\"name\":\""+p.getName()+"\", \"total\": "+(total.Item==null?0:total.Item.getLong(0))+"}]}";
			FileUtils.writeAllText(meta, new File(output+".metadata.json"));
		}
	}

//	private static void test(ReferenceGenomicRegion<MergeData> r,
//			String s) {
//		if (r.getData().getId()==16602963) {
//			System.out.println(s+": "+r);
//		}
//	}

	private static int compareWoUnmapped(ReferenceGenomicRegion<MergeData> a, ReferenceGenomicRegion<MergeData> b) {
		int re = a.compareTo(b);
		if (re==0 && a.getReference().equals(Chromosome.UNMAPPED)) re = -1; // this way multiplexing works
		return re;
	}
	
	private static ReferenceGenomicRegion<AlignedReadsData> mergeRegions(List<ReferenceGenomicRegion<MergeData>> list) {
		if (list.size()==1) {
//			AlignedReadsDataFactory.removeId(list.get(0).getData().ard);
			return new ImmutableReferenceGenomicRegion<AlignedReadsData>(list.get(0).getReference(), list.get(0).getRegion(),list.get(0).getData().ard);
		}
		
		AlignedReadsDataFactory fac = new AlignedReadsDataFactory(list.get(0).getData().ard.getNumConditions());
		fac.start();
		for (ReferenceGenomicRegion<MergeData> e : list) {
//			if (e.getData()println(e);
			fac.add(e.getData().ard, 0);
		}
		
		fac.makeDistinct(); // this is necessary if reads were not collapsed prior to alignment
		if (!fac.areTrulyDistinct()) {
			throw new RuntimeException("Not distinct: "+list.toString());
		}
		
		return new ImmutableReferenceGenomicRegion<AlignedReadsData>(list.get(0).getReference(), list.get(0).getRegion(),fac.create());
	}
	
	private static ReferenceGenomicRegion<MergeData>[] retainMinMismatchByPriority(int filterPrio, ReferenceGenomicRegion<MergeData>[] a, GenomicRegionFeatureProgram prio) {
		// there may be still multiple mappings to the same location! (e.g. due to transcriptomic to genomic)
		if (a.length==1) return a;
		
		
		int[] prios = new int[a.length];
		for (int i=0; i<a.length; i++) {
			
			if (prio!=null) {
				prio.accept(a[i]);
				String p = prio.getDataAsSingleton("Priority","").toString();
				if (!StringUtils.isInt(p))
					prios[i] = a[i].getData().getPriority(a[i]);
				else 
					prios[i] = Integer.parseInt(p);
			} else
				prios[i] = a[i].getData().getPriority(a[i]);
		}
		
		int priority = ArrayUtils.min(prios);
		if (priority==filterPrio)
			return null;
		
		int min = Integer.MAX_VALUE;
		int count = 0;
		for (int i=0; i<a.length; i++) {

			
			if (prios[i]==priority) {
				int v = a[i].getData().ard.getVariationCount(0);
				if (v<min)
					count = 0;
				if (v<=min) {
					count++;
					min = v;
				}
			}
		}
		if (count<a.length) {
			ReferenceGenomicRegion<MergeData>[] re = new ReferenceGenomicRegion[count];
			count = 0;
			for (int i=0; i<a.length; i++) {
				ReferenceGenomicRegion<MergeData> r = a[i];
				if (prios[i]==priority && min==r.getData().ard.getVariationCount(0))
					re[count++] = r;
			}
			
			Arrays.sort(re,FunctorUtils.naturalComparator());
			count = ArrayUtils.unique(re,FunctorUtils.naturalComparator());
			if (count<re.length)
				re = ArrayUtils.redimPreserve(re, count);
			
			return re;
		}
		
//		for obvious reasons check does not work with this:
//		int fmin = min;
//		int count = ArrayUtils.remove(a, r->r.getData().getVariationCount(0)==fmin);
//		if (count<a.length)
//			a = ArrayUtils.redimPreserve(a, count);
		
		Arrays.sort(a,FunctorUtils.naturalComparator());
		count = ArrayUtils.unique(a,FunctorUtils.naturalComparator());
		if (count<a.length)
			a = ArrayUtils.redimPreserve(a, count);
		return a;
	}
	
	private static Function<DefaultAlignedReadsData, Object> mgetter = Orm.getFieldGetter(DefaultAlignedReadsData.class, "multiplicity");
	
	private static void annotateMultiplicity(ReferenceGenomicRegion<MergeData>[] a) {
		
		
		for (ReferenceGenomicRegion<MergeData> r : a) {
//			if (r.getData().getId(0)==10923808)
//				System.out.println(r);
			int[] multiplicity = (int[]) mgetter.apply((DefaultAlignedReadsData) r.getData().ard);
			if (r.getReference().equals(Chromosome.UNMAPPED))
				multiplicity[0] = 0;
			else
				multiplicity[0] = a.length;
		}
	}
	
	private static ReferenceGenomicRegion<MergeData> correctChrM(Genomic g, ErrorProtokoll errors, ReferenceGenomicRegion<MergeData> r) {
		
		if (r.getReference().getName().equals("chrM")) {
		
			String read = r.getData().readSequence;
			
			CharSequence gseq = g.getSequence(r);
			if (gseq==null)
				errors.addError("No genomic sequence available",r.getReference());
			else {
				CharSequence rseq = r.getData().ard.genomeToRead(0, gseq);
				if (!read.equals(rseq)){ // translate position!
					ImmutableReferenceGenomicRegion<MergeData> r2 = new ImmutableReferenceGenomicRegion<MergeData>(r.getReference(), r.getRegion().translate(1),r.getData());
					
					read = r2.getData().readSequence;
					
					try {
						gseq = g.getSequence(r2);
					} catch (IndexOutOfBoundsException e) {
						return null;
					}
					if (gseq==null)
						errors.addError("No genomic sequence available",r2.getReference());
					else {
						rseq = r2.getData().ard.genomeToRead(0, gseq);
						if (read.equals(rseq)){ // ok!
							return r2;
						} else {
//							errors.addError("Sequences do not match",r, "Even after correcting for chrM, Sequences do not match for "+r+": Genome="+rseq+" SAM="+read);
							return null;
						}
					}
				}
			}
			
		}
		
		return r;
	}
	
	private static void checkSequence(Genomic g, ErrorProtokoll errors,
			ReferenceGenomicRegion<MergeData> a, boolean ignoreM) {
		String read = a.getData().readSequence;
		
		CharSequence gseq = g.getSequence(a);
		if (gseq==null)
			errors.addError("No genomic sequence available",a.getReference());
		else {
			CharSequence rseq = a.getData().ard.genomeToRead(0, gseq);
			if (!read.equals(rseq) && (!ignoreM || !a.getReference().getName().equals("chrM"))) {
				errors.addError("Sequences do not match",a, "Sequences do not match for "+a+": Genome="+rseq+" SAM="+read);
			}
		}
	}

	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("MergeSam <Options> <input>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -p\t\t\tShow progress");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println();
		
	}
	
//	
//	private static class SequenceBuffer {
//		private TreeMap<Integer,String> idToSeq = new TreeMap<Integer, String>();
//		
//		public void put(SAMRecord r) {
//			int id = Integer.parseInt(r.getReadName());
//			String seq = r.getReadString();
//			if (r.getReadNegativeStrandFlag())
//				seq = SequenceUtils.getDnaReverseComplement(seq);
//			synchronized(this) {
//				idToSeq.put(id, seq);
//			}
//		}
//		public String getAndShrink(int id) {
//			synchronized(this) {
//				Iterator<Integer> it = idToSeq.keySet().iterator();
//				while (it.hasNext() && it.next()<id) 
//					it.remove();
//				return idToSeq.get(id);
//			}
//		}
//
//	}
	
	private static class MergeObject {
		public String File;
		public String Genome;
		public boolean Transcriptomic;
		public int Priority;
		
		private Genomic g;
		
		public ExtendedIterator<ReferenceGenomicRegion<MergeData>> iterator() {
			ExtendedIterator<ReferenceGenomicRegion<MergeData>> re = SamToRegion.iterate(File, (ard,r)->new MergeData(ard,r.getReadString(),Priority));
			if (Transcriptomic) {
				re = re.map(g::transcriptToGenome).removeNulls();
//				if (Genome.equals("h.ens75")) // workaround for shift between ensembl and ucsc assembly in chrM: just do not map, i.e. loose all junction reads
//					re = re.filter(r->!r.getReference().getName().equals("chrM"));
			}
			return re;
		}
	}
	
	private static class MergeData {
		public DefaultAlignedReadsData ard;
		public String readSequence;
		public int priority;
		
		public MergeData(DefaultAlignedReadsData ard, String readSequence, int priorityFromFile) {
			this.ard = ard;
			this.readSequence = readSequence;
			this.priority = priorityFromFile;
			if (this.priority==Integer.MAX_VALUE) throw new RuntimeException("Use smaller priorities than "+Integer.MAX_VALUE);
		}

		public String getFasta() {
			return ">"+ard.getId(0)+"\n"+readSequence;
		}

		public int getPriority(ReferenceGenomicRegion<MergeData> r) {
			if (r.getReference().equals(Chromosome.UNMAPPED))
				return Integer.MAX_VALUE;
			return priority;
		}

		public int getId() {
			return ard.getId(0);
		}
		
		@Override
		public String toString() {
			return ard.toString();
		}
		
		@Override
		public int hashCode() {
			return ard.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof MergeData)) return false;
			MergeData o = (MergeData)obj;
			return ard.equals(o.ard);
		}
	}
	
	
//	private static class EntropyMultimappingEvaluator implements Consumer<ReferenceGenomicRegion<MergeData>> {
//
//		private Binning binning;
//		private Counter<Factor> counter;
//		
//		public EntropyMultimappingEvaluator() {
//			binning = new FixedSizeBinning(0, 4, 100);
//			counter = new Counter<Factor>("All",">=2 Locations",">=3 Locations",">=5 Locations",">=10 Locations");
//		}
//		
//		@Override
//		public void accept(ReferenceGenomicRegion<MergeData> t) {
//			int mm = t.getData().ard.getMultiplicity(0);
//			int d = 0;
//			if (mm>=2) d = 1;
//			if (mm>=3) d = 2;
//			if (mm>=5) d = 3;
//			if (mm>=10) d = 4;
//			counter.addAtMost(binning.apply(Entropy.compute(t.getData().readSequence, 2)),d);
//		}
//		
//		public Counter<Factor> getCounter() {
//			return counter.sort();
//		}
//		
//	}

	private static class MergeDataSerializer implements BinarySerializer<ReferenceGenomicRegion<MergeData>> {

		private HashMap<ReferenceSequence,Short> refMap = new HashMap<ReferenceSequence, Short>();
		private ArrayList<ReferenceSequence> revMap = new ArrayList<ReferenceSequence>();

		@Override
		public Class<ReferenceGenomicRegion<MergeData>> getType() {
			return (Class)ReferenceGenomicRegion.class;
		}

		@Override
		public void beginSerialize(BinaryWriter out) {
			HashMap<String, Object> map = new HashMap<String,Object>();
			map.put(AlignedReadsData.HASIDATTRIBUTE,1);
			map.put(AlignedReadsData.HASWEIGHTATTRIBUTE,0);
			map.put(AlignedReadsData.HASGEOMETRYATTRIBUTE,0);
			map.put(AlignedReadsData.CONDITIONSATTRIBUTE,1);
			out.getContext().setGlobalInfo(DynamicObject.from(map));
		}
		
		@Override
		public void beginDeserialize(BinaryReader in) {
			HashMap<String, Object> map = new HashMap<String,Object>();
			map.put(AlignedReadsData.HASIDATTRIBUTE,1);
			map.put(AlignedReadsData.HASWEIGHTATTRIBUTE,0);
			map.put(AlignedReadsData.HASGEOMETRYATTRIBUTE,0);
			map.put(AlignedReadsData.CONDITIONSATTRIBUTE,1);
			in.getContext().setGlobalInfo(DynamicObject.from(map));
		}
		
		@Override
		public void serialize(
				BinaryWriter out,
				ReferenceGenomicRegion<MergeData> object)
				throws IOException {
			short rid = refMap.computeIfAbsent(object.getReference(), r->(short)refMap.size());
			if (rid>=revMap.size()) revMap.add(object.getReference());
			
			out.putCShort(rid);
			FileUtils.writeGenomicRegion(out, object.getRegion());
			
			object.getData().ard.serialize(out);
			// sequence and priority arent needed after sorting
		}

		@Override
		public ReferenceGenomicRegion<MergeData> deserialize(
				BinaryReader in) throws IOException {
			int rid = in.getCShort();
			ArrayGenomicRegion reg = FileUtils.readGenomicRegion(in);
			DefaultAlignedReadsData ard = new DefaultAlignedReadsData();
			ard.deserialize(in);;
			MergeData d = new MergeData(ard, null, -1);
			
			return new ImmutableReferenceGenomicRegion<MergeData>(
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
