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

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataMerger;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.ParallellIterator;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.userInteraction.progress.ConsoleProgress;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

public class MergeCIT {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		if (args.length<3) {
			usage();
			System.exit(1);
		}
		
		boolean progress = false;
		boolean clear = false;
		HashSet<String> skip = new HashSet<>(); 
		String out = null;
		
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-p"))
				progress = true;
			else if (args[i].equals("-c"))
				clear = true;
			else if (args[i].equals("-s"))
				EI.split(args[++i], ',').toCollection(skip);
			else {
				out = args[i++];
				args = Arrays.copyOfRange(args, i, args.length);
				i = args.length;
			}
		}
		if (out==null) {
			usage();
			System.exit(1);
		}
		
		EI.wrap(args).map(File::new).throwArg(File::exists,"File %s does not exist!");
		
		if (args.length==1) {
			if (new File(out).equals(new File(args[0])))
				System.exit(0);
			if (clear) {
				new File(args[0]).renameTo(new File(out));
				if (new File(args[0]+".metadata.json").exists())
					new File(args[0]+".metadata.json").renameTo(new File(out+".metadata.json"));
			}
			else {
				Files.copy(Paths.get(args[0]),Paths.get(out));
				if (new File(args[0]+".metadata.json").exists())
					Files.copy(Paths.get(args[0]+".metadata.json"),Paths.get(out+".metadata.json"));
			}
			
			System.exit(0);
		}
		
		CenteredDiskIntervalTreeStorage<AlignedReadsData>[] storages = (CenteredDiskIntervalTreeStorage[]) 
				EI.wrap(args)
					.map(CenteredDiskIntervalTreeStorage::load)
					.toArray(new CenteredDiskIntervalTreeStorage[0]);
		boolean compressed = EI.wrap(storages).mapToInt(cit->cit.isCompressed()?1:0).sum()>=storages.length/2;
		
		DynamicObject[] metas = EI.wrap(storages).map(CenteredDiskIntervalTreeStorage::getMetaData).filter(d->!d.isNull()).toArray(DynamicObject.class);
		DynamicObject meta = metas.length==storages.length?DynamicObject.merge(metas):DynamicObject.getEmpty();
		
		
		ExtendedIterator<ImmutableReferenceGenomicRegion<? extends AlignedReadsData>>[] iterators= (ExtendedIterator[]) 
				EI.wrap(storages)
					.map(s->s.ei()
							.iff(skip.size()>0, ei->ei.filter(rgr->!skip.contains(rgr.getReference().getName())))
							.checkOrder((Comparator)FunctorUtils.naturalComparator(),()->s.getPath()))
					.toArray(new ExtendedIterator[0]);
		
		int[] conditions = EI.wrap(storages).mapToInt(s->s.getRandomRecord().getNumConditions()).toIntArray();
		AlignedReadsDataMerger merger = new AlignedReadsDataMerger(conditions);
		
		ParallellIterator<ImmutableReferenceGenomicRegion<? extends AlignedReadsData>> pit = (ParallellIterator<ImmutableReferenceGenomicRegion<? extends AlignedReadsData>>) FunctorUtils.parallellIterator((Iterator[])iterators, FunctorUtils.naturalComparator(), ImmutableReferenceGenomicRegion.class);
		
		CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> outCit = new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>(out, DefaultAlignedReadsData.class,compressed);
		outCit.fill(pit.map(merger::merge).iff(progress, ei->ei.progress(new ConsoleProgress(System.err),-1,e->e.toLocationString()+e.getData())),new ConsoleProgress(System.err));
		if (!meta.isNull())
			outCit.setMetaData(meta);
		
		if (clear) {
			for (String a : args) {
				new File(a).delete();
				new File(a+".metadata.json").delete();
			}
		}
		
	}

	private static void usage() {
		System.out.println("MergeCIT [-c] [-p] [-s skip1,skip2,...] <output> <file1> <file2> ... \n\n -c removes the input files after successful merging\n -p shows progress\n -s skip chromosomes");
	}
	
}
