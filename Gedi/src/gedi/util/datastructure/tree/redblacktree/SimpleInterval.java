package gedi.util.datastructure.tree.redblacktree;

public class SimpleInterval implements Interval{

	private int start;
	private int stop;
	
	public SimpleInterval(int start, int stop) {
		this.start = start;
		this.stop = stop;
	}

	@Override
	public int getStart() {
		return start;
	}

	@Override
	public int getStop() {
		return stop;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + start;
		result = prime * result + stop;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleInterval other = (SimpleInterval) obj;
		if (start != other.start)
			return false;
		if (stop != other.stop)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + start + ", " + stop + "]";
	}

	
	

}
