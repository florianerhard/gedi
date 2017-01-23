package gedi.util.parsing;

import java.util.function.Function;

public interface Parser<T> extends Function<String,T> {

	public default boolean canParse(String s) {
		try {
			if (apply(s)==null) return false;
			return true;
		} catch(Throwable e) {
			return false;
		}
	}

	Class<T> getParsedType();

}
