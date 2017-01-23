package gedi.plot.scale.transformer;

import java.util.HashMap;

import gedi.util.ArrayUtils;

public interface DiscreteInvertibleTransformer<T> {

	T transform(int v);
	int inverse(T v);
	
	public static <T> DiscreteInvertibleTransformer<T> array(T[] a) {
		HashMap<T, Integer> index = ArrayUtils.createIndexMap(a);
		return new DiscreteInvertibleTransformer<T>() {
			@Override
			public T transform(int v) {
				return a[v];
			}

			@Override
			public int inverse(T v) {
				return index.get(v);
			}
		};
	};

	

}
