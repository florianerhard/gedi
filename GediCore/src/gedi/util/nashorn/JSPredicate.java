/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package gedi.util.nashorn;

import gedi.util.mutable.MutableTuple;

import java.io.IOException;
import java.util.function.Predicate;

import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class JSPredicate<T> implements Predicate<T> {


	private ScriptObjectMirror p;
	private boolean useAsThis;
	
	public JSPredicate(String code) throws ScriptException {
		this(false,code);
	}
	public JSPredicate(boolean useAsThis, String code) throws ScriptException {
		this.useAsThis = useAsThis;
		
		if (!code.startsWith("function(")) {
			StringBuilder c = new StringBuilder();
			c.append("function() {\n");
			if (code.contains(";")) {
				c.append(code);
				c.append("}");
			} else {
				c.append("return "+code+";\n}");
			}
			code = c.toString();
		}
		p = new JS().execSource(code);
	}
	
	
	@Override
	public boolean test(T t) {
		if (useAsThis)
			return (boolean) p.call(t);
		if (t instanceof MutableTuple)
			return (boolean) p.call(null, ((MutableTuple)t).getArray());
		return (boolean) p.call(null, t);
	}

	
}
