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

import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.sequence.MismatchString;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

public class SamRetainOptimal implements UnaryOperator<Iterator<SAMRecord>> {

	
	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		
		Iterator<SAMRecord[]> ait = FunctorUtils.multiplexIterator(it, new SamRecordNameComparator(), SAMRecord.class);
		Iterator<SAMRecord[]> sit = FunctorUtils.mappedIterator(ait,a->{
			int min = Integer.MAX_VALUE;
			int min1 = Integer.MAX_VALUE;
			int min2 = Integer.MAX_VALUE;
			
			for (SAMRecord r : a) {
				if (!r.getReadUnmappedFlag()) {
					if (r.getReadPairedFlag() && r.getFirstOfPairFlag())
						min1 = Math.min(min1,getNumMis(r));
					else if (r.getReadPairedFlag() && r.getSecondOfPairFlag())
						min2 = Math.min(min2,getNumMis(r));
					else
						min = Math.min(min,getNumMis(r));
				}
			}
			if (min<Integer.MAX_VALUE || min1<Integer.MAX_VALUE || min2<Integer.MAX_VALUE) {
				
				int n = 0;
				for (int i=0; i<a.length; i++) {
					SAMRecord r = a[i];
					int cmin;
					if (r.getReadPairedFlag() && r.getFirstOfPairFlag())
						cmin = min1;
					else if (r.getReadPairedFlag() && r.getSecondOfPairFlag())
						cmin = min2;
					else
						cmin = min;
					
					if (getNumMis(r)==cmin)
						a[n++]=a[i];
				}
				
				if (n<a.length)
					a = ArrayUtils.redimPreserve(a, n);
			}
			return a;
		});
	
		return FunctorUtils.demultiplexIterator(sit, a->FunctorUtils.arrayIterator(a));
	}
	
	private int getNumMis(SAMRecord r) {
		Integer nm = r.getIntegerAttribute("NM");
		if (nm!=null) return nm.intValue();
		
		int re = 0;
		String md = r.getStringAttribute("MD");
		if (md!=null) re+=new MismatchString(md).getNumMismatches();
		
		for (CigarElement e : r.getCigar().getCigarElements()) {
			if (e.getOperator()==CigarOperator.D) re++;
			else if (e.getOperator()==CigarOperator.I) re++;
			else if (e.getOperator()!=CigarOperator.N && e.getOperator()!=CigarOperator.M)
				throw new RuntimeException("Cigar operator "+e.getOperator()+" not supported!");
		}
		return re;
	}
	
}