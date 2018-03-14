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
package gedi.bam.tools;

import gedi.util.StringUtils;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.SAMRecord;

public class VariationExtractor {
	
	
	private CharSequence genomicSequence;

	public VariationExtractor() {
	}
	
	public VariationExtractor(CharSequence genomicSequence) {
		this.genomicSequence= genomicSequence;
	}
	
	public int getVariationCount(SAMRecord r) {
		int re = 0;
		int ls = 0;
		int lr = 0;
		for (CigarElement e : r.getCigar().getCigarElements()) {
			switch (e.getOperator()){
			case I: re++; ls+=e.getLength(); break;
			case D: re++; lr+=e.getLength(); break;
			case M: 
				CharSequence read = getReadSequence(r,ls,ls+e.getLength());
				CharSequence ref = getReferenceSequence(r,lr,lr+e.getLength());
				ls+=e.getLength();
				lr+=e.getLength();
				re+=StringUtils.hamming(read, ref);
				break;
			case S:
			case N: break;
			default: throw new IllegalArgumentException("Cigar operator "+e.getOperator()+" unknown!");
			}
		}
		return re;
	}
	
	
	
	public CharSequence getReferenceSequence(SAMRecord r, int s, int e) {
		String md = r.getStringAttribute("MD");
		if (md==null) {
			if (genomicSequence==null) return StringUtils.repeatSequence('N', e-s);//throw new RuntimeException("No MD tag and no sequence given!");
			return genomicSequence.subSequence(s, e);
		}
		return BamUtils.restoreSequence(r,false).substring(s, e);
	}
	public CharSequence getReadSequence(SAMRecord r, int s, int e) {
		if (r.getReadString().length()==0 || r.getReadString().equals("*")) return StringUtils.repeatSequence('N', e-s);
		return r.getReadString().substring(s,e);
	}
	
}
