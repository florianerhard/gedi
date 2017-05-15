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

public interface OmlInterceptor {

	
	public default LinkedHashMap<String,String> getAttributes(OmlNode node, LinkedHashMap<String,String> attributes, HashMap<String,Object> context) {
		return attributes;
	}
	
	public default ArrayList<OmlNode> getChildren(OmlNode node, ArrayList<OmlNode> children, HashMap<String,Object> context) {
		return children;
	}

	public default String getName(OmlNode node, String name, HashMap<String,Object> context) {
		return name;
	}

	public default void setObject(OmlNode node, Object o, String id, String[] classes, HashMap<String,Object> context) {
		
	}
	
//	public default void newId(String id, OmlNode node, Object o, HashMap<String,Object> context) {
//		
//	}
	
	/**
	 * Non inlined children!
	 * @param parentNode
	 * @param childNode
	 * @param parent
	 * @param child
	 * @param context
	 */
	public default void childProcessed(OmlNode parentNode, OmlNode childNode, Object parent, Object child, HashMap<String,Object> context){
		
	}

//	public default void newClasses(String[] classes, OmlNode node, Object o, HashMap<String,Object> context) {
//		
//	}

	public default boolean useForSubtree() {
		return false;
	}
	
}
