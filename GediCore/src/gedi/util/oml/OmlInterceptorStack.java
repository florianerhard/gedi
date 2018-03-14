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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Stack;

public class OmlInterceptorStack extends Stack<OmlInterceptor> implements OmlInterceptor {

	@Override
	public LinkedHashMap<String,String> getAttributes(OmlNode node, LinkedHashMap<String,String> attributes, HashMap<String,Object> context) {
		for (OmlInterceptor i : this)
			attributes = i.getAttributes(node, attributes, context);
		return attributes;
	}
	
	@Override
	public ArrayList<OmlNode> getChildren(OmlNode node, ArrayList<OmlNode> children, HashMap<String,Object> context) {
		for (OmlInterceptor i : this)
			children = i.getChildren(node, children, context);
		return children;
	}

	@Override
	public String getName(OmlNode node, String name, HashMap<String,Object> context) {
		for (OmlInterceptor i : this)
			name = i.getName(node, name, context);
		return name;
	}
	
	@Override
	public void childProcessed(OmlNode parentNode, OmlNode childNode,
			Object parent, Object child, HashMap<String, Object> context) {
		for (OmlInterceptor i : this)
			i.childProcessed(parentNode, childNode, parent, child, context);
	}
	@Override
	public void setObject(OmlNode node, Object o, String id, String[] classes, HashMap<String,Object> context) {
		for (OmlInterceptor i : this)
			i.setObject(node, o, id, classes, context);
	}
	
//	@Override
//	public void newId(String id, OmlNode node, Object o, HashMap<String,Object> context) {
//		for (OmlInterceptor i : this)
//			i.newId(id, node, o, context);
//	}

	@Override
	public boolean useForSubtree() {
		return false;
	}
	
}
