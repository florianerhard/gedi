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
package gedi.util.job;

import gedi.util.dynamic.DynamicObject;
import gedi.util.mutable.MutableTuple;


/**
 * Needs to be stateless!
 * @author erhard
 *
 * @param <T>
 */
public interface Job<T> {

	Class[] getInputClasses();
	Class<T> getOutputClass();
	
	boolean isDisabled(ExecutionContext context);
	
	default DynamicObject meta(DynamicObject meta) {
		return meta;
	}
	
	T execute(ExecutionContext context,MutableTuple input);
	String getId();
	default void setInput(int i, Job job) {}
	default void addOutput(Job job) {}
	
}
