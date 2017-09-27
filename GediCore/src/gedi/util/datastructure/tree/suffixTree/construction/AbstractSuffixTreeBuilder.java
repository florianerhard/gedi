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

package gedi.util.datastructure.tree.suffixTree.construction;

import java.util.Arrays;

import gedi.util.StringUtils;
import gedi.util.datastructure.tree.suffixTree.tree.GeneralizedSuffixTreeIndex;
import gedi.util.sequence.Alphabet;

public abstract class AbstractSuffixTreeBuilder implements SuffixTreeBuilder, GeneralizedSuffixTreeIndex {

	private boolean alphabetExplicit;
	private Alphabet alphabet;
	private char[] generalizedAlphabet;
	private boolean check;
	private boolean generalizeToString = true;
	
	private CharSequence[] sequences;
	private int[] startPositions;
	
	public AbstractSuffixTreeBuilder() {}
	
	public AbstractSuffixTreeBuilder(Alphabet alphabet, boolean check) {
		this.alphabet = alphabet;
		this.check = check;
		alphabetExplicit = alphabet!=null;
	}
	
	protected char[] getGeneralizedAlphabet() {
		return generalizedAlphabet;
	}
	
	protected CharSequence buildGeneralizedInput(CharSequence[] input) {
		this.sequences = input;
		if (!alphabetExplicit)
			alphabet = Alphabet.buildAlphabet(StringUtils.concatSequences(input));
		
//		char[] term = alphabet.getNonAlphabetCharactersEx(input.length);
//		generalizedAlphabet = new char[alphabet.size()+term.length];
//		System.arraycopy(alphabet.toCharArray(), 0, generalizedAlphabet, 0, alphabet.size());
//		System.arraycopy(term, 0, generalizedAlphabet, alphabet.size(), term.length);
//		Arrays.sort(generalizedAlphabet);
		char[] term = alphabet.getNonAlphabetCharacters(1);
		generalizedAlphabet = new char[alphabet.size()+term.length];
		System.arraycopy(alphabet.toCharArray(), 0, generalizedAlphabet, 0, alphabet.size());
		System.arraycopy(term, 0, generalizedAlphabet, alphabet.size(), term.length);
		Arrays.sort(generalizedAlphabet);
		
		CharSequence[] termSeq = new CharSequence[input.length];
		for (int i=0; i<input.length; i++)
			termSeq[i] = new String(new char[] {term[0]});
		
		CharSequence[] a = new CharSequence[termSeq.length+input.length];
		for (int i=0; i<input.length; i++) {
			a[i*2] = input[i];
			a[i*2+1] = termSeq[i];
		}
		CharSequence re = StringUtils.concatSequences(a);
		if (generalizeToString)
			re = re.toString();
		
		startPositions = new int[sequences.length];
		for (int i=1; i<sequences.length; i++)
			startPositions[i] = startPositions[i-1]+sequences[i-1].length()+1; 
		
		return re;
	}

	protected void check(CharSequence s) {
		if (check && !alphabet.isValid(s))
			throw new RuntimeException("Invalid characters in s.");
	}
	
	public Alphabet getAlphabet() {
		return alphabet;
	}

	public int getSequenceIndexForGeneralizedPosition(int pos) {
		return indexForPos(pos);
	}
	
	public CharSequence getSequenceForGeneralizedPosition(int pos) {
		int ins = indexForPos(pos);
		return sequences[ins];
	}
	
	public int getSequencePositionForGeneralizedPosition(int pos) {
		int ins = indexForPos(pos);
		return pos-startPositions[ins];
	}

	@Override
	public int getNumSequences() {
		return sequences.length;
	}
	
	private int indexForPos(int pos) {
		int ins = Arrays.binarySearch(startPositions, pos);
		if (ins<0) ins=-ins-2;
		return ins;
	}
	
}
