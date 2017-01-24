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

package gedi.lfc.quick;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Function;

import org.apache.commons.math3.random.EmpiricalDistribution;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.ensembl.BiomartExonFileReader;
import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.math.stat.RandomNumbers;

public class ShiroguchiCounter {

	public static void main(String[] args) throws IOException {
		
		String path = "/home/users/erhard/biostor/seq/ngade/shiroguchi_randombarcodes/data/";
				
		MemoryIntervalTreeStorage<int[]> reads = new MemoryIntervalTreeStorage<int[]>(int[].class);
		
		String[] files = {"Shiroguchi_A_collapsed.bed","Shiroguchi_B_collapsed.bed","Shiroguchi_A_uncollapsed.bed","Shiroguchi_B_uncollapsed.bed"};
		for (int i=0; i<4; i++) {
			Iterator<String> it = new LineOrientedFile(path+files[i]).lineIterator();
			while (it.hasNext()) {
				String[] f = StringUtils.split(it.next(), '\t');
				Chromosome chr = Chromosome.obtain(f[0]);
				ArrayGenomicRegion region = new ArrayGenomicRegion(Integer.parseInt(f[1]),Integer.parseInt(f[2]));
				int c = Integer.parseInt(StringUtils.splitField(f[3], '|', 0));
				
				int[] counts = reads.getData(chr,region);
				if (counts==null) reads.add(chr, region, counts=new int[4]);
				
				counts[i]+=c;
			}
		}
		
		HashMap<String,String> map = new HashMap<String, String>();
		new LineOrientedFile(path+"U00096.2.genes.csv").lineIterator().forEachRemaining(s-> {
			String[] f = StringUtils.split(s, '\t');
			map.put(f[0], f[7]);
		});

		
		LineOrientedFile fragments = new LineOrientedFile("fragments.csv");
		fragments.startWriting();
		fragments.writef("Gene\tonlyA\tonlyB\tBoth\tLength\n");
		
		LineOrientedFile bias = new LineOrientedFile("bias.csv");
		bias.startWriting();
		bias.writef("OriginalA\tBiasA\tOriginalB\tBiasB\n");
		
		IntArrayList biasFactors = new IntArrayList();
		ArrayList<GeneData> geneData = new ArrayList<GeneData>();
		
		MemoryIntervalTreeStorage<Transcript> genes = new BiomartExonFileReader(path+"U00096.2.exons.csv", false).readIntoMemoryTakeFirst();
		for (ImmutableReferenceGenomicRegion<Transcript> g : genes.getReferenceGenomicRegions()) {
			ArrayList<ImmutableReferenceGenomicRegion<int[]>> frag = reads.getReferenceRegionsIntersecting(g.getReference().toStrandIndependent(), g.getRegion());
			
			GeneData gd = new GeneData();
			
			
			int l = g.getRegion().getTotalLength();
			for (ImmutableReferenceGenomicRegion<int[]> r : frag) {
				if (r.getData()[0]==0) gd.onlyB++;
				if (r.getData()[1]==0) gd.onlyA++;
				if (r.getData()[0]==0 && r.getData()[1]==0) throw new RuntimeException();
				
				bias.writef("%d\t%.0f\t%d\t%.0f\n",r.getData()[0],r.getData()[2]/(double)r.getData()[0],r.getData()[1],r.getData()[3]/(double)r.getData()[1]);
				if (r.getData()[0]>0) {
					biasFactors.add(r.getData()[2]/r.getData()[0]);
				}
				if (r.getData()[1]>0) {
					biasFactors.add(r.getData()[3]/r.getData()[1]);
				}
				
			}
			gd.both = frag.size()-gd.onlyA-gd.onlyB;
			fragments.writef("%s\t%d\t%d\t%d\t%d\n", map.get(g.getData().getTranscriptId()),gd.onlyA, gd.onlyB, gd.both, l);
			
			if (gd.onlyA+gd.onlyB+gd.both>0)
				geneData.add(gd);
		}
		
		fragments.finishWriting();
		bias.finishWriting();
		
		
		double fc = 1.4;
		int rep = 5;
		int nDiff = 1000;
		int n = 10000;
		int N = 6000;
		double noise = 0.05;
		
		LineOrientedFile countMatrix = new LineOrientedFile("countMatrix.csv");
		countMatrix.startWriting();
		
		LineOrientedFile downCountMatrix = new LineOrientedFile("countMatrix_downsampled.csv");
		downCountMatrix.startWriting();
		
		
		RandomNumbers rnd = new RandomNumbers();
		for (int i=0; i<n; i++) {
			GeneData gd = geneData.get(rnd.getUnif(0, geneData.size()));
			
//			int N = gd.both==0?Integer.MAX_VALUE/2:(int) (gd.onlyA+gd.onlyB+gd.both+gd.onlyA*gd.onlyB/gd.both);
			double p1 = (gd.onlyA+gd.both)/(double)N;
			double p2 = i<nDiff?p1/fc:p1;
			
			ArrayList<ReadData> list = new ArrayList<ReadData>();
			for (int r=0; r<rep*2; r++) {
				int k = rnd.getBinom(N, r<rep?p1:p2)+1;
				int hit = N==-1?0:rnd.getBinom(k, list.size()/(double)N);
				rnd.shuffle(list);
				for (int x=0; x<hit; x++)
					list.get(x).reads[r] = (int) rnd.getNormal(list.get(x).bias, list.get(x).bias*noise);
				for (int x=0; x<k-hit; x++)
					list.add(new ReadData(
							biasFactors.getInt(rnd.getUnif(0, biasFactors.size())),
							rep*2, r));
			}
			
			int[] c = new int[rep*2];
			for (ReadData d : list) {
				for (int r=0; r<c.length; r++) {
					c[r]+=d.reads[r];
				}
			}
			
			double[] down = new double[rep*2];
			for (ReadData d : list) {
				double max = ArrayUtils.max(d.reads);
				for (int r=0; r<down.length; r++) {
					down[r]+=d.reads[r]/max;
				}
			}
			
			countMatrix.writeLine(StringUtils.concat("\t", c));
			downCountMatrix.writeLine(StringUtils.concat("\t", down));
			
		}
		
		countMatrix.finishWriting();
		downCountMatrix.finishWriting();
		
	}
	
	private static class ReadData {
		int bias;
		int[] reads;
		public ReadData(int bias, int rep, int cr) {
			this.bias = bias;
			this.reads = new int[rep];
			this.reads[cr] = bias;
		}
	}
	
	
	private static class GeneData {
		int onlyA;
		int onlyB;
		int both;
	}
	
}
