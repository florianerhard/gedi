package gedi.proteomics.molecules.properties.mass;

public class Mass implements Comparable<Mass> {

	private long mass;
	private String shortName;
	private String name;
	
	Mass(long mass, String shortName, String name) {
		this.mass = mass;
		this.shortName = shortName;
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Mass)) return false;
		Mass m = (Mass) obj;
		return compareTo(m)==0;
	}
	
	@Override
	public int hashCode() {
		return (int)(mass ^ (mass >>> 32));
	}
	
	@Override
	public String toString() {
		return shortName;
	}
	
	@Override
	public int compareTo(Mass o) {
		return (mass<o.mass ? -1 : (mass==o.mass? 0 : 1));
	}
	
	public String getShortName() {
		return shortName;
	}
	
	public String getName() {
		return name;
	}
	
	public long getMass() {
		return mass;
	}

}
