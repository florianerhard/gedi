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

package gedi.riboseq.inference.orf;

import java.util.Arrays;
import java.util.HashSet;

import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.SequenceUtils;

public enum PriceOrfType {

	
	CDS,
	Ext,
	Trunc,
	Variant,
	uoORF,
	uORF,
	iORF,
	dORF,
	ncRNA,
	intronic,
	orphan;
	

	
	public boolean isAnnotated() {
		return this==CDS || this==Ext || this==Trunc || this==Variant;
	}

	public static PriceOrfType annotate(Genomic g, Iterable<ImmutableReferenceGenomicRegion<Transcript>> trs, ImmutableReferenceGenomicRegion<PriceOrf> orf) {
		PriceOrfType re = PriceOrfType.orphan;
		for (ImmutableReferenceGenomicRegion<Transcript> tr : trs) {
			PriceOrfType cre = annotate(g, tr, orf);
			if (cre.ordinal()<re.ordinal())
				re = cre;
		}
		return re;
	}
	public static PriceOrfType annotate(Genomic g, ImmutableReferenceGenomicRegion<Transcript> tr, ImmutableReferenceGenomicRegion<PriceOrf> orf) {
		GenomicRegion eStop = orf.map(new ArrayGenomicRegion(orf.getRegion().getTotalLength()-3,orf.getRegion().getTotalLength()));
		
		if (tr.getData().isCoding()) {
			ImmutableReferenceGenomicRegion<?> cds = new ImmutableReferenceGenomicRegion<>(tr.getReference(),tr.getData().getCds(tr.getReference(),tr.getRegion()));
			GenomicRegion cdsStop = cds.map(new ArrayGenomicRegion(cds.getRegion().getTotalLength()-3,cds.getRegion().getTotalLength()));
			boolean truncatedAnnotation = !SequenceUtils.checkCompleteCodingTranscript(g, tr);
			boolean inframeoverlap = RiboUtils.isInframeOverlap(orf.getRegion(),cds.getRegion());	
			boolean orfconsistent = cds.getRegion().containsUnspliced(orf.getRegion());
			boolean consistent = tr.getRegion().containsUnspliced(orf.getRegion());
			if (orf.getRegion().equals(cds.getRegion()))
				return PriceOrfType.CDS;
			if (eStop.equals(cdsStop) && orf.getRegion().getTotalLength()>cds.getRegion().getTotalLength() && orfconsistent)
				return PriceOrfType.Ext;
			if (eStop.equals(cdsStop) && orf.getRegion().getTotalLength()<cds.getRegion().getTotalLength() && orfconsistent)
				return PriceOrfType.Trunc;
			if (eStop.equals(cdsStop))
				return Variant;
			if (inframeoverlap && !truncatedAnnotation)
				return PriceOrfType.Variant;
			if (!consistent)
				return PriceOrfType.orphan;
			if (orf.startsDownstream(cds.getRegion()) && orf.getRegion().intersects(cds.getRegion()))
				return PriceOrfType.uoORF;
			if (orf.startsDownstream(cds.getRegion()))
				return uORF;
			if (orf.endsUpstream(cds.getRegion()))
				return PriceOrfType.dORF;
			if (truncatedAnnotation) 
				return PriceOrfType.orphan;
			if (cds.getRegion().containsUnspliced(orf.getRegion()))
				return iORF;
			if (tr.getRegion().removeIntrons().contains(orf.getRegion()))
				return PriceOrfType.intronic;
		} else if (tr.getRegion().containsUnspliced(orf.getRegion()))
				return ncRNA;
		return PriceOrfType.orphan;
	}
}
