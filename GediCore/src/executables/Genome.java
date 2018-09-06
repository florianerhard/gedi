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

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

import gedi.app.Gedi;
import gedi.core.genomic.Genomic;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.util.ArrayUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.formats.BedEntry;
import gedi.util.mutable.MutableTuple;
import gedi.util.nashorn.JSFunction;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.Progress;


public class Genome {

	
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
	
	
	private static String checkParam(String[] args, int index) throws UsageException {
		if (index>=args.length) throw new UsageException("Missing argument for "+args[index-1]);
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
	private static int checkMultiParam(String[] args, int index, ArrayList<String> re) throws UsageException {
		while (index<args.length && !args[index].startsWith("-")) 
			re.add(args[index++]);
		return index-1;
	}
	public static void start(String[] args) throws Exception {
		Gedi.startup();
		
		LineOrientedFile output = new LineOrientedFile(LineOrientedFile.STDOUT);
		
		boolean progress = false;

		String input = LineOrientedFile.STDOUT;
		boolean header = false;
		String separator = "\t";
		String locationexpression = "f[0]";
		String nameexpression = "";
		int flank = 0;
		boolean flankTolower = false;
		String empty = null;
		SequenceProvider seq = null;
		int w = 0;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-p")) {
				progress=true;
			}
			else if (args[i].equals("-H")) {
				header=true;
			}
			else if (args[i].equals("-g")) {
				ArrayList<String> names = new ArrayList<>();
				i = checkMultiParam(args, ++i, names);
				seq = Genomic.get(names);
			}
			else if (args[i].equals("-e")) {
				empty = checkParam(args,++i);
			}
			else if (args[i].equals("-f")) {
				flank = checkIntParam(args,++i);
				flankTolower = flank<0;
				flank = Math.abs(flank);
			}
			else if (args[i].equals("-w")) {
				w = checkIntParam(args,++i);
			}
			else if (args[i].equals("-l")) {
				locationexpression = checkParam(args,++i);
			}
			else if (args[i].equals("-o")) {
				output = new LineOrientedFile(checkParam(args,++i));
			}
			else if (args[i].equals("-n")) {
				nameexpression = checkParam(args,++i);
			}
			else if (args[i].equals("-s")) {
				separator = ParseUtils.parseDelimiter(checkParam(args,++i));
			}
			else if (args[i].equals("-D")){} 
			else if (!args[i].startsWith("-")) 
					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
		}
		
		if (i==args.length-1) 
			input = args[i++];
		if (i!=args.length) throw new UsageException("Unknown parameter "+args[i]);
		if (seq==null) throw new UsageException("No genome given!");
		
		
		BiConsumer<String[],MutableReferenceGenomicRegion> mapper = null;

		LineIterator lit = new LineOrientedFile(input).lineIterator();
		HeaderLine h = null;
		if (header) h = new HeaderLine(lit.next());
	
		MutableTuple tup = new MutableTuple(String[].class, HeaderLine.class).set(1,h);
		
		
		// configure location mapping
		if (locationexpression.equalsIgnoreCase("BED")) {
			mapper = (line, re)->BedEntry.parseValues(line).getReferenceGenomicRegion(re);
			separator = "\t";
			header = false;
		} else {
			JSFunction<MutableTuple, String> map1 = new JSFunction<MutableTuple, String>("function(f,h) "+locationexpression);
			mapper = (line,re)->re.set(new MutableReferenceGenomicRegion<String>().parse(map1.apply(tup.set(0, line)))); 
		}
		
		Function<MutableTuple,String> nameMapper = null;
		// configure location mapping
		if (!nameexpression.equals("")) {
			nameMapper = new JSFunction<MutableTuple, String>("function(f,h) "+nameexpression);
		}
				
	
		MutableReferenceGenomicRegion rgr = new MutableReferenceGenomicRegion();
		
		output.startWriting();
		
		Progress pro = progress?new ConsoleProgress(System.err):null;
		
		if (progress) pro.init();
	
		while (lit.hasNext()) {
			String[] f = StringUtils.split(lit.next(), separator);
			mapper.accept(f, rgr);
			String name = nameMapper!=null?nameMapper.apply(tup.set(0, f)):rgr.toLocationString();
			
			if (rgr.getReference()==null) {
				if (empty!=null)
					output.writef(">%s\n%s\n",name,empty);
			}
			else {
			
				CharSequence s = seq.getSequence(rgr.getReference(), rgr.getRegion().extendBack(flank).extendFront(flank));
				if (flankTolower)
					s = s.subSequence(0, flank).toString().toLowerCase()+s.subSequence(flank, s.length()-flank).toString()+s.subSequence(s.length()-flank, s.length()).toString().toLowerCase();
				
				if (w>0) {
					StringBuilder sb = new StringBuilder();
					for (int p=0; p<s.length(); p+=w)
						sb.append(s.subSequence(p, Math.min(s.length(),p+w))).append(p+w<s.length()?"\n":"");
					s = sb;
				}
				
				output.writef(">%s\n%s\n",name,s);
			}
			
			if (progress) pro.incrementProgress();	
		}
		output.finishWriting();
		if (progress) pro.finish();
		
	}

	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("Genome <Options> [<input>]");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -g <genome ...>\t\tGenome names");
		System.err.println(" -w <width>\t\tFasta line width (Default: 0 = no line breaks)");
		System.err.println(" -f <width>\t\tappend flanking sequences (negative values: to lower case!)");
		System.err.println(" -l <expression>\t\tLocation expression; javascript function body, f is array of fields; can be \"BED\" (Default: f[0])");
		System.err.println(" -n <expression>\t\tName expression; javascript function body, f is array of fields; (Default: nothing = location)");
		System.err.println(" -e message\t\tif Location cannot be parsed, report message instead of silently skipping it");
		System.err.println(" -s <delimiter>\t\tField delimiter (Default: \\t");
		System.err.println(" -o <file>\t\tSpecify output file (Default: stdout)");
		System.err.println(" -H\t\t\tFile has header");
		System.err.println(" -p\t\t\tShow progress");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println();
		
	}
	
}
