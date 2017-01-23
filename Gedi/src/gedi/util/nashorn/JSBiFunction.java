package gedi.util.nashorn;

import gedi.util.mutable.MutableTuple;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JSBiFunction<I,T,O> implements BiFunction<I,T,O> {

	private ScriptObjectMirror p;
	private boolean useFirstAsThis;
	
	public JSBiFunction(boolean useFirstAsThis, String code) throws ScriptException {
		this.useFirstAsThis = useFirstAsThis;
		p = new JS().execSource(code);
	}
	
	
	@Override
	public O apply(I t, T t2) {
		Object o1 = t;
		Object o2 = t2;
		if (o1 instanceof MutableTuple)
			o1 = ((MutableTuple)o1).getArray();
		if (o2 instanceof MutableTuple)
			o2 = ((MutableTuple)o2).getArray();
		
		if (useFirstAsThis)
			return (O) p.call(o1, o2);
		return (O) p.call(null, o1, o2);
	}

}
