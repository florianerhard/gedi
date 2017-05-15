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


import static org.junit.Assert.*;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.ensembl.BiomartExonFileReader;
import gedi.region.bam.BamGenomicRegionStorage;
import gedi.region.bam.BamMerge;
import gedi.util.ArrayUtils;
import gems.test.tools.StorageTester;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BamTest {

	/**
	 * Genome: AAAAAAAAAATTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCCAAAAAAAAAAAATTTTTTTTTTTTTTACCCCCCCCCCCCCCCCAAAAAAAAAAAAAA
	 * 
	 * reads:
	 * f1                TTTTTTTT---TTTTTTTTTTTACCCCCCCCCCCCC---Intron---TTTTTTTTTTTTTTGCCCCCCCCCCCCCCCC
	 * f2                TTTTTTTT---TTTTTTTTTTT-CCCCCCCCCCCCC---Intron---TTTTTTTTTTTTTTACCCCCCCCCCCCCCCC
	 * f3                TTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCC---Intron---TTTTTTTTTTTTTTACCCCCCCCCCCCCCCC
	 * f4               ATTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCC---Intron---TTTTTTTTTTTTTTACCCCCCCCCCCCCCC
	 * f5                 TTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCC---Intron---TTTTTTTTTTTTTTACCCCCCCCCCCCCCCCA
	 * and the reverse complements (r1-r5)
	 * 
	 * reads2:
	 * x1,x2             TTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCC---Intron---TTTTTTTTTTTTTTACCCCCCCCCCCCCCCC  (=f3)
	 * x3                CTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCC---Intron---TTTTTTTTTTTTTTACCCCCCCCCCCCCCCC  (Mismatch in the first base)
	 * x4                TTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCC---Intron---TTTTTTTTTTTTTTACCCCCCCCCCCCCCCG  (Mismatch in the last base)
	 * x5                TTGCTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCC---Intron---TTTTTTTTTTTTTTACCCCCCCCCCCCCCCC  (consecutive Mismatches)
	 * x6                                  TTTT-CCCCCCCCCCCCCAAAAAAAAAAAATTTTTTT
	 * 
	 * reads3
	 * l1            AAAATTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCCAAAA
	 * l2            AAAATTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCC
	 * l3            AAAATTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCCAAAAA
	 * l4            AAAATTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCCA
	 * l5            AAAATTTTTTTTAAATTTTTTTTTTT-CCCCCCCCCCCCCAAA
	 * 
	 */
	
	
	private BamGenomicRegionStorage ssStorage;
	private BamGenomicRegionStorage suStorage;
	@Before
	public void init() throws IOException {
		ssStorage = new BamGenomicRegionStorage(true, 
				"data/bam/reads.bam",
				"data/bam/reads2.bam",
				"data/bam/reads3.bam"
		);
		suStorage = new BamGenomicRegionStorage(false, 
				"data/bam/reads.bam",
				"data/bam/reads2.bam",
				"data/bam/reads3.bam"
		);
	}
	
	@Test
	public void genomicRegionsTests() {
		
		String[] presentPlus = {"6-44","6-46","6-48","6-49","6-50","9-45|57-87","10-45|57-88","11-45|57-89","28-64"};
		String[] presentMinus = {"9-45|57-87","10-45|57-88","11-45|57-89"};

		
		assertTrue(ssStorage.getReferenceSequences().contains(Chromosome.obtain("chr1+")));
		assertTrue(ssStorage.getReferenceSequences().contains(Chromosome.obtain("chr1-")));
		assertTrue(suStorage.getReferenceSequences().contains(Chromosome.obtain("chr1")));
		assertEquals(ssStorage.getReferenceSequences().size(),2);
		assertEquals(suStorage.getReferenceSequences().size(),1);
		
		
		Chromosome refp = Chromosome.obtain("chr1+");
		LinkedList<ArrayGenomicRegion> set = ssStorage.ei(refp).map(r->r.getRegion().toArrayGenomicRegion()).toList();//StreamSupport.stream(ssStorage.iterateGenomicRegions(refp), false).collect(Collectors.toCollection(()->new LinkedList<>()));
		
		for (String p : presentPlus) {
			assertFalse("Fewer than expected regions found: "+p,set.isEmpty());
			assertTrue("Expected region not found: "+p,set.removeFirst().equals(GenomicRegion.parse(p)));
		}
		assertTrue("More than the expected regions found:"+set.toString(),set.isEmpty());
		
		Chromosome refm = Chromosome.obtain("chr1-");
		set = ssStorage.ei(refm).map(r->r.getRegion().toArrayGenomicRegion()).toList();//StreamSupport.stream(ssStorage.iterateGenomicRegions(refm), false).collect(Collectors.toCollection(()->new LinkedList<>()));
		
		for (String p : presentMinus){
			assertFalse("Fewer than expected regions found: "+p,set.isEmpty());
			assertTrue("Expected region not found: "+p,set.removeFirst().equals(GenomicRegion.parse(p)));
		}
		assertTrue("More than the expected regions found:"+set.toString(),set.isEmpty());
		
		Chromosome refu = Chromosome.obtain("chr1");
		set = suStorage.ei(refu).map(r->r.getRegion().toArrayGenomicRegion()).toList();//StreamSupport.stream(suStorage.iterateGenomicRegions(refu), false).collect(Collectors.toCollection(()->new LinkedList<>()));
		
		for (String p : presentPlus){
			assertFalse("Fewer than expected regions found: "+p,set.isEmpty());
			assertTrue("Expected region not found: "+p,set.removeFirst().equals(GenomicRegion.parse(p)));
		}
		assertTrue("More than the expected regions found:"+set.toString(),set.isEmpty());
		
	}
	
	
	@Test
	public void readTests() {
		
		
		Chromosome refp = Chromosome.obtain("chr1+");
		Chromosome refm = Chromosome.obtain("chr1-");
		Chromosome refu = Chromosome.obtain("chr1");
		
		AlignedReadsDataFactory fac = new AlignedReadsDataFactory(ssStorage.getNumConditions());
		
		MemoryIntervalTreeStorage<AlignedReadsData> ssMem = new MemoryIntervalTreeStorage<AlignedReadsData>(AlignedReadsData.class);
		ssMem.asCollection().addAll(ssStorage.asCollection());
		MemoryIntervalTreeStorage<AlignedReadsData> suMem = new MemoryIntervalTreeStorage<AlignedReadsData>(AlignedReadsData.class);
		suMem.asCollection().addAll(suStorage.asCollection());

		fac.start().newDistinctSequence().setMultiplicity(0).setCount(new int[] {1,0,0}) //f1
				.addDeletion(8, "AAA")
				.addInsertion(22, "A")
				.addMismatch(49, 'A', 'G')
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {1,0,0}) //f2
				.addDeletion(8, "AAA")
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {1,2,0}) //f3+x1+x2
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {0,1,0}) //x3
				.addMismatch(0, 'T', 'C')
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {0,1,0}) //x4
				.addMismatch(65, 'C', 'G')
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {0,1,0}) //x5
				.addMismatch(2, 'T', 'G')
				.addMismatch(3, 'T', 'C');
		
		AlignedReadsData data = ssStorage.getData(refp,GenomicRegion.parse("10-45|57-88"));
		assertEquals(fac.create(),data);
		data = ssMem.getData(refp,GenomicRegion.parse("10-45|57-88"));
		assertEquals(fac.create(),data);
		
		fac.start().newDistinctSequence().setMultiplicity(0).setCount(new int[] {1,0,0}) //f1
				.addMismatch(16, 'T', 'C')
				.addInsertion(44, "T")
				.addDeletion(55, "TTT")
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {1,0,0}) //f2
				.addDeletion(55, "TTT")
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {1,0,0}); //f3
				
		data = ssStorage.getData(refm,GenomicRegion.parse("10-45|57-88"));
		assertEquals(fac.create(),data);
		data = ssMem.getData(refm,GenomicRegion.parse("10-45|57-88"));
		assertEquals(fac.create(),data);
		
		
		fac.start().newDistinctSequence().setMultiplicity(0).setCount(new int[] {2,0,0}) //f1
				.addDeletion(8, "AAA")
				.addInsertion(22, "A")
				.addMismatch(49, 'A', 'G')
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {2,0,0}) //f2
				.addDeletion(8, "AAA")
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {2,2,0}) //f3+x1+x2
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {0,1,0}) //x3
				.addMismatch(0, 'T', 'C')
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {0,1,0}) //x4
				.addMismatch(65, 'C', 'G')
			.newDistinctSequence().setMultiplicity(0).setCount(new int[] {0,1,0}) //x5
				.addMismatch(2, 'T', 'G')
				.addMismatch(3, 'T', 'C');
		
		data = suStorage.getData(refu,GenomicRegion.parse("10-45|57-88"));
		assertEquals(fac.create(),data);
		data = suMem.getData(refu,GenomicRegion.parse("10-45|57-88"));
		assertEquals(fac.create(),data);
		
		
	}
	
	
	@Test
	public void bigTest() throws IOException {
		BamMerge bams = BamMerge.fromFile("/home/proj/Herpes/Data/RibosomalProfiling/combined/new_pipeline/scripts/RNAseq.mapping");
		BamGenomicRegionStorage bam = new BamGenomicRegionStorage(true, bams);
		CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> cit = new CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>("/home/proj/Herpes/Data/RibosomalProfiling/combined/new_pipeline/output/RNAseq.cit",DefaultAlignedReadsData.class);
		
		
		MemoryIntervalTreeStorage<Transcript[]> trstorage = new BiomartExonFileReader("/mnt/biostor1/Data/Databases/Ensembl/v75/homo_exon_info.csv",true).readIntoMemoryArrayCombiner(Transcript.class);
		ImmutableReferenceGenomicRegion<Transcript[]>[] all = trstorage.getReferenceGenomicRegions().toArray(new ImmutableReferenceGenomicRegion[0]);
		ArrayUtils.shuffleSlice(all, 0, all.length);
		ImmutableReferenceGenomicRegion<Transcript[]>[] transcripts = Arrays.copyOf(all, 10);
		
		StorageTester tester = new StorageTester(transcripts);
		long start = System.currentTimeMillis();
		tester.test("cit", cit);
		System.out.println("CIT: "+(System.currentTimeMillis()-start));
		start = System.currentTimeMillis();
		tester.test("bam", bam);
		System.out.println("BAM: "+(System.currentTimeMillis()-start));
		
		start = System.currentTimeMillis();
		tester.testSamTools("samtools", bams);
		System.out.println("SAMTOOLS: "+(System.currentTimeMillis()-start));
		
	}
	
}
