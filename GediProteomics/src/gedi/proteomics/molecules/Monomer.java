package gedi.proteomics.molecules;

import gedi.util.GeneralUtils;

import java.util.HashMap;

public class Monomer implements Comparable<Monomer> {
	private String singleLetter;
	private Modification mod;
	
	private static String[] toThreeLetter = new String[256];
	private static char[] singleLetters;
	static {
		singleLetters = "ARNDCEQGHILKMFPSTWYV".toCharArray();
		toThreeLetter['A'] = "Ala";
		toThreeLetter['R'] = "Arg";
		toThreeLetter['N'] = "Asn";
		toThreeLetter['D'] = "Asp";
		toThreeLetter['C'] = "Cys";
		toThreeLetter['E'] = "Glu";
		toThreeLetter['Q'] = "Gln";
		toThreeLetter['G'] = "Gly";
		toThreeLetter['H'] = "His";
		toThreeLetter['I'] = "Ile";
		toThreeLetter['L'] = "Leu";
		toThreeLetter['K'] = "Lys";
		toThreeLetter['M'] = "Met";
		toThreeLetter['F'] = "Phe";
		toThreeLetter['P'] = "Pro";
		toThreeLetter['S'] = "Ser";
		toThreeLetter['T'] = "Thr";
		toThreeLetter['W'] = "Trp";
		toThreeLetter['Y'] = "Tyr";
		toThreeLetter['V'] = "Val";

	}
	
	private static boolean[] aa = new boolean[256];
	static {
		for (char c : singleLetters)
			aa[c] = true;
	}
	
	private Monomer(String singleLetter, Modification mod) {
		this.singleLetter = singleLetter;
		this.mod = mod;
		if (!isAminoAcid(singleLetter) && !isTerminus(singleLetter))
			throw new RuntimeException(singleLetter+" is no amino acid nor a terminus!");
	}
	
	@Override
	public String toString() {
		return String.valueOf(singleLetter)+(mod==null?"":"["+mod.getIndex()+"]");
	}

	public boolean isAminoAcid() {
		return singleLetter.charAt(0)!='^' && singleLetter.charAt(0)!='$';
	}
	
	public String getSingleLetter() {
		return singleLetter;
	}
	
	public Modification getModification() {
		return mod;
	}
	
	public static boolean isAminoAcid(String singleLetter) {
		return singleLetter.length()==1  && aa[singleLetter.charAt(0)];
	}
	
	public static boolean isTerminus(String singleLetter) {
		return singleLetter.length()==1  && (singleLetter.equals("^") || singleLetter.equals("$"));
	}
	
	public static boolean isNTerminus(String singleLetter) {
		return singleLetter.length()==1  && singleLetter.equals("^");
	}
	
	public static boolean isCTerminus(String singleLetter) {
		return singleLetter.length()==1  && singleLetter.equals("$");
	}
	
	public static char[] getSingleLetters() {
		return singleLetters;
	}
	
	public static String toThreeLetter(char aa) {
		return toThreeLetter[aa];
	}
	
	private static HashMap<Long,Monomer> cache = new HashMap<Long, Monomer>();
	public static Monomer obtain(char singleLetter, Modification mod) {
		long l = (mod==null?0:((long)mod.getIndex())<<32) | singleLetter;
		Monomer re = cache.get(l);
		if (re==null) cache.put(l,re = new Monomer(String.valueOf(singleLetter), mod));
		return re;
	}

	@Override
	public int compareTo(Monomer o) {
		int re = singleLetter.compareTo(o.singleLetter);
		if (re==0)
			re = GeneralUtils.saveCompare(mod,o.mod);
		return re;
	}

	public boolean isModified() {
		return mod!=null;
	}

	public Monomer getUnmodified() {
		return obtain(singleLetter.charAt(0),null);
	}

}
