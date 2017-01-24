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

package gedi.proteomics.molecules;

import gedi.util.mutable.MutableInteger;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * Two polymers are equal if their sequences are equal and they have the same amount of modifications (not position specific!)
 * @author erhard
 *
 */
public class Polymer extends AbstractList<Monomer> implements Comparable<Polymer> {

	private Monomer[] monomer;
	
	public Polymer(Monomer[] monomer) {
		this.monomer = monomer;
	}

	@Override
	public Monomer get(int index) {
		return monomer[index];
	}

	@Override
	public int size() {
		return monomer.length;
	}
	
	@Override
	public int hashCode() {
        int result = 1;

        for (Monomer element : monomer) 
            result = 31 * result + (element.getSingleLetter() == null ? 0 : element.getSingleLetter().hashCode());

        return result;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Polymer)) return false;
		Polymer p = (Polymer) o;
		if (p.monomer.length!=monomer.length) return false;
		for (int i=0; i<monomer.length; i++) {
			if (!monomer[i].getSingleLetter().equals(p.monomer[i].getSingleLetter()))
				return false;
		}
		boolean tm = hasModification();
		boolean om = p.hasModification();
		if ((tm && !om)||(!tm&&om)) return false;
		if (tm && om) {
			HashMap<Modification, MutableInteger> bag = new HashMap<Modification, MutableInteger>();
			for (Monomer aa : monomer)
				if (aa.getModification()!=null) {
					MutableInteger mi = bag.get(aa.getModification());
					if (mi==null) bag.put(aa.getModification(), mi = new MutableInteger(0));
					mi.N++;
				}
			for (Monomer aa : p.monomer)
				if (aa.getModification()!=null) {
					MutableInteger mi = bag.get(aa.getModification());
					if (mi==null) return false;
					mi.N--;
					if (mi.N==0)
						bag.remove(aa.getModification());
				}
			if (bag.size()>0) return false;
		}
		return true;
	}
	
	public Modification[] getModification() {
		ArrayList<Modification> re = new ArrayList<Modification>();
		for (Monomer m : monomer)
			if (m.getModification()!=null)
				re.add(m.getModification());
		Collections.sort(re);
		return re.toArray(new Modification[0]);
	}
	
	public boolean hasModification() {
		for (Monomer aa : monomer)
			if (aa.getModification()!=null) return true;
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Monomer aa : monomer)
			sb.append(aa.toString());
		return sb.toString();
	}
	
	public String toModificationLessString() {
		StringBuilder sb = new StringBuilder();
		for (Monomer aa : monomer)
			if (aa.isAminoAcid())
				sb.append(aa.getSingleLetter());
		return sb.toString();
	}

	@Override
	public int compareTo(Polymer o) {
		int n = Math.min(o.monomer.length,monomer.length);
		int re = 0;
		for (int i=0; i<n; i++) {
			re = monomer[i].compareTo(o.monomer[i]);
			if (re!=0) return re;
		}
		return monomer.length-o.monomer.length;
	}
	
	public Polymer createUnmodified() {
		Monomer[] m = new Monomer[monomer.length];
		for (int i=0; i<m.length; i++)
			m[i]=monomer[i].getUnmodified();
		return new Polymer(m);
	}

	
	public static boolean isPeptide(String seq) {
		int i=0; int n = seq.length();
		if (Monomer.isNTerminus(seq.substring(0,1))) i++;
		if (Monomer.isCTerminus(seq.substring(seq.length()-1))) n--;
		for (; i<n; i++)
			if (!Monomer.isAminoAcid(seq.substring(i,i+1))) return false;
		return true;
	}

	public int count(String l) {
		int re = 0;
		for (Monomer m : monomer)
			if (m.getSingleLetter().equals(l))
				re++;
		return re;
	}

	
}
