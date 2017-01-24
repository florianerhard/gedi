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
import gedi.app.Gedi;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.math.stat.RandomNumbers;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FastaIndexTest {

	
	@Test
	public void genomeSequenceTest() throws IOException, ClassNotFoundException {
		Gedi.startup();
		Genomic g = Genomic.get("h.ens75");
		RandomNumbers rnd = new RandomNumbers();
		int L = 10000;
		int R = 10000;
		
		String[] r = g.getSequenceNames().toArray(new String[0]);
		
		ReferenceGenomicRegion[] refs = new ReferenceGenomicRegion[R];
		String[] seq = new String[refs.length];
		for (int i=0; i<refs.length; i++) {
			Chromosome chr = Chromosome.obtain(rnd.getRandomElement(r),rnd.getBool());
			int s = rnd.getUnif(0,g.getLength(chr.getName())-L);
			refs[i] = new ImmutableReferenceGenomicRegion(chr, new ArrayGenomicRegion(s,s+L));
			seq[i] = g.getSequence(refs[i]).toString();
		}
		
		System.out.println("Collected references, starting threads");
		
		
		AtomicInteger tc = new AtomicInteger();
		
		class ConcurrentReader extends Thread {
			@Override
			public void run() {
				System.out.println("started "+tc.incrementAndGet());
				for (int i=0; i<refs.length; i++) {
					assertEquals("At "+i,seq[i], g.getSequence(refs[i]));
				}
				System.out.println("finished "+tc.decrementAndGet());
			}
		}
			
		for (int i=0; i<10; i++)
			new ConcurrentReader().start();
		while (tc.get()>0)
			Thread.yield();
			
		
	}
	
	
}
