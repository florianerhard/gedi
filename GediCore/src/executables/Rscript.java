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
import java.io.FileInputStream;
import java.util.logging.Logger;

import gedi.app.Config;
import gedi.app.Gedi;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.r.RRunner;

public class Rscript {

	private static final Logger log = Logger.getLogger( Rscript.class.getName() );
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
		System.err.println("R <Options> script.R...");
		System.err.println();
		System.err.println("Options:");
		System.err.println(" --x.y[3].z=val\t\tDefine a template variable; val can be json (e.g. --x='{\\\"prop\\\":\\\"val\\\"}'");
		System.err.println(" -j <json-file>\t\tDefine template variables");
		System.err.println(" -r <r-file>\t\tKeep the script file with this path");
		System.err.println(" -dontrun \t\tDo not run, only sensible with -r.");
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
	
	
	private static String checkParam(String[] args, int index) throws UsageException {
		if (index>=args.length || args[index].startsWith("-")) throw new UsageException("Missing argument for "+args[index-1]);
		return args[index];
	}
	
	private static String[] checkPair(String[] args, int index) throws UsageException {
		int p = args[index].indexOf('=');
		if (!args[index].startsWith("--") || p==-1) throw new UsageException("Not an assignment parameter (--name=val): "+args[index]);
		return new String[] {args[index].substring(2, p),args[index].substring(p+1)};
	}
	
	
	
	public static void start(String[] args) throws Exception {
		Gedi.startup(true);
		
		DynamicObject param = DynamicObject.getEmpty();
		String script = null;
		boolean dontrun = false;
		
		int i;
		for (i=0; i<args.length; i++) {
			
			if (args[i].equals("-h")) {
				usage(null,null);
				return;
			}
			else if (args[i].equals("-D")) {
			}else if (args[i].equals("-j")) {
				String path = checkParam(args, ++i);
				DynamicObject param1 = DynamicObject.parseJson(FileUtils.readAllText(new File(path)));
				param = DynamicObject.cascade(param,param1);
			}else if (args[i].equals("-r")) {
				script = checkParam(args, ++i);
			}else if (args[i].equals("-dontrun")) {
				dontrun = true;
			}
			else if (args[i].startsWith("--")) {
				String[] p = checkPair(args,i);
				
				DynamicObject param1=DynamicObject.parseExpression(p[0], DynamicObject.parseJsonOrString(p[1]));
				param = DynamicObject.cascade(param,param1);
			}
			else if (!args[i].startsWith("-")) 
					break;
			else throw new UsageException("Unknown parameter: "+args[i]);
			
		}

		
		boolean deleteScript = script==null;
		if (script==null) script = File.createTempFile("Rscript", ".R").getAbsolutePath();
		
		RRunner r = new RRunner(script);
		for (String n : param.getProperties())
			r.set(n,param.getEntry(n));
		
		for (; i<args.length; i++) {
			// try to find rscript:
			String rscript = args[i];
			if (Rscript.class.getResourceAsStream(rscript)!=null)
				r.addSource(Rscript.class.getResourceAsStream(rscript));
			else if (Rscript.class.getResourceAsStream("/resources/"+rscript)!=null)
				r.addSource(Rscript.class.getResourceAsStream("/resources/"+rscript));
			else if (Rscript.class.getResourceAsStream("/resources/R/"+rscript)!=null)
				r.addSource(Rscript.class.getResourceAsStream("/resources/R/"+rscript));
			else if (new File(rscript).exists())
				r.addSource(new FileInputStream(new File(rscript)));
			else new UsageException("Script not found!");
		}
		
		
		if (dontrun)
			r.dontrun(deleteScript);
		else
			r.run(deleteScript);

		
	}

	
}
