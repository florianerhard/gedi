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

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import gedi.app.Gedi;
import gedi.app.extension.ExtensionContext;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.LazyGenome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.workspace.loader.PreloadInfo;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.gui.genovis.SetLocationField;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.gui.genovis.TrackSelectionTreeButton;
import gedi.gui.genovis.VisualizationTrack;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.jhp.TemplateEngine;
import gedi.util.io.text.jhp.TemplateGenerator;
import gedi.util.io.text.jhp.display.AutoTemplateGenerator;
import gedi.util.io.text.jhp.display.DisplayTemplateGeneratorExtensionPoint;
import gedi.util.oml.OmlNodeExecutor;
import gedi.util.oml.OmlReader;
import gedi.util.oml.cps.CpsReader;
import gedi.util.oml.petrinet.Pipeline;

public class Display {

	private static final Logger log = Logger.getLogger( Template.class.getName() );
	public static void main(String[] args) {
		try {
			start(args);
		} catch (UsageException e) {
			usage("An error occurred: "+e.getMessage(),null);
			if (ArrayUtils.find(args, "-D")>=0)
				e.printStackTrace();
		} catch (Exception e) {
			System.err.println("An error occurred: "+e.getMessage());
			if (ArrayUtils.find(args, "-D")>=0)
				e.printStackTrace();
		}
	}
	
	private static void usage(String message, String additional) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("Display <Options>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -l <location>\t\t\tLocation string");
		System.err.println(" -g <genome1 genome2 ...>\t\t\tGenome names");
		System.err.println(" --x.y[3].z=val\t\tDefine a template variable; val can be json (e.g. --x='{\\\"prop\\\":\\\"val\\\"}'");
		System.err.println(" -j <json-file>\t\tDefine template variables");
		System.err.println(" -t <template-file>\t\tProcess a template");
		System.err.println(" -d <string>\t\tInsert template source");
		System.err.println(" -f <file1> [<file2> ...]\t\tLoad files and integrate them into a track according to extension system");
		System.err.println(" -out [<oml>]\t\tWrite current pipeline to oml file");
		System.err.println(" -tab [<file>]\t\tWrite current tracks table (to edit and supply to -f!)");
		System.err.println(" -dontshow \t\tDo not show viewer (useful in combination with -out or -tab!)");
		System.err.println("");
		System.err.println(" -h [<template>]	Print usage information (of this program and the given template)");
		System.err.println();
		System.err.println(" -oml [<file>]\t\tDisplay using the given oml file");
		System.err.println(" -include [<file>]\t\tInclude the given oml file (removing header/footer)");
		System.err.println();
		System.err.println(" -D\t\t\tOutput debugging information");
		System.err.println(" -h\t\t\tShow this message");
		System.err.println();
		if (additional!=null) {
			System.err.println(additional);
			System.err.println("");
		}
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
	private static String[] checkPair(String[] args, int index) throws UsageException {
		int p = args[index].indexOf('=');
		if (!args[index].startsWith("--") || p==-1) throw new UsageException("Not an assignment parameter (--name=val): "+args[index]);
		return new String[] {args[index].substring(2, p),args[index].substring(p+1)};
	}
	
	private static final String prefix = "<Pipeline>\n";
	private static final String suffix = "</Pipeline>\n";
	public static final String CPS_ID = "CPS_BUFFER";
	public static final String STORAGE_ID = "STORAGE_BUFFER";
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void start(String[] args) throws Exception {
		Gedi.startup(true);
		
		TemplateEngine te = new TemplateEngine();
		te.declareBuffer(CPS_ID);
		te.declareBuffer(STORAGE_ID);
		te.addTemplateSearchURL("classpath://Gedi/resources/templates/gediview/${name}");
		te.direct(prefix);
		String loc = null;
		Genomic g = null;
		boolean show = true;
		boolean sort=true;
		String omlFile = null;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				ArrayList<String> list = new ArrayList<>();
				i = checkMultiParam(args, i+1, list);
				if (list.isEmpty())
					usage(null,null);
				else {
					usage(null,"\n"+te.help(list.get(0)));
				}
				return;
			}
			else if (args[i].equals("-l")) {
				loc = checkParam(args,++i);
			}
			else if (args[i].equals("-f")) {
				ArrayList<String> l = new ArrayList<>();
				i = checkMultiParam(args, ++i, l);
				String[] files = l.toArray(new String[0]);

				files(te,files);
			}
			else if (args[i].equals("-oml")) {
				omlFile = checkParam(args,++i);
			}
			else if (args[i].equals("-include")) {
				String code = new LineOrientedFile(checkParam(args,++i)).readAllText();
				code = StringUtils.removeFooter(StringUtils.removeHeader(code, prefix),suffix);
				te.direct(code);
			}
			else if (args[i].equals("-out")) {
				new LineOrientedFile(checkParam(args,++i)).writeAllText(te.toString()+suffix);
				if (!te.getBuffer(CPS_ID).isEmpty())
					new LineOrientedFile(checkParam(args,i)+".cps").writeAllText(te.getBuffer(CPS_ID));
			}
			else if (args[i].equals("-tab")) {
				new LineOrientedFile(checkParam(args,++i)).writeAllText(te.getAndDiscardBuffer(STORAGE_ID));
			}
			else if (args[i].equals("-dontshow")) {
				show = false;
			}
			else if (args[i].equals("-dontsort")) {
				sort = false;
			}
			else if (args[i].equals("-g")) {
				ArrayList<String> gnames = new ArrayList<>();
				i = checkMultiParam(args, ++i, gnames);
				te.set("genomic", EI.wrap(gnames).concat(","));
				g = Genomic.get(gnames);
			}
			else if (args[i].equals("-D")) {
			}else if (args[i].equals("-j")) {
				te.json(checkParam(args, ++i));
			}
			else if (args[i].startsWith("--")) {
				String[] p = checkPair(args,i);
				te.parameter(p[0], p[1]);
			}else if (args[i].equals("-t")) {
				te.template(checkParam(args, ++i));
			}
//			else if (!args[i].startsWith("-")) 
//					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}

