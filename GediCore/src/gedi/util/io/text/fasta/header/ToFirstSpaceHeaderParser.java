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

package gedi.util.io.text.fasta.header;

import gedi.util.io.text.fasta.DefaultFastaHeaderParser;

import java.util.regex.Pattern;


public class ToFirstSpaceHeaderParser extends DefaultFastaHeaderParser {

	
	public ToFirstSpaceHeaderParser() {
		super(
			Pattern.compile("^>(.*?)\\s+(.*?)$"), 
			new String[] {
				"id","description"
			}
		);
	}

	
}
