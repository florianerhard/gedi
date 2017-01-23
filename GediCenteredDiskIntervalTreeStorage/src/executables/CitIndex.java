package executables;

import gedi.app.Gedi;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.BinarySerializableArrayList;
import gedi.util.datastructure.collections.longcollections.LongArrayList;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.formats.BedEntry;
import gedi.util.mutable.MutableTuple;
import gedi.util.nashorn.JSFunction;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.Progress;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

public class CitIndex {

	
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
		
		
		boolean progress = false;

		String input = null;
		boolean header = false;
		boolean silent = false;
		boolean unique = false;
		String separator = "\t";
		String locationexpression = "f[0]";
		String dataexpression = "LOC";
		
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
			else if (args[i].equals("-S")) {
				silent=true;
			}
			else if (args[i].equals("-l")) {
				locationexpression = checkParam(args,++i);
			}
			else if (args[i].equals("-d")) {
				dataexpression = checkParam(args,++i);
			}
			else if (args[i].equals("-u")) {
				unique = true;
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
		
		if (input==null) throw new UsageException("No input file!");
		
		Gedi.startup(true);
		
		BiConsumer<String[],MutableReferenceGenomicRegion> mapper = null;
		BiConsumer<String[],MutableReferenceGenomicRegion> dmapper = null;

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
		
		if (!dataexpression.equalsIgnoreCase("LOC")) {
			JSFunction<MutableTuple, Object> map1 = new JSFunction<MutableTuple, Object>("function(f,h) "+dataexpression);
			dmapper = (line,re)->re.setData(map1.apply(tup.set(0, line))); 
		}
		
		
		CitIndexOutput output = new CitIndexOutput();
		output.unique = unique;
		MutableReferenceGenomicRegion<Object> rgr = new MutableReferenceGenomicRegion<Object>();
		
		Progress pro = progress?new ConsoleProgress(System.err):null;
		
		if (progress) pro.init();
		
		while (lit.hasNext()) {
			String[] f = StringUtils.split(lit.next(), separator);
			mapper.accept(f, rgr);
			
			if (rgr.getReference()==null) {
				if (!silent)
					System.out.println("Cannot parse location for "+StringUtils.concat(separator, f));
			}
			else {
				if (dmapper!=null) {
					dmapper.accept(f, rgr);
					if (!unique) {
						BinarySerializableArrayList<Object> agg = new BinarySerializableArrayList<Object>();
						agg.add(rgr.getData());
						rgr.setData(agg);
					}
				}
				else
					rgr.setData(new LongArrayList(new long[] {lit.getOffset()}));
				
				output.add(rgr);
			}
			
			if (progress) pro.incrementProgress();	
		}
		
		if (output.tmp!=null)
			new CenteredDiskIntervalTreeStorage(input+".cit", output.tmp.getType()).fill(output.tmp);
		
		
		if (progress) pro.finish();
		
	}

	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("CitIndex <Options> <input>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -l <expression>\t\tLocation expression; javascript function body, f is array of fields; can be \"BED\" (Default: f[0])");
		System.err.println(" -d <expression>\t\tData expression; javascript function body, f is array of fields; can be \"LOC\" (Default: LOC, i.e. index the file pointer)");
		System.err.println(" -s <delimiter>\t\tField delimiter (Default: \\t");
		System.err.println(" -u\t\t\tEntries are unique, i.e. elements are not combined into a list for each genomic region");
		System.err.println(" -H\t\t\tFile has header");
		System.err.println(" -S\t\t\tSilent mode");
		System.err.println(" -p\t\t\tShow progress");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println();
		
	}
	
	private static class CitIndexOutput {
		
		boolean unique = false;
		MemoryIntervalTreeStorage<Object> tmp;
		Method adder;
		
		public void add(MutableReferenceGenomicRegion<Object> rgr) {
			if (tmp==null) {
				tmp = new  MemoryIntervalTreeStorage<Object>((Class)rgr.getData().getClass());
				
				for (Method m : rgr.getData().getClass().getDeclaredMethods()) {
					if (m.getName().equals("addAll")&&m.getParameterCount()==1&&m.getParameterTypes()[0].isAssignableFrom(rgr.getData().getClass()))
						adder = m;
				}
				if (adder==null)
					for (Method m : rgr.getData().getClass().getMethods()) {
						if (m.getName().equals("addAll")&&m.getParameterCount()==1&&m.getParameterTypes()[0].isAssignableFrom(rgr.getData().getClass()))
							adder = m;
					}
				
			}
			
			tmp.add(rgr.getReference(), rgr.getRegion(), rgr.getData(), (a,b)->{
				if (unique) throw new RuntimeException("Data not unique for "+rgr.toLocationString());
				try {
					adder.invoke(a, b);
				} catch (Exception e) {
					throw new RuntimeException("Could not invoke addAll!",e);
				}
				return a;
			});
		}
		
		
		
	}
	
}
