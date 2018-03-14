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
package gems.test.tools;


import gedi.bam.tools.BamUtils;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.region.bam.BamMerge;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.io.text.LineOrientedFile;
import htsjdk.samtools.SAMFileReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.ValidationStringency;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;


import org.junit.Assert;

public class StorageTester {

	
	private HashMap<String,int[][]> results = new HashMap<String, int[][]>();
	private ImmutableReferenceGenomicRegion<Transcript[]>[] transcripts;
	
	
	public StorageTester(ImmutableReferenceGenomicRegion<Transcript[]>[] transcripts) {
		this.transcripts = transcripts;
	}

	public void test(String name, GenomicRegionStorage<? extends AlignedReadsData> storage) throws IOException {
		test(name,storage,null);	
	}
	public void test(String name, GenomicRegionStorage<? extends AlignedReadsData> storage, LineOrientedFile output) throws IOException {
		if (output!=null)
			output.startWriting();
		int[][] re = new int[4][transcripts.length];
		
		for (int i=0; i<transcripts.length; i++) {
			ImmutableReferenceGenomicRegion<Transcript[]> rgr = transcripts[i];
			ReferenceSequence ref = rgr.getReference();
			GenomicRegion r = rgr.getRegion();
			if (output!=null)
				output.writef("%s\n",rgr.toString());
			
			// for each transcript
			NumericArray total = null;
			HashMap<GenomicRegion, ? extends AlignedReadsData> map = storage.getRegionsIntersectingMap(ref, r);
			GenomicRegion[] all = map.keySet().toArray(new GenomicRegion[0]);
			Arrays.sort(all);
			for (GenomicRegion reg : all) {
				AlignedReadsData d = map.get(reg);
				total = d.addTotalCountsForConditions(total, ReadCountMode.Weight);
				if (output!=null)
					output.writef(" %s\t%s\n",reg,d.toString());
			}
			
			if (total==null) {
				for (int c=0; c<4; c++)
					re[c][i]=0;
			} else {
				re[0][i] = (int) total.evaluate(NumericArrayFunction.Sum);
				re[1][i] = total.getInt(0);
				re[2][i] = total.getInt(total.length()/2);
				re[3][i] = total.getInt(total.length()-1);
			}
			
		}
		if (output!=null)
			output.finishWriting();
		
		
//		for (int i=0; i<keys.length; i++)
//			for (int j=i+1; j<keys.length; j++)
//				for (int c=0; c<4; c++){
//					System.out.println(keys[i]+" "+keys[j]+" "+c+":");
//					System.out.println(Arrays.toString(results.get(keys[i])[c]));
//					System.out.println(Arrays.toString(results.get(keys[j])[c]));
//				}
		for (String other : results.keySet())
			for (int c=0; c<4; c++)
				for (int a=0; a<transcripts.length; a++)
					Assert.assertEquals("For "+name+" and "+other+", "+c+" @"+transcripts[a],re[c][a],results.get(other)[c][a]);
		
		results.put(name, re);
	}
	
	
	public void testSamTools(String name, String path) {
		int[] uni = new int[]{1};
		int[][] re = new int[4][transcripts.length];
		
		SAMFileReader sr = new SAMFileReader(new File(path));
		
		for (int i=0; i<transcripts.length; i++) {
			ImmutableReferenceGenomicRegion<Transcript[]> rgr = transcripts[i];
			ReferenceSequence ref = rgr.getReference();
			GenomicRegion r = rgr.getRegion();
			// for each transcript
			int[] total = null;
			SAMRecordIterator it = sr.query(ref.getName(), r.getStart()-1, r.getEnd(), false);
			while (it.hasNext()) {
				SAMRecord rec = it.next();
				if (rec.getReadNegativeStrandFlag()==(ref.getStrand()==Strand.Minus)) {
					if (r.intersects(BamUtils.getRecordsGenomicRegion(rec))) {
						String c = rec.getStringAttribute("XR");
						if (c!=null) total = ArrayUtils.add(total,StringUtils.parseInt(StringUtils.split(c,',')));
						else total = ArrayUtils.add(total,uni);
					}
				}
			}
			it.close();
			if (total==null) {
				for (int c=0; c<4; c++)
					re[c][i]=0;
			} else {
				re[0][i] = ArrayUtils.sum(total);
				re[1][i] = total[0];
				re[2][i] = total[total.length/2];
				re[3][i] = total[total.length-1];
			}
			
//			if (total!=null)
//				System.out.println(ref+":"+r+"\t"+Arrays.toString(total));
		}
		sr.close();
		for (String other : results.keySet())
			for (int c=0; c<4; c++)
				for (int a=0; a<transcripts.length; a++)
					Assert.assertEquals("For "+name+" and "+other+", "+c+" @"+transcripts[a],re[c][a],results.get(other)[c][a]);
		
		results.put(name, re);
	}
	
	
	public void testSamTools(String name, BamMerge merge) {
		int[][] re = new int[4][transcripts.length];
		
		ContrastMapping map = merge.getMapping();
		
		SAMFileReader[] sr = new SAMFileReader[merge.getOriginalNames().length];
		for (int i=0; i<sr.length; i++) {
			sr[i] = new SAMFileReader(new File(merge.getOriginalNames()[i]));
			sr[i].setValidationStringency(ValidationStringency.SILENT);
		}
		
		for (int i=0; i<transcripts.length; i++) {
			ImmutableReferenceGenomicRegion<Transcript[]> rgr = transcripts[i];
			ReferenceSequence ref = rgr.getReference();
			GenomicRegion r = rgr.getRegion();
			// for each transcript
			int[] total = new int[map.getNumMergedConditions()];
			
			
			for (int s=0; s<sr.length; s++) {
				SAMRecordIterator it = sr[s].query(ref.getName(), r.getStart()-1, r.getEnd(), false);
				while (it.hasNext()) {
					SAMRecord rec = it.next();
					if (rec.getReadNegativeStrandFlag()==(ref.getStrand()==Strand.Minus)) {
						if (r.intersects(BamUtils.getRecordsGenomicRegion(rec))) {
							total[map.getMappedIndex(s)]++;
						}
					}
				}
				it.close();
			}
			
			re[0][i] = ArrayUtils.sum(total);
			re[1][i] = total[0];
			re[2][i] = total[total.length/2];
			re[3][i] = total[total.length-1];
			
//			if (total!=null)
//				System.out.println(ref+":"+r+"\t"+Arrays.toString(total));
		}
		for (int i=0; i<sr.length; i++)
			sr[i].close();
		
		
		for (String other : results.keySet())
			for (int c=0; c<4; c++)
				for (int a=0; a<transcripts.length; a++)
					Assert.assertEquals("For "+name+" and "+other+", "+c+" @"+transcripts[a],re[c][a],results.get(other)[c][a]);
		
		results.put(name, re);
	}
	
}
