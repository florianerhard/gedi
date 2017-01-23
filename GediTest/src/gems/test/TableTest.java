package gems.test;

import static org.junit.Assert.assertEquals;
import gedi.core.data.table.Table;
import gedi.core.data.table.TableMetaInformation;
import gedi.core.data.table.TableType;
import gedi.core.data.table.Tables;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.jdbc.H2Storage;
import gedi.jdbc.JdbcStorage;
import gedi.util.mutable.MutableInteger;
import gedi.util.orm.Orm;
import gedi.util.orm.OrmField;
import gedi.util.orm.OrmObject;
import gedi.util.orm.Orm.OrmInfo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import sun.misc.Unsafe;
import cern.colt.Arrays;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;

@RunWith(JUnit4.class)
//@AxisRange(min = 0, max = 20)
//@BenchmarkMethodChart(filePrefix = "benchmark-orm-lists")
public class TableTest {
//	@Rule
//	public BenchmarkRule benchmarkRun = new BenchmarkRule();

	private static TestOrm[] obj = new TestOrm[1000000];
	
	@BeforeClass
	public static void generateObj() throws IOException, SQLException {
		Random rnd = new Random();
		for (int i=0; i<obj.length; i++)
			obj[i] = new TestOrm(rnd.nextBoolean(), (byte) rnd.nextInt(256-128),(short)rnd.nextInt(256-128), rnd.nextInt(), rnd.nextLong(), rnd.nextFloat(), rnd.nextDouble(), ""+rnd.nextGaussian());
	}
	
	
//	@Test
	public void myorm() throws Exception {
		Unsafe unsafe = null;
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = (Unsafe)f.get(null);
		} catch (Exception e) { 
			throw new RuntimeException("Cannot get access to sun.misc.Unsafe!",e);
		}
		
		Object[] a = new Object[8];
		OrmInfo info = Orm.getInfo(TestOrm.class);
		for (int i=0; i<obj.length; i++) {

			long[] offset = info.getPointerOffsets();
			a[0] = unsafe.getBoolean(obj[i], offset[0]);
			a[1] = unsafe.getByte(obj[i], offset[1]);
			a[2] = unsafe.getShort(obj[i], offset[2]);
			a[3] = unsafe.getInt(obj[i], offset[3]);
			a[4] = unsafe.getLong(obj[i], offset[4]);
			a[5] = unsafe.getFloat(obj[i], offset[5]);
			a[6] = unsafe.getDouble(obj[i], offset[6]);
			a[7] = unsafe.getObject(obj[i], offset[7]);
			
			TestOrm test = Orm.create(TestOrm.class);
			if (test instanceof OrmObject)
				((OrmObject)test).preOrmAction();
			
			unsafe.putBoolean(test, offset[0], (boolean) a[0]);
			unsafe.putByte(test, offset[1], (byte) a[1]);
			unsafe.putShort(test, offset[2], (short) a[2]);
			unsafe.putInt(test, offset[3], (int) a[3]);
			unsafe.putLong(test, offset[4], (long) a[4]);
			unsafe.putFloat(test, offset[5], (float) a[5]);
			unsafe.putDouble(test, offset[6], (double) a[6]);
			unsafe.putObject(test, offset[7], a[7]);
			
			if (test instanceof OrmObject)
				((OrmObject)test).postOrmAction();
			
			assertEquals(test.f1, obj[i].f1);
			assertEquals(test.f2, obj[i].f2);
			assertEquals(test.f3, obj[i].f3);
			assertEquals(test.f4, obj[i].f4);
			assertEquals(test.f6, obj[i].f6,0);
			assertEquals(test.f7, obj[i].f7,0);
			assertEquals(test.f8, obj[i].f8);
			assertEquals(test.f5, obj[i].f5+obj[i].f3,0);
		}
	}
	
//	@Test
	public void orm() throws Exception {
		Object[] a = new Object[8];
		for (int i=0; i<obj.length; i++) {
			a = Orm.toArray(obj[i],a);
			TestOrm test = Orm.fromArray(a, TestOrm.class);
			assertEquals(test.f1, obj[i].f1);
			assertEquals(test.f2, obj[i].f2);
			assertEquals(test.f3, obj[i].f3);
			assertEquals(test.f4, obj[i].f4);
			assertEquals(test.f6, obj[i].f6,0);
			assertEquals(test.f7, obj[i].f7,0);
			assertEquals(test.f8, obj[i].f8);
			assertEquals(test.f5, obj[i].f5+obj[i].f3,0);
		}
	}

//	@Test
	public void direct() throws Exception {
		Object[] a = new Object[8];
		for (int i=0; i<obj.length; i++) {
			a[0] = obj[i].f1;
			a[1] = obj[i].f2;
			a[2] = obj[i].f3;
			a[3] = obj[i].f4;
			a[4] = obj[i].f5;
			a[5] = obj[i].f6;
			a[6] = obj[i].f7;
			a[7] = obj[i].f8;
			
			TestOrm test = new TestOrm((Boolean)a[0], (Byte)a[1], (Short)a[2], (Integer)a[3], (Long)a[4], (Float)a[5], (Double)a[6], (String)a[7]);
			test.postOrmAction();
			assertEquals(test.f1, obj[i].f1);
			assertEquals(test.f2, obj[i].f2);
			assertEquals(test.f3, obj[i].f3);
			assertEquals(test.f4, obj[i].f4);
			assertEquals(test.f6, obj[i].f6,0);
			assertEquals(test.f7, obj[i].f7,0);
			assertEquals(test.f8, obj[i].f8);
			assertEquals(test.f5, obj[i].f5+obj[i].f3);
		}
	}

	
	@Test
	public void table() throws Exception {
		Tables tabs = Tables.getInstance();
		TableMetaInformation<TestOrm> meta = tabs.buildMeta("junit", TestOrm.class);
		Table<TestOrm> table = tabs.createOrOpen(TableType.Gedi, meta);
		table.delete();
		
		table.beginAddBatch();
		for (int i=0; i<200; i++)
			table.add(obj[i]);
		table.endAddBatch();
		
		Iterator<Object> it = table.where("f6>0.5").page(0, 100,100).select("gediID, f6+f7 AS sum").orderBy("sum").iterate();
		boolean first = true;
		while (it.hasNext()) {
			Object o = it.next();
			if (first) {
				System.out.println(Arrays.toString(Orm.getOrmFieldNames(o.getClass())));
				first = false;
			}
			System.out.println(Arrays.toString(Orm.toArray(o, null)));
		}
		
		assertEquals(obj.length, table.size());
	}
	
	
	public static class TestOrm implements OrmObject {
		@OrmField boolean f1;
		@OrmField byte f2;
		@OrmField short f3;
		@OrmField int f4;
		@OrmField long f5;
		@OrmField float f6;
		@OrmField double f7;
		@OrmField String f8;

		public TestOrm(boolean f1, byte f2, short f3, int f4, long f5,
				float f6, double f7, String f8) {
			super();
			this.f1 = f1;
			this.f2 = f2;
			this.f3 = f3;
			this.f4 = f4;
			this.f5 = f5;
			this.f6 = f6;
			this.f7 = f7;
			this.f8 = f8;
		}



		@Override
		public void postOrmAction() {
			f5 = f5+f3;
		}
		
	}
	
}
