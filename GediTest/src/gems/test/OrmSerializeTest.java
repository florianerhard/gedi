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
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.datastructure.dataframe.BooleanDataColumn;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.datastructure.dataframe.DoubleDataColumn;
import gedi.util.datastructure.dataframe.FactorDataColumn;
import gedi.util.datastructure.dataframe.IntegerDataColumn;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.tsv.formats.Csv;
import gedi.util.math.stat.factor.Factor;
import gedi.util.mutable.MutableInteger;
import gedi.util.mutable.MutablePair;
import gedi.util.orm.ClassTree;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class OrmSerializeTest {
	
	@BeforeClass
	public static void init() {
		Gedi.startup();
	}
	

	@Test
	public void testCsvDf() throws IOException {
		DataFrame df = Csv.toDataFrame("/home/flo/work/binfo/singlecellpnfl/fuzzification/raw.csv");
		
		OrmSerializer s = new OrmSerializer(true,true);
		PageFileWriter out = new PageFileWriter("data/raw.data");
		s.serialize(out, df);
		s.clearObjectCache();
		PageFile in = out.read(true);
		
		DataFrame df2 = s.deserialize(in);
		assertTrue(in.eof());
		
		assertEquals(df.toString(),df2.toString());		
		in.close();
	}
	
	@Test
	public void testIntervalTree() throws IOException {
		IntervalTree<GenomicRegion, MutableInteger> it = new IntervalTree<GenomicRegion, MutableInteger>(Chromosome.obtain("chr1+"));
//		it.put(new ArrayGenomicRegion(23423,23433), new MutableInteger(1));
//		it.put(new ArrayGenomicRegion(13423,23433), new MutableInteger(2));
//		it.put(new ArrayGenomicRegion(23,33), new MutableInteger(3));
//		it.put(new ArrayGenomicRegion(233,234), new MutableInteger(4));
		
		OrmSerializer s = new OrmSerializer(true,true);
		PageFileWriter out = new PageFileWriter("data/raw.data");
		s.serialize(out, it);
		s.clearObjectCache();
		PageFile in = out.read(true);
		
		IntervalTree<GenomicRegion, MutableInteger> it2 = s.deserialize(in);
		
		assertEquals(it, it2);
		
	}
	
	@Test
	public void testFactorDf() throws IOException {
		List<Factor> f = Factor.fromStrings(Arrays.asList("A","B","A"));
		DataFrame df = new DataFrame();
		df.add(new FactorDataColumn("A", f.toArray(new Factor[0])));
			df.add(new DoubleDataColumn("B", new double[] {1,2,3}));
			df.add(new IntegerDataColumn("C", new int[] {1,2,3}));
			df.add(new BooleanDataColumn("D", new boolean[] {true,true,false}));
		
		
		OrmSerializer s = new OrmSerializer(true,true);
		PageFileWriter out = new PageFileWriter("data/orm.data");
		s.serialize(out, df);
		s.clearObjectCache();
		PageFile in = out.read(true);
		
		DataFrame df2 = s.deserialize(in);
		
		assertTrue(in.eof());
		
		assertEquals(df.toString(),df2.toString());		
		in.close();
	}
	
	public void testFactor() throws IOException {
		Factor[] f = Factor.fromStrings(EI.seq(1, 5000).map(i->"S"+i).list()).toArray(new Factor[0]);
		
		OrmSerializer s = new OrmSerializer(true,true);
		PageFileWriter out = new PageFileWriter("data/orm.data");
		s.serialize(out, f);
		s.clearObjectCache();
		PageFile in = out.read(true);
		
		Factor[] df2 = s.deserialize(in);
		
		assertTrue(in.eof());
		
		assertEquals(Arrays.toString(f),Arrays.toString(df2));		
		in.close();
	}
	
	@Test
	public void testFastSeri() throws IOException {
	
		ImmutableReferenceGenomicRegion<String> test = ImmutableReferenceGenomicRegion.parse("chr16-:10626622-10626643","12");
		
		ClassTree<ImmutableReferenceGenomicRegion<String>> tree = new ClassTree<ImmutableReferenceGenomicRegion<String>>(test);
		byte[] buff = tree.toBuffer(test);
		ImmutableReferenceGenomicRegion<String> test2 = tree.fromBuffer(buff);
		
		assertEquals(test, test2);
		
	}
	
	
	@Test
	public void testSimple() throws IOException {
		SimpleClass o = new SimpleClass();
		
		OrmSerializer s = new OrmSerializer();
		PageFileWriter out = new PageFileWriter("data/orm.data");
		s.serialize(out, o);
		
		PageFile in = out.read(true);
		
		SimpleClass o2 = s.deserialize(in);
		
		assertTrue(in.eof());
		
		assertEquals(o,o2);
	
		in.close();
	}
	
	@Test
	public void testPair() throws IOException {
		MutablePair<Integer, SimpleClass> p1 = new MutablePair<Integer, SimpleClass>(0,new SimpleClass());
		MutablePair<Integer, SimpleClass> p2 = new MutablePair<Integer, SimpleClass>(1,new SimpleClass());
		
		OrmSerializer s = new OrmSerializer();
		PageFileWriter out = new PageFileWriter("data/orm.data");
		s.serialize(out, p1);
		s.serialize(out, p2);
		
		PageFile in = out.read(true);
		
		MutablePair<Integer, SimpleClass> o1 = s.deserialize(in);
		MutablePair<Integer, SimpleClass> o2 = s.deserialize(in);
		
		assertTrue(in.eof());
		
		assertEquals(p1,o1);
		assertEquals(p2,o2);
		
		in.close();
	}
	
	
	@Test
	public void testSelf() throws IOException {
		ComplexClass o = new ComplexClass();
		
		OrmSerializer s = new OrmSerializer(true,false);
		PageFileWriter out = new PageFileWriter("data/orm.data");
		s.serialize(out, o);
		
		PageFile in = out.read(true);
		s.clearObjectCache();
		
		ComplexClass o2 = s.deserialize(in);
		
		assertTrue(in.eof());
		assertTrue(o2==o2.t);
		in.close();	
	}
	
	
	@Test
	public void testRgr() throws IOException {
		
		ImmutableReferenceGenomicRegion<AlignedReadsData> o = new ImmutableReferenceGenomicRegion<AlignedReadsData>(Chromosome.obtain("chr1+"), new ArrayGenomicRegion(4000,4002,50032,50100), new AlignedReadsDataFactory(1).start().newDistinctSequence().addMismatch(4, 'C', 'G',false).setCount(new int[]{5}).create());
				
		OrmSerializer s = new OrmSerializer(true,false);
		
		PageFileWriter out = new PageFileWriter("data/orm.data");
		s.serialize(out, o);
		s.clearObjectCache();
		
		PageFile in = out.read(true);
		
		ImmutableReferenceGenomicRegion<AlignedReadsData> o2 = s.deserialize(in);
		
		assertTrue(in.eof());
		
		assertEquals(o,o2);		
		in.close();
	}
	
	
	
	private static class SimpleClass {
		String a;
		int[] x;
		byte b;
		boolean[] bitmap;
		double d;
		String[] sa;
		
		public SimpleClass() {
			this.a = "TestString";
			this.x = new int[]{1,2,3};
			this.b = (byte)-4;
			this.bitmap = new boolean[] {true,true,true,true,true,true,true,false,true};
			this.d = Double.NaN;
			this.sa = new String[] {"","1","sadsd"};
		}

		@Override
		public String toString() {
			return "SimpleClass [a=" + a + ", x=" + Arrays.toString(x) + ", b="
					+ b + ", bitmap=" + Arrays.toString(bitmap) + ", d=" + d
					+ ", sa=" + Arrays.toString(sa) + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((a == null) ? 0 : a.hashCode());
			result = prime * result + b;
			result = prime * result + Arrays.hashCode(bitmap);
			long temp;
			temp = Double.doubleToLongBits(d);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + Arrays.hashCode(sa);
			result = prime * result + Arrays.hashCode(x);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SimpleClass other = (SimpleClass) obj;
			if (a == null) {
				if (other.a != null)
					return false;
			} else if (!a.equals(other.a))
				return false;
			if (b != other.b)
				return false;
			if (!Arrays.equals(bitmap, other.bitmap))
				return false;
			if (Double.doubleToLongBits(d) != Double.doubleToLongBits(other.d))
				return false;
			if (!Arrays.equals(sa, other.sa))
				return false;
			if (!Arrays.equals(x, other.x))
				return false;
			return true;
		}

		
	}
	
	
	private static class ComplexClass {
		ComplexClass t;
		public ComplexClass() {
			t = this;
		}
		
	}
	
	
	
}
