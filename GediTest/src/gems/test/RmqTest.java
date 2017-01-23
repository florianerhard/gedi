package gems.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.reference.Chromosome;
import gedi.util.algorithm.rmq.DiskMinMaxSumIndex;
import gedi.util.algorithm.rmq.DoubleDiskSuccinctRmaxq;
import gedi.util.algorithm.rmq.SuccinctRmaxq;
import gedi.util.algorithm.rmq.SuccinctRminq;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileView;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.math.stat.RandomNumbers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RmqTest {
	
	@Test
	public void fileTest() throws IOException {
		DiskGenomicNumericBuilder builder = new DiskGenomicNumericBuilder("data/file.rmq");
		builder.setReferenceSorted(true);
		for (int i=0; i<100; i++) {
			builder.addValue(Chromosome.obtain("chr"+i), 500, 59.0);
			System.out.println(i);
		}
		builder.build();
		
	}
	

//	@Test
	public void anotherSmallTest() throws IOException {
		int[] a = new int[34];
		for (int i=0; i<a.length; i++) {
			a[i] = i;
		}
		
		PageFileWriter out = new PageFileWriter("data/rmq/small.rmq");
		DiskMinMaxSumIndex.create(out, NumericArray.wrap(a),true,true,true);
		out.close();
		
		PageFile in = out.read(true);
		
		DiskMinMaxSumIndex rmq = new DiskMinMaxSumIndex(in); 
		System.out.println(rmq.getMaxIndex(0, 4));
		
	}
	
	
	public void smallTest() throws IOException {
		double[] a = new double[10000];
		for (int i=0; i<a.length; i++) {
			a[i] = i;
		}
		
		PageFileWriter out = new PageFileWriter("data/rmq/small.rmq");
		for (int i=0; i<100000; i++)
			out.putByte(4);
		DoubleDiskSuccinctRmaxq.create(out, a);
		for (int i=0; i<100000; i++)
			out.putByte(4);
		out.close();
		
		PageFile in = out.read(true);
		PageFileView view = new PageFileView(in,100000,in.size());
		
		DoubleDiskSuccinctRmaxq rmq = new DoubleDiskSuccinctRmaxq(view); 
		System.out.println(rmq.query(0, 4));
		
	}
	
	@Test
	public void rmqTest() throws IOException {
		int[] a = new int[10_000_000];
		double[] a1 = new double[10_000_000];
		RandomNumbers rnd = new RandomNumbers();
		for (int i=0; i<a.length; i++)
			a1[i] = a[i] = rnd.getUnif(0, Integer.MAX_VALUE/(a.length+1));
		SuccinctRminq rmq1 = new SuccinctRminq(a);
		SuccinctRmaxq rmq2 = new SuccinctRmaxq(a);
		
		
		PageFileWriter out = new PageFileWriter("data/rmq/test.rmq");
		DiskMinMaxSumIndex.create(out, NumericArray.wrap(a), true,true,true);
		out.close();
		
		PageFileWriter out2 = new PageFileWriter("data/rmq/test2.rmq");
		DiskMinMaxSumIndex.create(out2, NumericArray.wrap(a1), true,true,true);
		out2.close();
		
		
		DiskMinMaxSumIndex rmq = new DiskMinMaxSumIndex("data/rmq/test.rmq"); 
		DiskMinMaxSumIndex rmqd = new DiskMinMaxSumIndex("data/rmq/test2.rmq");
		long s = System.currentTimeMillis();
		rmq.loadToMemory();
		System.out.println("Load after "+(System.currentTimeMillis()-s)+"ms");
		rmqd.loadToMemory();
		System.out.println("Load after "+(System.currentTimeMillis()-s)+"ms");
		
		for (int c=0; c<1000; c++) {
			int i=rnd.getUnif(0, a.length);
			int j=rnd.getUnif(0, a.length);
			if (i>j) {int t = i; i=j; j=t;}
			
			int ind1 = rmq1.query(i, j);
			int ind2 = rmq2.query(i, j);
			int min = rmq.getMinIndex(i, j);
			int max = rmq.getMaxIndex(i, j);
			
			int mind = rmqd.getMinIndex(i, j);
			int maxd = rmqd.getMaxIndex(i, j);
//			int nind = i;
//			for (int t=i; t<j; t++)
//				if (a[t]>a[nind]) nind=t;
			
			assertEquals(a[ind1], a[min]);
			assertEquals(ind1, min);
			assertEquals(a[ind2], a[max]);
			assertEquals(ind2, max);
			
			assertEquals(a[ind1], a[mind]);
			assertEquals(ind1, mind);
			assertEquals(a[ind2], a[maxd]);
			assertEquals(ind2, maxd);
			
		}
	}
	
}
