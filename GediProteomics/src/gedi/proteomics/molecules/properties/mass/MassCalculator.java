package gedi.proteomics.molecules.properties.mass;

import gedi.proteomics.molecules.Modification;
import gedi.proteomics.molecules.Monomer;
import gedi.proteomics.molecules.Polymer;
import gedi.proteomics.molecules.properties.PropertyCalculator;

import java.util.HashMap;



public class MassCalculator implements PropertyCalculator {

	private MassFactory mf = MassFactory.getInstance();
	private HashMap<Modification,Long> modMasses = new HashMap<Modification, Long>();
	
	public double calculate(Polymer pep) {
		long mass = 0L;
		mass+=mf.getMassByShortName("Nterm").getMass();
		for (Monomer m : pep) {
			if (m.getModification()!=null) {
				Long mm = modMasses.get(m.getModification());
				if (mm==null) modMasses.put(m.getModification(), mm = mf.getMass(m.getModification().getComposition()));
				mass+=mm;
			}
			if (m.isAminoAcid())
				mass+=mf.getMassByShortName(m.getSingleLetter()).getMass();
		}
		mass+=mf.getMassByShortName("Cterm").getMass();
		return mf.getMass(mass);
	}
	
	
}
