package gems.test;

import static org.junit.Assert.*;
import gedi.util.sequence.ObjectEditDistanceGraphBuilder;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class QuickTest {

	
	@Test
	public void testEditDistance() {
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2,3), Arrays.asList(1,2,3)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2,3), Arrays.asList(1,3)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2,3), Arrays.asList(2,3)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2,3), Arrays.asList(1,2)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,3), Arrays.asList(1,2,3)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2), Arrays.asList(1,2,3)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(2,3), Arrays.asList(1,2,3)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1), Arrays.asList()));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(), Arrays.asList(1)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2), Arrays.asList(1)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2), Arrays.asList(2)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1), Arrays.asList(1,2)));
		assertTrue(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(2), Arrays.asList(1,2)));
		
		assertFalse(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,4,3), Arrays.asList(1,2,3)));
		assertFalse(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(4,2,3), Arrays.asList(1,2,3)));
		assertFalse(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2,4), Arrays.asList(1,2,3)));
		assertFalse(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1), Arrays.asList(2)));
		assertFalse(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(), Arrays.asList(1,2)));
		assertFalse(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(3), Arrays.asList(1,2)));
		assertFalse(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2,3), Arrays.asList(2)));
		assertFalse(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2,3), Arrays.asList(1)));
		assertFalse(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2,3), Arrays.asList(3)));
		assertFalse(ObjectEditDistanceGraphBuilder.isEditDistance1(Arrays.asList(1,2), Arrays.asList(3)));
		
		
		
		
	}
}
