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
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.BarcodeMapping;
import gedi.core.data.reads.BarcodedAlignedReadsData;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Strandness;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.region.bam.BamGenomicRegionStorage.PairedEndHandling;
import gedi.util.FileUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.userInteraction.progress.ConsoleProgress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class Bam2CIT {

	
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
		
		boolean is10x = false;
		boolean isUmi = false;
		String bcm = null;
		boolean progress = false;
		boolean keepIds = false;
		boolean compress = false;
		boolean var = true;
		boolean keepMito = false;
		boolean nosec = false;
		boolean unspec = false;
		boolean anti = false;
		boolean join = false;
		int head = -1;
		Genomic check = null;
		String out = null;
		PairedEndHandling peh = null;

		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-p"))
				progress = true;
			else if (args[i].equals("-check")) {
				ArrayList<String> gnames = new ArrayList<>();
				i = checkMultiParam(args, ++i, gnames);
				if (gnames.size()==0) throw new RuntimeException("No genomic given!");
				check = Genomic.get(gnames);
			} else if (args[i].equals("-id"))
				keepIds = true;
			else if (args[i].equals("-head"))
				head = checkIntParam(args,++i);
			else if (args[i].equals("-pe"))
				peh = ParseUtils.parseEnumNameByPrefix(checkParam(args,++i),true,PairedEndHandling.class);
			else if (args[i].equals("-compress"))
				compress = true;
			else if (args[i].equals("-novar"))
				var = false;
			else if (args[i].equals("-nosec"))
				nosec = true;
			else if (args[i].equals("-strandunspecific"))
				unspec = true;
			else if (args[i].equals("-anti"))
					anti = true;
			else if (args[i].equals("-")){}
			else if (args[i].equals("-10x")) {
				is10x = true;
				if (i<args.length-1 && !args[i+1].startsWith("-")){
					bcm = checkParam(args, ++i);
				}
			} 
			else if (args[i].equals("-umi")) 
				isUmi = true;
			else if (args[i].equals("-keepMito"))
				keepMito = true;
			else if (args[i].equals("-join"))
				join = true;
			else if (args[i].startsWith("-"))
				throw new IllegalArgumentException("Parameter "+args[i]+" unknown!");
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
		
		BamGenomicRegionStorage storage = new BamGenomicRegionStorage(!unspec,args);
		if (anti)
			storage.setStrandness(Strandness.Antisense);
		if (peh!=null)
			storage.setPairedEndHandling(peh);
			
		LineWriter incons = null;  
		if (check!=null)
			storage.check(check,incons = new LineOrientedFile(out+".inconsistent").write());
		storage.setIgnoreVariations(!var);
		storage.setOnlyPrimary(nosec);
		if (join) 
			storage.setJoinMates(true);
		
		storage.setKeepReadNames(keepIds);
		Class<?> dataClass = DefaultAlignedReadsData.class;
		
		if (isUmi) {
			storage.setUmi();
			dataClass = BarcodedAlignedReadsData.class;
		}
		
		if (is10x) {
			storage.set10x();
			dataClass = BarcodedAlignedReadsData.class;
			
			File[] bcs = EI.wrap(args)
				.map(bam->new File(new File(new File(bam).getAbsoluteFile().getParentFile(),"filtered_feature_bc_matrix"), "barcodes.tsv.gz")).toArray(File.class);
			
			if (EI.wrap(bcs).filter(f->f.exists()).count()==bcs.length) {
				System.err.println("Creating barcodes file!");
			
				String[] conds = storage.getMetaDataConditions();
				try (LineWriter wr = new LineOrientedFile(FileUtils.getExtensionSibling(out, ".barcodes.tsv")).write()) {
					wr.writeLine("Condition\tBarcode");
					for (int c=0; c<conds.length; c++) {
						for (String bc : EI.lines(bcs[c]).map(bc->StringUtils.removeFooter(bc, "-1")).loop()) 
							wr.writef("%s\t%s\n",conds[c],bc);
					}
				}
				
				
			}
			else {
				System.err.println("Do not create barcodes file, not all filtered_feature_bc_matrix found!");
			}
			
		}
		
		Gedi.startup(false);
		@SuppressWarnings("rawtypes")
		GenomicRegionStorage outStorage = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(Boolean.class, compress).add(String.class, out).add(Class.class, dataClass), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		int numCond = storage.getRandomRecord().getNumConditions();
		NumericArray mitocount = NumericArray.createMemory(numCond, NumericArrayType.Double);
		
		if (head>0 || !keepMito || bcm!=null) {
			ExtendedIterator<ImmutableReferenceGenomicRegion<AlignedReadsData>> it = storage.ei();
			if (head>0) it = it.head(head);
			if (!keepMito) it = it.filter(r->{
				boolean mito = r.getReference().isMitochondrial();
				if (mito) r.getData().addTotalCountsForConditions(mitocount, ReadCountMode.Weight);
				return !mito;
			});
			
			if (bcm!=null) {
				BarcodeMapping bc = new BarcodeMapping(storage.getMetaDataConditions(), bcm);
				it = it.map(r->{
					AlignedReadsData d = BarcodedAlignedReadsData.map((BarcodedAlignedReadsData)r.getData(), bc);
					if (d.getTotalCountOverall(ReadCountMode.All)==0) return null;
					return new ImmutableReferenceGenomicRegion<>(r.getReference(), r.getRegion(),d);
				}).removeNulls();
			}
			if (progress) it = it.progress();
			
			outStorage.fill(it);
		} else {
			outStorage.fill(storage,progress?new ConsoleProgress(System.err):null);
		}
		
		if (incons!=null)
			incons.close();
		
		DynamicObject meta = storage.getMetaData();
//		meta = meta.cascade(DynamicObject.from("conditions", 
//				DynamicObject.from(new Object[] {
//						DynamicObject.from("readlengths", storage.getReadLengths())
//				})
//				));
		if (storage.getNumConditions()==1 && meta.get(".conditions[0].name").isNull())
			meta = meta.cascade(DynamicObject.from("conditions", 
					DynamicObject.from(new Object[] {
							DynamicObject.from("name",storage.getName())
					})
					));
		outStorage.setMetaData(meta);
		
		if (!keepMito) {
			System.out.println("Condition\tMitochondrial");
			String[] conds = storage.getMetaDataConditions();
			for (int c=0; c<conds.length; c++) {
				System.out.print(conds[c]);
				System.out.println("\t"+mitocount.getDouble(c));
			}
			System.out.println();
		}
		System.out.println("--------------------");
		System.out.println("Total\t"+mitocount.sum());
		
	}

	private static void usage() {
		System.out.println("Bam2CIT [-p] [-id] [-nocompress] [-keepMito] [-novar] [-nosec] [-10x] <output> <file1> <file2> ... \n\n -p shows progress\n -id add ids to CIT");
	}
	
}
