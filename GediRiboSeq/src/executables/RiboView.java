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
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.gui.genovis.SetLocationField;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.gui.genovis.TrackSelectionTreeButton;
import gedi.gui.genovis.VisualizationTrack;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.PaintUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.gui.ColorPalettes;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.jhp.Jhp;
import gedi.util.io.text.tsv.formats.Csv;
import gedi.util.nashorn.JS;
import gedi.util.oml.OmlNodeExecutor;
import gedi.util.oml.OmlReader;
import gedi.util.oml.cps.CpsReader;
import gedi.util.oml.petrinet.Pipeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class RiboView {

	private static final Logger log = Logger.getLogger( DisplayCIT.class.getName() );
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
		System.err.println(" -l <location>\t\t\tLocation string (in the same format as in the address line in the browser)");
		System.err.println(" -a <folder ...>\t\t\tLoad all in the given folders");
		System.err.println(" -f <prefix>\t\t\tPrefix for input files");
		System.err.println(" -t <file>\t\t\tFile containing total counts (from RiboStatistics)");
		System.err.println(" -g <genome1 genome2 ...>\t\t\tGenome names");
		System.err.println(" -tracks <file1 file2...>\t\t\tBed or location files to form additional tracks");
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
		if (index>=args.length) throw new UsageException("Missing argument for "+args[index-1]);
		String re = args[index];
		if (!StringUtils.isInt(re)) throw new UsageException("Must be an integer: "+args[index-1]);
		return Integer.parseInt(args[index]);
	}

	private static double checkDoubleParam(String[] args, int index) throws UsageException {
		if (index>=args.length) throw new UsageException("Missing argument for "+args[index-1]);
		String re = args[index];
		if (!StringUtils.isNumeric(re)) throw new UsageException("Must be a double: "+args[index-1]);
		return Double.parseDouble(args[index]);
	}
	
	public static void start(String[] args) throws Exception {
		Gedi.startup(true);
		
		Genomic g = null;
		String[] prefix = null;
		String[] totalCountFile = null;
		String loc = null;
		String[] folder = null;
		String[] tracks = null;
		ArrayList<RiboViewOnline> onlines = new ArrayList<RiboViewOnline>();
		ArrayList<String> additionalTracks = new ArrayList<String>();
		boolean debug = false;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-f")) {
				ArrayList<String> l = new ArrayList<String>();
				i = checkMultiParam(args,++i,l);
				prefix = l.toArray(new String[0]);
			}
			else if (args[i].equals("-a")) {
				ArrayList<String> l = new ArrayList<String>();
				i = checkMultiParam(args,++i,l);
				folder = l.toArray(new String[0]);
			}
			else if (args[i].equals("-l")) {
				loc = checkParam(args,++i);
			}
			else if (args[i].equals("-t")) {
				ArrayList<String> l = new ArrayList<String>();
				i = checkMultiParam(args,++i,l);
				totalCountFile = l.toArray(new String[0]);
			}
			else if (args[i].equals("-tracks")) {
				i = checkMultiParam(args,++i,additionalTracks);
			}
			else if (args[i].equals("-simple")) {
				onlines.add(new RiboViewOnline(checkParam(args, ++i)));
			}
			else if (args[i].equals("-debug")) {
				debug = true;
			}
			else if (args[i].equals("-regu")) {
				onlines.add(new RiboViewOnline(checkDoubleParam(args, ++i),0));
			}
			else if (args[i].equals("-rho")) {
				onlines.add(new RiboViewOnline(-1,checkDoubleParam(args, ++i)));
			}
			else if (args[i].equals("-g")) {
				ArrayList<String> names = new ArrayList<>();
				i = checkMultiParam(args, ++i, names);
				g = Genomic.get(names);
			}
			else if (args[i].equals("-D")) {
			}
