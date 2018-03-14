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
package gedi.util.algorithm.string.alignment.pairwise;

public enum AlignmentMode {
	Global,Local,Freeshift,PrefixSuffix,BothPrefix;
	
	public static AlignmentMode fromString(String s) {
		if (s.toLowerCase().startsWith("g"))
			return Global;
		else if (s.toLowerCase().startsWith("l"))
			return Local;
		else if (s.toLowerCase().startsWith("f"))
			return Freeshift;
		else if (s.toLowerCase().startsWith("p"))
			return PrefixSuffix;
		else if (s.toLowerCase().startsWith("b"))
			return BothPrefix;
		else
			return null;
	}
}
