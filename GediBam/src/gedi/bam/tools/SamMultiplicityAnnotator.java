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

import gedi.util.FunctorUtils;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import htsjdk.samtools.SAMRecord;

public class SamMultiplicityAnnotator implements UnaryOperator<Iterator<SAMRecord>> {

	
	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		
		Iterator<SAMRecord[]> ait = FunctorUtils.multiplexIterator(it, new SamRecordNameComparator(), SAMRecord.class);
		Iterator<SAMRecord[]> sit = FunctorUtils.sideEffectIterator(ait,a->{
			int first = 0;
			int second = 0;
			
			for (SAMRecord r : a) {
				
				if (r.getReadPairedFlag() && r.getFirstOfPairFlag())
					first++;
				else if (r.getReadPairedFlag() && r.getSecondOfPairFlag())
					second++;
				else
					r.setAttribute("NH",a.length);
			}
			
			if (first+second>0)
				for (SAMRecord r : a)
					if (r.getFirstOfPairFlag())
						r.setAttribute("NH",first);
					else if (r.getSecondOfPairFlag())
						r.setAttribute("NH",second);
		});
	
		return FunctorUtils.demultiplexIterator(sit, a->FunctorUtils.arrayIterator(a));
	}
}