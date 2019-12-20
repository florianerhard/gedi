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
package gedi.riboseq.codonprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableTuple;

public class ExpandingCodonProcessorCounter extends CodonProcessorCounter {

	
	
	
	private Function<HashMap<String, Object>, ExtendedIterator<HashMap<String,Object>>> expander;
	
	public ExpandingCodonProcessorCounter(String prefix, Function<HashMap<String,Object>,ExtendedIterator<HashMap<String,Object>>> expander, String... varNames)  {
		super(prefix,varNames);
		this.expander = expander;
	}

	@Override
	public void count(HashMap<String, Object> vars, NumericArray count) {
		
		for (HashMap<String,Object> sub : expander.apply(vars).loop()) {
			if (testConditions(sub)) {
				setup(sub);
				count(count);
			}
		}
		
	}


	
		
	
}
