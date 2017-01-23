package gedi.core.data.reads;

import gedi.util.StringUtils;

public class AlignedReadsDeletion implements AlignedReadsVariation{
	
	private int position;
	private CharSequence sequence;
	
	public AlignedReadsDeletion(int position, CharSequence sequence) {
		this.position = position;
		this.sequence = sequence;
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
		return true;
	}

	@Override
	public boolean isInsertion() {
		return false;
	}

	@Override
	public boolean isSoftclip() {
		return false;
	}
	
	
	@Override
	public CharSequence getReferenceSequence() {
		return sequence;
	}

	@Override
	public CharSequence getReadSequence() {
		return "";
	}
	
	@Override
	public String toString() {
		return "D"+position+sequence;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AlignedReadsDeletion other = (AlignedReadsDeletion) obj;
		if (position != other.position)
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


}
