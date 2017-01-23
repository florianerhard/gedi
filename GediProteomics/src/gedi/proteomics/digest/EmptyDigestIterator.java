package gedi.proteomics.digest;

public class EmptyDigestIterator implements DigestIterator{

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public String next() {
		return null;
	}

	@Override
	public void remove() {
	}

	@Override
	public int getStartPosition() {
		return -1;
	}
	
	@Override
	public int getMissed() {
		return -1;
	}

	@Override
	public int getEndPosition() {
		return -1;
	}

}
