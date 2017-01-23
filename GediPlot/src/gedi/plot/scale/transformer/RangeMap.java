package gedi.plot.scale.transformer;

import java.util.Arrays;
import java.util.function.ToDoubleFunction;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.dataframe.DataColumn;
import jdistlib.disttest.NormalityTest;

/**
 * Contains a mapping from an array of objects to ranges between 0 and 1 such that (1) ranges are pairwise disjoint and (2) their union is [0,1].
 * @author flo
 *
 * @param <T>
 */
public class RangeMap<T> {

	private T[] obj;
	private Range[] ranges;

	/**
	 * Equispaced
	 * @param obj
	 */
	public RangeMap(T[] obj) {
		this.obj = obj;
		ranges = new Range[obj.length];
		double size = 1.0/ranges.length;
		for (int i=0; i<ranges.length; i++)
			ranges[i] = new Range(i*size,(i+1)*size);
	}

	public RangeMap(T[] obj, double[] weights) {
		this.obj = obj;
		ranges = new Range[obj.length];
		double last = 0;
		for (int i=0; i<ranges.length; i++) {
			ranges[i] = new Range(last,last+weights[i]);
			last = ranges[i].getMax();
		}
		for (int i=0; i<ranges.length; i++) 
			ranges[i] = new Range(ranges[i].getMin()/last,ranges[i].getMax()/last);
	}


	public RangeMap(T[] obj, ToDoubleFunction<T> weights) {
		this.obj = obj;
		ranges = new Range[obj.length];
		double last = 0;
		for (int i=0; i<ranges.length; i++) {
			ranges[i] = new Range(last,last+weights.applyAsDouble(obj[i]));
			last = ranges[i].getMax();
		}
		for (int i=0; i<ranges.length; i++) 
			ranges[i] = new Range(ranges[i].getMin()/last,ranges[i].getMax()/last);
	}

	public int count() {
		return obj.length;
	}

	
	public Range getRange(T o) {
		for (int i=0; i<obj.length; i++)
			if (obj[i].equals(o))
				return ranges[i];
		return null;
	}
	
	public Range getRange(int index) {
		return ranges[index];
	}
	
	public T getObject(int index) {
		return obj[index];
	}
	
	public T getObject(double val) {
		int low = 0;
		int high = obj.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			Range midVal = ranges[mid];
			int cmp = midVal.compareTo(val);

		if (cmp < 0)
			low = mid + 1;
		else if (cmp > 0)
			high = mid - 1;
		else
			return obj[mid]; // key found
		}
		return null;  // key not found.
	}

	



}
