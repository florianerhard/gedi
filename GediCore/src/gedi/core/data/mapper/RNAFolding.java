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

@GenomicRegionDataMapping(fromType=CharSequence.class,toType=PixelBlockToValuesMap.class)
public class RNAFolding implements GenomicRegionDataMapper<CharSequence, PixelBlockToValuesMap>{


	private boolean complement = false;
	
	public RNAFolding() {
		this(false);
	}
	
	public RNAFolding(boolean complement) {
		this.complement = complement;
	}


	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			CharSequence data) {
		
		
		if (data==null || data.length()==0) return new PixelBlockToValuesMap(pixelMapping, 0, NumericArrayType.Double);
		
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, 1, NumericArrayType.Double);
		String seq = complement?SequenceUtils.getDnaReverseComplement(data).toString():data.toString();
		double[] probs = new double[seq.length()];
		
		try {
			File tmp = File.createTempFile("folding", "_dp.ps",new File("."));
			tmp.deleteOnExit();
			String name = StringUtils.removeFooter(tmp.getName(),"_dp.ps");
			File tmp2 = new File(name+"_lunp");
			
			RunUtils.pipeInto(wr->wr.print(">"+name+"\n"+data.toString()), "RNAplfold","-d2","--noLP","-u1");
			for (String s : EI.lines(tmp2.getPath())
								.skip(2)
								.loop()) {
				
				int sepIndex=s.indexOf('\t');
				int p1 = Integer.parseInt(s.substring(0, sepIndex));
				double p = Double.parseDouble(s.substring(sepIndex+1));
				probs[p1-1]+=1-p;
			}
			tmp.delete();
		} catch (IOException e) {
			throw new RuntimeException("Could not fold!",e);
		}
		ImmutableReferenceGenomicRegion rgr = new ImmutableReferenceGenomicRegion<>(reference.toStrand(!complement), region);
		for (int i=0; i<probs.length; i++) {
			int p = rgr.map(i);
			int bi = re.getBlockIndex(reference, p);
			re.getValues(bi).set(0, Math.max(re.getValues(bi).getDouble(0), probs[i]));
		}
		
		return re;
	}



}
