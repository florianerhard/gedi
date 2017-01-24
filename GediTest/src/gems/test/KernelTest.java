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

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.reference.Chromosome;
import gedi.util.ArrayUtils;
import gedi.util.algorithm.rmq.DiskMinMaxSumIndex;
import gedi.util.algorithm.rmq.DoubleDiskSuccinctRmaxq;
import gedi.util.algorithm.rmq.SuccinctRmaxq;
import gedi.util.algorithm.rmq.SuccinctRminq;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileView;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.math.stat.kernel.Kernel;
import gedi.util.math.stat.kernel.PreparedIntKernel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class KernelTest {
	
	@Test
	public void preparedTest() throws IOException {
		PreparedIntKernel k = new Kernel() {
			
			@Override
			public double applyAsDouble(double operand) {
				return Math.pow(2,-Math.abs(operand));
			}
			
			@Override
			public double halfSize() {
				return 2;
			}
		}.prepare();
		
		
		assertEquals(0, k.applyAsDouble(-3),1E-6);
		assertEquals(0.25, k.applyAsDouble(-2),1E-6);
		assertEquals(0.5, k.applyAsDouble(-1),1E-6);
		assertEquals(1, k.applyAsDouble(0),1E-6);
		assertEquals(0.5, k.applyAsDouble(1),1E-6);
		assertEquals(0.25, k.applyAsDouble(2),1E-6);
		assertEquals(0, k.applyAsDouble(3),1E-6);
		
		
		int[] array = ArrayUtils.seq(0, 19, 1);
		
		assertEquals((0*1+1*0.5+2*0.25), k.applyToArraySlice(array, 0),1E-6);
		assertEquals((0*0.5+1*1+2*0.5+3*0.25), k.applyToArraySlice(array, 1),1E-6);
		assertEquals((0*0.25+1*0.5+2*1+3*0.5+4*0.25), k.applyToArraySlice(array, 2),1E-6);
		assertEquals((1*0.25+2*0.5+3*1+4*0.5+5*0.25), k.applyToArraySlice(array, 3),1E-6);
		assertEquals((15*0.25+16*0.5+17*1+18*0.5+19*0.25), k.applyToArraySlice(array, 17),1E-6);
		assertEquals((16*0.25+17*0.5+18*1+19*0.5), k.applyToArraySlice(array, 18),1E-6);
		assertEquals((17*0.25+18*0.5+19*1), k.applyToArraySlice(array, 19),1E-6);
		
		
		double[] a2 = new double[array.length];
		double[] exp = new double[array.length];
		for (int i=0; i<exp.length; i++) {
			exp[i]=k.applyToArraySlice(array, i);
			a2[i] = array[i];
		}
		
		k.processInPlace(a2, 0, a2.length);
		for (int i=0; i<a2.length; i++)
			assertEquals(exp[i], a2[i],1E-6);
		
		
	}
	
	
}
