package gems.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class GenomicRegionIntervalTreeTest {

	
	
	@Test
	public void genomicRegionIntervalTreeTest() {
		MemoryIntervalTreeStorage<Integer> storage = new MemoryIntervalTreeStorage<Integer>(Integer.class);
		ReferenceSequence ref = Chromosome.obtain("test");
		
		storage.add(ref,new ArrayGenomicRegion(0,10,90,100),1);
		storage.add(ref,new ArrayGenomicRegion(10,20,90,100),1);
		storage.add(ref,new ArrayGenomicRegion(19,20,30,40),1);
		storage.add(ref,new ArrayGenomicRegion(19,20,30,40),1,(a,b)->a+b);
		
		storage.add(ref,new ArrayGenomicRegion(40,50,60,70),1);
		
		storage.add(ref,new ArrayGenomicRegion(70,80,100,110),1);
		storage.add(ref,new ArrayGenomicRegion(100,110),1);
		
		storage.add(ref,new ArrayGenomicRegion(200,201),1);
		
//		Iterator<Set<ArrayGenomicRegion>> it = tree.iterateSingleLinkage();
//		assertTrue(it.hasNext());
//		assertEquals(3,it.next().size());
//		assertTrue(it.hasNext());
//		assertEquals(1,it.next().size());
//		assertTrue(it.hasNext());
//		assertEquals(2,it.next().size());
//		assertTrue(it.hasNext());
//		assertEquals(1,it.next().size());
//		assertFalse(it.hasNext());
		
		
		assertEquals(0,storage.getRegionsIntersectingList(ref,20, 25).size());
		assertEquals(2,storage.getRegionsIntersectingList(ref,19, 25).size());
		
		assertEquals(0,storage.getRegionsIntersectingList(ref,50, 59).size());
		assertEquals(4,storage.getRegionsIntersectingList(ref,99, 101).size());
		assertEquals(2, storage.getRegionsIntersectingMap(ref,30, 40).get(new ArrayGenomicRegion(19,20,30,40)).intValue());
		
	}
	
}
