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
package gedi.util.program;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.StringUtils;
import gedi.util.program.parametertypes.BooleanParameterType;
import gedi.util.userInteraction.progress.ConsoleProgress;

public class CommandLineHandler {

	private String[] args;
	private String title;
	private String description;
	
	public CommandLineHandler(String title, String description, String[] args) {
		this.title  = title;
		this.args = args;
		this.description = description;
	}
	

	/**
	 * If everything was fine, returns null, an error message otherwise
	 * @param spec
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public String parse(GediParameterSpec spec, GediParameterSet set) {
		try {
			spec.add("Commandline", getProgress(set),getD(set),getH(set),getHh(set),getHhh(set),getDry(set),getKeep(set));
			
			int i;
			for (i=0; i<args.length; i++) {
				
				if (args[i].startsWith("-")) {
					
					String name = args[i].substring(1);
					if (name.startsWith("-")) name = name.substring(1);
					
					String value = null;
					if (name.indexOf('=')>=0) {
						int ind = name.indexOf('=');
						value = name.substring(ind+1);
						name = name.substring(0, ind);
					}
					
					GediParameter param = spec.get(name);
					if (param==null)
						return "Unknown parameter: "+args[i];
					if (param.getType().hasValue() && value==null) {
						++i;
						if (i>=args.length || args[i].startsWith("-")) {
							return "Missing argument for "+args[i-1];
						}
						value = args[i];
						
						if (param.isMulti() && param.getType().parsesMulti()) {
							StringBuilder sb = new StringBuilder().append(value);
							while (i+1<args.length && !args[i+1].startsWith("-")) 
								sb.append(" ").append(args[++i]);
							param.set(param.getType().parse(sb.toString()));
						} else {
						
							param.set(param.getType().parse(value));
							if (param.isMulti()) {
								while (i+1<args.length && !args[i+1].startsWith("-")) {
									param.set(param.getType().parse(args[++i]));
								}
							}
						}
					} 
					else
						param.set(param.getType().parse(value));
				}
				else {
					return "Unknown parameter: "+args[i];
				}
			}
			
			return null;
		} catch (Throwable e) {
			return e.getMessage();
		}
	}


	/**
	 * 
	 * @param error
	 * @param input
	 * @param output
	 * @param verbosity 0,1,2
	 */
	public void usage(String error, GediParameterSpec input, GediParameterSpec output, int verbosity) {
		System.err.println(title);
		System.err.println(description);
		System.err.println();
		if (error!=null){
			System.err.println("Error:");
			System.err.println(error);
			System.err.println();
		} else
			System.err.println("Usage:");
		
		if (input.parameters.size()==0) {
			System.err.println("gedi -e "+title);
		} else {
			
			System.err.println("gedi -e "+title+" <Options>");
			System.err.println();
			
			if (verbosity==1)
				System.err.println(input.getMandatoryUsage());
			else
				System.err.println(input.getUsage(verbosity>2,verbosity>2,false));
			
		}
		
		if (output.parameters.size()>0 && verbosity>1) {
			String out = output.getUsage(verbosity>2,verbosity>2,false);
			if (out.length()>0) {
				System.err.println("Output:");
				System.err.println();
				System.err.println(out);
			}
		}
		
	}
	
	
	
	


	public static final String h = "h";
	public static final String hh = "hh";
	public static final String hhh = "hhh";
	public static final String progress = "progress";
	public static final String D = "D";
	public static final String dry = "dry";
	public static final String keep = "keep";
	
	public static GediParameter<Boolean> getD(GediParameterSet set) {return new GediParameter<>(set,D, "Verbose output of errors",false, new BooleanParameterType());}
	public static GediParameter<Boolean> getDry(GediParameterSet set) {return new GediParameter<>(set,dry, "Dry run", false,new BooleanParameterType());}
	public static GediParameter<Boolean> getProgress(GediParameterSet set) {return new GediParameter<>(set,progress, "Show progress", false,new BooleanParameterType());}
	public static GediParameter<Boolean> getH(GediParameterSet set) {return new GediParameter<>(set,h, "Show usage", false,new BooleanParameterType());}
	public static GediParameter<Boolean> getHh(GediParameterSet set) {return new GediParameter<>(set,hh, "Show verbose usage", false,new BooleanParameterType());}
	public static GediParameter<Boolean> getHhh(GediParameterSet set) {return new GediParameter<>(set,hhh, "Show extra verbose usage", false,new BooleanParameterType());}
	public static GediParameter<Boolean> getKeep(GediParameterSet set) {return new GediParameter<>(set,keep, "Do not remove temp files", false,new BooleanParameterType());}
	
	
}

