package gedi.util;

public class Checker {

	
	public static <T> T notNull(T check) {
		if (check==null) throw new RuntimeException("May not be null!");
		return check;
	}
	
	public static int not(int check, int not) {
		if (check==not) throw new RuntimeException("May not be "+not+"!");
		return check;
	}
	
	public static void equal(int a, int b) {
		if (a!=b) throw new RuntimeException("Not equal: "+a+" and "+b);
	}
	
}
