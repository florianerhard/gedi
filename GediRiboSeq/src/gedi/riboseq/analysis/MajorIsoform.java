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
package gedi.riboseq.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.riboseq.inference.orf.PriceOrfType;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.SparseMemoryFloatArray;
import gedi.util.datastructure.collections.PositionIterator;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

public class MajorIsoform implements BinarySerializable {
	
	private ImmutableReferenceGenomicRegion<Transcript> transcript;
	private float[][] psites;
	private int cond;
	
	
	/**
	 * This contains blocks of ORFs ending at the same stop codon; these indices refer to the first of those blocks (which is the called ORF)!
	 */
	private int[] distinctStopIndices;
	private ArrayGenomicRegion[] orfs; // induced on transcript!
	private PriceOrfType[] orftypes;

	
	public MajorIsoform() {
	}
			
	public MajorIsoform(Genomic genomic, ImmutableReferenceGenomicRegion<Transcript> transcript, ExtendedIterator<ImmutableReferenceGenomicRegion<PriceOrf>> eit) {
		this.transcript = transcript;
		this.psites = new float[transcript.getRegion().getTotalLength()][];
		
		buildOrfs(genomic,eit);
	}


	private void buildOrfs(Genomic genomic, ExtendedIterator<ImmutableReferenceGenomicRegion<PriceOrf>> eit) {
		TreeMap<Integer,ArrayGenomicRegion> firstorfs = new TreeMap<>();
		TreeMap<Integer,TreeMap<ArrayGenomicRegion,PriceOrfType>> map = new TreeMap<>();
		
		for (ImmutableReferenceGenomicRegion<PriceOrf> r : eit.loop()) {
			for (int i=0; i<r.getData().getNumAlternativeStartCodons(); i++) {
				ImmutableReferenceGenomicRegion<PriceOrf> orf = r.getData().getStartStop(r, i,false);
				ArrayGenomicRegion reg = transcript.induce(orf.getRegion());
				PriceOrfType type = PriceOrfType.annotate(genomic, transcript, orf);
				TreeMap<ArrayGenomicRegion, PriceOrfType> map2 = map.computeIfAbsent(reg.getStop(), x->new TreeMap<>());
				if (!map2.containsKey(reg)) {
					// this can happen if there are two annotated CDS that are intron consistent and both kept!					
					if (r.getData().isPredictedStartIndex(i)) firstorfs.put(reg.getStop(), reg);
					map2.put(reg, type);
				}
			}
		}
		MutableReferenceGenomicRegion<Transcript> cds = transcript.getData().getCds(transcript);
		GenomicRegion orf = cds.map(new ArrayGenomicRegion(0,cds.getRegion().getTotalLength()-3));
		ArrayGenomicRegion reg = transcript.induce(orf);
		PriceOrfType type = PriceOrfType.CDS;
		TreeMap<ArrayGenomicRegion, PriceOrfType> map2 = map.computeIfAbsent(reg.getStop(), x->new TreeMap<>());
		firstorfs.put(reg.getStop(), reg);
		map2.put(reg, type);
		
		int total = EI.wrap(map.values()).mapToInt(m->m.size()).sum();
		int index = 0;
		orfs = new ArrayGenomicRegion[total];
		orftypes = new PriceOrfType[total];
		
		distinctStopIndices = new int[firstorfs.size()];
		int dindex = 0;
		
		// first add the annotated orf
		int firstkey =  reg.getStop();
		orfs[index] = firstorfs.get(firstkey);
		orftypes[index] = map.get(firstkey).get(orfs[index]);
		distinctStopIndices[dindex++] = index;
		index++;
		// ... and its n-terminal variants
		for (ArrayGenomicRegion r : map.get(firstkey).keySet())
			if (!r.equals(firstorfs.get(firstkey))) {
				orfs[index] = r;
				orftypes[index] = map.get(firstkey).get(orfs[index]);
				index++;
			}
		
		// add all other orfs
		for (Integer key : firstorfs.keySet()) {
			if (key!=firstkey) {
				orfs[index] = firstorfs.get(key);
				orftypes[index] = map.get(key).get(orfs[index]);
				distinctStopIndices[dindex++] = index;
				index++;
				// ... and their n-terminal variants
				for (ArrayGenomicRegion r : map.get(key).keySet())
					if (!r.equals(firstorfs.get(key))) {
						orfs[index] = r;
						orftypes[index] = map.get(key).get(orfs[index]);
						index++;
					}
			}
		}
		
	}


	public ImmutableReferenceGenomicRegion<Transcript> getTranscript() {
		return transcript;
	}

