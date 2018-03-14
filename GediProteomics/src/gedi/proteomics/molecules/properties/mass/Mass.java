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

public class Mass implements Comparable<Mass> {

	private long mass;
	private String shortName;
	private String name;
	
	Mass(long mass, String shortName, String name) {
		this.mass = mass;
		this.shortName = shortName;
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Mass)) return false;
		Mass m = (Mass) obj;
		return compareTo(m)==0;
	}
	
	@Override
	public int hashCode() {
		return (int)(mass ^ (mass >>> 32));
	}
	
	@Override
	public String toString() {
		return shortName;
	}
	
	@Override
	public int compareTo(Mass o) {
		return (mass<o.mass ? -1 : (mass==o.mass? 0 : 1));
	}
	
	public String getShortName() {
		return shortName;
	}
	
	public String getName() {
		return name;
	}
	
	public long getMass() {
		return mass;
	}

}
