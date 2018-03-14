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

public class AlignedReadsMismatch implements AlignedReadsVariation {

	private int position;
	private CharSequence genomic;
	private CharSequence read;
	private boolean isFromSecondRead;
	
	public AlignedReadsMismatch(int position, CharSequence genomic, CharSequence read, boolean isFromSecondRead) {
		this.position = position;
		this.genomic = genomic;
		this.read = read;
		this.isFromSecondRead = isFromSecondRead;
	}

	@Override
	public int getPosition() {
		return position;
	}

	@Override
	public boolean isMismatch() {
		return true;
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
	public boolean isFromSecondRead() {
		return isFromSecondRead;
	}


	@Override
	public boolean isSoftclip() {
		return false;
	}
	
	
	@Override
	public CharSequence getReferenceSequence() {
		return genomic;
	}

	@Override
	public CharSequence getReadSequence() {
		return read;
	}
	
	@Override
	public String toString() {
		return "M"+position+genomic+read+(isFromSecondRead?"r":"");
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
		AlignedReadsMismatch other = (AlignedReadsMismatch) obj;
		if (genomic == null) {
			if (other.genomic != null)
				return false;
		} else if (!genomic.equals(other.genomic))
			return false;
		if (isFromSecondRead != other.isFromSecondRead)
			return false;
		if (position != other.position)
			return false;
		if (read == null) {
			if (other.read != null)
				return false;
		} else if (!read.equals(other.read))
			return false;
		return true;
	}

	
	
}
