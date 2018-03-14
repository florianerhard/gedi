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
package gems.test;


import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.diskintervaltree.DiskIntervalTreeStorage;
import gedi.ensembl.BiomartExonFileReader;
import gedi.jdbc.H2Storage;
import gedi.jdbc.MysqlStorage;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.util.ArrayUtils;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.math.stat.RandomNumbers;
import gems.test.tools.StorageTester;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@AxisRange(min = 0, max = 20)
@BenchmarkMethodChart(filePrefix = "benchmark-lists")
public class PerformanceTest {
	@Rule
	public BenchmarkRule benchmarkRun = new BenchmarkRule();

	private static StorageTester tester;
	private static H2Storage<DefaultAlignedReadsData> h2storage;
	private static MysqlStorage<DefaultAlignedReadsData> mysqlstorage;
	private static BamGenomicRegionStorage bamstorage;
	private static BamGenomicRegionStorage ubamstorage;
	private static DiskIntervalTreeStorage<DefaultAlignedReadsData> rbtstorage;
	private static CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> citstorage;
	
	private static H2Storage<DefaultAlignedReadsData> h2storagelocal;
	private static BamGenomicRegionStorage bamstoragelocal;
	private static BamGenomicRegionStorage ubamstoragelocal;
	private static DiskIntervalTreeStorage<DefaultAlignedReadsData> rbtstoragelocal;
	private static CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> citstoragelocal;

	private static H2Storage<DefaultAlignedReadsData> h2storagelocalhd;
	private static BamGenomicRegionStorage bamstoragelocalhd;
	private static BamGenomicRegionStorage ubamstoragelocalhd;
	private static DiskIntervalTreeStorage<DefaultAlignedReadsData> rbtstoragelocalhd;
	private static CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> citstoragelocalhd;

	@BeforeClass
	public static void readTranscripts() throws IOException, SQLException {
		
		RandomNumbers rnd = new RandomNumbers();
		
		MemoryIntervalTreeStorage<Transcript[]> trstorage = new BiomartExonFileReader("/mnt/biostor1/Data/Databases/Ensembl/v75/homo_exon_info.csv",true).readIntoMemoryArrayCombiner(Transcript.class);
		ImmutableReferenceGenomicRegion[] all = trstorage.asCollection().toArray(new ImmutableReferenceGenomicRegion[0]);
		rnd.shuffle(all);
		ImmutableReferenceGenomicRegion[] transcripts = Arrays.copyOf(all, 5000);
//		transcripts = new ReferenceGenomicRegion[] {
//				ReferenceGenomicRegion.parse("chr10-:13360082-13361356|13363973-13364065|13364834-13365047|13370350-13370450|13371697-13371788|13375816-13375971|13378242-13378350|13380704-13380808|13386757-13387028|13389999-13390280")
//		};
		
		tester = new StorageTester(transcripts);
		
		h2storage = new H2Storage<DefaultAlignedReadsData>("/home/proj/Herpes/Data/RibosomalProfiling/combined/new_pipeline/output/run2A_h2",false,false,DefaultAlignedReadsData.class);
		mysqlstorage = new MysqlStorage<DefaultAlignedReadsData>("/home/users/erhard/dbcfg/miRNADB2.config","run2A",DefaultAlignedReadsData.class);
		rbtstorage = new DiskIntervalTreeStorage<DefaultAlignedReadsData>("/home/proj/Herpes/Data/RibosomalProfiling/combined/new_pipeline/output/rbt");
		citstorage = new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>("/home/proj/Herpes/Data/RibosomalProfiling/combined/new_pipeline/output/run2A.cit",DefaultAlignedReadsData.class);
		bamstorage = new BamGenomicRegionStorage(true, "/home/proj/Herpes/Data/RibosomalProfiling/combined/new_pipeline/output/run2A_counts.bam");
		ubamstorage = new BamGenomicRegionStorage(true, "/home/proj/Herpes/Data/RibosomalProfiling/combined/new_pipeline/output/run2A_counts_uncompressed.bam");
		
		h2storagelocal = new H2Storage<DefaultAlignedReadsData>("/usr/local/storage/run2A/run2A_h2",false,false,DefaultAlignedReadsData.class);
		rbtstoragelocal = new DiskIntervalTreeStorage<DefaultAlignedReadsData>("/usr/local/storage/run2A/rbt");
		citstoragelocal = new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>("/usr/local/storage/run2A/run2A.cit",DefaultAlignedReadsData.class);
		bamstoragelocal = new BamGenomicRegionStorage(true, "/usr/local/storage/run2A/run2A_counts.bam");
		ubamstoragelocal = new BamGenomicRegionStorage(true, "/usr/local/storage/run2A/run2A_counts_uncompressed.bam");
		
		
//		h2storagelocalhd = new H2Storage<DefaultAlignedReadsData>("/local/storage/run2A/run2A_h2",false,false,DefaultAlignedReadsData.class);
//		rbtstoragelocalhd = new DiskIntervalTreeStorage<DefaultAlignedReadsData>("/local/storage/run2A/rbt");
//		citstoragelocalhd = new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>("/local/storage/run2A/run2A.cit",DefaultAlignedReadsData.class);
//		bamstoragelocalhd = new BamGenomicRegionStorage(true, "/local/storage/run2A/run2A_counts.bam");
		h2storagelocalhd = h2storagelocal;
		rbtstoragelocalhd = rbtstoragelocal;
		citstoragelocalhd = citstoragelocal;
		bamstoragelocalhd = bamstoragelocal;
		ubamstoragelocalhd = ubamstoragelocal;
		
	}
	
//	@Test
	public void H2_nfs() throws IOException, SQLException {
		tester.test("h2",h2storage,null);//new LineOrientedFile("h2")));
	}
	
//	@Test
	public void MySQL() throws IOException, SQLException {
		tester.test("mysql",mysqlstorage,null);//new LineOrientedFile("mysql")));
	}

