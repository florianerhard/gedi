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
package gedi.util.datastructure.charsequence;

import java.util.Iterator;

import gedi.util.ArrayUtils;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.charsequence.CharDag.CharDagVisitor;
import gedi.util.datastructure.charsequence.CharDag.SequenceVariant;

public class TranslateCharDagVisitor<R> implements CharDagVisitor<R> {

	private char[] codon = new char[3];
	private int p;
	private CharDagVisitor<R> parent;
	private boolean revert;
	
	public TranslateCharDagVisitor(CharDagVisitor<R> parent, boolean revert) {
		this.parent = parent;
		this.revert = revert;
	}

	@Override
	public int compareTo(CharDagVisitor<R> o1) {
		TranslateCharDagVisitor<R> o = (TranslateCharDagVisitor<R>)o1;
		int re = Integer.compare(p, o.p);
		if (re==0)
			re = ArrayUtils.compare(codon, o.codon);
		if (re==0)
			re = parent.compareTo(o.parent);
		return re;
	}

	@Override
	public Iterator<R> accept(char c, int pos) {
		codon[p++] = c;
		if (p==3) {
			p=0;
			String aa = SequenceUtils.code.get(revert?new CharIterator.ReverseArrayIterator(codon):new CharIterator.ArrayIterator(codon));
			if (aa==null) return parent.accept('X', pos);
			return parent.accept(aa.charAt(0), pos);
		}
		return null;
	}
	
	@Override
	public void shift(int pos, int shift, SequenceVariant variant) {
		parent.shift(pos,shift,variant);
	}

	@Override
	public CharDagVisitor<R> branch() {
		TranslateCharDagVisitor<R> re = new TranslateCharDagVisitor<>(parent.branch(),revert);
		re.codon = codon.clone();
		re.p = p;
		return re;
	}

	@Override
	public void prune(int pos) {
		parent.prune(pos-p);
	}

	@Override
	public Iterator<SequenceVariant> getVariants() {
		return parent.getVariants();
	}

}