//			else if (!args[i].startsWith("-")) 
//					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}
		
		HashSet<String> autoFolders = new HashSet<String>(Arrays.asList("rem2","price")); 


		if (totalCountFile==null && prefix!=null) {
			totalCountFile = new String[prefix.length];
			for (int c=0; c<prefix.length; c++) {
				String pref = prefix[c].replace("price", "stats");
				if (pref.endsWith(".annot")) pref = pref.substring(0, pref.length()-".annot".length());
				if (pref.endsWith(".merged")) pref = pref.substring(0, pref.length()-".merged".length());
				pref = pref+".total.stat";
				if (new File(pref).exists())
					totalCountFile[c] = pref;
				else {
					totalCountFile=null;
					break;
				}
			}
		}
		
		if (folder!=null) {
			// look in folders
			ArrayList<String> prefCreate = new ArrayList<String>();
			ArrayList<String> totalCreate = new ArrayList<String>();
			if (prefix!=null && totalCountFile!=null) {
				prefCreate.addAll(Arrays.asList(prefix));
				totalCreate.addAll(Arrays.asList(totalCountFile));
			}
			for (String f : folder) {
				for (String orfFile : EI.wrap(new File(f).list()).filter(p->p.endsWith(".orfs.cit") && !p.contains(".tmp.")).loop()) {
					String pref = new File(new File(f),orfFile.substring(0,orfFile.length()-".orfs.cit".length())).getPath();
					prefCreate.add(pref);
					File meta = new File(new File(f),orfFile+".metadata.json");
					if (meta.exists()) {
						totalCreate.add(meta.getPath());
					} else {
						String total = pref.substring(0, pref.length()-(orfFile.endsWith(".merged.orfs.cit")?".merged".length():0))+".stattotal.stat";
						if (!new File(total).exists() && autoFolders.contains(new File(total).getParentFile().getName())) {
							total = new File(total).getAbsoluteFile().getParentFile().getParentFile().getPath()+"/stats/"+new File(total).getName().replace("stattotal", "total");
							if (!new File(total).exists())
								total = new File(total).getAbsoluteFile().getParentFile().getParentFile().getPath()+"/report/"+new File(total).getName().replace("total.stat", "total.tsv");
							if (!new File(total).exists())
								total = EI.files(new File(total).getAbsoluteFile().getParentFile().getParentFile().getPath()+"/report/").filter(fi->fi.getPath().endsWith("total.tsv")).map(fi->fi.getAbsolutePath()).first();
						}
						
						totalCreate.add(total);
					}
					log.log(Level.INFO, "Adding "+pref);
				}
			}
			prefix = prefCreate.toArray(new String[0]);
			totalCountFile = totalCreate.toArray(new String[0]);
			
			ArrayList<String> tracksCreate = new ArrayList<String>();
			// look for additional tracks in folder
			for (String f : folder) {
				if (new File(f,"tracks").isDirectory()) {
					for (File track : new File(f,"tracks").listFiles()) {
						tracksCreate.add(track.getAbsolutePath());
					}
				}
			}
			tracks = tracksCreate.toArray(new String[0]);
		}
		else
			tracks = new String[0];
		
		
		if (additionalTracks.size()>0) 
			tracks = ArrayUtils.concat(additionalTracks.toArray(new String[0]),tracks);
		
		
		if (g==null) throw new UsageException("No genome given!");
		if (totalCountFile==null) throw new UsageException("No total count file given!");
		if (prefix==null) throw new UsageException("No input prefix given!");
		if (totalCountFile.length!=prefix.length) throw new UsageException("Differing lengths!");
		
		
		String[][] names = new String[prefix.length][];
		String[][] condRmq = new String[prefix.length][];
		String[][] condoptRmq = new String[prefix.length][];
		double[][] totals = new double[prefix.length][];
		String[] experimentNames = new String[prefix.length];
		String[] models = new String[prefix.length];
		
		HashMap context = new HashMap();
		context.put("genomic", g);
		
		Class<?> orfclass = null;
		
		for (int e=0; e<prefix.length; e++) {
			experimentNames[e] = FileUtils.getFullNameWithoutExtension(prefix[e]);
			models[e] = prefix[e]+".model";
			
			if (totalCountFile[e].endsWith("metadata.json")) {
				DynamicObject json = DynamicObject.parseJson(new LineOrientedFile(totalCountFile[e]).readAllText2());
				totals[e] = EI.wrap(json.getEntry("conditions").asArray()).mapToDouble(c->Math.max(1, c.getEntry("total").asDouble())).toDoubleArray();
				names[e] = EI.wrap(json.getEntry("conditions").asArray()).map(c->c.getEntry("name").asString()).toArray(String.class);
			}
			else {
				DataFrame df = Csv.toDataFrame(totalCountFile[e],true,0,null);
				df.remove(df.getColumn(0));
				names[e] = EI.seq(0, df.columns()).map(c->df.getColumn(c).name()).toArray(String.class);
				totals[e] = EI.seq(0, df.columns()).mapToDouble(c->df.getColumn(c).getDoubleValue(0)).toDoubleArray();
			}
			
			String uprefix = prefix[e];
			condRmq[e] = EI.seq(0,names[e].length).map(s->uprefix+"."+s+".codons.rmq").toArray(String.class);
			
			condoptRmq[e] = EI.seq(0,names[e].length).map(s->uprefix+".opt."+s+".codons.rmq").toArray(String.class);
			
			
			Path p = Paths.get(prefix[e]+".orfs.cit");
			GenomicRegionStorage<?> orfs = ((GenomicRegionStorage<?>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p)).toMemory();

			context.put("orfs"+e, orfs);
			orfclass = orfs.getType();
			
			String womer = StringUtils.removeFooter(prefix[e],".merged");
			String[] readPossi = {
					prefix[e]+".reads.cit",
					womer+".reads.cit",
					new File(new File(womer).getParentFile().getParentFile(),FileUtils.getNameWithoutExtension(womer)+".cit").getAbsolutePath()
			};
			
			for (int pi=0; pi<readPossi.length; pi++){
				p = Paths.get(readPossi[pi]);
				if (p.toFile().exists()) 
					break;
			}
			
			
			GenomicRegionStorage<AlignedReadsData> reads;
			if (p.toFile().exists())
				reads = ((GenomicRegionStorage<AlignedReadsData>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p));
			else
				reads = new MemoryIntervalTreeStorage<AlignedReadsData>(AlignedReadsData.class);
			context.put("reads"+e, reads);

		}
			
		String[] trackNames = EI.wrap(tracks).map(FileUtils::getNameWithoutExtension).toArray(String.class);
		for (int e=0; e< tracks.length; e++) {
			Path p = Paths.get(tracks[e]);
			try {
				log.log(Level.INFO, "Adding track "+tracks[e]);
				GenomicRegionStorage st = ((GenomicRegionStorage) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p));
				context.put("tracks"+e, st);
			} catch (Exception ex) {
				throw new RuntimeException("Cannot load "+p,ex);
			}			
		}

		
		
		log.info("Loading pipeline");
		
		JS js = new JS();
		js.putVariable("mergedrmq", EI.wrap(prefix).map(s->s+".codons.rmq").toArray(String.class));
		js.putVariable("conditionrmq", condRmq);
		
		js.putVariable("mergedoptrmq", EI.wrap(prefix).map(s->new File(s+".opt.codons.rmq").exists()?s+".opt.codons.rmq":null).toArray(String.class));
		js.putVariable("conditionoptrmq", condoptRmq);
		
		js.putVariable("tracks", trackNames);
		js.putVariable("names", names);
		js.putVariable("totals", totals);
		js.putVariable("experimentNames", experimentNames);
		js.putVariable("models", models);
		js.putVariable("onlines",onlines.toArray(new RiboViewOnline[0]));
		
		InputStream stream;
		if (orfclass==PriceOrf.class) {
			if (!checkOldRmq(prefix[0]+".codons.rmq"))
				stream = RiboView.class.getResourceAsStream(!onlines.isEmpty()?"pricevisonlineem2.oml.jhp":"pricevis2.oml.jhp");
			else
				stream = RiboView.class.getResourceAsStream(!onlines.isEmpty()?"pricevisonlineem.oml.jhp":"pricevis.oml.jhp");
		} else
			stream = RiboView.class.getResourceAsStream(!onlines.isEmpty()?"ribovisonlineem.oml.jhp":"ribovis.oml.jhp");
		
		String src = new LineIterator(stream).concat("\n");
		Jhp jhp = new Jhp(js);
		src = jhp.apply(src);
		if (debug)
			FileUtils.writeAllText(src, new File("debug.oml"));
		
		String cps = new LineIterator(RiboView.class.getResourceAsStream("/resources/colors.cps")).concat("\n");
		
		StringBuilder sb = new StringBuilder();
		for (int e=0; e<experimentNames.length; e++) {
			sb.append(".conditions").append(e).append(" { \"styles\":[\n");
			Color[] colors = ColorPalettes.Dark2.getPalette(names[e].length);
			for (int cc=0; cc<names[e].length; cc++) {
				sb.append("\t{ \"color\": \""+PaintUtils.encodeColor(colors[cc])+"\", \"name\": \""+names[e][cc]+"\" },\n");
			}
			sb.append("]}\n");
		}
		cps = cps+sb.toString();
		if (debug)
			FileUtils.writeAllText(cps, new File("debug.cps"));
		
		
		Pipeline pipeline = (Pipeline)new OmlNodeExecutor().addInterceptor(new CpsReader().parse(cps)).execute(new OmlReader().setJs(js).parse(src),context);
		
		
		
		
		
		if (loc==null || !loc.contains(":")) {
			ReferenceGenomicRegion<?> rgr = g.getNameIndex().get(loc);
			if (rgr!=null) {
				rgr = rgr.toMutable().transformRegion(r->r.extendFront(1)).toImmutable();// workaround for IndexGenome bug
				loc = rgr.toLocationString();
			} else {
				ReferenceSequence ref = loc==null?g.getTranscripts().getReferenceSequences().iterator().next():Chromosome.obtain(loc);
				GenomicRegion reg = g.getTranscripts().getTree(ref).getRoot().getKey().removeIntrons();
				reg = reg.extendAll(reg.getTotalLength()/3, reg.getTotalLength()/3);
				loc = ref.toPlusMinusString()+":"+reg.toRegionString();
			}
		}
		MutableReferenceGenomicRegion<Object> reg = ImmutableReferenceGenomicRegion.parse(g, loc).toMutable().toStrandIndependent();
		
