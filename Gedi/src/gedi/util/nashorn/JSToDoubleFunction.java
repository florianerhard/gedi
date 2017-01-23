package gedi.util.nashorn;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JSToDoubleFunction<I> implements ToDoubleFunction<I> {

	private ScriptObjectMirror p;
	
	public JSToDoubleFunction(String code) throws ScriptException {
		p = new JS().execSource(code);
		
	}
	
	
	@Override
	public double applyAsDouble(I t) {
		return (double) p.call(null, t);
	}

}
