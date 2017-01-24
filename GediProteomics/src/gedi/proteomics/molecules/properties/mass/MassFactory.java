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

import java.util.HashMap;

public class MassFactory {


	private static MassFactory instance;
	public static synchronized MassFactory getInstance() {
		if (instance==null)
			instance = new MassFactory();
		return instance;
	}

	public final Mass H;
	public final Mass O;
	public final Mass N;
	public final Mass S;
	public final Mass C;
	public final Mass P;
	
	public final Mass H_avg;
	public final Mass O_avg;
	public final Mass N_avg;
	public final Mass S_avg;
	public final Mass C_avg;
	public final Mass P_avg;
	
	public final Mass Water;
	public final Mass Ammonia;

	private Mass[] atoms = new Mass[256];
	private Mass[] atoms_avg = new Mass[256];
	private long[] ionDiffs = new long[256];
	private long[] aaMass = new long[256];
	
	private int exp;

	private HashMap<String,Mass> shortToMass = new HashMap<String, Mass>();
	private HashMap<String,Mass> nameToMass = new HashMap<String, Mass>();

	private MassFactory() {

		// average masses:
		H_avg = new Mass(1007940000L,"Hx","Hydrogen");
		O_avg = new Mass(15999400000L,"Ox","Oxygen");
		N_avg = new Mass(14006700000L,"Nx","Nitrogen");
		S_avg = new Mass(32065000000L,"Sx","Sulfur");
		C_avg = new Mass(12010700000L,"Cx","Carbon");
		P_avg = new Mass(30973760000L,"Px","Phosphate");

		// monoisotopic masses:
		H = new Mass(1007825035L,"H","Hydrogen");
		O = new Mass(15994914630L,"O","Oxygen");
		N = new Mass(14003074000L,"N","Nitrogen");
		S = new Mass(31972070700L,"S","Sulfur");
		C = new Mass(12000000000L,"C","Carbon");
		P = new Mass(30973762000L,"P","Phosphate");
		exp = -9;


		atoms['H'] = H;
		atoms['O'] = O;
		atoms['N'] = N;
		atoms['S'] = S;
		atoms['C'] = C;
		atoms['P'] = P;
		
		atoms_avg['H'] = H_avg;
		atoms_avg['O'] = O_avg;
		atoms_avg['N'] = N_avg;
		atoms_avg['S'] = S_avg;
		atoms_avg['C'] = C_avg;
		atoms_avg['P'] = P_avg;

		Water = new Mass(getMass("H2O"),"H2O","Water");
		Ammonia = new Mass(getMass("NH3"),"NH3","Ammonia");
		addMass("C3H5NO","A","Alanine");
		addMass("C6H12N4O","R","Arginine");
		addMass("C4H6N2O2","N","Asparagine");
		addMass("C4H5NO3","D","Aspartic acid");
		addMass("C3H5NOS","C","Cysteine");
		addMass("C5H7NO3","E","Glutamic acid");
		addMass("C5H8N2O2","Q","Glutamine");
		addMass("C2H3NO","G","Glycine");
		addMass("C6H7N3O","H","Histidine");
		addMass("C6H11NO","I","Isoleucine");
		addMass("C6H11NO","L","Leucine");
		addMass("C6H12N2O","K","Lysine");
		addMass("C5H9NOS","M","Methionine");
		addMass("C9H9NO","F","Phenylalanine");
		addMass("C5H7NO","P","Proline");
		addMass("C3H5NO2","S","Serine");
		addMass("C4H7NO2","T","Threonine");
		addMass("C11H10N2O","W","Tryptophan");
		addMass("C9H9NO2","Y","Tyrosine");
		addMass("C5H9NO","V","Valine");

		for (String s : shortToMass.keySet())
			if (s.length()==1)
				aaMass[s.charAt(0)] = shortToMass.get(s).getMass();
		
		addMass("C2H3NO","Carbamidomethyl (C)","Carbamidomethyl (C)");
		addMass("O","Oxidation (M)","Oxidation (M)");
		addMass("C2H2O","Acetyl (Protein N-term)","Acetyl (Protein N-term)");
		addMass("H6","Arg6","Arg6");
		addMass("H10","Arg10","Arg10");
		addMass("H4","Lys4","Lys4");
		addMass("H6","Lys6","Lys6");
		addMass("H8","Lys8","Lys8");
		
		addMass("H","Nterm","neutral N term");
		addMass("OH","Cterm","neutral C term");
		

		ionDiffs['a'] = -getMass("CHO");
		ionDiffs['b'] = -getMass("H");
		ionDiffs['c'] = getMass("NH2");
		ionDiffs['x'] = getMass("CO")-getMass("H");
		ionDiffs['y'] = getMass("H");
		ionDiffs['z'] = -getMass("NH2");
		ionDiffs['*'] = -Ammonia.getMass();
		ionDiffs['°'] = -Water.getMass();
		ionDiffs['N'] = getMassByShortName("Nterm").getMass();
		ionDiffs['C'] = getMassByShortName("Cterm").getMass();
	}

	public void addMass(String formula, String name) {
		getMass(getMass(formula),formula,name);
	}

	public void addMass(String formula, String shortName, String name) {
		getMass(getMass(formula),shortName,name);
	}

