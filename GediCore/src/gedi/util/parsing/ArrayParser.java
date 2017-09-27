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

import java.lang.reflect.Array;

import gedi.util.StringUtils;

public class ArrayParser<T> implements Parser<T[]> {
	
	private Parser<T> one;
	
	public ArrayParser(Parser<T> one) {
		this.one = one;
	}

	@Override
	public T[] apply(String s) {
		String[] f = StringUtils.split(s, ',');
		T[] re = (T[]) Array.newInstance(one.getParsedType(), f.length);
		for (int i=0; i<re.length; i++)
			re[i] = one.apply(f[i]);
		return re;
	}

	@Override
	public Class<T[]> getParsedType() {
		return (Class<T[]>) Array.newInstance(one.getParsedType(),0).getClass();
	}
}