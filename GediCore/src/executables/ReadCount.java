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
import java.util.ArrayList;
import java.util.HashMap;

import gedi.app.Gedi;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
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
	private static int checkMultiParam(String[] args, int index, ArrayList<String> re) {
		while (index<args.length && !args[index].startsWith("-")) 
			re.add(args[index++]);
		return index-1;
	}

	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		
		Gedi.startup(false);
		
		boolean progress = false;
		ReadCountMode mode = ReadCountMode.Weight;
		Genomic g = null;
		boolean showMT=false;
		boolean writemeta = true;
		
		int i;
		for (i=0; i<args.length; i++) {
			if (args[i].equals("-p"))
				progress = true;
			else if (args[i].equals("-h")) {
				usage();
				return;
			}
			else if (args[i].equals("-g")) {
				ArrayList<String> gnames = new ArrayList<>();
				i = checkMultiParam(args, ++i, gnames);
				g = Genomic.get(gnames);
			}
			else if (args[i].equals("-mito"))
				showMT = true;
			else if (args[i].equals("-nometa"))
				writemeta = false;
			else if (args[i].equals("-m"))
				mode = ParseUtils.parseEnumOrChoicesNameByPrefix(checkParam(args,++i), true, ReadCountMode.class);
			else
				break;
		}
		
		if (i+1!=args.length) {
			usage();
			System.exit(1);
		}
		
		
		Path p = Paths.get(args[i++]);
		WorkspaceItemLoader<GenomicRegionStorage<? extends AlignedReadsData>,GenomicRegionStoragePreload<AlignedReadsData>> loader = WorkspaceItemLoaderExtensionPoint.getInstance().get(p);
		if (loader==null)
			throw new RuntimeException("No loader available for "+p);
		GenomicRegionStorage<? extends AlignedReadsData> in = (GenomicRegionStorage<? extends AlignedReadsData>) loader.load(p);
		
		int numCond = in.getRandomRecord().getNumConditions();
		NumericArray total = NumericArray.createMemory(numCond, NumericArrayType.Double);
		NumericArray mito = NumericArray.createMemory(numCond, NumericArrayType.Double);
		
		HashMap<String,NumericArray> genomics = new HashMap<String,NumericArray>();
		HashMap<String,NumericArray> genomicsmito = new  HashMap<String,NumericArray>();
		Genomic ug = g;
		
		
		ReadCountMode imode = mode;
		in.ei()
			.iff(progress,ei->ei.progress(new ConsoleProgress(System.err), -1, r->"Processing "+r.toLocationStringRemovedIntrons()+" "+total.formatArray("%.2g",",")))
			.iff(g!=null, ei->ei.sideEffect(r->{
				if (ug.getOrigin(r.getReference())!=null) {
					String id = ug.getOrigin(r.getReference()).getId();
					NumericArray a = (r.getReference().isMitochondrial()?genomicsmito:genomics).computeIfAbsent(id, x->NumericArray.createMemory(numCond, NumericArrayType.Double));
					r.getData().addTotalCountsForConditions(a, imode);
				}
			}))
			.forEachRemaining(r->{
				r.getData().addTotalCountsForConditions(r.getReference().isMitochondrial()?mito:total, imode);
			});
		
		
		DynamicObject totals = DynamicObject.from("conditions",EI.seq(0,total.length()).map(c->DynamicObject.from("total", total.getDouble(c))).toArray(DynamicObject.class));
		if (g!=null) {
			for (String id : genomics.keySet()) {
				NumericArray a = genomics.get(id);
				DynamicObject gt = DynamicObject.from("conditions",EI.seq(0,a.length()).map(c->DynamicObject.from("total_"+id, a.getDouble(c))).toArray(DynamicObject.class));
				totals = totals.cascade(gt);
			}
		}
		
		if (writemeta) {
			DynamicObject meta = in.getMetaData().cascade(totals);
			in.setMetaData(meta);
		}

		System.out.print("Condition\ttotal\t"+(showMT?"mito\t":""));
		if (genomics.keySet().size()>1) 
			System.out.print(EI.wrap(genomics.keySet()).chain(showMT?EI.wrap(genomicsmito.keySet()).map(ge->ge+".mito"):EI.empty()).concat("\t"));
		System.out.println();
		
		String[] conds = in.getMetaDataConditions();
		for (int c=0; c<conds.length; c++) {
			System.out.print(conds[c]);
			System.out.print("\t"+total.getDouble(c));
			if (showMT)
				System.out.print("\t"+mito.getDouble(c));
			if (genomics.keySet().size()>1) {
				for (String ge : genomics.keySet())
					System.out.print("\t"+genomics.get(ge).getDouble(c));
				if (showMT)
					for (String ge : genomicsmito.keySet())
						System.out.print("\t"+genomicsmito.get(ge).getDouble(c));
			}
			System.out.println(); 
		}
//		EI.wrap(meta.getEntry("conditions").asArray()).map(d->d.getEntry("name").asString()+EI.singleton("total").chain(EI.wrap(genomics.keySet()).map(id->"total_"+id)).map(name->"\t"+d.getEntry(name).asDouble()).concat()).print();
		System.out.println("-----");
		System.out.print("Total");
		System.out.print("\t"+total.sum());
		if (showMT)
			System.out.print("\t"+mito.sum());
		if (genomics.keySet().size()>1) {
			for (String ge : genomics.keySet())
				System.out.print("\t"+genomics.get(ge).sum());
			if (showMT)
				for (String ge : genomicsmito.keySet())
					System.out.print("\t"+genomicsmito.get(ge).sum());
		}
		System.out.println(); 

//		System.out.println("Total"+EI.singleton("total").chain(EI.wrap(genomics.keySet()).map(id->"total_"+id)).map(name->"\t"+EI.wrap(meta.getEntry("conditions").asArray()).mapToDouble(d->d.getEntry(name).asDouble()).sum()).concat());
	}

	private static void usage() {
		System.out.println("ReadCount [-g <genomes>] [-p] [-nometa] [-m <mode>]<input>\n\n -MT don't skip mitochondrial reads\n -g count genome specific reads as well\n -p shows progress\n -m <mode> set read count mode  (One of: "+StringUtils.concat(',', ReadCountMode.values())+")\n\nOutputs a table and writes the total counts into the metadata file");
	}
	
}
