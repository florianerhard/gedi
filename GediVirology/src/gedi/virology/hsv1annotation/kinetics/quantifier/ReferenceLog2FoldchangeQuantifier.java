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

package gedi.virology.hsv1annotation.kinetics.quantifier;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.lfc.Log2FoldChange;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.functions.EI;

public class ReferenceLog2FoldchangeQuantifier extends KineticQuantifier {

	private QuantifierSet q;
	private int index;
	private ReferenceChooser referenceChooser;
	
	private Log2FoldChange foldChange;
	
	public ReferenceLog2FoldchangeQuantifier(String label,
			String repl, QuantifierSet q, int index, ReferenceChooser referenceChooser, Log2FoldChange foldChange) {
		super(label,repl, null);
		this.q = q;
		this.index = index;
		this.referenceChooser = referenceChooser;
		this.foldChange = foldChange;
	}

	@Override
	public NumericArray[] quantify(ReferenceGenomicRegion<?> fullRegion, ReferenceGenomicRegion<NameAnnotation>[] regions) {
		
		if (regions.length==0) return new NumericArray[0];
		
		
		NumericArray[][] data = EI.seq(0,q.getQuantifiers().size()).map(i->q.getQuantifiers().get(i).quantify(fullRegion,regions)).toArray(NumericArray[].class);
		
		NumericArray[] d = data[index];
		NumericArray[] re = new NumericArray[d.length];
		
		for (int i=0; i<d.length; i++) {
			int referenceRegion = referenceChooser.chooseReference(data,index,regions,i);
			log.fine(label+": For "+regions[i]+" ref: "+(referenceRegion==-1?"-":regions[referenceRegion]));
			if (referenceRegion==-1 || i==referenceRegion) {
				re[i] = NumericArray.createMemory(d[i].length(), d[i].getType());
				for (int j=0; j<d[i].length(); j++)
					re[i].setDouble(j, Double.NaN);
			} else {
				re[i] = ratio(d[i],d[referenceRegion]);
			}
		}
		
		return re;
	}

	private NumericArray ratio(NumericArray a, NumericArray b) {
		NumericArray re = NumericArray.createMemory(a.length(), a.getType());
		for (int i=0; i<a.length(); i++)
			re.set(i, foldChange.computeFoldChange(a.getDouble(i), b.getDouble(i)));
		return re;
	}
	
	@Override
	public NumericArray normalize(NumericArray data) {
		return data;
	}

	@FunctionalInterface
	public static interface ReferenceChooser {
		int chooseReference(NumericArray[][] data, int replicateIndex, ReferenceGenomicRegion<NameAnnotation>[] regions, int regionIndex);
	}
	
	public static class StrongestReferenceChooser implements ReferenceChooser{

		@Override
		public int chooseReference(NumericArray[][] data, int replicateIndex,
				ReferenceGenomicRegion<NameAnnotation>[] regions, int regionIndex) {
			return EI.along(data[0]).mapToDouble(j->{
				double sum = 0;
				for (int i=0; i<data.length; i++)
					sum+=NumericArrayFunction.Sum.applyAsDouble(data[i][j]);
				return sum;
			}).argmax();
		}
		
	}
	
	public static class DownstreamCDSReferenceChooser implements ReferenceChooser{

		@Override
		public int chooseReference(NumericArray[][] data, int replicateIndex,
				ReferenceGenomicRegion<NameAnnotation>[] regions, int regionIndex) {
			if (regions[regionIndex].getData().getName().contains("CDS")) return -1;
			
			Integer CDS = EI.along(regions)
				.filter(i->regions[i].getData().getName().contains("CDS"))
				.filter(i->regions[regionIndex].endsDownstream(regions[i].getRegion()))
				.reduce((a,b)->regions[a].endsDownstream(regions[b].getRegion())?a:b);
			if (CDS==null) return -1;
			return CDS;
		}
		
	}
	
}
