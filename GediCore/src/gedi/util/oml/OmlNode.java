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

package gedi.util.oml;

import gedi.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class OmlNode {

	public static final String IDATTRIBUTE = "id";
	public static final String CLASSATTRIBUTE = "class";
	
	
	// node properties
	private String name;
	private String id;
	private String[] classes = new String[0];
	private LinkedHashMap<String,String> attributes = new LinkedHashMap<String, String>();
	
	// tree structure
	private OmlNode parent;
	private ArrayList<OmlNode> children = new ArrayList<OmlNode>();
	private String text;
	

	public OmlNode(String name) {
		this.name = name;
	}

	
	
	@Override
	public String toString() {
		String re = "OmlNode [name=" + name + ", id=" + id + ", classes="
				+ Arrays.toString(classes) + ", attributes=" + StringUtils.toString(attributes) + "]";
		for (OmlNode n =parent; n!=null; n=n.parent)
			re+=" <- "+n.name;
		return re;
	}



	public void setId(String id) {
		this.id = id;
	}
	public String getId() {
		return id;
	}

	public void setClasses(String[] classes) {
		this.classes = classes;
	}
	
	public String[] getClasses() {
		return classes;
	}
	
	public void addAttribute(String key, String value) {
		attributes.put(key, value);
	}
	
	public void addChild(OmlNode node) {
		children.add(node);
	}
	
	public void setParent(OmlNode parent) {
		this.parent = parent;
	}
	
	public OmlNode getParent() {
		return parent;
	}
	
	public ArrayList<OmlNode> getChildren() {
		return children;
	}
	
	public String getName() {
		return name;
	}
	
	public OmlNode getChildById(String id) {
		for (int i=0; i<children.size(); i++)
			if (children.get(i).getId().equals("id"))
				return children.get(i);
		return null;
	}

	public LinkedHashMap<String, String> getAttributes() {
		return attributes;
	}

	public boolean isRoot() {
		return parent==null;
	}

	public String toOml() {
		return toOml(new StringBuilder(),0).toString();
	}

	private StringBuilder toOml(StringBuilder sb, int indent) {
		StringUtils.indent(sb,indent);
		sb.append("<").append(name);
		if (id!=null) sb.append(" id=\"").append(id).append("\"");
		for (String a : attributes.keySet())
			sb.append(" ").append(a).append("=\"").append(attributes.get(a)).append("\"");
		
		if (children.size()==0) {
			sb.append(" />\n");
		} else {
			sb.append(">\n");
			for (OmlNode n : children)
				n.toOml(sb, indent+1);
			StringUtils.indent(sb,indent);
			sb.append("</").append(name).append(">\n");
		}
		return sb;
	}



	public void addText(String text) {
		if (this.text==null) setText(text);
		else this.text = this.text+text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public String getText() {
		return text;
	}
	
	public boolean hasText() {
		return text!=null;
	}




	
}
