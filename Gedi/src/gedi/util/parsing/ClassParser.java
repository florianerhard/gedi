package gedi.util.parsing;

import gedi.app.classpath.ClassPathCache;

public class ClassParser implements Parser<Class<?>> {
	@Override
	public Class<?> apply(String s) {
		return ClassPathCache.getInstance().getClass(s);
	}

	@Override
	public Class<Class<?>> getParsedType() {
		return (Class)Class.class;
	}
}