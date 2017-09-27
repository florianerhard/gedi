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

import java.io.File;

import gedi.util.StringUtils;
import gedi.util.parsing.StringParser;
import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;

public class FileParameterType implements GediParameterType<File> {

	@Override
	public File parse(String s) {
		return new File(s);
	}

	@Override
	public Class<File> getType() {
		return File.class;
	}

	public static File getFile(String name, GediParameterSet set) {
		return new File(StringUtils.replaceVariables(name, s->{
			GediParameter<Object> param = set.get(s);
			if (param==null)
				throw new RuntimeException("Parameter "+s+" unknown!");
			if (param.get()==null) return "${"+s+"}";
			return StringUtils.toString(param.get());
		}));
	}
	
}
