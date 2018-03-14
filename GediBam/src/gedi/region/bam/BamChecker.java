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
package gedi.region.bam;

import java.io.IOException;

import gedi.bam.tools.BamUtils;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.io.text.LineWriter;
import htsjdk.samtools.SAMRecord;

public class BamChecker {

	private Genomic g;
	private LineWriter inconsistent;
	
	public BamChecker(Genomic g, LineWriter inconsistent) {
		this.g = g;
		this.inconsistent = inconsistent;
	}

	public void check(SAMRecord rec, ReferenceGenomicRegion<? extends AlignedReadsData> rgr) {
		String read = rec.getReadNegativeStrandFlag()?SequenceUtils.getDnaReverseComplement(rec.getReadString()):rec.getReadString();
		String genomic = g.getSequence(rgr).toString();
		AlignedReadsData ard = rgr.getData();
		
		if (ard.getDistinctSequences()!=1) throw new RuntimeException("Only for directly created data!");
		for (int v=0; v<ard.getVariationCount(0); v++) {
			if (ard.isSoftclip(0, v)) {
				if (ard.isSoftclip5p(0, v))
					check(ard.getVariation(0, v).toString(),rec,rgr,read.substring(0, ard.getSoftclip(0, v).length()),ard.getSoftclip(0, v));
				else 
					check(ard.getVariation(0, v).toString(),rec,rgr,read.substring(read.length()-ard.getSoftclip(0, v).length()),ard.getSoftclip(0, v));
			}
			else if (ard.isMismatch(0, v)) {
				int rpos = ard.mapToRead1(0, ard.getMismatchPos(0, v));
				check(ard.getVariation(0, v).toString(),rec,rgr,read.substring(rpos,rpos+1),ard.getMismatchRead(0, v));
				check(ard.getVariation(0, v).toString(),rec,rgr,genomic.substring(ard.getMismatchPos(0, v),ard.getMismatchPos(0, v)+1),ard.getMismatchGenomic(0, v));
			}
			else if (ard.isInsertion(0, v)) {
				int rpos = ard.mapToRead1(0, ard.getInsertionPos(0, v));
				rpos-=ard.getInsertion(0, v).length();
				check(ard.getVariation(0, v).toString(),rec,rgr,read.substring(rpos,rpos+ard.getInsertion(0, v).length()),ard.getInsertion(0, v));
			}
			else if (ard.isDeletion(0, v)) {
				check(ard.getVariation(0, v).toString(),rec,rgr,genomic.substring(ard.getDeletionPos(0, v),ard.getDeletionPos(0, v)+ard.getDeletion(0, v).length()),ard.getDeletion(0, v));
			}
			
		}
	}
	
	private void check(String var, SAMRecord rec, ReferenceGenomicRegion<? extends AlignedReadsData> rgr, String ref, CharSequence reco) {
		if (reco.length()!=ref.length()) 
			throw new RuntimeException(var+" Expected: "+ref+" From ARD: "+reco+"\n"+rec.getSAMString()+rgr);
		for (int i=0; i<reco.length(); i++)
			if (reco.charAt(i)!=ref.charAt(i) &&ref.charAt(i)!='N')
				throw new RuntimeException(var+" Expected: "+ref+" From ARD: "+reco+"\n"+rec.getSAMString()+rgr);
	}
	
	private void check(String var, SAMRecord rec,  SAMRecord mate, ReferenceGenomicRegion<? extends AlignedReadsData> rgr, String ref, CharSequence reco) {
		if (reco.length()!=ref.length()) 
			throw new RuntimeException(var+" Expected: "+ref+" From ARD: "+reco+"\n"+rec.getSAMString()+mate.getSAMString()+rgr);
		for (int i=0; i<reco.length(); i++)
			if (reco.charAt(i)!=ref.charAt(i) &&ref.charAt(i)!='N')
				throw new RuntimeException(var+" Expected: "+ref+" From ARD: "+reco+"\n"+rec.getSAMString()+mate.getSAMString()+rgr);
	}
	
	

