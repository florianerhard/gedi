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
package gedi.sra.processing;

import java.util.function.Consumer;
import java.util.function.Function;

import gedi.util.io.text.LineWriter;

public class DebugSraProcessor implements SraProcessor {


	@Override
	public boolean process(String sra, Function<SraTopLevel, Object> parser, Function<SraTopLevel, String> src,
			LineWriter out) {
		out.write2("debug");
		return false;
	}

}
