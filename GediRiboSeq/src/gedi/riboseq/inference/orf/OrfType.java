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

import gedi.core.data.annotation.Transcript;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;

public enum OrfType {

	
	CDS {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getData().hasStop() || !orf.getData().hasStart() || !orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			return cds.equals(orf.getData().getStartToStop(orf.getReference(), orf.getRegion()));
		}
	},
	Extended {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getData().hasStop || !orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			GenomicRegion crf = orf.getData().getStartToStop(orf.getReference(), orf.getRegion());
			if (!crf.containsUnspliced(cds)) return false;
			if (orf.getReference().getStrand().equals(Strand.Plus))
				return crf.getEnd()==cds.getEnd();
			else
				return crf.getStart()==cds.getStart();
		}
	},
	Truncated {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getData().hasStop || !orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			GenomicRegion crf = orf.getData().getStartToStop(orf.getReference(), orf.getRegion());
			if (!cds.containsUnspliced(crf)) return false;
			if (orf.getReference().getStrand().equals(Strand.Plus))
				return crf.getEnd()==cds.getEnd();
			else
				return crf.getStart()==cds.getStart();
		}
	},
	InFrameSnippet {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (orf.getData().hasStop || !orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			return cds.containsUnspliced(orf.getRegion()) && new ImmutableReferenceGenomicRegion<Void>(annotation.getReference(), cds).induce(orf.getRegion()).getStart()%3==0;
		}
	},
	OutOfFrameSnippet {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (orf.getData().hasStop || !orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			return cds.containsUnspliced(orf.getRegion());
		}
	},
	Isoform {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			ArrayGenomicRegion overlap = cds.intersect(orf.getRegion());
			if (overlap.isEmpty()) return false;
			int cdsFrame = new ImmutableReferenceGenomicRegion<Void>(annotation.getReference(), cds).induce(overlap).getStart()%3;
			int orfFrame = orf.induce(overlap).getStart()%3;
			return cdsFrame==orfFrame;
		}
	},
	uoORF {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			if (!cds.intersects(orf.getRegion())) return false;
			if (orf.getReference().getStrand().equals(Strand.Plus))
				return orf.getRegion().getStart()<cds.getStart();
			else
				return orf.getRegion().getStop()>cds.getStop();
		}
	},
	udORF {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			if (cds.intersects(orf.getRegion())) return false;
			if (orf.getReference().getStrand().equals(Strand.Plus))
				return orf.getRegion().getStart()<cds.getStart();
			else
				return orf.getRegion().getStop()>cds.getStop();
		}
	},
	doORF {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			if (!cds.intersects(orf.getRegion())) return false;
			if (orf.getReference().getStrand().equals(Strand.Plus))
				return orf.getRegion().getStop()>cds.getStop();
			else
				return orf.getRegion().getStart()<cds.getStart();
		}
	},
	ddORF {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			if (cds.intersects(orf.getRegion())) return false;
			if (orf.getReference().getStrand().equals(Strand.Plus))
				return orf.getRegion().getStop()>cds.getStop();
			else
				return orf.getRegion().getStart()<cds.getStart();
		}
	},
	iORF {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getReference().equals(annotation.getReference()) || !annotation.getData().isCoding())
				return false;
			GenomicRegion cds = annotation.getData().getCds(annotation.getReference(), annotation.getRegion());
			return cds.intersects(orf.getRegion());
		}
	},
	ncRNA {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getReference().equals(annotation.getReference()))
				return false;
			return !annotation.getData().isCoding() && annotation.getRegion().containsUnspliced(orf.getRegion());
		}
	},
	intronic {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			if (!orf.getReference().equals(annotation.getReference()))
				return false;
			return !annotation.getRegion().containsUnspliced(orf.getRegion()) && annotation.getRegion().removeIntrons().contains(orf.getRegion());
		}
	},
	oORF {
		@Override
		public boolean is(ReferenceGenomicRegion<Orf> orf,
				ReferenceGenomicRegion<Transcript> annotation) {
			return true;
		}
	};
	

	/**
	 * Must successively be evaluated for each of the items; the first one that reports true is the one!
	 * @param orf
	 * @param annotation
	 * @return
	 */
	public abstract boolean is(ReferenceGenomicRegion<Orf> orf, ReferenceGenomicRegion<Transcript> annotation);
	
	public boolean isAnnotated() {
		return this==CDS || this==Extended || this==Truncated || this==Isoform;
	}
	
	public static OrfType annotate(Iterable<ImmutableReferenceGenomicRegion<Transcript>> tr, ImmutableReferenceGenomicRegion<Orf> orf) {
		for (OrfType type : OrfType.values()) {
			for (ImmutableReferenceGenomicRegion<Transcript> t : tr) {
				if (type.is(orf, t)) {
					return type;
				}
			}
		}
		return OrfType.oORF;
	}
}
