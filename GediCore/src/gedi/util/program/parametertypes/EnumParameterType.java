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


import gedi.util.ParseUtils;
import gedi.util.StringUtils;

public class EnumParameterType<E extends Enum<E>> implements GediParameterType<E> {

	private Class<E> enumClass;
	
	public EnumParameterType(Class<E> enumClass) {
		this.enumClass = enumClass;
	}

	@Override
	public E parse(String s) {
		E re = ParseUtils.parseEnumNameByPrefix(s, true, enumClass);
		if (re==null) throw new RuntimeException("Could not parse "+s+" as a "+enumClass.getName()+" (Available: "+ParseUtils.getEnumTrie(enumClass, true).keySet().toString()+")");
		return re;
	}

	@Override
	public Class<E> getType() {
		return enumClass;
	}
	
	@Override
	public E getDefaultValue() {
		return enumClass.getEnumConstants()[0];
	}
	
	@Override
	public boolean parsesMulti() {
		return false;
	}
	
	@Override
	public String helpText() {
		return "Choices are : "+StringUtils.concat(",", enumClass.getEnumConstants());
	}

}
