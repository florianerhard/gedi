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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.program.parametertypes.FileParameterType;

public class GediParameterSpec {

	ArrayList<GediParameter> list = new ArrayList<>();
	LinkedHashMap<String,GediParameter[]> parameters = new LinkedHashMap<>();
	private LinkedHashMap<String,GediParameter> index = new LinkedHashMap<>();
	
	
	public GediParameterSpec() {
		parameters.put("General", new GediParameter[0]);
	}
	
	public GediParameterSpec(Collection<GediParameter> inputs) {
		this();
		add(inputs.toArray(new GediParameter[0]));
	}


	public void add(GediParameter...parameters) {
		add("General options",parameters);
	}
	
	public void addOrMoveToGeneral(String category, GediParameter param) {
		if (contains(param)) {
			for (String cat : parameters.keySet()) {
				int index = ArrayUtils.find(parameters.get(cat), p->p.getName().equals(param.getName()));
				if (index>=0)
					parameters.put(cat, ArrayUtils.removeItemFromArray(parameters.get(cat), index));
			}
			index.remove(param.getName());
			list.remove(param);
			add("General",param);
		} else
			add(category,param);
	}
	
	public void add(String category, GediParameter...parameters) {
		for (GediParameter p : parameters) {
			if (index.containsKey(p.getName()))
				throw new RuntimeException("More than one parameter with name "+p.getName());
			index.put(p.getName(), p);
			list.add(p);
		}
		if (this.parameters.containsKey(category)) 
			parameters = ArrayUtils.concat(this.parameters.get(category),parameters);
		this.parameters.put(category, parameters);
	}
	
	public int size() {
		return list.size();
	}
	
	public Collection<String> getNames() {
		return index.keySet();
	}


	public GediParameter<?> get(String name) {
		return index.get(name);
	}
	
	public GediParameter<?> get(int index) {
		return list.get(index);
	}

	public boolean contains(GediParameter s) {
		return index.containsKey(s.getName());
	}

	
	public String getMandatoryUsage() {
		StringBuilder sb = new StringBuilder();
		int pad = 0;
		for (String cat : parameters.keySet()) 
			for (GediParameter<?> p : parameters.get(cat))
				if (!p.hasDefault() && !p.isOptional() && !p.getType().isInternal() && p.getType().hasValue())
					pad = Math.max(pad,p.getUsage(-1).length());
		pad+=4;
		
		for (String cat : parameters.keySet()) {
			if (parameters.get(cat).length>0){
				sb.append(cat+":\n");
				int start = sb.length();
				for (GediParameter<?> p : parameters.get(cat)) {
					if (!p.hasDefault() && !p.isOptional() && !p.getType().isInternal() && p.getType().hasValue())
						sb.append(p.getUsage(pad)).append("\n");
					else if (cat.equals("Commandline"))
						sb.append(p.getUsage(pad)).append("\n");
				}
				if (sb.length()==start)
					sb.delete(sb.length()-cat.length()-2, sb.length());
				else
					sb.append("\n");
			}
		}
		
		return sb.toString();
	}
	
	public String getUsage(boolean files, boolean optionals, boolean internals) {
		StringBuilder sb = new StringBuilder();
		int pad = 0;
		for (String cat : parameters.keySet()) 
			for (GediParameter<?> p : parameters.get(cat))
				if (!p.isFile())
					pad = Math.max(pad,p.getUsage(-1).length());
		pad+=4;
		
		for (String cat : parameters.keySet()) {
			if (parameters.get(cat).length>0){
				sb.append(cat+":\n");
				int start = sb.length();
				for (GediParameter<?> p : parameters.get(cat)) {
					if (!p.isFile() && !p.isOptional() && !p.getType().isInternal())
						sb.append(p.getUsage(pad)).append("\n");
				}
				if (optionals)
					for (GediParameter<?> p : parameters.get(cat)) {
						if (!p.isFile() && p.isOptional() && !p.getType().isInternal())
							sb.append(p.getUsage(pad)).append("\n");
					}
				if (files) {
					for (GediParameter<?> p : parameters.get(cat)) {
						if (p.isFile() && !p.isOptional() && !p.getType().isInternal())
							sb.append(p.getUsage(pad)).append("\n");
					}
					if (optionals)
						for (GediParameter<?> p : parameters.get(cat)) {
							if (p.isFile() && p.isOptional() && !p.getType().isInternal())
								sb.append(p.getUsage(pad)).append("\n");
						}
				}
				if (internals) {
					for (GediParameter<?> p : parameters.get(cat)) {
						if (p.getType().isInternal())
							sb.append(p.getUsage(pad)).append("\n");
					}
				}
				if (sb.length()==start)
					sb.delete(sb.length()-cat.length()-2, sb.length());
				else
					sb.append("\n");
			}
		}
		
		return sb.toString();
	}
	
	public String getFiles() {
		StringBuilder sb = new StringBuilder();
		for (String cat : parameters.keySet()) {
			if (parameters.get(cat).length>0){
				sb.append(cat+":\n");
				for (GediParameter<?> p : parameters.get(cat)) {
					if (p.isFile())
						sb.append(p.getName()).append(" current value:").append(p.getFile()).append("\n");
				}
				sb.append("\n");
			}
		}
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return getUsage(true,true,true);
	}

	public void writeParameterFile(File file) throws IOException {
		LineWriter out = new LineOrientedFile(file.getPath()).write();
		for (GediParameter p : list) 
			out.writef("%s\t%s\n", p.getName(),StringUtils.toString(p.get()));
		out.close();
	}
}
