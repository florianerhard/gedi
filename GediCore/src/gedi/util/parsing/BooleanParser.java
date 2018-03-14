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
package gedi.util.parsing;

import java.util.Arrays;
import java.util.HashSet;

public class BooleanParser implements Parser<Boolean> {
	
	private HashSet<String> trues = new HashSet<String>(Arrays.asList("true","True","TRUE","T","x","y","yes"));
	private HashSet<String> falses = new HashSet<String>(Arrays.asList("false","False","FALSE","F",""," ","n","no"));
	
	@Override
	public Boolean apply(String s) {
		if (trues.contains(s)) return true;
		if (falses.contains(s)) return false;
		throw new IllegalArgumentException();
	}

	@Override
	public Class<Boolean> getParsedType() {
		return Boolean.class;
	}
}