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

package gedi.util.parsing;

import gedi.app.classpath.ClassPathCache;

public class ClassParser implements Parser<Class<?>> {
	@Override
	public Class<?> apply(String s) {
		return ClassPathCache.getInstance().getClass(s);
	}

	@Override
	public Class<Class<?>> getParsedType() {
		return (Class)Class.class;
	}
}