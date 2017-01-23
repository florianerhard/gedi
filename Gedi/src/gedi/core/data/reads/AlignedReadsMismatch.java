package gedi.core.data.reads;

import gedi.util.StringUtils;

public class AlignedReadsMismatch implements AlignedReadsVariation {

	private int position;
	private CharSequence genomic;
	private CharSequence read;
	
	public AlignedReadsMismatch(int position, CharSequence genomic, CharSequence read) {
		this.position = position;
		this.genomic = genomic;
		this.read = read;
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
		return "M"+position+genomic+read;
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
		} else if (!StringUtils.charsEqual(genomic,other.genomic))
			return false;
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
