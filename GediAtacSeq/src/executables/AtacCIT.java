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

import gedi.app.Gedi;
import gedi.app.extension.ExtensionContext;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.serialization.BinarySerializableSerializer;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;


public class AtacCIT {

	private static int checkMultiParam(String[] args, int index, ArrayList<String> re)  {
		while (index<args.length && !args[index].startsWith("-")) 
			re.add(args[index++]);
		return index-1;
	}

	private static int checkIntParam(String[] args, int index) {
		String re = checkParam(args, index);
		if (!StringUtils.isInt(re)) throw new RuntimeException("Must be an integer: "+args[index-1]);
		return Integer.parseInt(args[index]);
	}
	private static String checkParam(String[] args, int index)  {
		if (index>=args.length || args[index].startsWith("-")) throw new RuntimeException("Missing argument for "+args[index-1]);
		return args[index];
	}
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		if (args.length<2) {
			usage();
			System.exit(1);
		}
		
		boolean progress = false;
		
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-p"))
				progress = true;
			else {
				args = Arrays.copyOfRange(args, i, args.length);
				i = args.length;
			}
		}
		if (args.length!=1) {
			usage();
			System.exit(1);
		}
		
		String outPath = FileUtils.insertSuffixBeforeExtension(args[args.length-1],".atac");
		
		CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> in = new CenteredDiskIntervalTreeStorage<>(args[args.length-1]);
		CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> out = new CenteredDiskIntervalTreeStorage<>(outPath,in.getType(),in.isCompressed());

		
		ArrayList<String> refs = new ArrayList<>(EI.wrap(in.getReferenceSequences()).map(r->r.getName()).set());
		Collections.sort(refs);
		
		Comparator<MutableReferenceGenomicRegion<DefaultAlignedReadsData>> comp = (a,b)->a.getRegion().compareTo(b.getRegion());

		out.fill(
				EI.wrap(refs).unfold(refName->
					in.ei(Chromosome.obtain(refName,true)).map(r->r.toMutable())
					.merge(comp,in.ei(Chromosome.obtain(refName,false)).map(r->r.toMutable()))
					.map(read->read
							.transformReference(ref->ref.toStrandIndependent())
							.transformRegion(a->a.removeIntrons().extendFront(-4).extendBack(-5)))
					.fold(comp,()->new ArrayList<MutableReferenceGenomicRegion<DefaultAlignedReadsData>>(),(r,l)->{l.add(r); return l;})
					.map(AtacCIT::merge)
				)
				,progress?new ConsoleProgress(System.err):new NoProgress()
		);
		
		out.setMetaData(in.getMetaData());
		
	}
	
	public static MutableReferenceGenomicRegion<DefaultAlignedReadsData> merge(ArrayList<MutableReferenceGenomicRegion<DefaultAlignedReadsData>> list) {
		int[] count = new int[list.get(0).getData().getNumConditions()];
		for (MutableReferenceGenomicRegion<DefaultAlignedReadsData> r : list)
			for (int d=0; d<r.getData().getDistinctSequences(); d++)
				if (r.getData().getMultiplicity(d)<=1)
					r.getData().addCountsForDistinctInt(d, count, ReadCountMode.All);
		
		return list.get(0).transformData(r->AlignedReadsDataFactory.createSimple(count));
	}

	private static void usage() {
		System.out.println("AtacCIT [-p] <cit>\n\n -p shows progress");
	}
	
}
