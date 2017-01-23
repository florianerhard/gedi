package gems.test;

import static org.junit.Assert.assertEquals;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.jdbc.H2Storage;
import gedi.jdbc.JdbcStorage;
import gedi.util.mutable.MutableInteger;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JdbcTest {

	
	@Test
	public void genomicRegionIntervalTreeTest() throws SQLException, IOException {
		JdbcStorage<MutableInteger> storage = new H2Storage<MutableInteger>("./data/h2/test.h2",true,false,MutableInteger.class);
		ReferenceSequence ref = Chromosome.obtain("test");
		
		storage.add(ref,new ArrayGenomicRegion(0,10,90,100),new MutableInteger(1));
		storage.add(ref,new ArrayGenomicRegion(10,20,90,100),new MutableInteger(1));
		storage.add(ref,new ArrayGenomicRegion(19,20,30,40),new MutableInteger(1));
		storage.add(ref,new ArrayGenomicRegion(19,20,30,40),new MutableInteger(1),(a,b)->new MutableInteger(a.N+b.N));
		
		storage.add(ref,new ArrayGenomicRegion(40,50,60,70),new MutableInteger(1));
		
		storage.add(ref,new ArrayGenomicRegion(70,80,100,110),new MutableInteger(1));
		storage.add(ref,new ArrayGenomicRegion(100,110),new MutableInteger(1));
		
		storage.add(ref,new ArrayGenomicRegion(200,201),new MutableInteger(1));
		
		
		assertEquals(0,storage.getRegionsIntersectingList(ref,20, 25).size());
		System.out.println(storage.getRegionsIntersectingList(ref,19, 25));
		assertEquals(2,storage.getRegionsIntersectingList(ref,19, 25).size());
		
		assertEquals(0,storage.getRegionsIntersectingList(ref,50, 59).size());
		assertEquals(4,storage.getRegionsIntersectingList(ref,99, 101).size());
		assertEquals(2,storage.getRegionsIntersectingMap(ref,30, 40).get(new ArrayGenomicRegion(19,20,30,40)).intValue());
		
		storage.close();
		
	}
	
}
