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
