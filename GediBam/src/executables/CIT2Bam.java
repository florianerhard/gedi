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
import gedi.bam.tools.BamUtils;
import gedi.core.data.HasConditions;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.workspace.loader.WorkspaceItemLoader;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.util.FileUtils;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableInteger;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SAMFileHeader.SortOrder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

public class CIT2Bam {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		if (args.length<2) {
			usage();
			System.exit(1);
		}
		
		boolean progress = false;
		boolean pool = false;
		
		String in = null;
		String out = null;
		ArrayList<String> genomes = new ArrayList<>();
		
		int i;
		for (i=0; i<args.length; i++) {
			if (args[i].equals("-p"))
				progress = true;
			else if (args[i].equals("-pool")) {
				pool = true;
			} else if (args[i].equals("-g")) {
				for (i++; i<args.length-2 && !args[i].startsWith("-"); i++)
					genomes.add(args[i]);
				i--;
			} else {
				in = i<args.length?args[i++]:null;
				out = i<args.length?args[i]:null;
			}
		}
		if (out==null || in==null || i!=args.length) {
			usage();
			System.exit(1);
		}
		
		Gedi.startup(false);
		
		Path path = Paths.get(in);
		WorkspaceItemLoader<GenomicRegionStorage<AlignedReadsData>,?> loader = WorkspaceItemLoaderExtensionPoint.getInstance().get(path);
		if (loader==null) {
			System.err.println("Cannot load "+in);
			usage();
			System.exit(1);
		}
		
		Progress prog = progress?new ConsoleProgress():new NoProgress();
		Genomic g = Genomic.get(genomes);
		GenomicRegionStorage<AlignedReadsData> reads = loader.load(path);
		String[] conditions = reads.getMetaDataConditions();
		
		
		for (Output outp : makeOutputs(out,g, conditions, pool, reads.getRandomRecord())) {
			System.out.println("Writing to "+outp.name);
			prog.init();
			
			MutableInteger n = new MutableInteger();
			for (ImmutableReferenceGenomicRegion<AlignedReadsData> r : reads.ei().loop()) {
				
				for (int c : outp.conditions)
					for (SAMRecord sam : BamUtils.toSamRecords(r, outp.writer.getFileHeader(), c, ()->n.N+++"",g)) {
						outp.writer.addAlignment(sam);
					}
				prog.incrementProgress().setDescription(()->r.toLocationStringRemovedIntrons());
			}
			outp.writer.close();
			prog.finish();
		}
		
	}

	private static void usage() {
		System.out.println("CIT2Bam [-p] [-pool] -g <genomes> <input> <output>\n\n -p shows progress\n -pool write a single output bam, pooling all conditions\n");
	}
	
	
	private static Output[] makeOutputs(String out, Genomic g, String[] conditions, boolean pooled, AlignedReadsData ard) {
		
		if (pooled) {
		
			SAMFileHeader header = BamUtils.createHeader(g);
			header.setSortOrder(SortOrder.coordinate);
			
			SAMFileWriterFactory fac = new SAMFileWriterFactory();
			fac.setCreateIndex(true);
			fac.setTempDirectory(new File(out).getParentFile());
			
			SAMFileWriter writer = fac.makeBAMWriter(header, false,new File(out));
			return new Output[] {
				new Output(writer,out,EI.seq(0, ard.getNumConditions()).toIntArray())	
			};
		} 
		
		
		Output[] re = new Output[ard.getNumConditions()];
		for (int i=0; i<re.length;i++) {
			SAMFileHeader header = BamUtils.createHeader(g);
			header.setSortOrder(SortOrder.coordinate);
			
			SAMFileWriterFactory fac = new SAMFileWriterFactory();
			fac.setCreateIndex(true);
			fac.setTempDirectory(new File(out).getParentFile());
			
			String name = FileUtils.getFullNameWithoutExtension(out)+"."+conditions[i]+".bam";
			SAMFileWriter writer = fac.makeBAMWriter(header, false,new File(name));
			re[i] = new Output(writer,name,new int[] {i});	
		}
		return re;
	}
	
	private static class Output {
		SAMFileWriter writer;
		String name;
		int[] conditions;
		public Output(SAMFileWriter writer, String name, int[] conditions) {
			this.writer = writer;
			this.name = name;
			this.conditions = conditions;
		}
		
	}
	
}
