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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

public class CharDag {

	
	
	private CharIterator text;
	
	private SequenceVariant[] variants;
	
	
	public CharDag(CharIterator text, ExtendedIterator<SequenceVariant> variants) {
		this.text = text;
		this.variants = variants.toArray(SequenceVariant.class);
		Arrays.sort(this.variants);
	}
	
	public SequenceVariant[] getVariants() {
		return variants;
	}
	
	
	public <R> void traverse(CharDagVisitor<R> v, BiConsumer<R,Iterator<SequenceVariant>> re) {
		traverse(i->i==0?v:null, re);
	}
	
	public <R> void traverse(IntFunction<CharDagVisitor<R>> v, BiConsumer<R,Iterator<SequenceVariant>> re) {
		
		int nextVariantIndex = 0;
		int pos = -1;
		ArrayList<CharDagVisitor<R>> vs = new ArrayList<CharDagVisitor<R>>();
		TreeMap<Integer, ArrayList<CharDagVisitor<R>>> cont = new TreeMap<>();
		
		while (text.hasNext()) {
			pos++;
			
			CharDagVisitor<R> vvv = v.apply(pos);
			if (vvv!=null)
				vs.add(vvv);
			
			while (nextVariantIndex<variants.length && variants[nextVariantIndex].pos==pos) {
				
				ArrayList<CharDagVisitor<R>> bvs = new ArrayList<CharDagVisitor<R>>();
				for (CharDagVisitor<R> vv : vs) 
					bvs.add(vv.branch());
			
				if (bvs.size()>1) 
					Collections.sort(bvs);

				for (CharDagVisitor<R> sv : bvs) 
					sv.shift(pos,-variants[nextVariantIndex].from.length, variants[nextVariantIndex]);
				
				for (char c : variants[nextVariantIndex].to) {
					
					int index = 0;
					bvs.get(0).shift(pos,1, variants[nextVariantIndex]);
					Iterator<R> r = bvs.get(0).accept(c,pos);
					accept(re,r, bvs.get(0));
					for (int i=1; i<bvs.size(); i++) {
						bvs.get(i).shift(pos,1, variants[nextVariantIndex]);
						r = bvs.get(i).accept(c,pos);
						if (bvs.get(i).compareTo(bvs.get(index))!=0) {
							index++;
							accept(re,r, bvs.get(i));					
						}
						bvs.set(index, bvs.get(i));
					}
					while (bvs.size()!=index+1) {
						bvs.remove(bvs.size()-1);
					}
				}
				
				
				int conti = pos+variants[nextVariantIndex].from.length;
				ArrayList<CharDagVisitor<R>> cl = cont.get(conti);
				if (cl==null) cont.put(conti, bvs);
				else cl.addAll(bvs);
				nextVariantIndex++;
			}
			
			ArrayList<CharDagVisitor<R>> cl = cont.remove(pos);
			if (cl!=null) 
				vs.addAll(cl);
			
			if (vs.size()>1) 
				Collections.sort(vs);
			
			char c = text.nextChar();
			
			int index = 0;
			Iterator<R> r = vs.get(0).accept(c,pos);
			accept(re, r, vs.get(0));					
			for (int i=1; i<vs.size(); i++) {
				r = vs.get(i).accept(c,pos);
				if (vs.get(i).compareTo(vs.get(index))!=0) {
					index++;
					accept(re, r, vs.get(i));
				}
				vs.set(index, vs.get(i));
			}
			while (vs.size()!=index+1)
				vs.remove(vs.size()-1);
		
			for (CharDagVisitor<R> vv : vs)
				vv.prune(pos);
			
		}
		
	}
	
	
	
	private <R> void accept(BiConsumer<R,Iterator<SequenceVariant>> re, Iterator<R> r, CharDagVisitor<R> v) {
		if (r!=null) {
			while (r.hasNext())
				re.accept(r.next(),v.getVariants());					
		}
				
	}



	public static interface CharDagVisitor<R> extends Comparable<CharDagVisitor<R>> {
		Iterator<R> accept(char c, int pos);
		Iterator<SequenceVariant> getVariants();
		CharDagVisitor<R> branch();
		void shift(int pos, int shift, SequenceVariant variant);
		void prune(int pos);
	}
	
