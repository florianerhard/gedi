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

import gedi.util.GeneralUtils;
import gedi.util.StringUtils;

import java.util.Comparator;

import htsjdk.samtools.SAMRecord;

public class SamRecordPosComparator implements Comparator<SAMRecord> {

	
	private boolean strandspecific = true;
	
	
	public SamRecordPosComparator() {
	}
	
	public SamRecordPosComparator(boolean strandspecific) {
		this.strandspecific = strandspecific;
	}
	
	
	@Override
	public int compare(SAMRecord o1, SAMRecord o2) {
		int re = o1.getReferenceName().compareTo(o2.getReferenceName());
		if (re==0)
			re = Integer.compare(o1.getAlignmentStart(),o2.getAlignmentStart());
		if (re==0 && strandspecific)
			re = -Integer.compare(o1.getReadNegativeStrandFlag()?-1:1,o2.getReadNegativeStrandFlag()?-1:1);
		return re;
	}
}