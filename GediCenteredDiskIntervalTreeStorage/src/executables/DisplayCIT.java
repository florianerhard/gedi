package executables;

import gedi.app.Gedi;
import gedi.cascadingProperty.CpsReader;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.gui.genovis.SetLocationField;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.gui.genovis.TrackSelectionTreeButton;
import gedi.gui.genovis.VisualizationTrack;
import gedi.oml.OmlNodeExecutor;
import gedi.oml.OmlReader;
import gedi.oml.petrinet.Pipeline;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.jph.Jhp;
import gedi.util.nashorn.JS;

import java.awt.BorderLayout;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class DisplayCIT {

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
		System.err.println("RiboView <Options>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -l <location>\t\t\tLocation string");
		System.err.println(" -f <folder ...>\t\t\tLoad all in the given folders");
		System.err.println(" -c <cit-file> <cit-file> ...\t\t\tCit files");
		System.err.println(" -n <name> <name> ...\t\t\tNames (Default: Names of cit files)");
		System.err.println(" -g <genome1 genome2 ...>\t\t\tGenome names");
		System.err.println();
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println(" -h\t\t\tShow this message");
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
		Gedi.startup(true);
		
		Genomic g = null;
		String[] cits = null;
		String[] names = null;
		String loc = null;
		String[] folder = null;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-f")) {
				ArrayList<String> l = new ArrayList<String>();
				i = checkMultiParam(args,++i,l);
				folder = l.toArray(new String[0]);
			}
			else if (args[i].equals("-l")) {
				loc = checkParam(args,++i);
			}
			else if (args[i].equals("-c")) {
				ArrayList<String> l = new ArrayList<String>();
				i = checkMultiParam(args,++i,l);
				cits = l.toArray(new String[0]);
			}
			else if (args[i].equals("-n")) {
				ArrayList<String> l = new ArrayList<String>();
				i = checkMultiParam(args,++i,l);
				names = l.toArray(new String[0]);
			}
			else if (args[i].equals("-g")) {
				ArrayList<String> gnames = new ArrayList<>();
				i = checkMultiParam(args, ++i, gnames);
				g = Genomic.get(gnames);
			}
			else if (args[i].equals("-D")) {
			}
//			else if (!args[i].startsWith("-")) 
//					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}

		if (folder!=null) {
			// look in folders
			ArrayList<String> citCreate = new ArrayList<String>();
			if (cits!=null) citCreate.addAll(Arrays.asList(cits));
			for (String f : folder) {
				EI.wrap(new File(f).list()).filter(p->p.endsWith(".cit")).map(fn->f+"/"+fn).toCollection(citCreate);
				log.log(Level.INFO, "Adding all cits in "+f);
			}
			cits = citCreate.toArray(new String[0]);
		}
		
		if (g==null) throw new UsageException("No genome given!");
		if (cits==null) throw new UsageException("No cit files given!");

		if (names==null) 
			names = EI.wrap(cits).map(s->FileUtils.getNameWithoutExtension(s)).toArray(String.class);

		HashSet<String> u = new HashSet<String>();
		for (i=0; i<names.length; i++) {
			String n = names[i].replace('.', '_');
			while (u.contains(n)) {
				if (n.matches("_\\d+$"))
					n = n.substring(0, n.lastIndexOf('_')+1)+(1+Integer.parseInt(n.substring(n.lastIndexOf('_'))));
				else
					n = n+"_1";
			}
			names[i] = n;
			u.add(n);
		}
		
		
		HashMap context = new HashMap();
		context.put("genomic", g);
		context.put("names", names);
		for (i=0; i<cits.length; i++)
			context.put("cits"+i, new CenteredDiskIntervalTreeStorage(cits[i]));
		
		log.info("Loading pipeline");
		
		JS js = new JS();
		js.putVariable("names", names);
		
		InputStream stream = DisplayRMQ.class.getResourceAsStream("displaycit.oml.jhp");
		String src = new LineIterator(stream).concat("\n");
		Jhp jhp = new Jhp(js);
		src = jhp.apply(src);
		
		
		String cps = new LineIterator(DisplayRMQ.class.getResourceAsStream("/resources/colors.cps")).concat("\n");
		Pipeline pipeline = (Pipeline)new OmlNodeExecutor().addInterceptor(new CpsReader().parse(cps)).execute(new OmlReader().setJs(js).parse(src),context);
		
		
		if (loc==null || !loc.contains(":")) {
			ReferenceSequence ref = loc==null?g.getTranscripts().getReferenceSequences().iterator().next():Chromosome.obtain(loc);
			GenomicRegion reg = g.getTranscripts().getTree(ref).getRoot().getKey().removeIntrons();
			reg = reg.extendAll(reg.getTotalLength()/3, reg.getTotalLength()/3);
			loc = ref.toPlusMinusString()+":"+reg.toRegionString();
		}
		MutableReferenceGenomicRegion reg = new MutableReferenceGenomicRegion().parse(loc).toStrandIndependent();

		SwingGenoVisViewer viewer = new SwingGenoVisViewer(pipeline.getPetriNet());
		for (VisualizationTrack track : pipeline.getTracks())
			viewer.addTrack(track);

		JFrame frame = new JFrame();
		frame.setExtendedState(frame.getExtendedState()|JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().setLayout(new BorderLayout());
		JScrollPane scr = new JScrollPane(viewer);
		scr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		frame.getContentPane().add(scr, BorderLayout.CENTER);

		JPanel fl = new JPanel();
		fl.add(new TrackSelectionTreeButton(viewer));
		fl.add(new SetLocationField(viewer, true, g.getNameIndex()));
		frame.getContentPane().add(fl, BorderLayout.NORTH);

		frame.pack();
		frame.setVisible(true);
		viewer.setLocation(reg.getReference(),reg.getRegion());

		
	}
	
}
