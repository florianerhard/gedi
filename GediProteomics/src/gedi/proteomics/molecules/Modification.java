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



public class Modification implements Comparable<Modification> {
	
	public enum ModificationPosition {
		anywhere,proteinNterm,proteinCterm,anyNterm,anyCterm
	}
	
	private int index;
	private String title;
	private String description;
	private String composition;
	private ModificationPosition position;
	private boolean[] sites;
	private char[] s;
	
	public Modification(int index, String title, String description,
			String composition, ModificationPosition position, char[] sites) {
		this.index = index;
		this.title = title;
		this.description = description;
		this.composition = composition;
		this.position = position;
		this.s = sites;
		this.sites = new boolean[256];
		for (char site : sites)
			if (site=='-') {
				switch (position) {
				case anywhere: throw new RuntimeException("- not allowed for anywhere modifications!");
				case anyCterm: case proteinCterm:this.sites['$']=true;break;
				case anyNterm: case proteinNterm:this.sites['^']=true;break;
				}
			} else
				this.sites[site] = true;
	}

	public ModificationPosition getPosition() {
		return position;
	}
	
	public char[] getSites() {
		return s;
	}
	public boolean isValidSite(char AA, boolean proteinNterm, boolean proteinCterm) {
		if (position==ModificationPosition.proteinNterm)
			return proteinNterm && sites[AA];
		if (position==ModificationPosition.proteinCterm)
			return proteinCterm && sites[AA];
		return sites[AA];
	}
	
	public int getIndex() {
		return index;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getComposition() {
		return composition;
	}

	@Override
	public int compareTo(Modification o) {
		return index-o.index;
	}
	
	
	@Override
	public int hashCode() {
		return index;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof Modification && ((Modification)obj).index==index;
	}
	
	@Override
	public String toString() {
		return title;
	}
	
}
