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
import java.util.logging.Logger;

import javax.script.ScriptException;

import gedi.app.Config;
import gedi.app.Gedi;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.LogUtils.LogMode;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.StreamLineWriter;
import gedi.util.io.text.jhp.Jhp;

public class Cluster {

	
	private static final Logger log = Logger.getLogger( Cluster.class.getName() );
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
		System.err.println("Cluster [options] <runner> <name> <command>");
		System.out.println();
		System.out.println("To identify parameter names, inspect your runner method file!");
		System.err.println();
		System.err.println("	Options:");
		System.err.println("	runner			Specify runner method (file in $HOME/.gedi/cluster)");
		System.err.println("	name			Specify a name for the cluster job");
		System.err.println("	command			Specify the command to run in the cluster");
		System.err.println();
		System.err.println("	-dry			Dry run, write batch script to stdout!");
		System.err.println("	-dep <tokens>			Run conditional on given tokens (e.g. -dep 3712,3722)");
		System.err.println("	--x.y[3].z=val	Specify runtime variable; val can be json (e.g. --x='{\\\"prop\\\":\\\"val\\\"}'");
		System.err.println("	-j [json-file]	Specify runtime variables");
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
	
	
	
	
	private static void start(String[] args) throws UsageException, IOException, ScriptException {
		String name = null;
		String command = null;
		String runner = null;
		DynamicObject param = DynamicObject.getEmpty();
		boolean dry = false;
		String dep = null;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null);
				return;
			}
			else if (args[i].equals("-j")) {
				String jsonfile  = checkParam(args, ++i);
				DynamicObject param1=DynamicObject.parseJson(FileUtils.readAllText(new File(jsonfile)));
				param = DynamicObject.merge(param,param1);
			}
			else if (args[i].equals("-dry")) {
				dry = true;
			}
			else if (args[i].equals("-dep")) {
				dep = checkParam(args, ++i);
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
		
		if (i!=args.length-3)
			throw new UsageException("Not enough parameters!");

		runner = args[i++];
		name = args[i++];
		command = args[i++];
		
		Gedi.startup(true,LogMode.Silent);

		String batch;
		DynamicObject defaultParam = DynamicObject.getEmpty();
		
		if (new File(runner).exists()) {
			batch = FileUtils.readAllText(new File(runner));
			if (new File(runner+".json").exists()) 
				defaultParam = DynamicObject.parseJson(FileUtils.readAllText(new File(runner+".json"))); 
		} else if (new File(Config.getInstance().getClusterFolder()+"/"+runner).exists()) {
			batch = FileUtils.readAllText(new File(Config.getInstance().getClusterFolder()+"/"+runner));
			if (new File(Config.getInstance().getClusterFolder()+"/"+runner+".json").exists()) 
				defaultParam = DynamicObject.parseJson(FileUtils.readAllText(new File(Config.getInstance().getClusterFolder()+"/"+runner+".json"))); 
		} else {
			throw new UsageException("No cluster description "+runner+" found!");
		}

		
		
		DynamicObject json = DynamicObject.getEmpty();
		if (Config.getInstance().getConfig().isObject())
			json = DynamicObject.cascade(json,Config.getInstance().getConfig());
		if (defaultParam.isObject())
			json = DynamicObject.cascade(json,defaultParam);
		if (param.isObject())
			json = DynamicObject.cascade(json,param);
		
		Jhp jhp = new Jhp();
		jhp.getJs().setInterpolateStrings(false);
		jhp.getJs().setSelf(json);
		jhp.getJs().putVariable("log", log);
		jhp.getJs().putVariable("name", name);
		jhp.getJs().putVariable("command", command);
		jhp.getJs().injectObject(json);
		
		
		if (dry) { 
			LineWriter out = new StreamLineWriter(System.out);
			out.writeLine(jhp.apply(batch));
			out.close();
			
		}
		else {
			File f = File.createTempFile("cluster", ".bash");
			f.deleteOnExit();
			LineWriter out = new LineOrientedFile(f.getPath()).write();
			out.writeLine(jhp.apply(batch));
			out.close();
			String jid = jhp.getJs().invokeFunction("exec", f.getAbsolutePath(), dep);
			System.out.println(jid);
		}
		
		
	}
	
}


