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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import gedi.app.Gedi;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.feature.output.PlotReport;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.datastructure.dataframe.IntegerDataColumn;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.binning.IntegerBinning;
import gedi.util.math.stat.counting.Counter;
import gedi.util.math.stat.counting.ItemCount;
import gedi.util.math.stat.factor.Factor;
import gedi.util.nashorn.JS;
import gedi.util.plotting.Aes;
import gedi.util.plotting.GGPlot;
import gedi.util.userInteraction.log.ErrorProtokoll;
import gedi.util.userInteraction.log.LogErrorProtokoll;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class FastqFilter {

	

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
	
	
	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("FastqFilter [-ld <file>] [-min <length>] <input.fastq> [<partner.fastq>]");
		System.err.println();
		System.err.println("Filters fastq files for read length, writes length distribution and reindexes reads (integral).");
		System.err.println("Options:");
		System.err.println(" -min <length>\t\t\tMinimal length to keep reads (default: 18)");
		System.err.println(" -extract <from-to>\t\t\tExtract from read (trimming; e.g. 10-60: remove first 10 bases, take only next 50 bases)");
		System.err.println(" -overwrite\t\t\tInstead of writing to stdout, overwrite the input files!");
		System.err.println(" -smartseq\t\t\tRemove the first three bases and trim poly-A stretches (>=7) in the end!");
		System.err.println(" -ld <file>\t\t\tWrite length distribution (and plot it)");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println();
		
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
		
		boolean overwrite = false;
		boolean smartseq = false;
		int len = 18;
		String ld = null;
		ArrayGenomicRegion extract = null;
		int[] umi = null;
		
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-min")) {
				len = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-overwrite")) {
				overwrite = true;
			}
			else if (args[i].equals("-smartseq")) {
				smartseq = true;
			}
			else if (args[i].equals("-umi")) {
				try {
					umi = ArrayUtils.parseIntArray(checkParam(args, ++i),',');
					if (umi.length==1) umi = new int[] {umi[0],0,umi[0],0};
					else if (umi.length==2) umi = new int[] {umi[0],umi[1],umi[0],umi[1]};
					else if (umi.length!=4) throw new RuntimeException();
				} catch (Throwable t) {
					throw new UsageException("umi format: umi-length[,spacer-length[,umi-read2-length,spacer-read2-length][");
				}
			}
			else if (args[i].equals("-extract")) {
				extract = GenomicRegion.parse(checkParam(args, ++i));
			}
			else if (args[i].equals("-ld")) {
				ld = checkParam(args, ++i);
			}
			else if (args[i].equals("-D"))
			{}
			else
				break;
		}
		
		String inp = checkParam(args, i++);
		String inp2 = i<args.length?checkParam(args, i):null;

		Counter<Integer> histo = new Counter<>("Read length",1);
		
		LineWriter out1;
		LineWriter out2;
		Runnable finish = ()->{};
		if(!overwrite) {
			out1 = out2 = new LineOrientedFile().write();
		} else {
			out1 = LineWriter.tmp(true);
			out2 = inp2!=null?LineWriter.tmp(true):null;
			finish = ()->{
				try {
					Files.move(Paths.get(out1.toString()), Paths.get(inp), StandardCopyOption.REPLACE_EXISTING);
					if (inp2!=null)
						Files.move(Paths.get(out2.toString()), Paths.get(inp2), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new RuntimeException("Could not overwrite!",e);
				}
				
			};
		}
		
		int n = 0;
		LineIterator it = new LineOrientedFile(inp).lineIterator();
		if (inp2==null) {
			if (umi!=null) 
				len = len+umi[0]+umi[1];
			while (it.hasNext()) {
				it.next();
				String seq = it.next();
				it.next();
				String q = it.next();
				if (extract!=null) {
					q = SequenceUtils.extractSequence(extract, q);
					seq = SequenceUtils.extractSequence(extract, seq);
				}
				if (smartseq) {
					int trim = Math.min(3, seq.length());
					int pa = countPolyA(seq);
					if (pa<7) pa=0;
					pa = Math.min(pa, seq.length()-trim);
					seq = seq.substring(trim, seq.length()-pa);
					q = q.substring(trim, q.length()-pa);
				}
				if (seq.length()>=len) {
					out1.write("@");
					out1.write(n+++"");
					if (umi!=null) {
						out1.write("#");
						out1.write(seq.substring(0,umi[0]));
						seq=seq.substring(umi[0]+umi[1]);
						q=q.substring(umi[0]+umi[1]);
					}
					out1.writeLine();
					out1.writeLine(seq);
					out1.writeLine("+");
					out1.writeLine(q);
				}
				histo.add(seq.length());
			}
			out1.close();
		}
		else {
			char[] aumi = umi!=null?new char[1+umi[0]+umi[2]]:new char[1];
			aumi[0]='#';
			
			LineIterator it2 = new LineOrientedFile(inp2).lineIterator();
			while (it.hasNext() && it2.hasNext()) {
				it.next();					it2.next();
				String seq1 = it.next(); 	String seq2 = it2.next();
				it.next();					it2.next();
				String q1 = it.next();		String q2 = it2.next();
				histo.add(seq1.length()+seq2.length());
				
				if (extract!=null) {
					q1 = SequenceUtils.extractSequence(extract, q1);
					seq1 = SequenceUtils.extractSequence(extract, seq1);
				}
				
				if (extract!=null) {
					q2 = SequenceUtils.extractSequence(extract, q2);
					seq2 = SequenceUtils.extractSequence(extract, seq2);
				}
				if (seq1.length()+seq2.length()>=len) {
					
					if (umi!=null) {
						seq1.getChars(0, umi[0], aumi, 1);
						seq2.getChars(0, umi[3], aumi, 1+umi[0]);
						seq1=seq1.substring(umi[0]+umi[1]);
						q1=q1.substring(umi[0]+umi[1]);
						seq2=seq2.substring(umi[2]+umi[3]);
						q2=q2.substring(umi[2]+umi[3]);
					}
					
					out1.write("@");
					out1.write(n+"");
					if (umi!=null) out1.write(aumi);
					out1.writeLine();
					out1.writeLine(seq1);
					out1.writeLine("+");
					out1.writeLine(q1);
					
					out2.write("@");
					out2.write(n+++"");
					if (umi!=null) out1.write(aumi);
					out2.writeLine();
					out2.writeLine(seq2);
					out2.writeLine("+");
					out2.writeLine(q2);
				}
			}
			if (it.hasNext() || it2.hasNext())
				throw new RuntimeException("Input files do not match!");
			out1.close(); out2.close();
		}
		
		finish.run();
		
		if (ld!=null) {
			histo.sort();
			FileUtils.writeAllText(histo.toString(),new File(ld));
			String png = FileUtils.getExtensionSibling(ld,"png");
			String title = FileUtils.getNameWithoutExtension(inp);
			DataFrame df = histo.toDataFrame();
			if (df.getIntegerColumn(1).apply(NumericArrayFunction.Max)<10*df.rows()){
				int from = histo.first();
				int to = histo.last();
				IntegerBinning binning = new IntegerBinning(EI.seq(from,to+1,(to-from)/25).iff((to-from+1%25)!=0, ei->ei.chain(EI.wrap(to+1))).toIntArray());
				Counter<Factor> binned = histo.bin(ll->binning.apply(ll.doubleValue()));
				df = binned.toDataFrame();
				df.ggplot(Aes.x(df.getColumn(0).name())).geom_ecdf().rotateLabelsX().png(png);
			}
			df.ggplot(Aes.x(df.getColumn(0).name()),Aes.y(df.getColumn(1).name())).geom_barxy().rotateLabelsX().png(png);
			PlotReport pr = new PlotReport("Trimmed reads", StringUtils.toJavaIdentifier(inp+"_fastqfilter"), title, "Distribution of read lengths after adapter trimming", png, null, null, ld);
			FileUtils.writeAllText(DynamicObject.from("plots",new Object[] {pr}).toJson(), new File(FileUtils.getExtensionSibling(ld,"report.json")));
		}
	}



	private static int countPolyA(String seq) {
		int re = 0;
		for (int i=seq.length()-1; i>=0 && seq.charAt(i)=='A'; i--)
			re++;
		return re;
	}
}
