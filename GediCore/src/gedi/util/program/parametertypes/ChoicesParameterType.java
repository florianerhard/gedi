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
package gedi.util.program.parametertypes;


import java.util.Map;

import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;

public class ChoicesParameterType<E> implements GediParameterType<E> {

	private Map<String,E> choices;
	private Class<E> cls;
	
	public ChoicesParameterType(Map<String,E> choices, Class<E> cls) {
		this.choices = choices;
		this.cls = cls;
	}

	@Override
	public E parse(String s) {
		E re = ParseUtils.parseChoicesByPrefix(s, true, choices);
		if (re==null) throw new RuntimeException("Could not parse "+s+" (Available: "+EI.wrap(choices.keySet()).concat(",")+")");
		return re;
	}

	@Override
	public Class<E> getType() {
		return cls;
	}
	
	@Override
	public E getDefaultValue() {
		return choices.get(choices.keySet().iterator().next());
	}
	
	@Override
	public boolean parsesMulti() {
		return false;
	}
	
	@Override
	public String helpText() {
		return "Choices are : "+EI.wrap(choices.keySet()).concat(",");
	}

}
