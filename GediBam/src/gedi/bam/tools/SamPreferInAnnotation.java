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

import gedi.core.data.annotation.Transcript;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.sequence.MismatchString;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

public class SamPreferInAnnotation implements UnaryOperator<Iterator<SAMRecord>> {

	private GenomicRegionStorage<Transcript> annotation;
	private int maxDiff = 1;
	private int[][] stat = new int[5][5];
	
	public SamPreferInAnnotation(GenomicRegionStorage<Transcript> annotation) {
		this.annotation = annotation;
	}
	
	public SamPreferInAnnotation setMaxDiff(int maxDiff) {
		this.maxDiff = maxDiff;
		return this;
	}
	
	public void printStat() {
		System.out.println("MM in Not annotated X MM in annotated ");
		System.out.println(ArrayUtils.matrixToString(stat));
	}

	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		Iterator<SAMRecord[]> ait = FunctorUtils.multiplexIterator(it, new SamRecordNameComparator(), SAMRecord.class);
		Iterator<SAMRecord[]> sit = FunctorUtils.mappedIterator(ait,a->{
			
			int mina = Integer.MAX_VALUE;
			int min = Integer.MAX_VALUE;
			
			for (SAMRecord r : a) {
				if (!r.getReadUnmappedFlag()) {
					if (r.getReadPairedFlag())
						throw new RuntimeException("Paired reads not supported yet!");
					
					boolean inanno = annotation.getReferenceRegionsIntersecting(BamUtils.getReference(r), BamUtils.getArrayGenomicRegion(r)).size()>0;
					if (inanno) 
						mina = Math.min(mina,getNumMis(r));
					else 
						min = Math.min(min,getNumMis(r));
				}
			}
			
			stat[Math.min(4,min)][Math.min(4,mina)]++;
			
			if (mina<Integer.MAX_VALUE && mina-maxDiff<=min) {
			
				int n = 0;
				for (int i=0; i<a.length; i++) {
					SAMRecord r = a[i];
					boolean inanno = annotation.getReferenceRegionsIntersecting(BamUtils.getReference(r), BamUtils.getArrayGenomicRegion(r)).size()>0;
					if (inanno && getNumMis(r)==mina)
						a[n++]=a[i];
				}
				
				if (n<a.length)
					a = ArrayUtils.redimPreserve(a, n);
			}
			else if (min<Integer.MAX_VALUE) {
				
				int n = 0;
				for (int i=0; i<a.length; i++) {
					SAMRecord r = a[i];
					if (getNumMis(r)==min)
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