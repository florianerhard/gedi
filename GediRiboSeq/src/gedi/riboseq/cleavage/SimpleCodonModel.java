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
package gedi.riboseq.cleavage;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.StringUtils;

import java.util.HashMap;

public class SimpleCodonModel {

	private HashMap<String,Integer> map = new HashMap<String, Integer>();
	private String[] spec;

	// 28->12 29-12 29L->13 30L->13
	public SimpleCodonModel(String[] li) {
		this.spec = li;
		for (String l : li) {
			String[] p = StringUtils.split(l, "->");
			if (p.length!=2) throw new RuntimeException("Not the proper format for simple model: "+l);
			map.put(p[0], Integer.parseInt(p[1]));
		}
	}
	
	public String[] getSpec() {
		return spec;
	}

	public int getPosition(
			ImmutableReferenceGenomicRegion<AlignedReadsData> read, int d) {
		String key = read.getRegion().getTotalLength()+"";
		if (RiboUtils.hasLeadingMismatch(read.getData(), d)) {
			if (RiboUtils.isLeadingMismatchInsideGenomicRegion(read.getData(), d)) 
				key+="L";
			else
				key = (read.getRegion().getTotalLength()+1)+"L";
		}
		Integer re = map.get(key);
		return re==null?-1:re;
	}

}