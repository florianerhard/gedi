package gedi.util.math.stat.regression;

import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.datastructure.tree.redblacktree.OrderStatisticTree;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;


public class MedianRegression implements BinarySerializable {

	
	private double[] x;
	private double[] reg;
	
	/**
	 * x and y are changed!
	 * @param x
	 * @param y
	 * @param offset
	 * @param length
	 * @param win
	 */
	public MedianRegression(double[] x, double[] y, double win) {
		x = x.clone();
		y = y.clone();
		
		OrderStatisticTree<Double> order = new OrderStatisticTree<Double>(true);
		
		reg = new double[x.length];
		ArrayUtils.parallelSort(x,y);
		int l = 0;
		int r = 0;
		for (int c=0; c<x.length; c++) {
			
			for (; x[l]<x[c]-win; l++) 
				order.remove(y[l]);
			for (;r<x.length && x[r]<x[c]+win; r++)
				order.add(y[r]);
			double median = median(order);
			reg[c] = median;
		}
		this.x = x;
	}
	
	private double median(OrderStatisticTree<Double> order) {
		int n = order.size()/2;
		if ((order.size()&1)==1) // odd
			return order.getRanked(n+1);
		// even
		return (order.getRanked(n)+order.getRanked(n+1))*0.5;
	}
	
	public double predict(double x) {
		int i = Arrays.binarySearch(this.x,0,reg.length, x);
		if (i>=0)return reg[i];
		if (i==-1) return reg[0];
		if (i==-reg.length-1) return reg[reg.length-1];
		return (reg[-i-2]+reg[-i-1])*0.5;
	}

	public double[] getKnownX() {
		return x;
	}
	
	public double[] getKnownValue() {
		return reg;
	}
	
	public MedianRegression(PageFile in) throws IOException {
		deserialize(in);
	}
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putInt(0);
		FileUtils.writeDoubleArray(out, x);
		FileUtils.writeDoubleArray(out, reg);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		in.getInt();
		x = FileUtils.readDoubleArray(in);
		reg = FileUtils.readDoubleArray(in);
	}

	
}
