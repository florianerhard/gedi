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
package gedi.core.data.table;

import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;

public class GediNamingPolicy implements NamingPolicy {

	private String name;
	
	public GediNamingPolicy(String name) {
		this.name = name;
	}

	@Override
	public String getClassName(String prefix, String source, Object key,
			Predicate names) {
		if (prefix == null) {
            prefix = "gedi.generated";
        } else if (prefix.startsWith("java")) {
            prefix = "$" + prefix;
        }
        String base =
            prefix + "."+name+"$$" + 
            Integer.toHexString(key.hashCode());
        String attempt = base;
        int index = 2;
        while (names.evaluate(attempt))
            attempt = base + "_" + index++;
        return attempt;
	}

}