	public void set(int pos, NumericArray data) {
		if (psites[pos]!=null) throw new RuntimeException("Already occupied, cannot be !");
		psites[pos] = data.toFloatArray();
		
		if (cond!=0 && cond!=data.length())
			throw new IllegalArgumentException("Data has wrong length!");
		
		cond = data.length();
	}
	
	
	/**
	 * Induced on transcript! Does not include stop
	 * @param orf
	 * @return
	 */
	public ArrayGenomicRegion getOrf(int orf) {
		return orfs[orf];
	}
	
	public int getOrfGroups() {
		return distinctStopIndices.length;
	}
	
	public int getOrfsInGroup(int grp) {
		return grp==distinctStopIndices.length-1?orfs.length-distinctStopIndices[grp]:distinctStopIndices[grp+1]-distinctStopIndices[grp];
	}
	
	public int getOrfId(int grp, int index) {
		return distinctStopIndices[grp]+index;
	}
	
	public PriceOrfType getOrfType(int orf) {
		return orftypes[orf];
	}
	
	
	/**
	 * Pos is induced on transcript!
	 * @param pos
	 * @return
	 */
	private NumericArray getPosition(int pos) {
		return psites[pos]==null?null:NumericArray.wrap(psites[pos]);
	}

	
	public PositionIterator<NumericArray> iterateAminoAcids(int orf) {
		return new PositionIterator<NumericArray>() {
			int orfPos = 0;
			
			@Override
			public int nextInt() {
				int re = orfPos/3;
				orfPos+=3;
				return re;
			}
			
			@Override
			public NumericArray getData() {
				NumericArray re = getPosition(orfs[orf].map(orfPos-3));
				return re;
			}

			@Override
			public boolean hasNext() {
				return orfPos<orfs[orf].getTotalLength();
			}
			
		};
	}

	public int getNumberOfCodons(int orf) {
		return (int) iterateAminoAcids(orf).data().removeNulls().count();
	}
	
	public NumericArray getSum(int orf) {
		return iterateAminoAcids(orf).data().removeNulls().reduce(NumericArray.createMemory(cond, NumericArrayType.Float), (e,s)->{s.add(e); return s;});
	}


	public NumericArray getReadsPerCodon(int orf) {
		int l = getAminoAcidLength(orf);
		return getSum(orf).applyInPlace(d->d/l);
	}


	public int getAminoAcidLength(int orf) {
		return orfs[orf].getTotalLength()/3;
	}



	@Override
	public void serialize(BinaryWriter out) throws IOException {
		if (cond==0) throw new RuntimeException("No data!");
		FileUtils.writeReferenceSequence(out, transcript.getReference());
		FileUtils.writeGenomicRegion(out, transcript.getRegion());
		transcript.getData().serialize(out);
		out.putCInt(cond);
		for (float[] d : psites) {
			if (d==null)
				out.putFloat(Float.NaN);
			else
				for (int i=0; i<cond; i++)
					out.putFloat(d[i]);
		}
		FileUtils.writeIntArray(out, distinctStopIndices);
		out.putCInt(orfs.length);
		for (int i=0; i<orfs.length; i++){
			FileUtils.writeGenomicRegion(out, orfs[i]);
			out.putCInt(orftypes[i].ordinal());
		}
	}



	@Override
	public void deserialize(BinaryReader in) throws IOException {
		Transcript tr = new Transcript();
		transcript = new ImmutableReferenceGenomicRegion<>(FileUtils.readReferenceSequence(in), FileUtils.readGenomicRegion(in),tr);
		tr.deserialize(in);

		cond = in.getCInt();
		psites = new float[transcript.getRegion().getTotalLength()][];
		for (int p=0; p<psites.length; p++) {
			float f = in.getFloat();
			if (!Double.isNaN(f)) {
				psites[p] = new float[cond];
				psites[p][0] = f;
				for (int i=1; i<cond; i++)
					psites[p][i] = in.getFloat();
			}
		}
		distinctStopIndices = FileUtils.readIntArray(in);
		orfs = new ArrayGenomicRegion[in.getCInt()];
		orftypes = new PriceOrfType[orfs.length];
		for (int i=0; i<orfs.length; i++){
			orfs[i] = FileUtils.readGenomicRegion(in);
			orftypes[i] = PriceOrfType.values()[in.getCInt()];
		}
	}

	@Override
	public String toString() {
		return transcript.getData().getTranscriptId()+" "+orfs.length+" ORFs in "+distinctStopIndices.length+" groups";
				
	}
	
	
}
