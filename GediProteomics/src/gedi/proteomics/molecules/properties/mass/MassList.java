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
package gedi.proteomics.molecules.properties.mass;

import java.util.AbstractList;
import java.util.Arrays;


public class MassList extends AbstractList<Mass> implements Comparable<MassList> {

	Mass[] masses;
	long mass;
	
	public MassList(long mass) {
		this.masses = new Mass[0];
		this.mass = mass;
	}
	public MassList(Mass[] masses) {
		this.masses = masses;
		Arrays.sort(masses);
		mass = 0L;
		for (Mass m : masses)
			mass+=m.getMass();
	}

	@Override
	public Mass get(int index) {
		return masses[index];
	}

	@Override
	public int size() {
		return masses.length;
	}

	public boolean containsAll(MassList c) {
		if (masses.length<c.masses.length) return false;
		int i=0; int j=0;
		while (i<masses.length && j<c.masses.length) {
			int comp = masses[i].compareTo(c.masses[j]);
			if (comp<=0) 
				i++;
			if (comp>=0)
				j++;
		}
		return i>=c.masses.length && j==c.masses.length;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(masses);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MassList))
			return false;
		return Arrays.equals(masses, ((MassList)o).masses);
	}
	
	public long getMass() {
		return mass;
	}

//	public long getPeptideMass(MassFactory f, int charge) {
//		long re = (2+charge)*f.H.getMass()+f.O.getMass();
//		re+=mass;
//		return re;
//	}
//	
//	public long getYMass(MassFactory f, int charge) {
//		long re = (2+charge)*f.H.getMass()+f.O.getMass();
//		re+=mass;
//		return re;
//	}
//	
//	public long getBMass(MassFactory f, int charge) {
//		long re = charge*f.H.getMass();
//		re+=mass;
//		return re;
//	}
//	
//	public long getInternalMass(MassFactory f, int charge) {
//		long re = (1+charge)*f.H.getMass();
//		re+=mass;
//		return re;
//	}

	@Override
	public int compareTo(MassList o) {
		int re = (mass<o.mass ? -1 : (mass==o.mass? 0 : 1));
		if (re!=0) return re;
		
		int n = Math.min(masses.length, o.masses.length);
	    for (int k=0; k<n; k++) {
	    	int c = masses[k].compareTo(o.masses[k]);
	    	if (c!=0) return c;
	    }
		return masses.length-o.masses.length;
	}
	
}
