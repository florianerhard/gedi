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
package gedi.riboseq.inference.codon;

import gedi.util.StringUtils;

public enum CodonType {
	Start, Stop, NoncanonicalStart, Any;

	public static CodonType get(String codonTriplett) {
		if (codonTriplett==null || codonTriplett.length()!=3) return null;
		if (codonTriplett.equals("ATG"))
			return Start;
		if (codonTriplett.equals("TAA") || codonTriplett.equals("TGA") || codonTriplett.equals("TAG"))
			return Stop;
		if (StringUtils.hamming("ATG", codonTriplett)==1)
			return NoncanonicalStart;
		return Any;
	}
}
