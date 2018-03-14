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
