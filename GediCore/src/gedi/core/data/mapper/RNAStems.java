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

package gedi.core.data.mapper;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataMerger;
import gedi.core.data.reads.ConditionMappedAlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.FunctorUtils;
import gedi.util.RunUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.mutable.MutableTriple;
import gedi.util.mutable.MutableTuple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

@GenomicRegionDataMapping(fromType=CharSequence.class,toType=IntervalTree.class)
public class RNAStems implements GenomicRegionDataMapper<CharSequence, IntervalTree<GenomicRegion, Void>>{


	private boolean complement = false;
	
	private int minlength=4;
	private int maxdist = 100;
	
	public RNAStems() {
		this(false);
	}
	
	public RNAStems(boolean complement) {
		this.complement = complement;
	}


	@Override
	public IntervalTree<GenomicRegion, Void> map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			CharSequence data) {
		
		
		IntervalTree<GenomicRegion, Void> re = new IntervalTree<GenomicRegion, Void>(reference);
		if (data==null || data.length()==0) return re;
		
		
		String seq = SequenceUtils.toRna(complement?SequenceUtils.getDnaReverseComplement(data).toString():data.toString());

		ImmutableReferenceGenomicRegion rgr = new ImmutableReferenceGenomicRegion<>(reference.toStrand(!complement), region);
		for (int l=0; l<seq.length(); l++) {
			for (int r=Math.min(seq.length()-1, l+2*minlength+2*maxdist); r>=l+2*minlength; r--) {
				int len = getMaximalStemLength(seq,l,r);
				if (len>=minlength && r-l+1-2*len<=maxdist) {
					GenomicRegion mpd = rgr.map(new ArrayGenomicRegion(l,l+len,r-len+1,r+1));
					if (EI.wrap(re.iterateIntervalsIntersecting(mpd, i->i.contains(mpd))).count()==0)
						re.add(mpd);
				}
			}
		}
		
		return re;
	}

	private int getMaximalStemLength(String seq, int l, int r) {
		int re = 0;
		for (;re<(r-l+1)/2 && SequenceUtils.canPair(seq.charAt(l+re), seq.charAt(r-re)); re++);
		return re;
	}



}