	public void addMass(long mass, String shortName, String name) {
		getMass(mass,shortName,name);
	}

	public Mass getMassByShortName(String shortName) {
		return shortToMass.get(shortName);
	}

	private Mass getMass(long mass, String shortName, String name) {
		if (!nameToMass.containsKey(name)) {
			Mass m = new Mass(mass,shortName,name);
			shortToMass.put(shortName, m);
			nameToMass.put(name, m);
		}
		return nameToMass.get(name);
	}
	
	public double getMass(Mass mass) {
		return mass.getMass()*Math.pow(10,exp);
	}
	
	public double getMass(long mass) {
		return mass*Math.pow(10,exp);
	}

	public long getMass(String formula) {
		long re = 0L;
		for (int i=0; i<formula.length(); i++) {
			if (formula.charAt(i)==' ') continue;
			
			Mass m = atoms[formula.charAt(i)]; 
			long add = 0;
			if (i+1<formula.length() && formula.charAt(i+1)=='x') {
				i++;
				add = H.getMass();
			}
			
			int n = 1;
			
			if (i+1<formula.length() && formula.charAt(i+1)=='(') {
				i+=2;
				int j=i;
				for (;j<formula.length() && formula.charAt(j)!=')'; j++);
				if (j==formula.length()) throw new RuntimeException("No closing bracket!");
				n = Integer.parseInt(formula.substring(i,j));
				i = j;
			} else {
				int j = i+1;
				for (;j<formula.length() && Character.isDigit(formula.charAt(j)); j++);
				if (j-i>1) {
					n = Integer.parseInt(formula.substring(i+1,j));
					i=j-1;
				}
			}

			re+=m.getMass()*n+add;
		}
		return re;
	}
	
	

	public int getExp() {
		return exp;
	}



	public Mass[] getAminoAcids() {
		return new Mass[] {
				getMassByShortName("G"),
				getMassByShortName("A"),
				getMassByShortName("S"),
				getMassByShortName("P"),
				getMassByShortName("V"),
				getMassByShortName("T"),
				getMassByShortName("C"),
				getMassByShortName("I"),
				getMassByShortName("L"),
				getMassByShortName("N"),
				getMassByShortName("D"),
				getMassByShortName("Q"),
				getMassByShortName("K"),
				getMassByShortName("E"),
				getMassByShortName("M"),
				getMassByShortName("H"),
				getMassByShortName("F"),
				getMassByShortName("R"),
				getMassByShortName("Y"),
				getMassByShortName("W")
		};
	}
	
	public Mass[] getAminoAcidsWoI() {
		return new Mass[] {
				getMassByShortName("G"),
				getMassByShortName("A"),
				getMassByShortName("S"),
				getMassByShortName("P"),
				getMassByShortName("V"),
				getMassByShortName("T"),
				getMassByShortName("C"),
//				getMassByShortName("I"),
				getMassByShortName("L"),
				getMassByShortName("N"),
				getMassByShortName("D"),
				getMassByShortName("Q"),
				getMassByShortName("K"),
				getMassByShortName("E"),
				getMassByShortName("M"),
				getMassByShortName("H"),
				getMassByShortName("F"),
				getMassByShortName("R"),
				getMassByShortName("Y"),
				getMassByShortName("W")
		};
	}

	public double getPeptideMass(String seq) {
		long mass = 0;
		for (int i=0; i<seq.length(); i++) {
			Mass m = getMassByShortName(seq.substring(i,i+1));
			if (m==null) throw new RuntimeException("Unknown amino acid: "+seq.substring(i, i+1));
			mass += m.getMass();
		}
		return getMass(mass);
	}

	public MassList getPeptide(String seq) {
		Mass[] solution =  new Mass[seq.length()];
		for (int i=0; i<seq.length(); i++)
			solution[i] = getMassByShortName(seq.substring(i,i+1));
		return new MassList(solution);
	}
	
	public boolean isPeptide(String seq) {
		for (int i=0; i<seq.length(); i++)
			if (getMassByShortName(seq.substring(i,i+1))==null) return false;
		return true;
	}
	
	
	
	public Mass getIon(String name, String seq, String nType, String cType, int charge) {
		long mass = 0L;
		if (nType.length()==0) nType="N";
		if (cType.length()==0) nType="C";
		for (int i=0; i<nType.length(); i++) mass+=ionDiffs[nType.charAt(i)];
		for (int i=0; i<seq.length(); i++) mass+=aaMass[seq.charAt(i)];
		for (int i=0; i<cType.length(); i++) mass+=ionDiffs[cType.charAt(i)];
		mass+=charge*H.getMass();
		return new Mass(mass,name,nType+"-"+seq+"-"+cType);
	}
	
	public long getIonMass(String seq, String nType, String cType, int charge) {
		long mass = 0L;
		if (nType.length()==0) nType="N";
		if (cType.length()==0) nType="C";
		for (int i=0; i<nType.length(); i++) mass+=ionDiffs[nType.charAt(i)];
		for (int i=0; i<seq.length(); i++) mass+=aaMass[seq.charAt(i)];
		for (int i=0; i<cType.length(); i++) mass+=ionDiffs[cType.charAt(i)];
		mass+=charge*H.getMass();
		return mass;
	}

}
