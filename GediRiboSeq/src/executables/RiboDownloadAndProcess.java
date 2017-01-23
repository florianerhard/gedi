package executables;

import gedi.app.Gedi;
import gedi.core.genomic.Genomic;
import gedi.util.FileUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.jph.Jhp;
import gedi.util.mutable.MutablePair;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class RiboDownloadAndProcess {

	private static final Logger log = Logger.getLogger( EstimateRiboModel.class.getName() );
	public static void main(String[] args) {
		try {
			start(args);
		} catch (UsageException e) {
			usage("An error occurred: "+e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("An error occurred: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private static void usage(String message) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("RiboDownloadAndProcess [options] <json-file>");
		System.out.println();
		System.out.println("If there are cit files in the folder, processing is skipped and only stat and price is executed");
		System.err.println();
		System.err.println("	Options:");
		System.err.println("	-tmp		Specify temp folder");
		System.err.println("	-test		Download only the first 10k reads per SRR");
		System.err.println("	-map		Do the mapping step (if there are geo/srr/fastq entries in the json file, this should be done; only useful to omit that when the cit files are already there; not used for cit entries)");
		System.err.println("	-prio		Do priority mapping (not used for cit entries)");
		System.err.println("	-nopr		Do the not priority mapping (not used for cit entries; if neither prio nor nopr is specified, no cit file will be produced!)");
		System.err.println("	-rescue		Call rescue and do the following steps for the result (if -norescue is not given, the cit file is replaced!)");
		System.err.println("	-norescue	Do the following steps also for the not rescued cit file");
		System.err.println("	-stat		Compute statistics and plots");
		System.err.println("	-price		Call price");
		System.err.println("	-keeptrimmed	Keep the trimmed fastq files");
		System.err.println("");
		System.err.println("	-all		equal to -map -prio -nopr -rescue -norescue -stat -price");
		System.err.println("	-defaults	equal to -map -prio -rescue -stat -price");
		System.err.println("	<omit all>	equal to -map -prio -rescue -stat -price");
		System.err.println("");
		
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
	
	private static enum RefType {
		Genomic(3,false),Transcriptomic(2,true),Both(-1,false),rRNA(1,true);
		
		public int prio;
		public boolean norc;
		private RefType(int prio, boolean norc) {
			this.prio = prio;
			this.norc = norc;
		}
		
		
	}
	
	public static void start(String[] args) throws Exception {
		
		boolean test = false;
		boolean map = false;
		boolean price = false;
		boolean stat = false;
		boolean prio = false;
		boolean nopr = false;
		boolean rescue = false;
		boolean norescue = false;
		boolean keeptrimmed = false;
		String tmp = "tmp/";
		double[] lambdas = new double[0];
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-stat")) {
				stat = true;
			}
			else if (args[i].equals("-all")) {
				stat = map = price = norescue = rescue = prio = nopr = true;
			}
			else if (args[i].equals("-test")) {
				test = true;
			}
			else if (args[i].equals("-map")) {
				map = true;
			}
			else if (args[i].equals("-keeptrimmed")) {
				keeptrimmed = true;
			}
			else if (args[i].equals("-price")) {
				price = true;
			}
			else if (args[i].equals("-tmp")) {
				tmp = checkParam(args, ++i);
			}
			else if (args[i].equals("-norescue")) {
				norescue = true;
			}
			else if (args[i].equals("-nopr")) {
				nopr = true;
			}
			else if (args[i].equals("-prio")) {
				prio = true;
			}
			else if (args[i].equals("-lambdas")) {
				lambdas = StringUtils.parseDouble(StringUtils.split(checkParam(args, ++i), ','));
			}
			else if (args[i].equals("-rescue")) {
				rescue = true;
			}
			else if (args[i].equals("-defaults")) {
				map = price = stat = prio = rescue = true;
			}
			else if (args[i].equals("-D")) {
			}
			else if (!args[i].startsWith("-")) 
					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}
		
		if (i==0) { // no parameters, use defaults
			map = price = stat = prio = rescue = true;
		}
		
		if (i!=args.length-1 || !new File(args[i]).exists())
			throw new UsageException("No Json file given!");
		
		Gedi.startup(true);
		Pattern srr = Pattern.compile("SRR\\d+");
		
		String collection = FileUtils.getNameWithoutExtension(args[i]);
		
		log.info("Reading json");
		DynamicObject json = DynamicObject.parseJson(new LineOrientedFile(args[i]).readAllText());
		
		String startcodon = EI.wrap(json.getEntry("startcodon").asArray()).map(d->EI.wrap(d.asArray()).map(x->x.asString()).concat("/")).concat(" ");
		if (startcodon.length()>0) startcodon = "-t "+startcodon;
		
		log.info("Extract references");
		DynamicObject refs = json.getEntry("references");
		
		ArrayList<ReferenceInfo> infos = new ArrayList<ReferenceInfo>();
		
		for (String r : refs.getProperties()) {
			Genomic genomic = Genomic.get(r);
			RefType type = ParseUtils.parseEnumNameByPrefix(refs.getEntry(r).asString(), true, RefType.class);
			
			if (type==RefType.Both) {
				ReferenceInfo ri = new ReferenceInfo(RefType.Genomic,genomic);
				if (ri.index!=null)
					infos.add(ri);
				ri = new ReferenceInfo(RefType.Transcriptomic,genomic);
				if (ri.index!=null)
					infos.add(ri);
				
			} else {
				ReferenceInfo ri = new ReferenceInfo(type,genomic);
				if (ri.index!=null)
					infos.add(ri);
			}
		}
		
		
		LinkedHashMap<String, MutablePair<String, String[]>> nameToModeAndFiles = new LinkedHashMap<String, MutablePair<String, String[]>>();
		ArrayList<String> scripts = new ArrayList<String>();
		ArrayList<String> cits = new ArrayList<String>();
		ArrayList<String> bams = new ArrayList<String>();
		ArrayList<String> bamnames = new ArrayList<String>();

		if (new File(collection).isDirectory())
			for (File f : new File(collection).listFiles(f->f.getPath().endsWith(".cit"))) {
				log.info("Existing cit file: "+f.getPath());
				cits.add(StringUtils.removeFooter(f.getName(),".cit"));
			}
		
		if (!cits.isEmpty()) {
			log.info("Using existing cit files!");
			map = prio = rescue = false;
			norescue = true;
		}
		else {
			log.info("Collecting read data");
			for (DynamicObject d : json.getEntry("datasets").asArray()) {
				if (d.hasProperty("gsm")) {
					String gsm = d.getEntry("gsm").asString();
					DynamicObject json2 = DynamicObject.parseJson(
							new LineIterator(new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=sra&term="+gsm+"&retmode=json").openStream())
							.concat(""));
					DynamicObject[] arr = json2.get(".esearchresult.idlist").asArray();
					if (arr.length!=1) throw new RuntimeException("Did not get a unique id for "+gsm+": "+json2);
					String xml = new LineIterator(new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=sra&id="+arr[0].asString()+"&rettype=docsum").openStream())
						.concat("");
					String[] srrs = EI.wrap(srr.matcher(xml)).sort().toArray(String.class);
					log.info("SRA entry: Name: "+d.getEntry("name")+" gsm: "+gsm+" id: "+arr[0].asString()+" SRR: "+Arrays.toString(srrs));
					nameToModeAndFiles.put(d.getEntry("name").asString(), new MutablePair<>("SRR",srrs));
					scripts.add(d.getEntry("name").asString());
				} else if (d.hasProperty("sra")) {
					String[] srrs = StringUtils.split(d.getEntry("sra").asString(), ',');
					log.info("SRA entry: Name: "+d.getEntry("name")+" srr: "+srr);
					nameToModeAndFiles.put(d.getEntry("name").asString(), new MutablePair<>("SRR",srrs));
					scripts.add(d.getEntry("name").asString());
				} else if (d.hasProperty("fastq")) {
					String fastq = d.getEntry("fastq").asString();
					log.info("Fastq entry: Name: "+d.getEntry("name")+" fastq: "+fastq);
					nameToModeAndFiles.put(d.getEntry("name").asString(), new MutablePair<>("FASTQ",new String[] {fastq}));
					scripts.add(d.getEntry("name").asString());
				} else if (d.hasProperty("cit")) {
					log.info("Cit entry: Name: "+d.getEntry("name")+" cit: "+d.getEntry("cit").asString());
					cits.add(StringUtils.removeFooter(d.getEntry("cit").asString(),".cit"));
				} else if (d.hasProperty("bam")) {
					log.info("Bam entry: Name: "+d.getEntry("name")+" bam: "+d.getEntry("bam").asString());
					bams.add(d.getEntry("bam").asString());
					bamnames.add(d.getEntry("name").asString());
				}
			}
		}
		
		
		log.info("Writing scripts");
		new File(collection+"/logs").mkdirs();
		new File(collection+"/scripts").mkdirs();
		
		if (nameToModeAndFiles.size()>0) {
			String src = new LineIterator(RiboDownloadAndProcess.class.getResource("ribodownload.sh.jhp").openStream()).concat("\n");
			String oml = new LineIterator(RiboDownloadAndProcess.class.getResource("merge_priority.oml").openStream()).concat("\n");
			Jhp jhp = new Jhp();
			jhp.getJs().putVariable("logfolder", new File(collection+"/logs").getAbsolutePath());
			jhp.getJs().putVariable("jobs", infos.toArray(new ReferenceInfo[0]));
			jhp.getJs().putVariable("prio", prio);
			jhp.getJs().putVariable("nopr", nopr);
			jhp.getJs().putVariable("test", test);
			jhp.getJs().putVariable("keeptrimmed", keeptrimmed);
			
			jhp.getJs().setInterpolateStrings(false);
			
			for (String name : nameToModeAndFiles.keySet()) {
				jhp.getJs().putVariable("mode", nameToModeAndFiles.get(name).Item1);
				jhp.getJs().putVariable("name", name);
				jhp.getJs().putVariable("files", EI.wrap(nameToModeAndFiles.get(name).Item2).concat(" "));
				new LineOrientedFile(collection+"/scripts/"+name+".sh").writeAllText(jhp.apply(src));
				new File(collection+"/scripts/"+name+".sh").setExecutable(true);
				new LineOrientedFile(collection+"/scripts/"+name+".prio.oml").writeAllText(oml);
				if (prio)
					ReferenceInfo.writeTable(collection+"/scripts/"+name+".prio.csv",infos,true);
				if (nopr)
					ReferenceInfo.writeTable(collection+"/scripts/"+name+".nopr.csv",infos,false);
			}
		} else {
			map = false;
			prio = false;
			nopr = false;
		}
		
		String src = new LineIterator(RiboDownloadAndProcess.class.getResource("process.sh.jhp").openStream()).concat("\n");
		Jhp jhp = new Jhp();
		jhp.getJs().putVariable("bams", bams.toArray(new String[0]));
		jhp.getJs().putVariable("bamnames", bamnames.toArray(new String[0]));
		jhp.getJs().putVariable("collection", collection);
		jhp.getJs().putVariable("map", map);
		jhp.getJs().putVariable("rescue", rescue);
		jhp.getJs().putVariable("norescue", norescue);
		jhp.getJs().putVariable("price", price);
		jhp.getJs().putVariable("tmp", tmp);
		jhp.getJs().putVariable("stat", stat);
		jhp.getJs().putVariable("startcodon", startcodon);
		jhp.getJs().putVariable("prio", prio);
		jhp.getJs().putVariable("nopr", nopr);
		jhp.getJs().putVariable("lambdas", lambdas);
		jhp.getJs().putVariable("scripts", scripts.toArray(new String[0]));
		jhp.getJs().putVariable("cits", cits.toArray(new String[0]));
		jhp.getJs().putVariable("genomes", EI.wrap(refs.getProperties()).unique(false).concat(" "));
		
		jhp.getJs().setInterpolateStrings(false);
		new LineOrientedFile(collection+"/scripts/"+collection+".sh").writeAllText(jhp.apply(src));
		new File(collection+"/scripts/"+collection+".sh").setExecutable(true);
		
	}
	
	
	public static class ReferenceInfo {
		public boolean norc;
		public String index;
		public String type;
		public int priority;
		
		private Genomic genomic;
		private RefType refType;
		
		
		public ReferenceInfo(RefType t, Genomic genomic) {
			this.genomic = genomic;
			this.refType = t;
			priority = t.prio;
			norc = t.norc;
			type = genomic.getId()+"."+t.name();
			index = genomic.getInfos().get("bowtie-"+(t==RefType.Transcriptomic?"transcriptomic":"genomic"));
		}


		@Override
		public String toString() {
			return "ReferenceInfo [norc=" + norc + ", index=" + index
					+ ", type=" + type + ", priority=" + priority + "]";
		}
		
		public static void writeTable(String path, ArrayList<ReferenceInfo> l, boolean prio) throws IOException {
			try (LineWriter lw = new LineOrientedFile(path).write()) {
				lw.writeLine("File\tGenome\tTranscriptomic\tPriority");
				for (ReferenceInfo r : l)
					lw.writef("%s.sam\t%s\t%b\t%d\n", r.type, r.genomic.getId(), r.refType==RefType.Transcriptomic,prio?r.priority:2);
			}
		}
	
		
	}
}
