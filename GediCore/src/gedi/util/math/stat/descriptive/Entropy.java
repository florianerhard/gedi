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
package gedi.util.math.stat.descriptive;

import gedi.util.functions.EI;
import gedi.util.mutable.MutableInteger;

import java.util.HashMap;
import java.util.Iterator;

public class Entropy {

	
	public static double compute(String str) {
		return compute(EI.substrings(str,1));
	}
	public static double compute(String str,int s) {
		return compute(EI.substrings(str,s));
	}
	
	public static <I> double compute(Iterator<I> it) {
		double s = 0;
		HashMap<I,MutableInteger> c = new HashMap<I, MutableInteger>();
		while (it.hasNext()) {
			I n = it.next();
			MutableInteger d = c.get(n);
			if (d==null) c.put(n, d=new MutableInteger(0));
			d.N++;
			s++;
		}
		double re = 0;
		for (MutableInteger d : c.values()) 
			re+=d.N==0?0:(d.N/s*Math.log(d.N/s)/Math.log(2));
		return -re;
	}
	
	
	public static double maxEntropy(int alphabet) {
		return Math.log(1.0/alphabet)/Math.log(2);
	}
	
}
