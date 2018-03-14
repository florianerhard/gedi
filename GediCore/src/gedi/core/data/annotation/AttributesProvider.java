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
package gedi.core.data.annotation;

import java.util.Set;

/**
 * Properties
 * @author erhard
 *
 */
public interface AttributesProvider {

	
	Set<String> getAttributeNames();
	Object getAttribute(String name);
	
	
	default int getIntAttribute(String name) { return (Integer)getAttribute(name);}
	default double getDoubleAttribute(String name) { return (Double)getAttribute(name);}
	default String getStringAttribute(String name) { return (String)getAttribute(name);}
	

	
}