//		MutableReferenceGenomicRegion reg = new MutableReferenceGenomicRegion().parse(loc).toStrandIndependent();

		SwingGenoVisViewer viewer = new SwingGenoVisViewer(pipeline.getPetriNet());
		for (VisualizationTrack track : pipeline.getTracks())
			viewer.addTrack(track);

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().setLayout(new BorderLayout());
		JScrollPane scr = new JScrollPane(viewer, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		frame.getContentPane().add(scr, BorderLayout.CENTER);

		JPanel fl = new JPanel();
		fl.add(new TrackSelectionTreeButton(viewer));
		fl.add(new SetLocationField(viewer, true, g));
		frame.getContentPane().add(fl, BorderLayout.NORTH);

		frame.pack();
		frame.setVisible(true);
		viewer.setLocation(reg.getReference(),reg.getRegion());
		frame.setExtendedState(frame.getExtendedState()|Frame.MAXIMIZED_BOTH);
		
		
	}
	
	private static boolean checkOldRmq(String file) throws IOException {
		PageFile f = new PageFile(file);
		if (!f.getAsciiChars(DiskGenomicNumericBuilder.MAGIC.length()).equals(DiskGenomicNumericBuilder.MAGIC))
			throw new RuntimeException("Not a valid file!");
		
		
		int refs = f.getInt();
		Chromosome chr = Chromosome.read(f);
		long pos = f.getLong();
		long cur = f.position();
		f.position(pos);
		char type = f.getAsciiChar();
		int size = f.getInt();
		int numCond = f.getInt();
		
		int a = f.getInt();
		int b = f.getInt();
		int c = f.getInt();
		return a+1==b && b+1==c;
	}


	public static class RiboViewOnline {
		public double regu;
		public double rho;
		public String simpleModel;
		public RiboViewOnline(double regu, double rho) {
			this.regu = regu;
			this.rho = rho;
		}
		public RiboViewOnline(String simpleModel) {
			this.simpleModel = simpleModel;
		}
		
		
	}
	
}
