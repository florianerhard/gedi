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

import java.util.Comparator;

import htsjdk.samtools.SAMRecord;

public class SamRecordLongIdNameComparator implements Comparator<SAMRecord> {

	@Override
	public int compare(SAMRecord o1, SAMRecord o2) {
		
		String n1 = o1.getReadName();
		String n2 = o2.getReadName();
		
		if (!StringUtils.isInt(n1) || !StringUtils.isInt(n2))
			throw new RuntimeException("Ids must be integers!");
		
		return Long.compare(Long.parseLong(n1),Long.parseLong(n2));
		
	}
}