		if (!show) 
			return;
		
		if (g==null) throw new UsageException("No genomes given!");
		
		log.info("Loading pipeline");
		
		String src = te.toString()+suffix;
		String cps = te.getBuffer(CPS_ID);
		
		if (omlFile!=null) {
			src = new LineOrientedFile(omlFile).readAllText();
			if (new File(omlFile+".cps").exists())
				cps = new LineOrientedFile(omlFile+".cps").readAllText();
		}
		
		String cpsc = new LineIterator(Template.class.getResourceAsStream("/resources/colors.cps")).concat("\n");
		OmlNodeExecutor oml = new OmlNodeExecutor()
				.addInterceptor(new CpsReader().parse(cpsc));
		oml.addInterceptor(new CpsReader().parse(cps));
		
		Pipeline pipeline = (Pipeline)oml.execute(new OmlReader().parse(src));
		if (sort)
			pipeline.sortPlusMinusTracks();
		
		if (loc==null) {
			ReferenceSequence ref = loc==null?EI.wrap(g.getSequenceNames()).map(s->Chromosome.obtain(s,true)).filter(r->r.isStandard()).first():Chromosome.obtain(loc);
			GenomicRegion reg;
			if (g.getTranscripts().getTree(ref)==null || g.getTranscripts().getTree(ref).isEmpty()) 
				reg = new ArrayGenomicRegion(g.getLength(ref.getName())/2-100,g.getLength(ref.getName())/2+100);
			else
				reg = g.getTranscripts().getTree(ref).getRoot().getKey().removeIntrons();
			reg = reg.extendAll(reg.getTotalLength()/3, reg.getTotalLength()/3);
			loc = ref.toPlusMinusString()+":"+reg.toRegionString();
		}
		MutableReferenceGenomicRegion reg = ImmutableReferenceGenomicRegion.parse(g, loc).toMutable().toStrandIndependent();

		
		
		SwingGenoVisViewer viewer = new SwingGenoVisViewer(pipeline.getPetriNet());
		for (VisualizationTrack track : pipeline.getTracks())
			viewer.addTrack(track);
		viewer.setGenome(new LazyGenome(g));

		JFrame frame = new JFrame();
		frame.setExtendedState(frame.getExtendedState()|JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().setLayout(new BorderLayout());
		JScrollPane scr = new JScrollPane(viewer);
		scr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		frame.getContentPane().add(scr, BorderLayout.CENTER);

		JPanel fl = new JPanel();
		fl.add(new TrackSelectionTreeButton(viewer));
		fl.add(new SetLocationField(viewer, true, g));
		frame.getContentPane().add(fl, BorderLayout.NORTH);

		frame.pack();
		frame.setVisible(true);
		viewer.setLocation(reg.getReference(),reg.getRegion());

		
	}

	public static String eachFile(String prefix) throws IOException {
		if (prefix.endsWith("/"))
			return EI.fileNames(prefix).map(f->{ try{return files(f); } catch (Exception e) {throw new RuntimeException(e);}}).concat("\n");
		
		String dir = new File(prefix).getParent();
		String pp = new File(prefix).getName();
		return EI.fileNames(dir)
				.filter(s->s.startsWith(pp))
				.map(f->{ try{return files(dir+"/"+f); } catch (Exception e) {throw new RuntimeException(e);}}).concat("\n");
		
	}
	
	public static String files(String... files) throws UsageException, IOException {
		return files(new TemplateEngine().addTemplateSearchURL("classpath://Gedi/resources/templates/gediview/${name}"),files).toString();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static TemplateEngine files(TemplateEngine te, String... files) throws UsageException, IOException {
		TemplateGenerator[] dpl = null;
		
		for (int j=0; j<files.length; j++){
			WorkspaceItemLoader loader = WorkspaceItemLoaderExtensionPoint.getInstance().get(Paths.get(files[j]));
			if (loader==null) 
				throw new UsageException("No loader for "+files[j]);
			PreloadInfo pre = loader.getPreloadInfo(Paths.get(files[j]));
			AutoTemplateGenerator<PreloadInfo<?, ?>, ?> one = DisplayTemplateGeneratorExtensionPoint.getInstance().get(new ExtensionContext().add(files[j]).add(pre),pre);
			if (one==null) throw new UsageException("No template for "+pre);
			if (dpl==null) dpl = (TemplateGenerator[]) Array.newInstance(one.getClass(), files.length);
			dpl[j] = one;
		}
		
		te.accept(dpl);
		return te;
	}
	
}
