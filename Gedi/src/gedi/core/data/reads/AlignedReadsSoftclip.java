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

	private int position;
	private CharSequence read;
	
	public AlignedReadsSoftclip(int position, CharSequence read) {
		this.position = position;
		this.read = read;
	}

	@Override
	public int getPosition() {
		return position;
	}

	@Override
	public boolean isMismatch() {
		return false;
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
		return (position==0?"5p":"3p")+read;
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
		if (position != other.position)
			return false;
		if (read == null) {
			if (other.read != null)
				return false;
		} else if (!StringUtils.charsEqual(read,other.read))
			return false;
		return true;
	}
	
	
}
