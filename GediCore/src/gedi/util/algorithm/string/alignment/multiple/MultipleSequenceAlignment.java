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
package gedi.util.algorithm.string.alignment.multiple;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;

import gedi.app.extension.ExtensionContext;
import gedi.core.data.annotation.Transcript;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.Workspace;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.charsequence.CharArrayCharSequence;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.fasta.FastaEntry;
import gedi.util.io.text.fasta.FastaFile;

public class MultipleSequenceAlignment {
	
	
	private GenomicRegionStorage<MsaBlock> storage; /// w.r.t. MSA, i.e. + strand only!
	private String[] species;

	
	public MultipleSequenceAlignment(GenomicRegionStorage<MsaBlock> storage) {
		this.storage = storage;
	}
	
	public GenomicRegionStorage<MsaBlock> getStorage() {
		return storage;
	}
	
	
	public void save(String file, boolean compress) {
		GenomicRegionStorage<MsaBlock> cl = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, file).add(Class.class, MsaBlock.class).add(Boolean.class,compress), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		cl.fill(storage);
	}
	
	public String[] getSpecies() {
		if (species==null)
			species = EI.wrap(storage.getMetaData().getEntry("species").asArray()).map(d->d.asString()).toArray(String.class);
		return species;
	}
	
	public CharSequence[] getMsa(ReferenceGenomicRegion<?> location) {
		char[][] re = new char[getSpecies().length][location.getRegion().getTotalLength()];
		for (int i=0; i<re.length; i++) 
			Arrays.fill(re[i], ' ');
		
		for (ReferenceGenomicRegion<MsaBlock> block : storage.ei(location.getReference().toPlusStrand(),location.getRegion()).loop()) {
			ArrayGenomicRegion inter = block.getRegion().intersect(location.getRegion());
			for (int i=0; i<re.length; i++) {
				char[] interS = SequenceUtils.extractSequence(block.getRegion().induce(inter), block.getData().getRows()[i]);
				ArrayGenomicRegion bre = location.getRegion().induce(inter);
				int off = 0;
				for (int p=0; p<bre.getNumParts(); p++) {
					System.arraycopy(interS, off, re[i], bre.getStart(p), bre.getLength(p));
					off+=bre.getLength(p);
				}
			}
		}
		CharSequence[] re2 = new CharSequence[re.length];
		for (int i=0; i<re.length; i++) 
			re2[i] = new CharArrayCharSequence(re[i]);
		return re2;
	}
	
	public static MultipleSequenceAlignment fromFile(String file) throws IOException {
		return new MultipleSequenceAlignment(Workspace.loadItem(file));
	}
		
	
	public static MultipleSequenceAlignment fromAlignmentFasta(String refName, String fasta) throws IOException {
		FastaEntry[] en = new FastaFile(fasta).entryIterator().toArray(FastaEntry.class);
		String[] species = EI.wrap(en).map(fe->fe.getHeader().substring(1)).toArray(String.class);
		
		LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
		meta.put("species", species);
		
		char[][] ali = EI.wrap(en).map(fe->fe.getSequence().toCharArray()).toArray(char[].class);
		
		MemoryIntervalTreeStorage<MsaBlock> st = new MemoryIntervalTreeStorage<>(MsaBlock.class);
		st.add(new ImmutableReferenceGenomicRegion<MsaBlock>(Chromosome.obtain(refName,true), new ArrayGenomicRegion(0,ali[0].length),new MsaBlock(ali)));
		st.setMetaData(DynamicObject.from(meta));
		return new MultipleSequenceAlignment(st);
	}

}
