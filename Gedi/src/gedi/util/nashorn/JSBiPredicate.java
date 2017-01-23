package gedi.util.nashorn;

import gedi.util.mutable.MutableTuple;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JSBiPredicate<T,S> implements BiPredicate<T,S> {

	private ScriptObjectMirror p;
	private boolean useFirstAsThis;
	
	public JSBiPredicate(boolean useFirstAsThis, String code) throws ScriptException {
		this.useFirstAsThis = useFirstAsThis;
		p = new JS().execSource(code);
		
	}
	
	
	@Override
	public boolean test(T t, S s) {
		Object o1 = t;
		Object o2 = s;
		if (o1 instanceof MutableTuple)
			o1 = ((MutableTuple)o1).getArray();
		if (o2 instanceof MutableTuple)
			o2 = ((MutableTuple)o2).getArray();
		
		if (useFirstAsThis)
			return (boolean) p.call(o1, o2);
		return (boolean) p.call(null, o1, o2);
	}

}
