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
import java.util.ArrayList;

import gedi.util.StringUtils;
import gedi.util.program.parametertypes.FileParameterType;
import gedi.util.program.parametertypes.GediParameterType;

public class GediParameter<T> {

	private T defaultValue;
	private T value;
	private ArrayList<T> values;
	private GediParameterType<T> type;
	private String name;
	private String description;
	private GediParameterSet set;
	boolean optional;
	boolean multi;
	boolean removeFile = false;

	public GediParameter(GediParameterSet set, String name, String description, boolean multi, GediParameterType<T> type) {
		this(set, name,description,multi,type,type.getDefaultValue(),false);
	}
	
	public GediParameter(GediParameterSet set, String name, String description, boolean multi, GediParameterType<T> type, T defaultValue) {
		this(set,name,description,multi,type,defaultValue,false);
	}
	
	public GediParameter(GediParameterSet set, String name, String description, boolean multi, GediParameterType<T> type, boolean optional) {
		this(set, name,description,multi,type,type.getDefaultValue(),optional);
	}
	
	public GediParameter(GediParameterSet set, String name, String description, boolean multi, GediParameterType<T> type, T defaultValue, boolean optional) {
		this.set = set;
		this.value = this.defaultValue = defaultValue;
		this.name = name;
		this.description = description;
		this.multi = multi;
		this.type = type;
		this.optional = optional;
		if (multi) values = new ArrayList<>();
		set.add(this);
	}

	public String getName() {
		return name;
	}
	
	public GediParameter<T> setRemoveFile(boolean removeFile) {
		this.removeFile = removeFile;
		return this;
	}
	
	public boolean isRemoveFile() {
		return removeFile;
	}
	
	public boolean isOptional() {
		return optional;
	}
	
	public boolean isMulti() {
		return multi;
	}
	
	public String getUsage(int pad) {
		StringBuilder sb = new StringBuilder();
		if (isOptional()) sb.append("[");
		
		if (isFile()) sb.append(getFile());
		else {
			sb.append(" -").append(name);
			if (type.hasValue())
				sb.append(" <").append(name).append(">");
		}
		
		if (pad==-1) return sb.toString();
		
		pad-=sb.length();
		for (int i=0; i<pad; i++)
			sb.append(' ');
		sb.append(description);
		
		if (defaultValue!=null && type.hasValue()) sb.append(" (default: ").append(defaultValue).append(")");
		
		String help = type.helpText();
		if (help!=null)
			sb.append(" ").append(help);
		
		if (isOptional()) sb.append("]");
		
		return sb.toString();
	}
	
	public boolean hasDefault(){
		return defaultValue!=null;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void set(T value) {
		if (isMulti())
			this.values.add(value);
		this.value = value;
	}
	
	public boolean isFile() {
		return type.getType()==File.class;
	}
	
	public File getFile() {
		return FileParameterType.getFile(getName(),getParameterSet());
	}

	public T get() {
		if (isFile()) {
			File f = getFile();
			if (f.exists() && f.length()>0) return (T) f;
			return null;
		}
		return value;
	}
	
	public ArrayList<T> getList() {
		return values;
	}
	
	public GediParameterType<T> getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GediParameter other = (GediParameter) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name+"="+get();
	}

	public GediParameterSet getParameterSet() {
		return set;
	}

	
	
	
	
}
