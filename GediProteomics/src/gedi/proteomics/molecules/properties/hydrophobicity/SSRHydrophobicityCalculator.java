package gedi.proteomics.molecules.properties.hydrophobicity;

import gedi.proteomics.molecules.Polymer;
import gedi.proteomics.molecules.properties.PropertyCalculator;


public class SSRHydrophobicityCalculator implements PropertyCalculator {


	@Override
	public double calculate(Polymer pep) {
		return Hydrophobicity3.TSUM3(pep.toModificationLessString());
	}
}