	public void check(SAMRecord rec, SAMRecord mate, ReferenceGenomicRegion<? extends AlignedReadsData> rgr) {
		String read1 = rec.getReadString(); //rec.getReadNegativeStrandFlag()?SequenceUtils.getDnaReverseComplement(rec.getReadString()):rec.getReadString();
		String read2 = SequenceUtils.getDnaComplement(mate.getReadString());
		if (rgr.getReference().isMinus()) {
			read1 = SequenceUtils.getDnaReverseComplement(read1);
			read2 = SequenceUtils.getDnaReverseComplement(read2);
		}
		String genomic = g.getSequence(rgr).toString();
		AlignedReadsData ard = rgr.getData();
		
		if (ard.getDistinctSequences()!=1) throw new RuntimeException("Only for directly created data!");
		for (int v=0; v<ard.getVariationCount(0); v++) {
			String read = ard.isVariationFromSecondRead(0, v)?read2:read1;
			if (ard.isSoftclip(0, v)) {
				if (ard.isSoftclip5p(0, v))
					check(ard.getVariation(0, v).toString(),rec,mate,rgr,read.substring(0, ard.getSoftclip(0, v).length()),ard.getSoftclip(0, v));
				else 
					check(ard.getVariation(0, v).toString(),rec,mate,rgr,read.substring(read.length()-ard.getSoftclip(0, v).length()),ard.getSoftclip(0, v));
			}
			else if (ard.isMismatch(0, v)) {
				int rpos = ard.mapToRead(0, ard.getMismatchPos(0, v), ard.isVariationFromSecondRead(0, v));
				if (ard.isVariationFromSecondRead(0, v))
					rpos-=ard.getReadLength1(0);
				check(ard.getVariation(0, v).toString(),rec,mate,rgr,read.substring(rpos,rpos+1),ard.getMismatchRead(0, v));
				check(ard.getVariation(0, v).toString(),rec,mate,rgr,genomic.substring(ard.getMismatchPos(0, v),ard.getMismatchPos(0, v)+1),ard.isVariationFromSecondRead(0, v)?SequenceUtils.getDnaReverseComplement(ard.getMismatchGenomic(0, v)):ard.getMismatchGenomic(0, v));
			}
			else if (ard.isInsertion(0, v)) {
				int rpos = ard.mapToRead(0, ard.getInsertionPos(0, v),ard.isVariationFromSecondRead(0, v));
				rpos-=ard.getInsertion(0, v).length();
				if (ard.isVariationFromSecondRead(0, v))
					rpos-=ard.getReadLength1(0);
				check(ard.getVariation(0, v).toString(),rec,mate,rgr,read.substring(rpos,rpos+ard.getInsertion(0, v).length()),ard.getInsertion(0, v));
			}
			else if (ard.isDeletion(0, v)) {
				check(ard.getVariation(0, v).toString(),rec,mate,rgr,genomic.substring(ard.getDeletionPos(0, v),ard.getDeletionPos(0, v)+ard.getDeletion(0, v).length()),ard.isVariationFromSecondRead(0, v)?SequenceUtils.getDnaReverseComplement(ard.getDeletion(0, v)):ard.getDeletion(0, v));
			}
			
		}
	}

	public void addInconsistent(SAMRecord rec, SAMRecord mate) {
		IntArrayList coords1 = BamUtils.getGenomicRegionCoordinates(rec);
		IntArrayList coords2 = BamUtils.getGenomicRegionCoordinates(mate);
		GenomicRegion re1 = new ArrayGenomicRegion(coords1);
		GenomicRegion re2 = new ArrayGenomicRegion(coords2);
		try {
			inconsistent.write(rec.getSAMString());
			inconsistent.write(mate.getSAMString());
			inconsistent.writef("%s\t%s\t%s\n",re1,re2,re1.union(re2));
		} catch (IOException e) {
		}
		
	}

}
