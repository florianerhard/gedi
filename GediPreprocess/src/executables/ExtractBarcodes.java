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
import java.util.ArrayList;
import java.util.HashMap;

import gedi.app.Gedi;
import gedi.core.region.feature.output.PlotReport;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.diskarray.VariableSizeDiskArrayBuilder;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.io.randomaccess.serialization.BinarySerializer;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.jhp.TemplateEngine;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.math.stat.counting.Counter;
import gedi.util.nashorn.JS;
import gedi.util.plotting.Aes;
import gedi.util.plotting.GGPlot;
import gedi.util.r.RRunner;
import gedi.util.sequence.CountDnaSequence;
import gedi.util.userInteraction.log.ErrorProtokoll;
import gedi.util.userInteraction.log.LogErrorProtokoll;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class ExtractBarcodes {

	

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
		System.err.println("ExtractBarcodes [-l <length>] [-json <json>] <input.fastq> <prefix>");
		System.err.println();
		System.err.println("Extracts barcodes into a binary file <prefix>.barcodes, collapses all equal reads into <prefix>.fastq and writes barcode statistics (-bd) while optinally extracting names from the json file.");
		System.err.println("Options:");
		System.err.println(" -l <len>\t\t\t5' barcode length (otherwise take from json)");
		System.err.println(" -json <file>\t\t\tjson file to extract barcode names");
		System.err.println(" -p\t\t\tShow progress");
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
		
		Progress progress = new NoProgress();
		int len = -1;
		int offset = -1;
		HashMap<String,String> barcodeMap = new HashMap<>();
		
		if (args.length<2) {
			usage("Not enough parameters");
			System.exit(1);
		}
		
		String inp = args[args.length-2];
		String prefix = args[args.length-1];
		
		int i;
		for (i=0; i<args.length-2; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-l")) {
				len = checkIntParam(args, ++i);
			}
			else if (args[i].equals("-p")) {
				progress = new ConsoleProgress(System.err);
			}
			else if (args[i].equals("-json")) {
				DynamicObject barcodes = DynamicObject.parseJson(FileUtils.readAllText(new File(checkParam(args, ++i))));
				if (barcodes.hasProperty("datasets")) 
					barcodes = EI.wrap(barcodes.getEntry("datasets").asArray())
					.filter(d->new File(d.getEntry("fastq").asString()).equals(new File(inp)))
					.getUniqueResult("More than one element for the given file in json!", "File not found in json!")
					.getEntry("barcodes");
				
				EI.wrap(barcodes.getEntry("condition").asArray())
					.toMap(barcodeMap,d->d.getEntry("barcode").asString(),d->d.getEntry("name").asString());
				
				len = barcodes.getEntry("length").asInt();
				offset = barcodes.getEntry("offset").asInt();
			}
			else if (args[i].equals("-D"))
			{}
			else
				break;
		}
		
		if (len==-1) throw new UsageException("No length given!");
		
		VariableSizeDiskArrayBuilder<CountDnaSequence> builder = new VariableSizeDiskArrayBuilder<>(prefix+".barcodes");
		LineWriter out = new LineOrientedFile(prefix+".fastq").write();
		
		Counter<String> counter = new Counter<>("Barcode",1);
		
		int sub = -1;
		if (!barcodeMap.isEmpty()) sub = barcodeMap.keySet().iterator().next().length();
		
		pos = len;
		int n = 0;
		for (ArrayList<String> l : new LineOrientedFile(inp).lineIterator()
				.pattern(false,true,false,false)
				.filter(s->s.length()>=pos)
				.sort(new StringSerializer(),ExtractBarcodes::compareSeq)
				.progress(progress, -1, s->"Processing read "+s)
				.fold(ExtractBarcodes::compareSeq, ()->new ArrayList<String>(), (s,l)->{
					l.add(s);
					return l;
				})
			.loop()) {
			
			for (String s : l) {
				String bc = s.substring(0,len);
				if (offset!=-1) bc = bc.substring(offset);
				if (sub!=-1) bc = bc.substring(0,sub);
				bc = barcodeMap.getOrDefault(bc, bc);
				counter.add(bc);
			}
				
			// replace N by random base
			for (i=0; i<l.size(); i++) {
				if (l.get(i).substring(0,len).contains("N")) {
					char[] a = l.get(i).toCharArray();
					for (int p=0; p<len; p++)
						if (a[p]=='N')
							a[p] = SequenceUtils.nucleotides[RandomNumbers.getGlobal().getUnif(0, 4)];
					l.set(i, String.valueOf(a));
				}
			}
			l.sort(ExtractBarcodes::compareBc);
			builder.add(
				EI.wrap(l)
				.fold(ExtractBarcodes::compareBc, 
						()->new SeqCount(), 
						(seq,sc)->sc.add(seq))
				.map(s->s.get())
				);
			
			out.writeLine("@"+n++);
			out.writeLine(l.get(0).substring(len));
			out.writeLine("+");
			out.writeLine(StringUtils.repeat("I", l.get(0).length()-len));
			
		}
		
		out.close();
		builder.finish();
		
		
		counter.sort();
		FileUtils.writeAllText(counter.toString(),new File(prefix+".tsv"));
		String png = prefix+".png";
		String title = FileUtils.getNameWithoutExtension(png);
		
		TemplateEngine tem = new TemplateEngine();
		tem.parameter("png", png);
		tem.parameter("tsv", prefix+".tsv");
		tem.parameter("length", sub+"");
		tem.push(prefix+".R");
		tem.template("/resources/R/barcodes.R");
		tem.finish();
		
		RRunner r = new RRunner(prefix+".R");
		r.run(false);
		
		PlotReport pr = new PlotReport("Barcodes", StringUtils.toJavaIdentifier(inp+"_barcodes"), title, "Distribution of barcodes to reads.", png, prefix+".R", prefix+".tsv");
		FileUtils.writeAllText(DynamicObject.from("plots",new Object[] {pr}).toJson(), new File(prefix+".report.json"));
	}
	
	
	private static class SeqCount {
		public String seq;
		public int count;
		public CountDnaSequence get() {
			return new CountDnaSequence(seq, count);
		}
		public SeqCount add(String seq) {
			this.seq = seq.substring(0,pos);
			count++;
			return this;
		}
	}
	

	private static int pos;
	public static int compareSeq(String a, String b) {
		int len1 = a.length();
        int len2 = b.length();
        int lim = Math.min(len1, len2);

        int k = pos;
        while (k < lim) {
            char c1 = a.charAt(k);
            char c2 = b.charAt(k);
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
	}
	
	public static int compareBc(String a, String b) {
		int len1 = a.length();
        int len2 = b.length();

        int k = 0;
        while (k < pos) {
            char c1 = a.charAt(k);
            char c2 = b.charAt(k);
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
	}
	
	public static class StringSerializer implements BinarySerializer<String> {

		@Override
		public Class<String> getType() {
			return String.class;
		}

		@Override
		public void serialize(BinaryWriter out, String object) throws IOException {
			out.putString(object);
		}

		@Override
		public String deserialize(BinaryReader in) throws IOException {
			return in.getString();
		}

		@Override
		public void serializeConfig(BinaryWriter out) throws IOException {
		}

		@Override
		public void deserializeConfig(BinaryReader in) throws IOException {
		}
		
	}
}
