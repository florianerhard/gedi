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

import java.util.function.Function;

import gedi.util.StringUtils;
import gedi.util.mutable.MutableTuple;

public abstract class JobAdapter<TO> implements Job<TO> {

	
	private Class[] input;
	private Class<TO> output;
	protected String id = null;
	

	public JobAdapter(Class[] input, Class<TO> output) {
		this.input = input;
		this.output = output;
	}
	

	public JobAdapter(Class input, Class<TO> output) {
		this.input = new Class[] {input};;
		this.output = output;
	}

	@Override
	public Class[] getInputClasses() {
		return input;
	}

	@Override
	public Class getOutputClass() {
		return output;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean isDisabled(ExecutionContext context) {
		return false;
	}
	

}
