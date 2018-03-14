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

public class FunctionJobAdapter<TO> extends JobAdapter<TO> {

	private Function<MutableTuple,TO> function;

	public FunctionJobAdapter(Class[] input, Class<TO> output, Function<MutableTuple,TO> function) {
		super(input,output);
		this.function = function;
	}
	

	public FunctionJobAdapter(Class input, Class<TO> output, Function<MutableTuple,TO> function) {
		super(input,output);
		this.function = function;
	}

	@Override
	public TO execute(ExecutionContext context, MutableTuple input) {
		return function.apply(input);
	}
	

}
