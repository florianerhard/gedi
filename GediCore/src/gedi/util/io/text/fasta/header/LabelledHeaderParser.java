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

import gedi.util.StringUtils;
import gedi.util.io.text.fasta.AbstractFastaHeaderParser;

import java.util.Map;

public class LabelledHeaderParser extends AbstractFastaHeaderParser {

	

	public LabelledHeaderParser() {
		super(-1);
	}

	@Override
	public Map<String, String> parseHeader(String header) {
		Map<String, String> re = getMap();
		if (header.startsWith(">"))
			header = header.substring(1);
		
		String[] entries = StringUtils.split(header, '|');
		for (String entry : entries) {
			String[] pair = StringUtils.split(entry, '=');
			if (pair.length==2)
				re.put(pair[0], pair[1]);
		}
		return re;
	}

	@Override
	public String getId(String header) {
		String[] entries = StringUtils.split(header, '|');
		String[] pair = StringUtils.split(entries[0], '=');
		return pair[1];
	}


}
