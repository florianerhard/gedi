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

public class AlignedReadsSoftclip implements AlignedReadsVariation {

	private boolean p5;
	private CharSequence read;
	private boolean isFromSecondRead;
	
	public AlignedReadsSoftclip(boolean p5, CharSequence read, boolean isFromSecondRead) {
		this.p5 = p5;
		this.read = read;
		this.isFromSecondRead = isFromSecondRead;
	}

	@Override
	public int getPosition() {
		return p5?0:Integer.MAX_VALUE;
	}

	@Override
	public boolean isMismatch() {
		return false;
	}

	@Override
	public boolean isFromSecondRead() {
		return isFromSecondRead;
	}
	
	@Override
	public boolean isDeletion() {
		return false;
	}

	@Override
	public boolean isInsertion() {
		return false;
	}


	@Override
	public boolean isSoftclip() {
		return true;
	}
	
	
	@Override
	public CharSequence getReferenceSequence() {
		return "";
	}

	@Override
	public CharSequence getReadSequence() {
		return read;
	}
	
	@Override
	public String toString() {
		return (p5?"5p":"3p")+read+(isFromSecondRead?"r":"");
	}

	@Override
	public int hashCode() {
		return hashCode2();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AlignedReadsSoftclip other = (AlignedReadsSoftclip) obj;
		if (p5 != other.p5)
			return false;
		if (isFromSecondRead != other.isFromSecondRead)
			return false;
		if (read == null) {
			if (other.read != null)
				return false;
		} else if (!StringUtils.charsEqual(read,other.read))
			return false;
		return true;
	}
	
	
}
