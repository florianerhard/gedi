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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.script.ScriptException;

import gedi.app.Config;
import gedi.app.Gedi;
import gedi.app.extension.ExtensionContext;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.dynamic.impl.MapDynamicObject;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.jhp.Jhp;
import gedi.util.io.text.jhp.JhpParameterException;
import gedi.util.job.pipeline.PipelineRunner;
import gedi.util.job.pipeline.PipelineRunnerExtensionPoint;

public class Pipeline {

	
	private static final Logger log = Logger.getLogger( Pipeline.class.getName() );
	public static void main(String[] args) {
		try {
			start(args);
		} catch (UsageException e) {
			usage("An error occurred: "+e.getMessage(), e.additional);
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("An error occurred: "+e.getMessage());
			e.printStackTrace();
		}
	}
	

	private static void usage(String message, String additional) {
		System.err.println();
		if (message!=null){
			System.err.println(message);
			System.err.println();
		}
		System.err.println("Pipeline [options] <template> [<template>...]");
		System.out.println();
		System.out.println("Takes one or more templates (either file names or labelled names from the resources), and executes them, thereby replacing values from json-files or given by --var=val.");
		System.err.println();
		System.err.println("	Options:");
		System.err.println("	-e 				Execute immediately");
		System.err.println("	-n [name]		Specify name (Default: Name of the last -j parameter, or the last template)");
		System.err.println("	-o [file]		Specify output file (Default: $wd/scripts/start.bash)");
		System.err.println("	-wd [folder]	Specify working directory (Default: `pwd`/$name)");
		System.err.println("	-log [folder]	Specify log folder (Default $wd/log)");
		System.err.println("	-r [runner]		Specify runner method (serial/parallel/cluster(<clustername>)); see gedi -e Cluster for details about clusternames)");
		System.err.println("	--x.y[3].z=val	Specify other runtime variable; val can be json (e.g. --x='{\\\"prop\\\":\\\"val\\\"}'");
		System.err.println("	-j [json-file]	Specify other runtime variables");
		System.err.println("");
		System.err.println("	-h [<template>...]	Print usage information (of this program and the given templates)");
		System.err.println("");
		if (additional!=null) {
			System.err.println(additional);
			System.err.println("");
		}
	}
	
	
	private static class UsageException extends Exception {
		String additional = null;
		public UsageException(String msg) {
			super(msg);
		}
		public UsageException(String msg, String additional) {
			super(msg);
			this.additional = additional;
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
	private static String[] checkPair(String[] args, int index) throws UsageException {
		int p = args[index].indexOf('=');
		if (!args[index].startsWith("--") || p==-1) throw new UsageException("Not an assignment parameter (--name=val): "+args[index]);
		return new String[] {args[index].substring(2, p),args[index].substring(p+1)};
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
	
	
	
	
	private static void start(String[] args) throws UsageException, IOException, ScriptException, InterruptedException {
		String out = null;
		String wd = null;
		String name = null;
		String lastJsonName = null;
		String logfolder = null;
		String runner = "serial";
		DynamicObject param = DynamicObject.getEmpty();
		boolean exec = false;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				ArrayList<String> list = new ArrayList<>();
				i = checkMultiParam(args, i+1, list);
				if (list.isEmpty())
					usage(null,null);
				else {
					Jhp jhp = new Jhp();
					for (String t : list)
						jhp.apply(readSrc(t), "0");
					usage(null,"\nTemplate variables:\n\n"+jhp.getParameters().getUsage("wd","name","tokens"));
				}
				return;
			}
			else if (args[i].equals("-log")) {
				logfolder = checkParam(args, ++i);
			}
			else if (args[i].equals("-wd")) {
				wd = checkParam(args, ++i);
			}
			else if (args[i].equals("-o")) {
				out = checkParam(args, ++i);
			}
			else if (args[i].equals("-n")) {
				name = checkParam(args, ++i);
			}
			else if (args[i].equals("-e")) {
				exec = true;
			}
			else if (args[i].equals("-r")) {
				runner = checkParam(args, ++i);
				DynamicObject param1=DynamicObject.parseExpression("runner", DynamicObject.parseJsonOrString(runner));
				param = DynamicObject.merge(param,param1);
			}
			else if (args[i].equals("-j")) {
				String jsonfile  = checkParam(args, ++i);
				DynamicObject param1=DynamicObject.parseJson(FileUtils.readAllText(new File(jsonfile)));
				param = DynamicObject.merge(param,param1);
				lastJsonName = jsonfile;
			}
			else if (args[i].equals("-D")) {
			}
			else if (!args[i].startsWith("-")) 
					break;
			else if (args[i].startsWith("--")) {
				String[] p = checkPair(args,i);
				
				DynamicObject param1=DynamicObject.parseExpression(p[0], DynamicObject.parseJsonOrString(p[1]));
				param = DynamicObject.merge(param,param1);
			}
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}
		
		if (i>args.length-1)
			throw new UsageException("No templates given!");
		
		if (name==null && lastJsonName!=null) name = FileUtils.getNameWithoutExtension(lastJsonName);
		if (name==null) name = FileUtils.getNameWithoutExtension(args[args.length-1]);
		
		if (name==null) throw new UsageException("No name given (either -n or by using the name of the last -j parameter)");
		
//		if (wd==null && lastJsonName!=null) wd = new File(StringUtils.removeFooter(lastJsonName, ".json")).getAbsolutePath();
		if (wd==null) wd = "./"+name;
		wd = new File(wd).getAbsolutePath();
		
		HashMap<String,Object> defaults = new HashMap<>();
		defaults.put("wd", wd);
		defaults.put("tmp",System.getProperty("java.io.tmpdir"));
		defaults.put("name",name);
		DynamicObject json = DynamicObject.from(defaults);
		
		Gedi.startup(true);
		
		log.info("Setting up runner");
		PipelineRunner pipelineRunner = PipelineRunnerExtensionPoint.getInstance().get(new ExtensionContext(), runner);
		if (pipelineRunner==null) throw new UsageException("Could not setup runner!");
		
		log.info("Reading json");
		if (Config.getInstance().getConfig().isObject())
			json = DynamicObject.cascade(Config.getInstance().getConfig(),json);
		if (param.isObject())
			json = DynamicObject.cascade(json,param);
		
		HashMap<String,Object> over = new HashMap<>();
		if (wd!=null) 	over.put("wd", new File(wd).getAbsolutePath());
		if (name!=null)	over.put("name", name);
		over.put("id", json.getEntry("name"));
		over.put("logfolder",logfolder==null?json.getEntry("wd")+"/log":new File(logfolder).getAbsolutePath());
		
		json = DynamicObject.cascade(json,DynamicObject.from(over));
		Jhp jhp = new Jhp();
		jhp.getJs().putVariable("log", log);
		jhp.getJs().putVariable("js", jhp.getJs());
		jhp.getJs().setInterpolateStrings(false);
		jhp.getJs().setSelf(json);
		
		
		if (out==null) out = jhp.getJs().getVariable("wd")+"/scripts/start.bash";
		
		new File(out).getAbsoluteFile().getParentFile().mkdirs();
		new File((String)jhp.getJs().getVariable("logfolder")).mkdirs();
		
		
		String paramFile = jhp.getJs().getVariable("wd")+"/"+jhp.getJs().getVariable("name")+".params.json";
		FileUtils.writeAllText(json.toJson(), new File(paramFile));
		
		
		PipelineFileProcessor proc = new PipelineFileProcessor(jhp,paramFile,out, pipelineRunner);
		jhp.getJs().injectObject(proc);
		
		for (; i<args.length; i++) {
			try {
				proc.include(args[i]);
				proc.jhp.getParameters().nextFile();
			} catch (JhpParameterException e) {
				throw new UsageException("Variable not set: "+e.getVariableName(),jhp.getParameters().getUsage());
			}
		}
		proc.finish();
		
		
		if (exec)
			Runtime.getRuntime().exec(out).waitFor();
	}
	
