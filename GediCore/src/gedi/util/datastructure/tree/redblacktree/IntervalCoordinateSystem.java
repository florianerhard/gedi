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
package gedi.util.datastructure.tree.redblacktree;

import gedi.util.StringUtils;


public class IntervalCoordinateSystem {
	
	public static final IntervalCoordinateSystem BED = new IntervalCoordinateSystem(true, true, false);
	public static final IntervalCoordinateSystem GFF = new IntervalCoordinateSystem(false, true, true);
	public static final IntervalCoordinateSystem ENSEMBL = new IntervalCoordinateSystem(false, true, true);
	public static final IntervalCoordinateSystem JAVA = new IntervalCoordinateSystem(true, true, false);
	
	
	/**
	 * onebased,exclusive,exclusive
	 */
	int flags = 0; 
	
	public IntervalCoordinateSystem(boolean zerobased, boolean startInclusive, boolean endInclusive) {
		if (!zerobased)
			flags|=(1<<0);
		if (!startInclusive)
			flags|=(1<<1);
		if (!endInclusive)
			flags|=(1<<2);
	}
	
	public IntervalCoordinateSystem(String label) {
		if (label.toLowerCase().startsWith("ens"))
			label = "o-i-i";
		else if (label.toLowerCase().startsWith("gff"))
			label = "o-i-i";
		else if (label.toLowerCase().startsWith("bed"))
			label = "z-i-e";
		
		String[] n = StringUtils.split(label, '-');
		if (n[0].toLowerCase().startsWith("o"))
			flags|=(1<<0);
		if (n[1].toLowerCase().startsWith("e"))
			flags|=(1<<1);
		if (n[2].toLowerCase().startsWith("e"))
			flags|=(1<<2);
	}
	
	public int convertStartTo(int pos, IntervalCoordinateSystem to) {
		return convertStart(pos, flags, to.flags);
	}
	
	public int convertStartToDefault(int pos) {
		return convertStart(pos, flags, 4);
	}
	
	public int convertEndTo(int pos, IntervalCoordinateSystem to) {
		return convertEnd(pos, flags, to.flags);
	}
	
	public int convertEndToDefault(int pos) {
		return convertEnd(pos, flags, 4);
	}
	

	static int convertStart(int pos, int from, int to) {
		return pos+(int)Math.signum((to&1)-(from&1))+(int)Math.signum((from&2)-(to&2));
	}
	
	static int convertEnd(int pos, int from, int to) {
		return pos+(int)Math.signum((to&1)-(from&1))+(int)Math.signum((to&4)-(from&4));
	}
	
	@Override
	public String toString() {
		return String.format("%s-%s-%s", 
				(flags&1)==1?"onebased":"zerobased",
				(flags&2)==2?"exclusive":"inclusive",
				(flags&4)==4?"exclusive":"inclusive"
					);
						
	}
	
	@Override
	public int hashCode() {
		return flags;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof IntervalCoordinateSystem && ((IntervalCoordinateSystem)obj).flags==flags;
	}
	
}
