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

package gedi.ensembl;

import gedi.core.reference.ReferenceSequence;
import gedi.util.StringUtils;

public class Ensembl {

	
	public static boolean isStandardChromosome(ReferenceSequence ref) {
		String s = ref.getName();
		if (s.startsWith("chr")) s=s.substring(3);
		else return false;
		return s.equals("M") || s.equals("X") || s.equals("Y") || StringUtils.isInt(s);
	}

	public static String correctChr(String s) {
		if (s.equals("MT") || s.equals("M")) return "chrM";
		if (s.equals("X") || s.equals("Y")) return "chr"+s;
		if(StringUtils.isInt(s)) return "chr"+s;
		return s;
	}
}
