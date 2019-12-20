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
package gedi.util.program.parametertypes;

import gedi.util.parsing.LongParser;

public class LongParameterType implements GediParameterType<Long> {

	@Override
	public Long parse(String s) {
		return new LongParser().apply(s);
	}

	@Override
	public Class<Long> getType() {
		return Long.class;
	}

}
