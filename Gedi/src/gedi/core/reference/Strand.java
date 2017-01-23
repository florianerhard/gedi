package gedi.core.reference;

public enum Strand {
	Plus,Minus,Independent;
	
	
	public static Strand parse(String s) {
		if (s==null || s.length()==0 || s.equals("0") || s.equals(".")) return Strand.Independent;
		if (s.equals("+") || s.equals("1")) return Strand.Plus;
		if (s.equals("-") || s.equals("-1")) return Strand.Minus;
		return valueOf(s);
	}
	
	public String toString() {
		if (this==Plus) return "+";
		if (this==Minus) return "-";
		return "";
	}

	public String getGff() {
		if (this==Plus) return "+";
		if (this==Minus) return "-";
		return ".";
	}
	public String getEnsembl() {
		if (this==Plus) return "1";
		if (this==Minus) return "-1";
		return "0";
	}

	public static int compare(Strand a, Strand b) {
		return a.ordinal()-b.ordinal();
	}

	public Strand toOpposite() {
		if (this==Plus) return Minus;
		if (this==Minus) return Plus;
		return Independent;
	}
	
}