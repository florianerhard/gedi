package gedi.util.nashorn;

import gedi.util.mutable.MutableTuple;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JSFunction<I,O> implements Function<I,O> {

	private ScriptObjectMirror p;
	private boolean useAsThis;
	
	public JSFunction(String code) throws ScriptException {
		this(false,code);
	}
	public JSFunction(boolean useAsThis, String code) throws ScriptException {
		this.useAsThis = useAsThis;
		p = new JS().execSource(code);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public O apply(I t) {
		if (t instanceof MutableTuple)
			return (O) p.call(null, ((MutableTuple)t).getArray());
		if (useAsThis)
			return (O) p.call(t);
		return (O) p.call(null, t);
	}

}
