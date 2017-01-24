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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.region.ArrayGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.io.randomaccess.PageFileWriter;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GenomicRegionTest {

	
	
	@Test
	public void serializeTest() throws IOException {
		
		ArrayGenomicRegion reg = new ArrayGenomicRegion(13256197,13256199,13260388,13260409,13264753,13264760);
		PageFileWriter wr = new PageFileWriter("data/test.reg");
		FileUtils.writeGenomicRegion(wr, reg);
		
		ArrayGenomicRegion reg2 = FileUtils.readGenomicRegion(wr.read(true));
		
		assertEquals(reg, reg2);
		
	}
	
	
	
	
	@Test
	public void ardTest() throws IOException {
		
		AlignedReadsDataFactory fac = new AlignedReadsDataFactory(2);
		fac.start();
		fac.newDistinctSequence();
		fac.addMismatch(4, 'C', 'G');
		fac.addDeletion(7, "AC");
		fac.setCount(new int[] {5,6});
		fac.setMultiplicity(2);
		fac.setId(8);
		
		fac.newDistinctSequence();
		fac.addInsertion(4, "TG");
		fac.setCount(new int[] {1,2});
		fac.setMultiplicity(6);
		fac.setId(1);
		AlignedReadsData first = fac.create();
		
		fac.start();
		fac.newDistinctSequence();
		fac.addInsertion(4, "TG");
		fac.setCount(new int[] {1,2});
		fac.setMultiplicity(6);
		fac.setId(1);
		
		fac.newDistinctSequence();
		fac.addDeletion(7, "AC");
		fac.addMismatch(4, 'C', 'G');
		fac.setCount(new int[] {5,6});
		fac.setMultiplicity(2);
		fac.setId(8);
		AlignedReadsData second = fac.create();
		
		fac.start();
		fac.newDistinctSequence();
		fac.addInsertion(4, "TG");
		fac.setCount(new int[] {1,2});
		fac.setMultiplicity(6);
		fac.setId(1);
		
		fac.newDistinctSequence();
		fac.addDeletion(7, "AT");
		fac.addMismatch(4, 'C', 'G');
		fac.setCount(new int[] {5,6});
		fac.setMultiplicity(2);
		fac.setId(8);
		AlignedReadsData third = fac.create();
		
		// distinct sequences are switched, as well as the two variations in d=0 / d=1

		
		assertEquals(first.hashCode(), second.hashCode());
		
		assertTrue(first.equals(second));
		assertTrue(second.equals(first));
		
		
		assertNotEquals(first.hashCode(), third.hashCode());
		
		assertFalse(first.equals(third));
		assertFalse(third.equals(first));
			
		
	}
	
}
