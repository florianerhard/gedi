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

package gedi.motif.splice;

import java.io.InputStream;
import java.util.HashMap;

import gedi.util.StringUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleIterator;
import gedi.util.functions.EI;
import gedi.util.io.text.LineIterator;
import gedi.util.sequence.Alphabet;

public class SpliceDonorMotif {

	
	private HashMap<CharSequence,Double> scoreMap = new HashMap<>();
	private double[] bgd = new double[256];
	private static double[] c1 = new double[256];
	private static double[] c2 = new double[256];
	
	static {
		c1['A'] = 0.004;
		c1['C'] = 0.0032;
		c1['G'] = 0.9896;
		c1['T'] = 0.0032;
		c2['A'] = 0.0034; 
		c2['C'] = 0.0039; 
		c2['G'] = 0.0042; 
		c2['T'] = 0.9884;
	}
	
	public SpliceDonorMotif() {
		this(SpliceDonorMotif.class.getResourceAsStream("/splice5sequences"),SpliceDonorMotif.class.getResourceAsStream("/me2x5"));
	}

	public SpliceDonorMotif(InputStream sequences, InputStream scores) {
		new LineIterator(sequences).fuse(String.class,new LineIterator(scores)).forEachRemaining(a->scoreMap.put(a[0], Double.parseDouble(a[1])));
		
		bgd['A'] = 0.27;
		bgd['C'] = 0.23;
		bgd['G'] = 0.23;
		bgd['T'] = 0.27; 
	}
	
	
	
	public double scoreSite(String seq) {
		if (seq.length()!=9) throw new IndexOutOfBoundsException("Must have length of 9!");
		
		seq = seq.toUpperCase();
		if (!Alphabet.getDna().isValid(seq)) return Double.NaN;
		
		double a = c1[seq.charAt(3)]*c2[seq.charAt(4)]/(bgd[seq.charAt(3)]*bgd[seq.charAt(4)]);
		double b = scoreMap.get(String.valueOf(new char[] {seq.charAt(0),seq.charAt(1),seq.charAt(2),seq.charAt(5),seq.charAt(6),seq.charAt(7),seq.charAt(8)}));
		
		return Math.log(a*b)/Math.log(2);
	}
	
	public DoubleIterator iterateSites(String seq) {
		if (seq.length()<9) return new DoubleIterator.EmptyDoubleIterator();
		return EI.substrings(seq, 9).mapToDouble(s->scoreSite(s));
	}
	
	public double[] scoreSites(String seq) {
		double[] re = new double[seq.length()-9];
		for (int i=0; i<re.length; i++)
			re[i] = scoreSite(seq.substring(i, i+9));
		return re;
	}
	
	
}
