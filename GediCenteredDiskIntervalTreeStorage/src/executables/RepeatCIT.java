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
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.userInteraction.progress.ConsoleProgress;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class RepeatCIT {

	private static String checkParam(String[] args, int index)  {
		if (index>=args.length || args[index].startsWith("-")) throw new RuntimeException("Missing argument for "+args[index-1]);
		return args[index];
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		if (args.length<3) {
			usage();
			System.exit(1);
		}
		
		boolean progress = false;
		String repeats = null;
		LineWriter verbose = null;
		CenteredDiskIntervalTreeStorage<?> in = null;
		CenteredDiskIntervalTreeStorage<?> out = null;
		
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-p"))
				progress = true;
			else if (args[i].equals("-r"))
				repeats = checkParam(args, ++i);
			else if (args[i].startsWith("-v")){
				String file = args[i].substring(2);
				if (file.length()==0)
					file = LineOrientedFile.STDOUT;
				verbose = new LineOrientedFile(file).write();
			} else {
				in = new CenteredDiskIntervalTreeStorage(checkParam(args, i++));
				out= new CenteredDiskIntervalTreeStorage(checkParam(args, i++),in.getType());
				if (i!=args.length)
					throw new RuntimeException("Unknown parameter "+args[i]);
			}
		}
		if (repeats==null) {
			usage();
			System.exit(1);
		}
		
		
		
		MemoryIntervalTreeStorage<ImmutableReferenceGenomicRegion<Void>> copyTo2From = new MemoryIntervalTreeStorage<ImmutableReferenceGenomicRegion<Void>>((Class)ImmutableReferenceGenomicRegion.class);
		ExtendedIterator<ImmutableReferenceGenomicRegion<ImmutableReferenceGenomicRegion<Void>>> pit = new LineOrientedFile(repeats)
			.lineIterator()
			.map(a->StringUtils.split(a, '\t'))
			.<ImmutableReferenceGenomicRegion[]>map(a->(ImmutableReferenceGenomicRegion[])EI.wrap(a)
						.map(r->ImmutableReferenceGenomicRegion.parse(r))
						.toArray(new ImmutableReferenceGenomicRegion[0]))
			.map(b->{
				ImmutableReferenceGenomicRegion[] a = (ImmutableReferenceGenomicRegion[])b;
				return new ImmutableReferenceGenomicRegion<ImmutableReferenceGenomicRegion<Void>>(a[1].getReference(), a[1].getRegion(),a[0]);
			});
		copyTo2From.fill(pit);

		HashSet<ReferenceSequence> refs = new HashSet<ReferenceSequence>();
		refs.addAll(copyTo2From.getReferenceSequences());
		refs.addAll(in.getReferenceSequences());
		
		ExtendedIterator<ImmutableReferenceGenomicRegion<?>> it = EI.empty();
		for (ReferenceSequence ref : refs) {
			if (copyTo2From.getReferenceSequences().contains(ref)) {
				ExtendedIterator<ImmutableReferenceGenomicRegion<?>> rit = (ExtendedIterator)in.ei(ref);
				for (ImmutableReferenceGenomicRegion<ImmutableReferenceGenomicRegion<Void>> to2From : copyTo2From.ei(ref).loop()) 
					rit = rit.chain(copyRegions(verbose,in,to2From,to2From.getData()));
				rit = rit.sort();
				it = it.chain((ExtendedIterator)rit);
				
			} else {
				it = it.chain((ExtendedIterator)in.ei(ref));
			}
		}
		if (progress)
			it = it.progress(new ConsoleProgress(System.err),-1,r->r.toLocationStringRemovedIntrons());
		
		out.fill((ExtendedIterator)it);
		
		
		out.setMetaData(in.getMetaData());
		
		if (verbose!=null)
			verbose.close();
		
	}

	private static Iterator<ImmutableReferenceGenomicRegion<?>> copyRegions(LineWriter verbose,
			CenteredDiskIntervalTreeStorage<?> in,
			ImmutableReferenceGenomicRegion<?> to,
			ImmutableReferenceGenomicRegion<?> from) {
		System.out.println(from.toLocationString()+" -> "+to.toLocationString());
		return in.ei(from)
			.filter(e->{
				boolean re = from.getRegion().contains(e.getRegion());
				if (verbose!=null && !re) {
					verbose.writeLine2("Filtered out "+e+", not contained in "+from);
				}
				ImmutableReferenceGenomicRegion mapped = new ImmutableReferenceGenomicRegion(to.getReference(),to.map(from.induce(e.getRegion())),e.getData());
				if (re && in.contains(mapped.getReference(), mapped.getRegion())) {
					if (verbose!=null && !in.getData(mapped.getReference(), mapped.getRegion()).equals(mapped.getData())) 
						verbose.writeLine2("Filtered out "+mapped+", already present: "+in.getData(mapped.getReference(), mapped.getRegion()));
					re = false;
				}
				
				
				return re;
			})
			.map(e->{
				ImmutableReferenceGenomicRegion re = new ImmutableReferenceGenomicRegion(to.getReference(),to.map(from.induce(e.getRegion())),e.getData());
				if (verbose!=null) 
					verbose.writeLine2("Mapped: "+e+" -> "+re+" ("+from+")");
				return re;
			});
	}

	private static void usage() {
		System.out.println("RepeatCIT -r repeats.txt [-p] [-v] [-v<file>] <input> <output> ... \n\n -r Repeat locations in format <from>\\t<to>\n -p shows progress");
	}
	
}
