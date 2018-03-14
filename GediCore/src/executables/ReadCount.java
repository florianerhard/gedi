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
import java.nio.file.Path;
import java.nio.file.Paths;

import gedi.app.Gedi;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStoragePreload;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.userInteraction.progress.ConsoleProgress;


public class ReadCount {

	private static String checkParam(String[] args, int index) {
		if (index>=args.length || args[index].startsWith("-")) throw new RuntimeException("Missing argument for "+args[index-1]);
		return args[index];
	}
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		
		boolean progress = false;
		ReadCountMode mode = ReadCountMode.Weight;
		
		int i;
		for (i=0; i<args.length; i++) {
			if (args[i].equals("-p"))
				progress = true;
			else if (args[i].equals("-h")) {
				usage();
				return;
			}
			else if (args[i].equals("-m"))
				mode = ParseUtils.parseEnumNameByPrefix(checkParam(args,++i), true, ReadCountMode.class);
			else
				break;
		}
		
		if (i+1!=args.length) {
			usage();
			System.exit(1);
		}
		
		Gedi.startup(false);
		
		Path p = Paths.get(args[i++]);
		WorkspaceItemLoader<GenomicRegionStorage<? extends AlignedReadsData>,GenomicRegionStoragePreload<AlignedReadsData>> loader = WorkspaceItemLoaderExtensionPoint.getInstance().get(p);
		if (loader==null)
			throw new RuntimeException("No loader available for "+p);
		GenomicRegionStorage<? extends AlignedReadsData> in = (GenomicRegionStorage<? extends AlignedReadsData>) loader.load(p);
		
		int numCond = in.getRandomRecord().getNumConditions();
		NumericArray total = NumericArray.createMemory(numCond, NumericArrayType.Double);
		
		ReadCountMode imode = mode;
		in.ei().iff(progress,ei->ei.progress(new ConsoleProgress(System.err), -1, r->"Processing "+r.toLocationStringRemovedIntrons()+" "+total.formatArray("%.2g",",")))
				.forEachRemaining(r->r.getData().addTotalCountsForConditions(total, imode));
		
		
		DynamicObject totals = DynamicObject.from("conditions",EI.seq(0,total.length()).map(c->DynamicObject.from("total", total.getDouble(c))).toArray(DynamicObject.class));
		
		DynamicObject meta = in.getMetaData().cascade(totals);
		in.setMetaData(meta);

		EI.wrap(meta.getEntry("conditions").asArray()).map(d->d.getEntry("name").asString()+"\t"+d.getEntry("total").asDouble()).print();
		System.out.println("-----");
		System.out.println("Total\t"+EI.wrap(meta.getEntry("conditions").asArray()).mapToDouble(d->d.getEntry("total").asDouble()).sum());
	}

	private static void usage() {
		System.out.println("ReadCount [-p] [-m <mode>]<input>\n\n -p shows progress\n -m <mode> set read count mode  (One of: "+StringUtils.concat(',', ReadCountMode.values())+")\n\nOutputs a table and writes the total counts into the metadata file");
	}
	
}