	public static class PosLengthVariant implements Comparable<PosLengthVariant> {
		private int pos;
		private int len;
		private SequenceVariant variant;
		public PosLengthVariant(int pos, int len, SequenceVariant variant) {
			super();
			this.pos = pos;
			this.len = len;
			this.variant = variant;
		}
		public int getPos() {
			return pos;
		}
		public int getLen() {
			return len;
		}
		public SequenceVariant getVariant() {
			return variant;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + len;
			result = prime * result + pos;
			result = prime * result + ((variant == null) ? 0 : variant.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PosLengthVariant other = (PosLengthVariant) obj;
			if (len != other.len)
				return false;
			if (pos != other.pos)
				return false;
			if (variant == null) {
				if (other.variant != null)
					return false;
			} else if (!variant.equals(other.variant))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return "[" + pos + ", " + len + ", " + variant + "]";
		}
		public void incrementLen(int shift) {
			this.len+=shift;
		}
		@Override
		public int compareTo(PosLengthVariant o) {
			if (o.pos==pos) return Integer.compare(hashCode(), o.hashCode());
			return Integer.compare(pos, o.pos);
		}

		
	}
	
	public static class KmerCharDagVisitor implements CharDagVisitor<CharSequence> {

		private CharRingBuffer buff;
		private int n = 0;
		private LinkedList<PosLengthVariant> correction = new LinkedList<PosLengthVariant>();
		
		private KmerCharDagVisitor() {
		}
			
		public KmerCharDagVisitor(int k) {
			this.buff = new CharRingBuffer(k);
		}

		@Override
		public Iterator<CharSequence> accept(char c, int pos) {
			this.buff.add(c);
			if (++n>=this.buff.capacity())
				return EI.singleton(this.buff.toString()+"@"+pos+", "+StringUtils.toString(correction));
			return null;
		}
		
		@Override
		public int compareTo(CharDagVisitor<CharSequence> o) {
			return buff.compareTo(((KmerCharDagVisitor)o).buff);
		}

		@Override
		public CharDagVisitor<CharSequence> branch() {
			KmerCharDagVisitor re = new KmerCharDagVisitor();
			re.buff = new CharRingBuffer(buff);
			re.n = n;
			re.correction.addAll(correction);
			return re;
		}

		@Override
		public void shift(int pos, int shift, SequenceVariant variant) {
			if (correction.size()>0 && correction.getLast().getPos()==pos && correction.getLast().getVariant().equals(variant)) {
				correction.getLast().incrementLen(shift);
				if (correction.getLast().getLen()==0) correction.removeLast();
			} else
				correction.add(new PosLengthVariant(pos, shift,variant));
		}

		@Override
		public void prune(int pos) {
			Iterator<PosLengthVariant> it = correction.iterator();
			while (it.hasNext() && it.next().getPos()<pos-buff.capacity())
				it.remove();			
		}

		@Override
		public Iterator<SequenceVariant> getVariants() {
			return EI.wrap(correction).map(plv->plv.getVariant());
		}
		
	}
	
	public static class SequenceVariant implements Comparable<SequenceVariant> {
		int pos;
		char[] from;
		char[] to;
		String label;
		
		public SequenceVariant(int pos, char[] from, char[] to, String label) {
			this.pos = pos;
			this.from = from;
			this.to = to;
			this.label = label;
		}

		@Override
		public int compareTo(SequenceVariant o) {
			return Integer.compare(pos, o.pos);
		}

		public int end() {
			return pos+from.length;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(from);
			result = prime * result + pos;
			result = prime * result + Arrays.hashCode(to);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SequenceVariant other = (SequenceVariant) obj;
			if (!Arrays.equals(from, other.from))
				return false;
			if (pos != other.pos)
				return false;
			if (!Arrays.equals(to, other.to))
				return false;
			return true;
		}

		@Override
		public String toString() {
			if (label!=null)
				return label;
			return pos+" "+String.valueOf(from)+">"+String.valueOf(to);
		}
		
		
		
	}
	
	
}
