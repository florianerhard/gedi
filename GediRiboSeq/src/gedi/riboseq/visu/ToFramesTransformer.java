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
package gedi.riboseq.visu;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ConditionMappedAlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.cleavage.SimpleCodonModel;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.DoubleArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.math.optim.NNLS;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.CholeskyDecomposition;

@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class,toType=PixelBlockToValuesMap.class)
public class ToFramesTransformer implements GenomicRegionDataMapper<PixelBlockToValuesMap,PixelBlockToValuesMap> {

	
	private Strand fixedStrand;
	
	
	public ToFramesTransformer(Strand strand) throws IOException {
		this.fixedStrand = strand;
	}
	
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region, PixelLocationMapping pixelMapping,
			PixelBlockToValuesMap data) {
		
		
		
		if (data.size()==0) return new PixelBlockToValuesMap(pixelMapping, 0, NumericArrayType.Double);
		
		
		Strand strand = fixedStrand;
		if (strand==null && reference.getStrand()==Strand.Independent)
			throw new RuntimeException("Set fixed strand!");
		
		if (strand==null) strand = reference.getStrand();
		
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(data.getBlocks(), 3, NumericArrayType.Double);
		
		for (int i=0; i<data.size(); i++) {
			PixelLocationMappingBlock b = data.getBlock(i);
			for (int f=0; f<3; f++) {
				double val = data.getValues(i).getDouble(f);
				if (val>0) {
					if (b.getBasePairs()==1) {
						for (int o=0; o<3 && i+o<re.size(); o++)
							re.getValues(i+o).setDouble(f, val);
					} else {
						int stop = re.getBlockIndex(reference, b.getStopBp()+2);
						for (int o=i; o<=stop; o++)
							re.getValues(o).setDouble(f, val);
					}
				}
			}
		}

		
		return re;
		
		
	}

	


}
