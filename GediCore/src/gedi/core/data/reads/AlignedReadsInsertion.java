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

public class AlignedReadsInsertion implements AlignedReadsVariation {
	
	
	private int position;
	private CharSequence sequence;
	private boolean isFromSecondRead;
	
	public AlignedReadsInsertion(int position, CharSequence sequence, boolean isFromSecondRead) {
		this.position = position;
		this.sequence = sequence;
		this.isFromSecondRead = isFromSecondRead;
	}

	@Override
	public int getPosition() {
		return position;
	}

	@Override
	public boolean isFromSecondRead() {
		return isFromSecondRead;
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
		return true;
	}
	

	@Override
	public boolean isSoftclip() {
		return false;
	}

	@Override
	public CharSequence getReferenceSequence() {
		return "";
	}

	@Override
	public CharSequence getReadSequence() {
		return sequence;
	}
	
	@Override
	public String toString() {
		return "I"+position+sequence+(isFromSecondRead?"r":"");
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AlignedReadsInsertion other = (AlignedReadsInsertion) obj;
		if (position != other.position)
			return false;
		if (isFromSecondRead != other.isFromSecondRead)
			return false;
		if (sequence == null) {
			if (other.sequence != null)
				return false;
		} else if (!StringUtils.charsEqual(sequence,other.sequence))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return hashCode2();
	}

	@Override
	public AlignedReadsInsertion reposition(int newPos) {
		return new AlignedReadsInsertion(newPos, sequence, isFromSecondRead);
	}
}
