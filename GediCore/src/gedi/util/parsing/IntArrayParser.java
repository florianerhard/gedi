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

import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;

public class IntArrayParser implements Parser<int[]> {
	@Override
	public int[] apply(String s) {
		return ParseUtils.parseRangePositions(s, -1, new IntArrayList()).toIntArray();
//		return StringUtils.parseInt(StringUtils.split(s, ','));
	}

	@Override
	public Class<int[]> getParsedType() {
		return int[].class;
	}
}