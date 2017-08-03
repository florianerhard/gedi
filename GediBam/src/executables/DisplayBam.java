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
import gedi.core.data.annotation.NameColorAnnotation;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.gui.genovis.SetLocationField;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.gui.genovis.TrackSelectionTreeButton;
import gedi.gui.genovis.VisualizationTrack;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.region.bam.Strandness;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.PaintUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.gui.ColorPalettes;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.jhp.Jhp;
import gedi.util.mutable.MutableTriple;
import gedi.util.nashorn.JS;
import gedi.util.oml.OmlNodeExecutor;
import gedi.util.oml.OmlReader;
import gedi.util.oml.cps.CpsReader;
import gedi.util.oml.petrinet.Pipeline;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class DisplayBam extends JPanel {

	private static final Logger log = Logger.getLogger( DisplayBam.class.getName() );
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
		System.err.println("DisplayBam <Options>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -l <location>\t\t\tLocation string");
		System.err.println(" -r <folder>\t\t\tRoot folder for entries in table");
		System.err.println(" -f <folder ...>\t\t\tLoad all in the given folders");
		System.err.println(" -bamres <mbppp>\t\t\tSpecifiy maximal number of basepairs per pixel for the bam files");
		System.err.println(" -seqres <mbppp>\t\t\tSpecifiy maximal number of basepairs per pixel for the sequence (0 to deactivate sequence)");
		System.err.println(" -s <strandness>\t\t\tStrandness: one of Unspecific, Specific, Inverted (Default: Specific)");
		System.err.println(" -d <bam-file> <bam-file> ...\t\t\tBam files");
		System.err.println(" -n <name> <name> ...\t\t\tNames (Default: Names of bam files)");
		System.err.println(" -g <genome1 genome2 ...>\t\t\tGenome names");
		System.err.println(" -t\t\t\t\t\tOnly write descriptor template");
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
		String[] bams = null;
		String[] names = null;
		String loc = null;
		String[] folder = null;
		boolean template = false;
		double bammbppp = 100;
		double seqmbppp = 1;
		ReadCountMode mode = ReadCountMode.Weight;
		String root = null;
		
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
			else if (args[i].equals("-bamres")) {
				bammbppp = checkDoubleParam(args, ++i);
			}
			else if (args[i].equals("-seqres")) {
				seqmbppp = checkDoubleParam(args, ++i);
			}
			else if (args[i].equals("-mode")) {
				mode = ReadCountMode.valueOf(checkParam(args, ++i));
			}
			else if (args[i].equals("-l")) {
				loc = checkParam(args,++i);
			}
			else if (args[i].equals("-r")) {
				root = checkParam(args,++i);
			}
			else if (args[i].equals("-d")) {
				ArrayList<String> l = new ArrayList<String>();
				i = checkMultiParam(args,++i,l);
				bams = l.toArray(new String[0]);
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
			else if (args[i].equals("-t")) {
				template = true;
			}
			else if (args[i].equals("-D")) {
			}
//			else if (!args[i].startsWith("-")) 
//					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}

		if (folder!=null) {
			// look in folders
			ArrayList<String> bamCreate = new ArrayList<String>();
			if (bams!=null) bamCreate.addAll(Arrays.asList(bams));
			for (String f : folder) {
				EI.wrap(new File(f).list()).filter(p->p.endsWith(".bam")).map(fn->f+"/"+fn).toCollection(bamCreate);
				log.log(Level.INFO, "Adding all bams in "+f);
			}
			bams = bamCreate.toArray(new String[0]);
		}
		
		if (g==null) throw new UsageException("No genome given!");
		if (bams==null) throw new UsageException("No bam files given!");
		
		DisplayBam db = new DisplayBam(g);
		db.setMode(mode);
		db.setBamResolution(bammbppp);
		db.setSeqResolution(seqmbppp);
		db.setGenomicLocation(loc);
		
		if (bams.length==1 && !bams[0].endsWith(".bam"))
			db.loadTracks(root,bams[0]);
		else
			db.loadTracks(bams, names, template);

		JFrame frame = new JFrame();
		frame.setExtendedState(frame.getExtendedState()|JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(db);
		frame.pack();
		frame.setVisible(true);
	}
	
	
	private Genomic g;
	private ReadCountMode mode = ReadCountMode.Weight;
	private double bammbppp = 100;
	private double seqmbppp = 1;
	private MutableReferenceGenomicRegion<?> genomicLocation;
	private SwingGenoVisViewer viewer;
	
	public DisplayBam(Genomic g) {
		super(new BorderLayout());
		this.g = g;
	}
	
	public void loadTracks(String bamview) throws IOException {
		loadTracks(createTracks(null, bamview));
	}
	
	public void loadTracks(String root, String bamview) throws IOException {
		loadTracks(createTracks(root, bamview));
	}
	
	public void loadTracks(String[] bams, String[] names, boolean template) throws IOException {
		loadTracks(createTracks(bams,names,template));
	}
	
	public void loadTracks(DisplayBamTrack[] tracks) throws IOException {
		removeAll();
		
		viewer = createViewPanel(tracks,g,mode,bammbppp,seqmbppp);
		JScrollPane scr = new JScrollPane(viewer);
		scr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(scr, BorderLayout.CENTER);

		JPanel fl = new JPanel();
		fl.add(createBamViewSelector());
		fl.add(new TrackSelectionTreeButton(viewer));
		fl.add(new SetLocationField(viewer, true, g));
		add(fl, BorderLayout.NORTH);
		
		if (genomicLocation!=null)
			viewer.setLocation(genomicLocation.getReference(),genomicLocation.getRegion());
	}
	
	
	private JButton createBamViewSelector() {
		JButton re = new JButton("Load track table");
		re.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
				if (fc.showOpenDialog(DisplayBam.this)==JFileChooser.APPROVE_OPTION) {
					try {
						loadTracks(fc.getSelectedFile().getAbsolutePath());
					} catch (IOException e1) {
						log.log(Level.SEVERE,"Could not load table!",e1);
					}
				}
			}
		});
		return re;
	}

	public void setGenomicLocation(String locationString) {
		setGenomicLocation(parseLocation(g, locationString));
	}
	
	public void setGenomicLocation(MutableReferenceGenomicRegion<?> genomicLocation) {
		this.genomicLocation = genomicLocation;
		if (viewer!=null)
			viewer.setLocation(genomicLocation.getReference(),genomicLocation.getRegion());
	}
	
	public SwingGenoVisViewer getViewer() {
		return viewer;
	}
	
	public MutableReferenceGenomicRegion<?> getGenomicLocation() {
		return genomicLocation;
	}
	
	public ReadCountMode getMode() {
		return mode;
	}


	public void setMode(ReadCountMode mode) {
		this.mode = mode;
	}

	public double getBamResolution() {
		return bammbppp;
	}

	public void setBamResolution(double bammbppp) {
		this.bammbppp = bammbppp;
	}

	public double getSeqResolution() {
		return seqmbppp;
	}

	public void setSeqResolution(double seqmbppp) {
		this.seqmbppp = seqmbppp;
	}

	private static MutableReferenceGenomicRegion<Void> parseLocation(Genomic g, String loc) {
		if (loc==null || !loc.contains(":")) {
			ReferenceSequence ref = loc==null?g.getTranscripts().getReferenceSequences().iterator().next():Chromosome.obtain(loc);
			GenomicRegion reg = g.getTranscripts().getTree(ref).getRoot().getKey().removeIntrons();
			reg = reg.extendAll(reg.getTotalLength()/3, reg.getTotalLength()/3);
			loc = ref.toPlusMinusString()+":"+reg.toRegionString();
		}
		return new MutableReferenceGenomicRegion().parse(loc).toStrandIndependent();
	}

	
	private static SwingGenoVisViewer createViewPanel(DisplayBamTrack[] tracks, Genomic g, ReadCountMode mode, double bammbppp, double seqmbppp) throws IOException {
		log.info("Size factors: \n"+EI.wrap(tracks).demultiplex(t->EI.wrap(t.experiments)).map(e->e.label+"\t"+e.getSizeFactor()).concat("\n"));
		StringBuilder colors = new StringBuilder();
		
		HashMap context = new HashMap();
		context.put("genomic", g);
		context.put("mode", mode);
		for (int e=0; e<tracks.length; e++)
			context.put("tracks"+e,tracks[e].get());
		
		ArrayList<String> extratracks = new ArrayList<String>();
		if (new File("tracks").isDirectory())
			for (File f : new File("tracks").listFiles()) {
				if (f.getPath().endsWith(".locations")) {
					log.info("Reading track: "+f);
					context.put("extras"+extratracks.size(),readLocations(f, colors));
					extratracks.add(FileUtils.getNameWithoutExtension(f));
				}
			}
		
		
		log.info("Loading pipeline");
		
		JS js = new JS();
		js.putVariable("tracks", tracks);
		js.putVariable("bammbppp", bammbppp);
		js.putVariable("seqmbppp", seqmbppp);
		js.putVariable("extratracks", extratracks.toArray());
		
		InputStream stream = DisplayBam.class.getResourceAsStream("displaybam.oml.jhp");
		String src = new LineIterator(stream).concat("\n");
		Jhp jhp = new Jhp(js);
		src = jhp.apply(src);
		
		String cps = new LineIterator(DisplayBam.class.getResourceAsStream("/resources/colors.cps")).concat("\n");
		for (DisplayBamTrack t : tracks) {
			colors.append(".").append(t.trackName).append(" { \"styles\": [");
			colors.append(EI.wrap(t.experiments).map(e->"{ \"color\": \""+PaintUtils.encodeColor(e.color)+"\", \"name\": \""+e.label+"\" }").concat(","));
			colors.append("]}\n");
		}
		Pipeline pipeline = (Pipeline)new OmlNodeExecutor()
			.addInterceptor(new CpsReader().parse(cps))
			.addInterceptor(new CpsReader().parse(colors.toString()))
			.execute(new OmlReader().setJs(js).parse(src),context);
		
		SwingGenoVisViewer viewer = new SwingGenoVisViewer(pipeline.getPetriNet());
		for (VisualizationTrack track : pipeline.getTracks())
			viewer.addTrack(track);
		return viewer;
	}

	private static DisplayBamTrack[] createTracks(String root, String bamview) throws IOException {
		
		// descriptor file
		ArrayList<DisplayBamTrack> tracksMap = new ArrayList<>();
		LinkedHashMap<String, LinkedHashMap<String,ArrayList<MutableTriple<String, String, String>>>> expMap = new LinkedHashMap<>();
		
		for (String[] f : new LineOrientedFile(bamview).lineIterator("#").filter(s->s.length()>0).map(a->StringUtils.split(a, '\t')).skip(1).loop()) 
			expMap.computeIfAbsent(f[2], x->new LinkedHashMap<>()).computeIfAbsent(f[1],x->new ArrayList<>()).add(new MutableTriple<>(f[0], f[3],f.length>4?f[4]:"Specific"));
		
		for (String track : expMap.keySet()) {
			String[] colNames = EI.wrap(expMap.get(track).keySet()).map(label->expMap.get(track).get(label).get(0).Item2).toArray(String.class);
			Color[] colors = new Color[colNames.length];
			for (int j = 0; j < colNames.length; j++) {
				colors[j] = PaintUtils.parseColor(colNames[j]);
				if (colors[j]==null)
					colors[j] = ColorPalettes.get(colNames[j], colNames.length)[j];
			}
			
			ArrayList<DisplayBamExperiment> exps = new ArrayList<DisplayBam.DisplayBamExperiment>();
			String[] strandness = EI.wrap(expMap.get(track).values()).<String>demultiplex(l->EI.wrap(l).map(t->t.Item3)).toArray(String.class);
			int ind = 0;
			for (String label : expMap.get(track).keySet()) {
				String[] files = EI.wrap(expMap.get(track).get(label)).map(p->p.Item1).toArray(String.class);
				exps.add(new DisplayBamExperiment(label, root, files, colors[ind],Strandness.valueOf(strandness[ind])));
				ind++;
			}
			tracksMap.add(new DisplayBamTrack(track,exps.toArray(new DisplayBamExperiment[0])));
		}
		return tracksMap.toArray(new DisplayBamTrack[0]);
	}
	
	private static DisplayBamTrack[] createTracks(String[] bams, String[] names, boolean template) throws IOException {

		if (names==null) 
			names = EI.wrap(bams).map(s->StringUtils.removeFooter(s, ".bam")).toArray(String.class);

		HashSet<String> u = new HashSet<String>();
		for (int i=0; i<names.length; i++) {
			String n = StringUtils.trim(names[i].replace('.', '_').replace('/', '_'),'_');
			while (u.contains(n)) {
				if (n.matches("_\\d+$"))
					n = n.substring(0, n.lastIndexOf('_')+1)+(1+Integer.parseInt(n.substring(n.lastIndexOf('_'))));
				else
					n = n+"_1";
			}
			names[i] = n;
			u.add(n);
		}
		if (template) {
			
			System.out.println("# files with the same label are treated as resequencing runs, i.e. are merged to a single data source; Color can be a hex color specification (with leading #) or any of the ColorPalettes entries, Strandness is Specific/Unspecific/Inverted");
			System.out.println("File\tLabel\tTrack\tColor\tStrandness");
			for (int c=0; c<bams.length; c++) {
				System.out.printf("%s\t%s\t%d\t%s\t%s\n",bams[c],names[c],c,"Accent","Specific");
			}
			System.exit(0);
		}
		
		DisplayBamTrack[] tracks = new DisplayBamTrack[bams.length];
		for (int c=0; c<bams.length; c++) {
			tracks[c] = new DisplayBamTrack(names[c], new DisplayBamExperiment[] {new DisplayBamExperiment(names[c], null, new String[] {bams[c]}, Color.black, Strandness.Specific)});
		}
			
		return tracks;
	}

	private static MemoryIntervalTreeStorage<NameColorAnnotation> readLocations(File f, StringBuilder sb) throws IOException {
		HashMap<String,Color> types = new HashMap<String, Color>();
		ExtendedIterator<ImmutableReferenceGenomicRegion<NameColorAnnotation>> it = new LineOrientedFile(f.getAbsolutePath()).lineIterator("#").map(a->StringUtils.split(a, '\t')).map(a->ImmutableReferenceGenomicRegion.parse(a[0], new NameColorAnnotation(a[1], checkColor(a[2],types))));
		MemoryIntervalTreeStorage<NameColorAnnotation> re = new MemoryIntervalTreeStorage<NameColorAnnotation>(NameColorAnnotation.class);
		re.fill(it);
		
		Color[] colors = ColorPalettes.Set2.getPalette(types.size());
		String[] in = types.keySet().toArray(new String[0]);
		Arrays.sort(in);
		
		IdentityHashMap<Color,Color> colorMap = new IdentityHashMap<Color, Color>();
		for (int i=0; i<in.length; i++)
			colorMap.put(types.get(in[i]), colors[i]);
		
		re.ei().map(r->r.getData()).forEachRemaining(nc->nc.setColor(colorMap.get(nc.getColor())));
		
		sb.append(".").append(FileUtils.getNameWithoutExtension(f)).append(" { \"styles\": [");
		sb.append(EI.along(in).map(e->"{ \"color\": \""+PaintUtils.encodeColor(colors[e])+"\", \"name\": \""+in[e]+"\" }").concat(","));
		sb.append("]}\n");
		
		
		return re;
	}


	private static Color checkColor(String c, HashMap<String,Color> types) {
		Color col = PaintUtils.parseColor(c);
		if (col!=null) return col;
		return types.computeIfAbsent(c, x->new Color(0));
	}


	public static class DisplayBamTrack {
		public String trackName;
		public DisplayBamExperiment[] experiments;
		public DisplayBamTrack(String trackName,
				DisplayBamExperiment[] experiments) {
			this.trackName = trackName;
			this.experiments = experiments;
		}
		
		public BamGenomicRegionStorage get() throws IOException {
			BamGenomicRegionStorage re = new BamGenomicRegionStorage(EI.wrap(experiments).demultiplex(e->EI.wrap(e.files).<String>map(f->new File(e.root,f).getAbsolutePath())).toArray(String.class));
			re.setStrandness(EI.wrap(experiments).map(e->e.strandness).toArray(Strandness.class));
			ContrastMapping mapping = new ContrastMapping();
			int index = 0;
			for (int e=0; e<experiments.length; e++) {
				for (int i=0; i<experiments[e].files.length; i++)
					mapping.addMapping(index++, e, experiments[e].label);
			}
			re.setMapping(mapping);
			return re;
		}
		
		public String getSizeFactorString() {
			return EI.wrap(experiments).map(e->e.getSizeFactor()).concat(",");
		}
	}
	
	
	public static class DisplayBamExperiment {
		public String label;
		public String[] files;
		public Color color;
		public String root;
		public Strandness strandness;
		
		public DisplayBamExperiment(String label, String root, String[] files, Color color, Strandness strandness) {
			this.label = label;
			this.files = files;
			this.color = color;
			this.root = root;
			this.strandness = strandness;
		}
		
		private long sf =-1;
		public long getSizeFactor() {
			if (sf==-1) {
				sf = 0;
				for (String f : files) {
					File file = new File(root,f);
					SamReader sam = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.SILENT).open(file);
					for (int i=0; i<sam.getFileHeader().getSequenceDictionary().size(); i++)
						sf+=((SamReader.Indexing)sam).getIndex().getMetaData(0).getAlignedRecordCount();
				}
			}
			return sf;
		}
		
	}
}
