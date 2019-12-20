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
package executables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.util.ArrayUtils;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.array.SparseMemoryFloatArray;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.math.stat.RandomNumbers;

public class RiboSim {

	
	public static void main(String[] args) throws IOException {
		
		Genomic g = Genomic.get("h.ens90");
		HashMap<String, ImmutableReferenceGenomicRegion<Transcript>> map = 
				g.getTranscripts().ei()
					.filter(t->SequenceUtils.checkCompleteCodingTranscript(g, t))
					.map(t->t.getData().getCds(t).toImmutable())
					.indexSmallest(t->t.getData().getGeneId(), (a,b)->Integer.compare(
							b.getRegion().getTotalLength(),
							a.getRegion().getTotalLength()));
		
		ArrayList<ImmutableReferenceGenomicRegion<Transcript>> trs = 
				EI.wrap(map.values()).filter(r->r.getReference().equals(Chromosome.obtain("1-"))).list();
		
		MemoryIntervalTreeStorage<SparseMemoryFloatArray> codons = new MemoryIntervalTreeStorage<>(SparseMemoryFloatArray.class); 
		MemoryIntervalTreeStorage<PriceOrf> orfs = new MemoryIntervalTreeStorage<>(PriceOrf.class); 
		
		RandomNumbers rnd = new RandomNumbers(42);
		
		double[] stn = {1,0.1,0.2};
		int ncond = 2;
		
		int n = 0;
		for (ImmutableReferenceGenomicRegion<Transcript> tr : trs) {
			
			PriceOrf orf = new PriceOrf(tr.getData().getTranscriptId(), n++, new double[ncond][0], 1).reset();
			orfs.add(new ImmutableReferenceGenomicRegion<>(tr.getReference(), tr.getRegion(), orf));
			
			String s = g.getSequence(tr).toString();
			int ncodons = 100;
			
			double[][] probs = new double[ncond][tr.getRegion().getTotalLength()-3];
			for (int i=0; i<probs[0].length; i++)
				for (int c=0; c<probs.length; c++)
					probs[c][i] = stn[i%3];
			for (int i=0; i<s.length(); i+=3)
				if (s.substring(i, i+3).equals("ATG") && i+12<probs[1].length)
					probs[1][i+12]*=1.3;
			
			for (int c=0; c<probs.length; c++) {
				ArrayUtils.normalize(probs[c]);
				ArrayUtils.cumSumInPlace(probs[c], 1);
			}
			
			SparseMemoryFloatArray[] profiles = new SparseMemoryFloatArray[probs[0].length];
			for (int i=0; i<ncodons; i++) {
				for (int c=0; c<probs.length; c++) {
					int pos = rnd.getCategorial(probs[c]);
					if (profiles[pos]==null)
						profiles[pos] = new SparseMemoryFloatArray(probs.length);
					profiles[pos].add(c,1);
				}
			}
			
			for (int i=0; i<profiles.length; i++)
				if (profiles[i]!=null) {
					GenomicRegion cpos = tr.map(new ArrayGenomicRegion(i,i+3));
					codons.add(new ImmutableReferenceGenomicRegion<>(tr.getReference(), cpos,profiles[i]));
				}
		}
		
		
		new CenteredDiskIntervalTreeStorage<>("test.codons.cit", SparseMemoryFloatArray.class).fill(codons);
		CenteredDiskIntervalTreeStorage out = new CenteredDiskIntervalTreeStorage<>("test.orfs.cit", PriceOrf.class);
		out.fill(orfs);
		out.setMetaData(DynamicObject.from("conditions", EI.seq(0, ncond).map(i->DynamicObject.from("name","C"+i)).toArray()));
		
	}
	
}
