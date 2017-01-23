package gedi.virology.hsv1annotation.kinetics.quantifier;

import gedi.virology.hsv1annotation.kinetics.provider.KineticRegionProvider;

import java.util.Arrays;
import java.util.List;

public class QuantifierSet {

	private KineticRegionProvider regionProvider;
	private String label;
	private String[] conditions;
	private List<KineticQuantifier> quantifiers;
	public QuantifierSet(KineticRegionProvider regionProvider, String label,
			String[] conditions, List<KineticQuantifier> quantifiers) {
		super();
		this.regionProvider = regionProvider;
		this.label = label;
		this.conditions = conditions;
		this.quantifiers = quantifiers;
	}
	public KineticRegionProvider getRegionProvider() {
		return regionProvider;
	}
	public String getLabel() {
		return label;
	}
	public String[] getConditions() {
		return conditions;
	}
	public List<KineticQuantifier> getQuantifiers() {
		return quantifiers;
	}
	@Override
	public String toString() {
		return "QuantifierSet [regionProvider=" + regionProvider + ", label="
				+ label + ", conditions=" + Arrays.toString(conditions)
				+ ", quantifiers=" + quantifiers + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(conditions);
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((quantifiers == null) ? 0 : quantifiers.hashCode());
		result = prime * result
				+ ((regionProvider == null) ? 0 : regionProvider.hashCode());
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
		QuantifierSet other = (QuantifierSet) obj;
		if (!Arrays.equals(conditions, other.conditions))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (quantifiers == null) {
			if (other.quantifiers != null)
				return false;
		} else if (!quantifiers.equals(other.quantifiers))
			return false;
		if (regionProvider == null) {
			if (other.regionProvider != null)
				return false;
		} else if (!regionProvider.equals(other.regionProvider))
			return false;
		return true;
	}
	
	
	
}