	@Test
	public void BAM_storage_nfs() throws IOException {
		tester.test("bam",bamstorage,null);//new LineOrientedFile("bam"));
	}
	
	@Test
	public void uBAM_storage_nfs() throws IOException {
		tester.test("ubam",ubamstorage,null);//new LineOrientedFile("bam"));
	}
	
	@Test
	public void RBT_nfs() throws IOException {
		tester.test("rbt",rbtstorage, null);//new LineOrientedFile("rbt")));
	}
	
	@Test
	public void CIT_nfs() throws IOException {
		tester.test("cit",citstorage, null);//, new LineOrientedFile("cit"));
	}

	@Test
	public void BAM_samtools_nfs() throws IOException {
		tester.testSamTools("samtools","/home/proj/Herpes/Data/RibosomalProfiling/combined/new_pipeline/output/run2A_counts.bam");
	}
	
	@Test
	public void uBAM_samtools_nfs() throws IOException {
		tester.testSamTools("usamtoolsnfs","/home/proj/Herpes/Data/RibosomalProfiling/combined/new_pipeline/output/run2A_counts_uncompressed.bam");
	}
	
	
	@Test
	public void H2_ssd() throws IOException, SQLException {
		tester.test("h2ssd",h2storagelocal,null);//new LineOrientedFile("h2")));
	}
	

	@Test
	public void BAM_storage_ssd() throws IOException {
		tester.test("bamssd",bamstoragelocal,null);// new LineOrientedFile("bam")));
	}
	
	@Test
	public void uBAM_storage_ssd() throws IOException {
		tester.test("ubamssd",ubamstoragelocal,null);// new LineOrientedFile("bam")));
	}
	
	@Test
	public void RBT_ssd() throws IOException {
		tester.test("rbtssd",rbtstoragelocal,null);//new LineOrientedFile("rbt")));
	}
	
	@Test
	public void CIT_ssd() throws IOException {
		tester.test("citssd",citstoragelocal,null);//new LineOrientedFile("cit")));
	}
	
	@Test
	public void BAM_samtools_ssd() throws IOException {
		tester.testSamTools("samtoolsssd","/local/storage/run2A/run2A_counts.bam");
	}

	@Test
	public void BAM_samtools_hdd() throws IOException {
		tester.testSamTools("samtoolshdd","/usr/local/storage/run2A/run2A_counts.bam");
	}
	
	
	@Test
	public void H2_hdd() throws IOException, SQLException {
		tester.test("h2hdd",h2storagelocalhd,null);//new LineOrientedFile("h2")));
	}
	
	@Test
	public void uBAM_samtools_ssd() throws IOException {
		tester.testSamTools("usamtoolsssd","/local/storage/run2A/run2A_counts_uncompressed.bam");
	}

	@Test
	public void uBAM_samtools_hdd() throws IOException {
		tester.testSamTools("usamtoolshdd","/usr/local/storage/run2A/run2A_counts_uncompressed.bam");
	}
	
	@Test
	public void uBAM_storage_hdd() throws IOException {
		tester.test("ubamhdd",ubamstoragelocalhd,null);// new LineOrientedFile("bam")));
	}

	@Test
	public void BAM_storage_hdd() throws IOException {
		tester.test("bamhdd",bamstoragelocalhd,null);// new LineOrientedFile("bam")));
	}
	
	@Test
	public void RBT_hdd() throws IOException {
		tester.test("rbthdd",rbtstoragelocalhd,null);//new LineOrientedFile("rbt")));
	}
	
	@Test
	public void CIT_hdd() throws IOException {
		tester.test("cithdd",citstoragelocalhd,null);//new LineOrientedFile("cit")));
	}


}
