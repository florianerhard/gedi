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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import gedi.app.Gedi;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.IgnoreVariationsAlignedReadsData;
import gedi.core.data.reads.SelectDistinctSequenceAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.core.region.feature.output.FeatureStatisticOutput;
import gedi.core.region.feature.output.PlotReport;
import gedi.core.region.feature.output.SecondaryPlot;
import gedi.core.region.feature.special.Downsampling;
import gedi.core.workspace.Workspace;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.jhp.TemplateEngine;
import gedi.util.java.sizeof.RamUsageEstimator;
import gedi.util.nashorn.JS;
import gedi.util.oml.OmlNodeExecutor;
import gedi.util.oml.OmlReader;
import gedi.util.oml.petrinet.GenomicRegionFeaturePipeline;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class Stats {

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
		System.err.println("Stats <Options> <storage-file>");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" -l <location>\t\t\tLocation string");
		System.err.println(" -g <genome1 genome2 ...>\t\t\tGenome names");
		System.err.println(" -downsampling <method>\t\t\tSet downsampling method (one of "+StringUtils.concat(",", Downsampling.values())+", default is "+Downsampling.values()[0]+"); be aware that depending on the -ignoreReadVariations flag, this may either downsample within a variation group or for all reads mapped at a specific location!");
		System.err.println(" -mode <mode>\t\t\tSet read count mode (one of "+StringUtils.concat(",", ReadCountMode.values())+", default is "+ReadCountMode.Weight+")");
		System.err.println(" -ignoreReadVariations\t\t\tIgnores all variations in reads");
		System.err.println(" -count\t\t\tOnly count reads; implies ignoreReadVariations!");
		System.err.println(" -interactive\t\t\tDisplay all plots progressively in a graphical window");
		System.err.println(" -noplots\t\t\tDo not generate any plots!");
		System.err.println(" -prefix <prefix>\t\t\tPrefix for output files (default: report/<filename>.)");
		System.err.println(" -out [<oml>]\t\tWrite current pipeline to oml file (and do not process storage)");
		System.err.println("");
		System.err.println(" -h [<template>]	Print usage information (of this program and the given template)");
		System.err.println(" --x.y[3].z=val\t\tDefine a template variable; val can be json (e.g. --x='{\\\"prop\\\":\\\"val\\\"}'");
		System.err.println(" -j <json-file>\t\tDefine template variables");
		System.err.println(" -t <template-file>\t\tProcess a template (default: reads for a storage containing reads, default otherwise)");
		System.err.println();
		System.err.println(" -oml [<oml>]\t\tProcess using the given oml file");
		System.err.println();
		System.err.println(" -nthreads <int>\t\t\tRun with given number of threads (default: min(8 or number of available threads))");
		System.err.println(" -p\t\t\tShow progress");
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
	
	@SuppressWarnings("unchecked")
	public static void start(String[] args) throws Exception {
		Gedi.startup(true);
		
		TemplateEngine te = new TemplateEngine();
		te.addTemplateSearchURL("classpath://Gedi/resources/templates/stats/${name}");
		Genomic g = null;
		String out = null;
		int nthreads = Math.min(8,Runtime.getRuntime().availableProcessors());
		Progress progress = new NoProgress();
		String oml = null;
		String template = null;
		boolean interactive = false;
		boolean ignoreVariations = false;
		boolean count = false;
		boolean noplots = false;
		Downsampling downsampling  = Downsampling.values()[0];
		ReadCountMode mode = ReadCountMode.Weight;
		String loc = null;
		
		for (int i=0; i<args.length; i++) {
			
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
		}
		
		String storageFile = checkParam(args, args.length-1);
		GenomicRegionStorage<?> storage = Workspace.loadItem(storageFile);
		boolean isRead = AlignedReadsData.class.isAssignableFrom(storage.getType());
		te.set("storage", storage);
		te.set("isRead", isRead);
		
		if (isRead) {
			GenomicRegionStorage<? extends AlignedReadsData> s = (GenomicRegionStorage<? extends AlignedReadsData>) storage;
			String[] labels = EI.wrap(storage.getMetaData().getEntry("conditions").asArray()).map(d->d.getEntry("name").asString()).toArray(String.class);
			if (labels.length!=s.getRandomRecord().getNumConditions())
				labels = EI.seq(0,s.getRandomRecord().getNumConditions()).map(i->"C"+i).toArray(String.class);
			HashSet<String> uni = new HashSet<>();
			for (int i=0; i<labels.length; i++) {
				if (labels[i].isEmpty() || !uni.add(labels[i]))
					labels[i] = "C"+i;
			}
			te.set("labels", labels);
		}
		te.set("prefix", new File(args[args.length-1]).getAbsoluteFile().getParent()+"/report/"+FileUtils.getNameWithoutExtension(args[args.length-1])+".");
		
		args = ArrayUtils.redimPreserve(args, args.length-1);
		
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
			else if (args[i].equals("-oml")) {
				oml = checkParam(args,++i);
			}
			else if (args[i].equals("-ignoreReadVariations")) {
				ignoreVariations = true;
			}
			else if (args[i].equals("-count")) {
				count = true;
				ignoreVariations = true;
			}
			else if (args[i].equals("-noplots")) {
				noplots = true;
			}
			else if (args[i].equals("-downsampling")) {
				downsampling = ParseUtils.parseEnumNameByPrefix(checkParam(args,++i),true,Downsampling.class);
			}
			else if (args[i].equals("-mode")) {
				mode = ParseUtils.parseEnumNameByPrefix(checkParam(args,++i),true,ReadCountMode.class);
			}
			else if (args[i].equals("-out")) {
				out = checkParam(args, ++i);
			}
			else if (args[i].equals("-p")) {
				progress = new ConsoleProgress(System.err);
			}
			else if (args[i].equals("-interactive")) {
				interactive = true;
			}
			else if (args[i].equals("-prefix")) {
				te.set("prefix", checkParam(args, ++i));
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
				template = checkParam(args, ++i);
			}else if (args[i].equals("-nthreads")) {
				nthreads = checkIntParam(args, ++i);
			}
//			else if (!args[i].startsWith("-")) 
//					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}

		
		if (oml!=null) {
			log.info("Loading pipeline from file");
			te.direct(new LineOrientedFile(oml).readAllText());
		} else if (template!=null) {
			log.info("Loading pipeline from given template: "+template);
			te.template(template);
		} else  if (isRead) {
			if (count) {
				log.info("Loading read count pipeline");
				te.template("countreads");
			} else {
				log.info("Loading read pipeline");
				te.template("reads");
			}
		} else {
			log.info("Loading default pipeline");
			te.template("default");
		}
		
		String src = te.toString();
		if (out!=null) {
			new LineOrientedFile(out).writeAllText(src);
			log.info("Written pipeline to file!");
			return;
		}

		if (g==null) throw new UsageException("No genomes given!");
	
		log.info("Executing pipeline");
		OmlNodeExecutor exec = new OmlNodeExecutor();
		
		HashMap context = new HashMap();
		context.put("genomic", g);

		GenomicRegionFeaturePipeline pipe = (GenomicRegionFeaturePipeline)exec.execute(new OmlReader().parse(src),context);
		
		GenomicRegionFeatureProgram program = pipe.getProgram();
		if (interactive)
			new JS().putVariable("program", program).execSource("FX.observeResults(program.getResultProducers())");
		
		log.info("Using prefix "+te.<String>get("prefix"));
		
		log.info("Using downsampling mode: "+downsampling);
//		program.setBenchmark(true);
		program.setThreads(nthreads);
		
		if (noplots) {
			log.info("Skipping plots!");
			EI.seq(0, program.getNumFeatures())
					.map(ind->program.getFeature(ind))
					.castFiltered(FeatureStatisticOutput.class)
					.forEachRemaining(f->f.getPlots().clear());
		}
		
		if (storage.getRandomRecord() instanceof AlignedReadsData) {
			log.info("Using read mode: "+mode);
			GenomicRegionStorage<? extends AlignedReadsData> s = (GenomicRegionStorage<? extends AlignedReadsData>) storage;
			GenomicRegionFeatureProgram<? extends AlignedReadsData> p = program;
			Downsampling udownsampling = downsampling;
			ReadCountMode umode = ignoreVariations?ReadCountMode.Weight:mode;
			p.setDataToCounts((ard,b)->udownsampling.downsample(ard.getCountsForDistinct(b, 0, umode)));
			
			program.begin();
			progress.init();
			progress.setCount((int) storage.size());
			
			MutableReferenceGenomicRegion<AlignedReadsData> mut = new MutableReferenceGenomicRegion<AlignedReadsData>();
			if (ignoreVariations)
				for (ImmutableReferenceGenomicRegion<? extends AlignedReadsData> r : s.ei(loc).loop()) {
					program.accept(mut.set(r.getReference(),r.getRegion(),new IgnoreVariationsAlignedReadsData(r.getData(),mode)));
					progress.incrementProgress();
				}
			else
				for (ImmutableReferenceGenomicRegion<? extends AlignedReadsData> r : s.ei(loc).loop()) {
					for (int d=0; d<r.getData().getDistinctSequences(); d++) {
						program.accept(mut.set(r.getReference(),r.getRegion(),new SelectDistinctSequenceAlignedReadsData(r.getData(),d)));
					}
					progress.incrementProgress();
				}
			
			
			progress.finish();
			program.end();
			
			for (DataFrame df : EI.seq(0, p.getNumFeatures())
				.map(ind->p.getFeature(ind))
				.filter(f->f.getId().endsWith("total.stat"))
				.castFiltered(FeatureStatisticOutput.class)
				.getUnique(false, false)
					.map(f->f.getResults())
					.loop()) {
			
				DynamicObject totals = DynamicObject.from("conditions",EI.wrap(df.getRow(0)).skip(1).map(c->DynamicObject.from("total",c)).toArray(DynamicObject.class));
				
				DynamicObject meta = s.getMetaData().cascade(totals);
				s.setMetaData(meta);
			}
		}
		else {
		
			program.begin();
			progress.init();
			progress.setCount((int) storage.size());
			storage.ei(loc).progress(progress, (int)storage.size(), r->r.toLocationStringRemovedIntrons()).forEachRemaining(program);
			progress.finish();
			program.end();
		}
//		new AlignedReadsDataToFeatureProgram(program).setProgress(progress).processStorage(storage);
//		program.printBenchmark();
		
		log.info("Finished producing statistics");
		
		if (!count) {
			File json = new File(te.<String>get("prefix")+"report.json");
			
			
			ArrayList<PlotReport> plots = EI.seq(0, program.getNumFeatures())
				.map(ind->program.getFeature(ind))
				.castFiltered(FeatureStatisticOutput.class)
				.unfold(f->EI.wrap(f.getPlots()))
				.filter(f->f.getImageFile()!=null)
				.map(f->new PlotReport(f.getSection(),
							StringUtils.toJavaIdentifier(f.getName()), 
							StringUtils.removeHeader(f.getTitle(),storage.getName()+"."), 
							f.getDescription(),
							new File(f.getImageFile()).getName(),
							findSecondaryPlots(new File(f.getImageFile())),
							new File(f.getScriptFile()).getName(),
							new File(f.getCsvFile()).getName()
							)
				)
					
				.list();
			
			LinkedHashMap<String, String> map = new LinkedHashMap<>();
			map.put("title", storage.getName());
			map.put("downsampling", downsampling.name());
			map.put("mode", mode.name());
			DynamicObject dprops = DynamicObject.from(map);
			DynamicObject dplots = DynamicObject.from("plots", DynamicObject.from(plots));
			
			FileUtils.writeAllText(dprops.merge(dplots).toJson(),json);
			
			String[] jsons = EI.wrap(json.getAbsoluteFile().getParentFile().list())
					.filter(s->s.endsWith("report.json"))
					.sort((a,b)->Long.compare(new File(a).lastModified(), new File(b).lastModified()))
					.map(a->new File(json.getParentFile(),a).getPath())
					.toArray(String.class);
			
			if (jsons.length>0)
				new Report(jsons).write(new LineOrientedFile(json.getParentFile(),"index.html").write());
		}
	}
	
	private static SecondaryPlot[] findSecondaryPlots(File main) {
		String pref = FileUtils.getNameWithoutExtension(main)+".";
		String ext = "."+FileUtils.getExtension(main);
		try {
			SecondaryPlot[] re = EI.fileNames(main.getParent())
					.filter(f->f.startsWith(pref) && f.endsWith(ext) && !f.equals(main.getName()))
					.map(f->new SecondaryPlot(f.substring(pref.length(),f.length()-ext.length()), f))
					.toArray(SecondaryPlot.class);
			if (re.length==0) return null;
			re = ArrayUtils.concat(new SecondaryPlot[] {new SecondaryPlot("Combined", main.getName())},re);
			return re;
		} catch (IOException e) {
			throw new RuntimeException("Could not identify secondary images!",e);
		}
	}
	
}
