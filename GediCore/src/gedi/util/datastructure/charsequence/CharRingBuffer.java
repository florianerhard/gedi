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

public class CharRingBuffer {

	protected char[] b;
	protected int next;
	
	public CharRingBuffer(CharRingBuffer p) {
		this.b = p.b.clone();
		this.next = p.next;
	}
	
	public CharRingBuffer(int len) {
		this.b = new char[len];
	}
	
	public CharRingBuffer clone() {
		CharRingBuffer re = new CharRingBuffer(b.length);
		System.arraycopy(b, 0, re.b, 0, b.length);
		re.next = next;
		return re;
	}
	
	public CharRingBuffer add(String s) {
		for (int i=0; i<s.length(); i++)
			add(s.charAt(i));
		return this;
	}
	
	public CharRingBuffer add(char c) {
		b[next++] = c;
		if (next==b.length) next=0;
		return this;
	}
	
	
	public int capacity() {
		return b.length;
	}
	
	public char getLast() {
		int l = next-1;
		if (l<0) l=b.length+l;
		return b[l];
	}
	
	public CharSequence getLast(int n) {
		char[] r = new char[n];
		int s = Math.max(0, next-n);
		System.arraycopy(b, s, r, n-next+s, next-s);
		if (next<n)
			System.arraycopy(b, b.length-n+next, r, 0, n-next);
		return new CharArrayCharSequence(r);
	}
	
	
	@Override
	public String toString() {
		return getLast(b.length).toString();
	}
	
	public CharRingBuffer resize(int len) {
		String c = toString();
		next = 0;
		b = new char[len];
		add(c);
		return this;
	}

	public int compareTo(CharRingBuffer buff) {
		if (buff.b.length!=b.length) return toString().compareTo(buff.toString());
		int tp = next-1;
		int tb = buff.next-1;
		for (int i=0; i<b.length; i++) {
			if (tp<0) tp = b.length-1;
			if (tb<0) tb = buff.b.length-1;
			int r = Character.compare(b[tp], buff.b[tb]);
			if (r!=0) return r;
			tp--;
			tb--;
		}
		return 0;
	}
	
	
	
}
