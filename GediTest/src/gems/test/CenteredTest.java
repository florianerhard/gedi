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

import static org.junit.Assert.assertEquals;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.mutable.MutableInteger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CenteredTest {

	@Test
	public void clongTest() throws IOException {
		PageFileWriter test = new PageFileWriter("data/clong.bin");
		test.putCLong((long)Integer.MAX_VALUE/2-1);
		test.putCLong((long)Integer.MAX_VALUE/2);
		test.putCLong((long)Integer.MAX_VALUE/2+1);
		test.putCLong((long)Integer.MAX_VALUE-1);
		test.putCLong((long)Integer.MAX_VALUE);
		test.putCLong((long)Integer.MAX_VALUE+1);
		PageFile read = test.read(true);
		
		assertEquals((long)Integer.MAX_VALUE/2-1,read.getCLong());
		assertEquals((long)Integer.MAX_VALUE/2,read.getCLong());
		assertEquals((long)Integer.MAX_VALUE/2+1,read.getCLong());
		assertEquals((long)Integer.MAX_VALUE-1,read.getCLong());
		assertEquals((long)Integer.MAX_VALUE,read.getCLong());
		assertEquals((long)Integer.MAX_VALUE+1,read.getCLong());
		
		read.close();
	}
	
	@Test
	public void genomicRegionIntervalTreeTest() throws SQLException, IOException {
		MemoryIntervalTreeStorage<MutableInteger> storage = new MemoryIntervalTreeStorage<MutableInteger>(MutableInteger.class);
		ReferenceSequence ref = Chromosome.obtain("test");
		ReferenceSequence ref2 = Chromosome.obtain("testX");
		
		storage.add(ref,new ArrayGenomicRegion(0,10,90,100),new MutableInteger(1));
		storage.add(ref,new ArrayGenomicRegion(10,20,90,100),new MutableInteger(1));
		storage.add(ref,new ArrayGenomicRegion(19,20,30,40),new MutableInteger(1));
		storage.add(ref,new ArrayGenomicRegion(19,20,30,40),new MutableInteger(1),(a,b)->new MutableInteger(a.N+b.N));
		
		storage.add(ref,new ArrayGenomicRegion(40,50,60,70),new MutableInteger(1));
		
		storage.add(ref,new ArrayGenomicRegion(70,80,100,110),new MutableInteger(1));
		storage.add(ref,new ArrayGenomicRegion(100,110),new MutableInteger(1));
		
		storage.add(ref,new ArrayGenomicRegion(200,201),new MutableInteger(1));
		
		storage.add(ref2,new ArrayGenomicRegion(0,10,90,100),new MutableInteger(1));
		storage.add(ref2,new ArrayGenomicRegion(10,20,90,100),new MutableInteger(1));
		storage.add(ref2,new ArrayGenomicRegion(19,20,30,40),new MutableInteger(1));
		storage.add(ref2,new ArrayGenomicRegion(19,20,30,40),new MutableInteger(1),(a,b)->new MutableInteger(a.N+b.N));
		
		storage.add(ref2,new ArrayGenomicRegion(40,50,60,70),new MutableInteger(1));
		
		storage.add(ref2,new ArrayGenomicRegion(70,80,100,110),new MutableInteger(1));
		storage.add(ref2,new ArrayGenomicRegion(100,110),new MutableInteger(1));
		
		storage.add(ref2,new ArrayGenomicRegion(200,201),new MutableInteger(1));
	
		
		new File("data/test.cit").delete();
		CenteredDiskIntervalTreeStorage<MutableInteger> disk = new CenteredDiskIntervalTreeStorage<MutableInteger>("data/test.cit", MutableInteger.class);
		disk.fill(storage);
		
		assertEquals(0,disk.getRegionsIntersectingList(ref,20, 25).size());
		assertEquals(2,disk.getRegionsIntersectingList(ref,19, 25).size());
		
		assertEquals(0,disk.getRegionsIntersectingList(ref,50, 59).size());
		assertEquals(4,disk.getRegionsIntersectingList(ref,99, 101).size());
		assertEquals(2, disk.getRegionsIntersectingMap(ref,30, 40).get(new ArrayGenomicRegion(19,20,30,40)).intValue());
		
		
		assertEquals(0,disk.getRegionsIntersectingList(ref2,20, 25).size());
		assertEquals(2,disk.getRegionsIntersectingList(ref2,19, 25).size());
		
		assertEquals(0,disk.getRegionsIntersectingList(ref2,50, 59).size());
		assertEquals(4,disk.getRegionsIntersectingList(ref2,99, 101).size());
		assertEquals(2, disk.getRegionsIntersectingMap(ref2,30, 40).get(new ArrayGenomicRegion(19,20,30,40)).intValue());
		
	}
	
}
