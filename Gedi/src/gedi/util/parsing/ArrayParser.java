package gedi.util.parsing;

import java.lang.reflect.Array;

import gedi.util.StringUtils;

public class ArrayParser<T> implements Parser<T[]> {
	
	private Parser<T> one;
	
	public ArrayParser(Parser<T> one) {
		this.one = one;
	}

	@Override
	public T[] apply(String s) {
		String[] f = StringUtils.split(s, ',');
		T[] re = (T[]) Array.newInstance(one.getParsedType(), f.length);
		for (int i=0; i<re.length; i++)
			re[i] = one.apply(f[i]);
		return re;
	}

	@Override
	public Class<T[]> getParsedType() {
		return (Class<T[]>) Array.newInstance(one.getParsedType(),0).getClass();
	}
}