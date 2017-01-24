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

package gedi.core.reference;

import java.util.HashSet;

import gedi.util.StringUtils;



public interface ReferenceSequence extends Comparable<ReferenceSequence> {

	String getName();
	Strand getStrand();
	
	/**
	 * Strips the leading chr, if present; e.g. used by Ensembl
	 * @return
	 */
	default String getChrStrippedName()  {
		String name = getName();
		if (name.startsWith("chr")) return name.substring(3);
		return name;
	}
	
	ReferenceSequence toChrStrippedReference();
	
	default ReferenceSequence toStrandIndependent() {
		return Chromosome.obtain(getName());
	}
	
	default ReferenceSequence toPlusStrand() {
		return Chromosome.obtain(getName(),Strand.Plus);
	}
	default ReferenceSequence toMinusStrand() {
		return Chromosome.obtain(getName(),Strand.Minus);
	}
	
	default ReferenceSequence toStrand(Strand strand) {
		if (strand==null) return this;
		return Chromosome.obtain(getName(),strand);
	}
	
	default ReferenceSequence toStrand(boolean strand) {
		return Chromosome.obtain(getName(),strand);
	}
	
	default ReferenceSequence toOppositeStrand() {
		if (getStrand()==Strand.Independent) return this;
		if (getStrand()==Strand.Plus) return toMinusStrand();
		return toPlusStrand();
	}
	
	default int compareTo(ReferenceSequence o) {
		String a = getName();
		String b = o.getName();
		int re = compareChromosomeNames(a,b);
		if (re==0)
			re = getStrand().ordinal()-o.getStrand().ordinal();
		return re;
	}
	
	
	public static int compareChromosomeNames(String a, String b) {
		if (a.equals(b)) return 0;
		String ca = a;
		String cb = b;
		if (a.startsWith("chr")) ca = a.substring(3);
		if (b.startsWith("chr")) cb = b.substring(3);
		if (ca.equals(cb)) return 0;
		
		boolean an = StringUtils.isInt(ca);
		boolean bn = StringUtils.isInt(cb);
		
		if (an&&bn) return Integer.compare(Integer.parseInt(ca), Integer.parseInt(cb));
		return a.compareTo(b);
	}

	
	default String toPlusMinusString() {
		return getName()+getStrand().toString();
	}
	
	default ReferenceSequence correctName(HashSet<String> availableNames) {
		if (availableNames.contains(getName())) return this;
		if (getName().startsWith("chr")) {
			String nn = getName().substring(3);
			if (availableNames.contains(nn)) return Chromosome.obtain(nn,getStrand());
		}
		if (availableNames.contains("chr"+getName())) return Chromosome.obtain("chr"+getName(),getStrand());
		return null;
	}
	default boolean isStandard() {
		int digits = 0;
		int max = 0;
		for (int i=0; i<getName().length(); i++) {
			if (Character.isDigit(getName().charAt(i)))
				digits++;
			else
				digits = 0;
			max = Math.max(max, digits);
		}
		return max<4;
			
	}
	
}