	public static class PipelineFileProcessor {
		private Jhp jhp;
		private String paramFile;
		private String out;
		private PipelineRunner pipelineRunner;
		private HashMap<String, PipelineOutputOptions> outputs = new HashMap<>();
		
		private LineWriter jswriter = new LineWriter() {
			
			@Override
			public void write(String line) throws IOException {
				try {
					jhp.getJs().invokeFunction("print",line);
				} catch (ScriptException e) {
					throw new IOException(e);
				}
			}
			
			@Override
			public void flush() throws IOException {
			}
			
			@Override
			public void close() throws IOException {
			}
		};
		
		public PipelineFileProcessor(Jhp jhp, String paramFile, String out, PipelineRunner pipelineRunner) {
			super();
			this.jhp = jhp;
			this.paramFile = paramFile;
			this.out = out;
			this.pipelineRunner = pipelineRunner;
		}

		public void include(String name) throws IOException, ScriptException, UsageException {
			processTemplate(name,out);
		}
		
		public void processTemplate(String name, String out) throws IOException, ScriptException, UsageException {
			
			PipelineOutputOptions options = new PipelineOutputOptions();
			
			Object old = jhp.getJs().getVariable("output");
			
			jhp.getJs().putSystemVariable("output", options);
			
			
			log.info("Writing to "+out);
			
			LineWriter writer = outputs.computeIfAbsent(out, x->{
				LineOrientedFile file = new LineOrientedFile(new File(x).getAbsolutePath());
				return options.write(file);
			}).writer;
			
			writer.writeLine(jhp.apply(readSrc(name)));
			options.setExecutable(name.endsWith(".sh"));
			
			jhp.getJs().putSystemVariable("output", old);
			
		}
		
		
		
		public void prerunner(String name, int... tokens) {
			if (tokens.length==1 && tokens[0]==0) tokens = new int[0];
			pipelineRunner.prerunner(jswriter, name, paramFile, jhp.getJs(), tokens);
		}
		public int postrunner(String name) {
			return pipelineRunner.postrunner(jswriter, name, paramFile, jhp.getJs());
		}
		
		private void finish() throws IOException {
			for (PipelineOutputOptions lw : outputs.values()) {
				log.info("Wrote file "+lw.file);
				lw.finish();
			}
		}
		
		
	}
	
	public static class PipelineOutputOptions {
		public boolean executable = false;
		public LineOrientedFile file;
		public LineWriter writer;

		


		public void setExecutable(boolean executable) {
			this.executable = executable;
		}
		
		
		private void finish() throws IOException {
			writer.close();
			if (executable)
				file.setExecutable(true);
			
		}
		private PipelineOutputOptions write(LineOrientedFile file) {
			this.file = file;
			writer = file.write();
			return this;
		}
	}


	public static String readSrc(String name) throws IOException, UsageException {
		String src;
		if (!new File(name).exists()) {
			URL res = Pipeline.class.getResource("/resources/pipeline/"+name);
			if (res!=null) {
				src = new LineIterator(res.openStream()).concat("\n");
				log.info("Processing template "+name);
			} else {
				throw new UsageException("Template "+name+" unknown!");
			}
		}
		else {
			src = FileUtils.readAllText(new File(name));
			log.info("Processing template file "+name);
		}
		return src;
	}
}


