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

package gedi.core.data.annotation;

import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;

public class Transcript implements BinarySerializable {

	private String geneId;
	private String transcriptId;
	private int codingStart;
	private int codingEnd;
	
	public Transcript() {
	}
	
	public Transcript(String geneId, String transcriptId,
			int codingStart, int codingEnd) {
		this.geneId = geneId;
		this.transcriptId = transcriptId;
		this.codingStart = codingStart;
		this.codingEnd = codingEnd;
	}
	public String getGeneId() {
		return geneId;
	}
	public String getTranscriptId() {
		return transcriptId;
	}
	/**
	 * Not the start codon position, its the leftmost position on the genome)
	 * @return
	 */
	public int getCodingStart() {
		return codingStart;
	}
	public int getCodingEnd() {
		return codingEnd;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + codingEnd;
		result = prime * result + codingStart;
		result = prime * result + ((geneId == null) ? 0 : geneId.hashCode());
		result = prime * result
				+ ((transcriptId == null) ? 0 : transcriptId.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Transcript other = (Transcript) obj;
		if (codingEnd != other.codingEnd)
			return false;
		if (codingStart != other.codingStart)
			return false;
		if (geneId == null) {
			if (other.geneId != null)
				return false;
		} else if (!geneId.equals(other.geneId))
			return false;
		if (transcriptId == null) {
			if (other.transcriptId != null)
				return false;
		} else if (!transcriptId.equals(other.transcriptId))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Transcript [geneId=" + geneId + ", transcriptId="
				+ transcriptId + ", codingStart=" + codingStart
				+ ", codingEnd=" + codingEnd + "]";
	}

	
	public void deserialize(BinaryReader in) throws IOException {
		geneId = in.getString();
		transcriptId = in.getString();
		codingStart = in.getInt();
		codingEnd = in.getInt();
	}

	
	public boolean isCoding() {
		return getCodingStart()>0;
	}
	
	public GenomicRegion getPart(ReferenceSequence reference, GenomicRegion transcriptRegion, int part) {
		switch (part) {
		case 0: return get5Utr(reference, transcriptRegion);
		case 1: return getCds(reference, transcriptRegion);
		case 2: return get3Utr(reference, transcriptRegion);
		default: throw new IndexOutOfBoundsException("Part must be 0,1 or 2!");
		}
	}
	
	public GenomicRegion getCds(ReferenceSequence reference, GenomicRegion transcriptRegion) {
		if (!isCoding()) throw new RuntimeException("Not a coding transcript!");
		return transcriptRegion.intersect(new ArrayGenomicRegion(getCodingStart(),getCodingEnd()));
	}
	
	public GenomicRegion get5Utr(ReferenceSequence reference, GenomicRegion transcriptRegion) {
		if (!isCoding()) throw new RuntimeException("Not a coding transcript!");
		if (reference.getStrand()==Strand.Minus)
			return transcriptRegion.intersect(new ArrayGenomicRegion(getCodingEnd(),transcriptRegion.getEnd()));
		return transcriptRegion.intersect(new ArrayGenomicRegion(transcriptRegion.getStart(),getCodingStart()));
	}
	public GenomicRegion get3Utr(ReferenceSequence reference, GenomicRegion transcriptRegion) {
		if (!isCoding()) throw new RuntimeException("Not a coding transcript!");
		if (reference.getStrand()==Strand.Minus)
			return transcriptRegion.intersect(new ArrayGenomicRegion(transcriptRegion.getStart(),getCodingStart()));
		return transcriptRegion.intersect(new ArrayGenomicRegion(getCodingEnd(),transcriptRegion.getEnd()));
	}
	
	public static ReferenceGenomicRegion<Transcript> getCds(ReferenceGenomicRegion<Transcript> rgr) {
		return rgr.toMutable().transformRegion(r->rgr.getData().getCds(rgr.getReference(), rgr.getRegion()));
	}
	
	public static ReferenceGenomicRegion<Transcript> get5Utr(ReferenceGenomicRegion<Transcript> rgr) {
		return rgr.toMutable().transformRegion(r->rgr.getData().get5Utr(rgr.getReference(), rgr.getRegion()));
	}
	public static ReferenceGenomicRegion<Transcript> get3Utr(ReferenceGenomicRegion<Transcript> rgr) {
		return rgr.toMutable().transformRegion(r->rgr.getData().get3Utr(rgr.getReference(), rgr.getRegion()));
	}
	
	
	public void serialize(BinaryWriter out) throws IOException {
		out.putString(getGeneId());
		out.putString(getTranscriptId());
		out.putInt(getCodingStart());
		out.putInt(getCodingEnd());
	}
	
}
