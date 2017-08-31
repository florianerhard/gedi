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
import java.util.HashMap;

import gedi.app.Gedi;
import gedi.core.region.feature.output.PlotReport;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
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
		System.err.println("FastqFilter [-ld <file>] [-min <length>] <input.fastq>");
		System.err.println();
		System.err.println("Filters fastq files for read length, writes length distribution and reindexes reads (integral).");
		System.err.println("Options:");
		System.err.println(" -min <length>\t\t\tMinimal length to keep reads (default: 18)");
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
		
		
		int len = 18;
		String ld = null;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-min")) {
				len = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-ld")) {
				ld = checkParam(args, ++i);
			}
			else if (args[i].equals("-D"))
			{}
			else
				break;
		}
		
		String inp = checkParam(args, i);

		Counter<Integer> histo = new Counter<>("Read length",1);
		
		int n = 0;
		LineIterator it = new LineOrientedFile(inp).lineIterator();
		while (it.hasNext()) {
			it.next();
			String seq = it.next();
			it.next();
			String q = it.next();
			histo.add(seq.length());
			if (seq.length()>=len) {
				System.out.print("@");
				System.out.println(n++);
				System.out.println(seq);
				System.out.println("+");
				System.out.println(q);
			}
		}
		
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
			PlotReport pr = new PlotReport("Trimmed reads", StringUtils.toJavaIdentifier(inp+"_fastqfilter"), title, "Distribution of read lengths after adapter trimming", png, null, ld);
			FileUtils.writeAllText(DynamicObject.from("plots",new Object[] {pr}).toJson(), new File(FileUtils.getExtensionSibling(ld,"report.json")));
		}
	}
}
