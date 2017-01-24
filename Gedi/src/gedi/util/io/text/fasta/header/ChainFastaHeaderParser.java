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

import gedi.util.io.text.fasta.FastaHeaderParser;

import java.util.Map;


public class ChainFastaHeaderParser implements FastaHeaderParser{

	private FastaHeaderParser[] chain;
	
	public ChainFastaHeaderParser(FastaHeaderParser... chain) {
		this.chain = chain;
	}

	@Override
	public boolean canParse(String header) {
		for (FastaHeaderParser p : chain)
			if (p.canParse(header))
				return true;
		return false;
	}

	@Override
	public String getId(String header) {
		for (FastaHeaderParser p : chain)
			if (p.canParse(header))
				return p.getId(header);
		return null;
	}

	@Override
	public boolean isReUseMap() {
		return chain[0].isReUseMap();
	}

	@Override
	public Map<String, String> parseHeader(String header) {
		for (FastaHeaderParser p : chain)
			if (p.canParse(header))
				return p.parseHeader(header);
		return null;
	}

	@Override
	public void setReUseMap(boolean reUseMap) {
		for (FastaHeaderParser p : chain)
			p.setReUseMap(reUseMap);
	}
	
	
	
}
