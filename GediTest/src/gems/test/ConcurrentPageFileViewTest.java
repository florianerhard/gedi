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
import gedi.util.ReflectionUtils;
import gedi.util.io.randomaccess.ConcurrentPageFile;
import gedi.util.io.randomaccess.ConcurrentPageFileView;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.math.stat.RandomNumbers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConcurrentPageFileViewTest {

	@BeforeClass
	public static void setPageSize() {
		try {
			ReflectionUtils.setStatic(PageFile.class, "pageSize", 1024*434);
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("Cannot set page size!",e);
		}
	}
	
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
		out.setPageSize(1024*143);
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
	public void concurrentByteArrayTest() throws IOException, ClassNotFoundException {
		
		RandomNumbers rnd = new RandomNumbers();
		byte[] a = new byte[10_000_000];
		for (int i=0; i<a.length; i++) {
			a[i] = (byte)rnd.getUnif(Byte.MIN_VALUE, Byte.MAX_VALUE);
		}
		
		PageFileWriter out = new PageFileWriter("data/pagefile.bin");
		out.put(a, 0, a.length);
		ConcurrentPageFile in = out.readConcurrently(true);
		in.setUnmap(false);
		int R = 1_000_000;
		
		AtomicInteger tc = new AtomicInteger();
		
		class ConcurrentReader extends Thread {
			RandomNumbers trnd = new RandomNumbers();
			@Override
			public void run() {
				try {
					tc.incrementAndGet();
					ConcurrentPageFileView view = new ConcurrentPageFileView(in);
					for (int i=0; i<R; i++) {
						int index = trnd.getUnif(0,a.length);
						assertEquals(a[index],view.get(index));
					}
					tc.decrementAndGet();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
			
		for (int i=0; i<10; i++)
			new ConcurrentReader().start();
		while (tc.get()>0)
			Thread.yield();
			
		in.close();

		new File(in.getPath()).delete();
		
	}
	
	
	
}
