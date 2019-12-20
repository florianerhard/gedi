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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import gedi.app.Gedi;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.AlignedReadsVariation;
import gedi.core.data.reads.BarcodedAlignedReadsData;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.feature.output.PlotReport;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.binning.IntegerBinning;
import gedi.util.math.stat.counting.Counter;
import gedi.util.math.stat.factor.Factor;
import gedi.util.plotting.Aes;
import gedi.util.r.RRunner;
import gedi.util.sequence.DnaSequence;
import gedi.util.userInteraction.progress.ConsoleProgress;

public class DedupUMI {

	private static final Logger log = Logger.getLogger( DisplayRMQ.class.getName() );
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
		System.err.println("DedupUMI <Options> file.cit");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -prefix <prefix>\t\t\tprefix for .cit, .dedup.tsv, dedupmm.tsv");
		System.err.println(" -stats \t\t\tProduce stats");
		System.err.println(" -plot \t\t\tProduce stats and plots");
		System.err.println();
		System.err.println(" -h\t\t\tShow this message");
		System.err.println(" -progress\t\t\tShow progress");
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
		Gedi.startup(false);
		
		boolean stats = false;
		boolean plot = false;
		boolean progress = false;
		String prefix = null;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-prefix")) 
				prefix = checkParam(args,++i);
			else if (args[i].equals("-stats")) 
				stats = true;
			else if (args[i].equals("-plot")) 
				stats = plot = true;
			else if (args[i].equals("-progress")) 
				progress = true;
			else if (args[i].equals("-D")) {} 
			else if (!args[i].startsWith("-")) 
					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
		}
		
		String inp = checkParam(args, i++);

		if (!new File(inp).exists()) {
			usage("File "+inp+" does not exist!");
			System.exit(1);
		}

		CenteredDiskIntervalTreeStorage<BarcodedAlignedReadsData> storage = new CenteredDiskIntervalTreeStorage<BarcodedAlignedReadsData>(inp);
		if (storage.getRandomRecord().getClass()!=BarcodedAlignedReadsData.class) throw new UsageException("No UMIs found in file!");
		
		boolean replace = inp.equals(prefix+".cit");
		String outfile = replace?prefix+".tmp.cit":prefix+".cit";
		CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> out = new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>(outfile,DefaultAlignedReadsData.class,false);
		
		int[][][] mm = new int[5][5][3];
		IntArrayList dc = new IntArrayList(100);
		
		ConsoleProgress prog = progress?new ConsoleProgress(System.err):null;
		
		out.fill(storage.ei()
				.iff(progress, ei->ei.progress(prog, (int)storage.size(), l->l.toLocationString()))
				.map(r->dedup(r,dc,mm))
				,prog
				);
		
		out=null;
		if (replace)
			new File(outfile).renameTo(new File(prefix+".cit"));
		
		if (stats) {
			LineWriter dedup = new LineOrientedFile(prefix+".dedup.count.tsv").write();
			dedup.writeLine("Duplication\tCount");
			for (int d=1; d<dc.size(); d++)
				if (dc.getInt(d)>0)
					dedup.writef("%d\t%d\n", d,dc.getInt(d));
			dedup.close();
			
			LineWriter dedupmm = new LineOrientedFile(prefix+".dedup.mm.tsv").write();
			dedupmm.writeLine("Genomic\tRead\tRetainedDedup\tRetainedDup\tTotal");
			for (int g=0; g<mm.length; g++) for (int r=0; r<mm[g].length; r++)
				if (mm[g][r][2]>0)
					dedupmm.writef("%s\t%s\t%d\t%d\t%d\n",SequenceUtils.nucleotides[g],SequenceUtils.nucleotides[r],mm[g][r][0],mm[g][r][1],mm[g][r][2]);
			dedupmm.close();
		}
		
		if (plot) {
			
			try {
				Logger.getGlobal().info("Running R scripts for plotting");
				RRunner r = new RRunner(prefix+".dedup.count.R");
				r.set("file",prefix+".dedup.count.tsv");
				r.set("out",prefix+".dedup.count.png");
				r.addSource(DedupUMI.class.getResourceAsStream("/resources/R/plotdedupcount.R"));
				r.run(false);

				r = new RRunner(prefix+".dedup.mm.R");
				r.set("file",prefix+".dedup.mm.tsv");
				r.set("out",prefix+".dedup.mm.png");
				r.addSource(DedupUMI.class.getResourceAsStream("/resources/R/plotdedupmm.R"));
				r.run(false);
				
				
				PlotReport pr = new PlotReport("Deduplication", StringUtils.toJavaIdentifier(inp+"_dedup"),
						FileUtils.getNameWithoutExtension(inp), "Distribution of deduplication events", prefix+".dedup.count.png", null, prefix+".dedup.count.R", prefix+".dedup.count.tsv");
				PlotReport pr2 = new PlotReport("Deduplication Mismatchcorrection", StringUtils.toJavaIdentifier(inp+"_dedup_Mismatches"),
						FileUtils.getNameWithoutExtension(inp), "Distribution of corrected mismatches", prefix+".dedup.mm.png", null, prefix+".dedup.mm.R", prefix+".dedup.mm.tsv");
				FileUtils.writeAllText(DynamicObject.from("plots",new Object[] {pr,pr2}).toJson(), new File(prefix+".dedup.report.json"));
				
			} catch (Throwable e) {
				Logger.getGlobal().log(Level.SEVERE, "Could not plot!", e);
			}
			
		}
		
	}

	private static ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> dedup(
			ImmutableReferenceGenomicRegion<BarcodedAlignedReadsData> r, IntArrayList dc,
			int[][][] mm) {
		
		ArrayList<DefaultAlignedReadsData> pre = new ArrayList<>();
		
		for (int c=0; c<r.getData().getNumConditions(); c++) {
			HashMap<DnaSequence,ReadMerger> regions = new HashMap<>();
			for (int d=0; d<r.getData().getDistinctSequences(); d++) {
				for (DnaSequence bc : r.getData().getBarcodes(d, c)) {
					ReadMerger p = regions.computeIfAbsent(bc, x->new ReadMerger());
					p.add(r.getData(),d, mm);
				}
			}
			
			for (DnaSequence pp : regions.keySet()) {
				
				ReadMerger p = regions.get(pp);
				
				
				AlignedReadsDataFactory fac = new AlignedReadsDataFactory(r.getData().getNumConditions());
				fac.start();
				fac.newDistinctSequence();
				fac.setMultiplicity(1);
				fac.setCount(c, 1);
				p.addVariations(fac,mm);
				
				dc.increment(p.count);
				
				
				DefaultAlignedReadsData ard = fac.create();
				pre.add(ard);
				if (pre.size()>10) {
					fac = new AlignedReadsDataFactory(r.getData().getNumConditions());
					fac.start();
					for (DefaultAlignedReadsData lard : pre) 
						fac.add(lard, 0);
					fac.makeDistinct();
					pre.clear();
					pre.add(fac.create());
				}
			}
		}
		
		if (pre.size()==1)
			return new ImmutableReferenceGenomicRegion<>(r.getReference(), r.getRegion(), pre.get(0));
		else {
			AlignedReadsDataFactory fac = new AlignedReadsDataFactory(r.getData().getNumConditions());
			fac.start();
			for (DefaultAlignedReadsData ard : pre) 
				fac.add(ard, 0);
			fac.makeDistinct();
			return new ImmutableReferenceGenomicRegion<>(r.getReference(),r.getRegion(),fac.create());
		}
		
		
	}
	
	
	private static class ReadMerger extends HashMap<Integer,Counter<AlignedReadsVariation>>{

		private int count = 0;
		
		public void addVariations(AlignedReadsDataFactory fac, int[][][] mm) {
			for (Integer pos : keySet()) {
				// majority vote
				Counter<AlignedReadsVariation> ctr = get(pos);
				AlignedReadsVariation maxVar = ctr.getMaxElement(0);
				int total = ctr.total()[0];
				if (maxVar!=null && ctr.get(maxVar)[0]>count-total) {
					fac.addVariation(maxVar);
					if (maxVar.isMismatch()) {
						getForVar(mm, maxVar)[0]++;
						getForVar(mm, maxVar)[1]+=ctr.get(maxVar, 0);
					}
				}
			}
		}
		
		private int[] getForVar(int[][][] mm, AlignedReadsVariation var) {
			return mm[SequenceUtils.inv_nucleotides[var.getReferenceSequence().charAt(0)]][SequenceUtils.inv_nucleotides[var.getReadSequence().charAt(0)]];
		}

		public void add(BarcodedAlignedReadsData r, int d, int[][][] mm) {
			count++;
			int vc = r.getVariationCount(d);
			for (int v=0; v<vc; v++) {
				AlignedReadsVariation vari = r.getVariation(d, v);
				if (vari.isDeletion() || vari.isInsertion() || vari.isMismatch()) {
					int pos = vari.getPosition();
					computeIfAbsent(pos, x->new Counter<>()).add(vari);
				}
				if (vari.isMismatch())
					getForVar(mm, vari)[2]++;
			}
		}
		
	}
}
