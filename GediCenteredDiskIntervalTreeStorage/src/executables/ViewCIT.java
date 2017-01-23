package executables;

import gedi.app.Gedi;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.longcollections.LongArrayList;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.TriFunction;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.tsv.formats.BedEntry;
import gedi.util.nashorn.JSTriFunction;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class ViewCIT {

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
	public static <T> void start(String[] args) throws Exception {
		Gedi.startup();
		
		LineOrientedFile output = new LineOrientedFile(LineOrientedFile.STDOUT);
		
		Progress progress = new NoProgress();
		String query = null;
		String nameExpr = null;
		String scoreExpr = null;
		CitOutputMode mode = null;
		
		boolean list = false;
				
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-l")) {
				list=true;
			}
			else if (args[i].equals("-p")) {
				progress = new ConsoleProgress(System.err);
			}
			else if (args[i].equals("-q")) {
				query=checkParam(args, ++i);
			}
			else if (args[i].equals("-m")) {
				mode = ParseUtils.parseEnumNameByPrefix(checkParam(args, ++i), true, CitOutputMode.class);
			}
			else if (args[i].equals("-name")) {
				nameExpr=checkParam(args, ++i);
			}
			else if (args[i].equals("-score")) {
				scoreExpr=checkParam(args, ++i);
			}
			else if (args[i].equals("-o")) {
				output = new LineOrientedFile(checkParam(args, ++i));
			}
			else if (args[i].equals("-D")){} 
			else if (!args[i].startsWith("-")) 
					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
		}
		
		
		if (!new File(args[i]).exists()) {
			usage("File "+args[i]+" does not exist!");
			System.exit(1);
		}
		
		CenteredDiskIntervalTreeStorage<T> storage = new CenteredDiskIntervalTreeStorage<T>(args[i]);
		
		if (list) {
			output.startWriting();
			output.writeLine(args[i]+": "+storage.getType().getName());
			ReferenceSequence[] refs = (ReferenceSequence[]) storage.getReferenceSequences().toArray(new ReferenceSequence[0]);
			Arrays.sort(refs);
			for (ReferenceSequence r : refs) {
				output.writef("%s%s\t%d\n",r.getName(),r.getStrand(),storage.size(r));
			}
			output.writeLine("----------------");
			output.writef("Total\t%d\n",storage.size());
			output.finishWriting();
			return;
		}
		
		if (mode==CitOutputMode.Cit && output.isPipe())
			throw new UsageException("For mode Cit, specify output file!");
		
		
		LineWriter wr = mode!=CitOutputMode.Cit?output.write():null;
		CenteredDiskIntervalTreeStorage citout = mode==CitOutputMode.Cit?new CenteredDiskIntervalTreeStorage(output.getPath(), storage.getType()):null;
		
		
		ExtendedIterator<ImmutableReferenceGenomicRegion<T>> it = storage.ei(query);
		if (!(progress instanceof NoProgress))
			it = it.progress(progress, (int) storage.size(), r->r.toMutable().transformRegion(reg->reg.removeIntrons()).toLocationString());
		
		
		Consumer<ImmutableReferenceGenomicRegion<T>> sink = null;
		
		if (storage.getType()==LongArrayList.class && new File(StringUtils.removeFooter(args[0], ".cit")).exists() && (mode==null || mode==CitOutputMode.Indexed)) {
			PageFile ext = new PageFile(StringUtils.removeFooter(args[0], ".cit"));
			sink = rgr->{
				try {
					wr.writeLine(rgr.toLocationString());
					LongArrayList l = (LongArrayList) rgr.getData();
					for (int j=0; j<l.size(); j++) {
						ext.position(l.getLong(j));
					
						wr.writeLine(ext.readLine());
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				
			};
			it = it.endAction(()->{
				try {
					ext.close();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			});
		}
		else if (mode==CitOutputMode.Bed) {

			Gedi.startup(true);
			TriFunction<T, ReferenceSequence, GenomicRegion, Double> scorer = scoreExpr!=null?new JSTriFunction<T, ReferenceSequence, GenomicRegion, Double>(false,"function(d,ref,reg) "+scoreExpr):(d,ref,reg)->0.0;
			TriFunction<T, ReferenceSequence, GenomicRegion, String> namer = nameExpr!=null?new JSTriFunction<T, ReferenceSequence, GenomicRegion, String>(false,"function(d,ref,reg) "+nameExpr):(d,ref,reg)->".";
			
			sink = rgr->{
				try {
					Object n = namer.apply(rgr.getData(), rgr.getReference(), rgr.getRegion());
					ScoreNameAnnotation sc = new ScoreNameAnnotation(
							StringUtils.toString(n),
							scorer.apply(rgr.getData(), rgr.getReference(), rgr.getRegion()));
					wr.writeLine(new BedEntry(new ImmutableReferenceGenomicRegion<ScoreNameAnnotation>(rgr.getReference(), rgr.getRegion(), sc)).toString());
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			};
			
		}
		else if (mode==CitOutputMode.Cit) {
			citout.fill(it);
		}
		else {
			TriFunction<T, ReferenceSequence, GenomicRegion, String> namer = nameExpr!=null?new JSTriFunction<T, ReferenceSequence, GenomicRegion, String>(false,"function(d,ref,reg) "+nameExpr):(d,ref,reg)->d.toString();
			
			sink = rgr->{
				try {
					Object n = namer.apply(rgr.getData(), rgr.getReference(), rgr.getRegion());
					wr.writeLine(rgr.toLocationString()+"\t"+StringUtils.toString(n));
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			};
		}
		
		if (sink!=null)
			it.forEachRemaining(sink);
		
		if (wr!=null)
			wr.close();
	}

	
	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("ViewCIT <Options> <file>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -l \t\tOnly list the number of elements per reference");
		System.err.println(" -q <location>\t\tOnly output elements overlapping the given query (i.e. whole reference or genomic region)");
		System.err.println(" -o <file>\t\tSpecify output file (Default: stdout)");
		System.err.println(" -m <mode>\t\toutput mode: Bed/Location/Cit (Default: location)");
		System.err.println(" -name <js>\t\tjavascript function body to generate name (variable d is current data, ref the reference, reg the region)");
		System.err.println(" -score <js>\t\tjavascript function body to generate score (variable d is current data, ref the reference, reg the region)");
		System.err.println();
		System.err.println(" -p\t\t\tShow progress");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println();
		
	}
	
	
	private enum CitOutputMode {
		Location,Bed,Indexed,Cit
	}
	
}
