package gems.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.math.stat.RandomNumbers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.nustaq.serialization.FSTConfiguration;

@RunWith(JUnit4.class)
public class PageFileTest {

	
	@Test
	public void primitiveTestNoBulk() throws IOException, ClassNotFoundException {
		byteArrayTest(false, false);
	}
	
	@Test
	public void primitiveTestBulkWrite() throws IOException, ClassNotFoundException {
		byteArrayTest(true, false);
	}
	
	@Test
	public void primitiveTestBulkRead() throws IOException, ClassNotFoundException {
		byteArrayTest(false, true);
	}
	
	@Test
	public void primitiveTestBulkBoth() throws IOException, ClassNotFoundException {
		byteArrayTest(true, true);
	}
	
	
	public void byteArrayTest(boolean bulkWrite, boolean bulkRead) throws IOException, ClassNotFoundException {
		
		RandomNumbers rnd = new RandomNumbers();
		byte[] a = new byte[10_000_000];
		for (int i=0; i<a.length; i++) {
			a[i] = (byte)rnd.getUnif(Byte.MIN_VALUE, Byte.MAX_VALUE);
		}
		
		PageFileWriter out = new PageFileWriter("data/pagefile.bin");
		out.putString("Test");
		out.putInt(a.length);
		if (bulkWrite)
			out.put(a, 0, a.length);
		else {
			for (int i=0; i<a.length; i++)
				out.put(a[i]);
		}
		
		PageFile in = out.read(true);
			
		String n = in.getString();
		byte[] r = new byte[in.getInt()];
		if (bulkRead)
			in.get(r, 0, r.length);
		else {
			for (int i=0; i<r.length; i++)
				r[i] = in.get();
		}
		
		assertEquals("Test", n);
		assertTrue(Arrays.equals(a, r));

		assertTrue(in.eof());
			
		in.close();

		new File(in.getPath()).delete();
		
	}
	
	@Test
	public void primitiveTest() throws IOException, ClassNotFoundException {
		
		double[] a = new RandomNumbers().getNormal(1_000_000).toDoubleArray();
		
		PageFileWriter out = new PageFileWriter("data/pagefile.bin");
		
		
		out.putString("Test");
		out.putInt(a.length);
		for (int i=0; i<a.length; i++)
			out.putDouble(a[i]);

		
		PageFile in = out.read(true);
			
		String n = in.getString();
		double[] r = new double[in.getInt()];
		for (int i=0; i<r.length; i++)
			r[i] = in.getDouble();
		
		assertEquals("Test", n);
		assertTrue(Arrays.equals(a, r));

		assertTrue(in.eof());
			
		in.close();
		new File(in.getPath()).delete();
		
	}
	
	
	@Test
	public void primitiveFstTest() throws IOException, ClassNotFoundException {
		
		double[] a = new RandomNumbers().getNormal(1_000_000).toDoubleArray();
		
		PageFileWriter out = new PageFileWriter("data/pagefile.bin");
		FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();		
		
		byte[] b = conf.asByteArray(a);
		
		out.putString("Test");
		out.putInt(b.length);
		out.put(b, 0, b.length);

		
		PageFile in = out.read(true);
			
		String n = in.getString();
		byte[] buff = new byte[in.getInt()];
		in.get(buff, 0, buff.length);
		
		double[] r = (double[])conf.getObjectInput(buff).readObject();
		
		assertEquals("Test", n);
		assertTrue(Arrays.equals(a, r));

		assertTrue(in.eof());
			
		in.close();
		new File(in.getPath()).delete();
		
	}
	
	

	@Test
	public void boxFstTest() throws IOException, ClassNotFoundException {
		
		Double[] a = new RandomNumbers().getNormal(1_000_000).toArray(Double.class);
		
		PageFileWriter out = new PageFileWriter("data/pagefile.bin");
		FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();		
		
		byte[] b = conf.asByteArray(a);
		
		out.putString("Test");
		out.putInt(b.length);
		out.put(b, 0, b.length);

		
		PageFile in = out.read(true);
			
		String n = in.getString();
		byte[] buff = new byte[in.getInt()];
		in.get(buff, 0, buff.length);
		
		Double[] r = (Double[])conf.getObjectInput(buff).readObject();
		
		assertEquals("Test", n);
		assertTrue(Arrays.equals(a, r));

		assertTrue(in.eof());
			
		in.close();
		new File(in.getPath()).delete();
		
	}
	
}
