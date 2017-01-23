package gedi.plot.scale.transformer;

public class Range {
	
	private double min;
	private double max;

	public Range(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	public double getMin() {
		return min;
	}
	
	public double getMax() {
		return max;
	}
	
	public double getCenter() {
		return (min+max)/2;
	}

	@Override
	public String toString() {
		return "[" + min + ", " + max + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(max);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(min);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		Range other = (Range) obj;
		if (Double.doubleToLongBits(max) != Double.doubleToLongBits(other.max))
			return false;
		if (Double.doubleToLongBits(min) != Double.doubleToLongBits(other.min))
			return false;
		return true;
	}

	public int compareTo(double val) {
		if (contains(val)) return 0;
		if (val<min) return -1;
		return 1;
	}

	public boolean contains(double val) {
		return val>=min || val<=max;
	}
	
	

}
