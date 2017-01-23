package gedi.util.nashorn;

import gedi.util.functions.TriFunction;
import gedi.util.mutable.MutableTuple;

import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JSTriFunction<T1,T2,T3,O> implements TriFunction<T1,T2,T3,O> {

	private ScriptObjectMirror p;
	private boolean useFirstAsThis;
	
	public JSTriFunction(boolean useFirstAsThis, String code) throws ScriptException {
		this.useFirstAsThis = useFirstAsThis;
		p = new JS().execSource(code);
	}
	
	
	@Override
	public O apply(T1 t1,T2 t2,T3 t3) {
		Object o1 = t1;
		Object o2 = t2;
		Object o3 = t3;
		if (o1 instanceof MutableTuple)
			o1 = ((MutableTuple)o1).getArray();
		if (o2 instanceof MutableTuple)
			o2 = ((MutableTuple)o2).getArray();
		if (o3 instanceof MutableTuple)
			o3 = ((MutableTuple)o3).getArray();
		
		if (useFirstAsThis)
			return (O) p.call(o1, o2, o3);
		return (O) p.call(null, o1, o2, o3);
	}

}
