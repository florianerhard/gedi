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

package gedi.util.io.text.fasta;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFastaHeaderParser implements FastaHeaderParser {

	private boolean reUseMap = true;
	private Map<String,String> map;
	private int numFields;
	
	public AbstractFastaHeaderParser(int numFields) {
		this.numFields = numFields;
	}

	public boolean isReUseMap() {
		return reUseMap;
	}

	public void setReUseMap(boolean reUseMap) {
		this.reUseMap = reUseMap;
	}

	@Override
	public boolean canParse(String header) {
		return numFields<0 || parseHeader(header).size()==numFields;
	}
	
	protected Map<String, String> getMap() {
		Map<String,String> re = 
			 reUseMap && map!=null ? map : new HashMap<String, String>();
		re.clear();
		return re;
	}
	
	
}
