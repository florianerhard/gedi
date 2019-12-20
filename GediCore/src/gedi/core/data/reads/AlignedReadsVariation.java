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
package gedi.core.data.reads;

import gedi.util.StringUtils;


public interface AlignedReadsVariation extends Comparable<AlignedReadsVariation> {
	
	
	int getPosition();
	
	boolean isFromSecondRead();
	
	boolean isMismatch();
	
	boolean isDeletion();
	
	boolean isInsertion();
	
	boolean isSoftclip();
	
	
	CharSequence getReferenceSequence();
	CharSequence getReadSequence();
	
	AlignedReadsVariation reposition(int newPos);
	
	default int compareTo(AlignedReadsVariation o) {
		int re = getPosition()-o.getPosition();
		if (re!=0) return re;
		if (isMismatch()!=o.isMismatch()) return isMismatch()?-1:1;
		if (isDeletion()!=o.isDeletion()) return isDeletion()?-1:1;
		if (isInsertion()!=o.isInsertion()) return isInsertion()?-1:1;
		if (isSoftclip()!=o.isSoftclip()) return isSoftclip()?-1:1;
		if (isFromSecondRead()!=o.isFromSecondRead()) return isFromSecondRead()?1:-1;
		re = getReadSequence().toString().compareTo(o.getReadSequence().toString());
		if (re!=0) return re;
		return getReadSequence().toString().compareTo(o.getReadSequence().toString());
	}
	
	default int hashCode2() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getPosition();
		if (isMismatch())
			result = prime * result + 37;
		if (isDeletion())
			result = prime * result + 87;
		if (isInsertion())
			result = prime * result + 113;
		if (isSoftclip())
			result = prime * result + 191;
		if (isFromSecondRead())
			result = prime * result + 223;
		result = prime * result + getReadSequence().hashCode();
		result = prime * result + getReferenceSequence().hashCode();
		return result;
	}

	
	/**
	 * As produced by {@link #toString()}
	 * @param s
	 * @return
	 */
	public static AlignedReadsVariation fromString(String s) {
		String rest = s.substring(1);
		if (rest.startsWith("p"))
			rest = s.substring(1);
		
		int n = StringUtils.countPrefixInt(rest);
		if (n==0) throw new IllegalArgumentException("No pos found in "+s);
		
		int pos = Integer.parseInt(rest.substring(0, n));
		rest = rest.substring(n);
		boolean second = rest.endsWith("r");
		if (second)
			rest = rest.substring(0,rest.length()-1);
		
		if (s.startsWith("M"))
			return new AlignedReadsMismatch(pos, rest.substring(0,1), rest.substring(1, 2),second);
		
		if (s.startsWith("I"))
			return new AlignedReadsInsertion(pos, rest,second);
		
		if (s.startsWith("D"))
			return new AlignedReadsDeletion(pos, rest,second);
		
		if (s.startsWith("5"))
			return new AlignedReadsSoftclip(true, rest,second);
		
		if (s.startsWith("3"))
			return new AlignedReadsSoftclip(false, rest,second);
		
		throw new IllegalArgumentException("Must start with M/I/D: "+s);
	}
	
}
