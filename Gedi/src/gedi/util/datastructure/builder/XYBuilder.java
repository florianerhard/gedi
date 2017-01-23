package gedi.util.datastructure.builder;

import java.io.IOException;
import java.io.RandomAccessFile;

import gedi.util.FileUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

public class XYBuilder implements BinarySerializable {

	private DoubleArrayList x;
	private DoubleArrayList y;
	
	public XYBuilder() {
		this(1000);
	}
	
	public XYBuilder(int initialCapacity) {
		this.x = new DoubleArrayList(initialCapacity);
		this.y = new DoubleArrayList(initialCapacity);
	}
	
	public void add(double x, double y) {
		this.x.add(x);
		this.y.add(y);
	}
	
	public int size() {
		return x.size();
	}
	
	public double getX(int index) {
		return x.getDouble(index);
	}
	
	public double getY(int index) {
		return y.getDouble(index);
	}
	
	public double[] getAllX() {
		return x.toDoubleArray();
	}
	
	public double[] getAllY() {
		return y.toDoubleArray();
	}

	public XYBuilder(PageFile in) throws IOException {
		deserialize(in);
	}
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		FileUtils.writeDoubleArrayList(out, x);
		FileUtils.writeDoubleArrayList(out, y);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		x = FileUtils.readDoubleArrayList(in);
		y = FileUtils.readDoubleArrayList(in);
	}

	
}
