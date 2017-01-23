package gedi.util.datastructure.dataframe;

import gedi.util.ReflectionUtils;
import gedi.util.datastructure.collections.bitcollections.BitList;
import gedi.util.datastructure.collections.doublecollections.DoubleCollection;
import gedi.util.datastructure.collections.intcollections.IntCollection;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.math.stat.factor.Factor;

import java.lang.reflect.Array;
import java.util.Collection;

public interface DataColumn<A> extends BinarySerializable {

	String name();
	int size();
	
	boolean getBooleanValue(int row);
	int getIntValue(int row);
	double getDoubleValue(int row);
	Factor getFactorValue(int row);
	
	void setBooleanValue(int row, boolean val);
	void setIntValue(int row, int val);
	void setDoubleValue(int row, double val);
	void setFactorValue(int row, Factor val);
	
	void copyValueTo(int fromIndex, DataColumn to, int toIndex);
	
	boolean isInteger();
	boolean isDouble();
	boolean isFactor();
	boolean isBoolean();
	
	A getRaw();
	DataColumn<A> newInstance(String name, int length);
	String toString(int row);
	
	
	
	public static <T> DataColumn<T> fromCollection(String name, Collection<T> list) {
		if (list==null || list.isEmpty()) throw new IllegalArgumentException("No list given!");
		
		if (list instanceof BitList)
			return (DataColumn<T>) new BooleanDataColumn(name,((BitList)list).toBitVector());
		if (list instanceof IntCollection)
			return (DataColumn<T>) new IntegerDataColumn(name,((IntCollection)list).toIntArray());
		if (list instanceof DoubleCollection)
			return (DataColumn<T>) new DoubleDataColumn(name,((DoubleCollection)list).toDoubleArray());
		
		Object first = list.iterator().next();
		
		if (first instanceof Number) {
			boolean isint = true;
			for (T e : list)
				if (!(e instanceof Integer)) {
					isint = false;
					break;
				}
			if (isint) {
				int[] a = new int[list.size()];
				int ind = 0;
				for (T e : list)
					a[ind++] = ((Number)e).intValue();
				return (DataColumn<T>) new IntegerDataColumn(name,a);
			} else {
				double[] a = new double[list.size()];
				int ind = 0;
				for (T e : list)
					a[ind++] = ((Number)e).doubleValue();
				return (DataColumn<T>) new DoubleDataColumn(name,a);
			}
		}
		
		if (first instanceof Factor) {
			return (DataColumn<T>) new FactorDataColumn(name, list.toArray(new Factor[0]));
		}
		throw new RuntimeException("Cannot put a "+first.getClass().getName()+" into a data frame!");
		
	}
	
}
