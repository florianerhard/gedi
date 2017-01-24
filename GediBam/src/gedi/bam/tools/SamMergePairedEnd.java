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

import gedi.core.reference.Chromosome;
import gedi.region.bam.FactoryGenomicRegion;
import gedi.util.FunctorUtils;
import gedi.util.io.text.LineOrientedFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.UnaryOperator;

import htsjdk.samtools.SAMRecord;


/**
 * Not properly tested yet!!
 * @author erhard
 *
 */
public class SamMergePairedEnd implements UnaryOperator<Iterator<SAMRecord>> {

	private LineOrientedFile out;
	private boolean loose = false;
	private boolean join = true;
	private boolean ignoreVariation;
	
	public SamMergePairedEnd() {
	}

	public SamMergePairedEnd(LineOrientedFile out) {
		this.out = out;
	}

	public SamMergePairedEnd(String path) {
		this.out = new LineOrientedFile(path);
	}
	
	public SamMergePairedEnd setLoose(boolean loose) {
		this.loose  = loose;
		return this;
	}
	
	public SamMergePairedEnd setJoin(boolean join) {
		this.join = join;
		return this;
	}
	
	public SamMergePairedEnd setIgnoreVariation(boolean ignore) {
		this.ignoreVariation = ignore;
		return this;
	}
	
	

	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		int[] cumNumCoord = {1};
		
		Iterator<SAMRecord[]> ait = loose?FunctorUtils.multiplexIterator(it, new SamRecordNameLooseComparator(), SAMRecord.class):FunctorUtils.multiplexIterator(it, new SamRecordNameComparator(), SAMRecord.class);
		Iterator<SAMRecord[]> sit = FunctorUtils.mappedIterator(ait,a->{
			ArrayList<SAMRecord> first = new ArrayList<SAMRecord>(a.length/2);
			ArrayList<SAMRecord> second = new ArrayList<SAMRecord>(a.length/2);
			
			for (SAMRecord r : a) {
				if (!r.getReadUnmappedFlag()) {
					if (r.getReadPairedFlag() && r.getFirstOfPairFlag() && r.getProperPairFlag())
						first.add(r);
					else if (r.getReadPairedFlag() && r.getSecondOfPairFlag() && r.getProperPairFlag())
						second.add(r);
					else if (!r.getReadPairedFlag())
						throw new RuntimeException("Not a paired-end read: "+r.getSAMString());
				} else if (out!=null){
					try {
						if (!out.isWriting()) out.startWriting();
						out.writef(">%s\n%s\n",r.getReadName(),r.getReadString());
					} catch (Exception e) {
						throw new RuntimeException("Could not write unmapped reads!",e);
					}
				}
			}
			
			if (first.size()!=second.size())
				throw new RuntimeException("Mapping problem: number of first mate and second mate mappings not equal: "+a[0].getReadName());
			
			SAMRecord[] re = new SAMRecord[first.size()];
			for (int i=0; i<first.size(); i++) {
				SAMRecord f = first.get(i);
				SAMRecord s = second.get(i);
				if (!f.getMateReferenceName().equals(s.getReferenceName()) || f.getMateAlignmentStart()!=s.getAlignmentStart())
					throw new RuntimeException("Mapping problem: first and second do not match: \n"+f+s);
				
				FactoryGenomicRegion m = BamUtils.getFactoryGenomicRegion(f, s, cumNumCoord, join,true,ignoreVariation,false);
				m.add(f,s, 0);
				Chromosome chr = Chromosome.obtain(f.getReferenceName(), !f.getReadNegativeStrandFlag());
				re[i] = BamUtils.createRecord(f.getHeader(), chr, m, m.create(), 0, f.getReadName(), Math.min(f.getMappingQuality(),s.getMappingQuality()));
			}
			
			return re;
		});
	
		return FunctorUtils.demultiplexIterator(sit, a->FunctorUtils.arrayIterator(a));
	}
	
	
}