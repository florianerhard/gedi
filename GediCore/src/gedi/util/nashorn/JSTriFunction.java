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
