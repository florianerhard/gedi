package gedi.util;


public class GeneralUtils {
	public static boolean isEqual(Object a, Object b) {
		if (a==null && b==null) return true;
		return a!=null && a.equals(b);
	}

	public static <T> T orDefault(T test, T def) {
		return test==null?def:test;
	}

	public static <T extends Comparable<T>> int saveCompare(T a, T b) {
		if (a==null && b==null) return 0;
		if (a==null) return -1;
		if (b==null) return 1;
		return a.compareTo(b);
	}
	
	public static double EPS = 1E-8;
	public static boolean isEqual(double a, double b) {
		return isEqual(a,b,EPS);
	}
	public static boolean isEqual(double a, double b, double eps) {
		if (Double.isNaN(a) && Double.isNaN(b)) return true;
		return Math.abs(a-b)<eps;
	}

	public static short checkedIntToShort(int i) {
		if (i<Short.MIN_VALUE || i>Short.MAX_VALUE) throw new RuntimeException("Short overflow: "+i);
		return (short) i;
	}

	public static short checkedLongToShort(long i) {
		if (i<Short.MIN_VALUE || i>Short.MAX_VALUE) throw new RuntimeException("Short overflow: "+i);
		return (short) i;
	}
	
	public static int checkedLongToInt(long i) {
		if (i<Integer.MIN_VALUE || i>Integer.MAX_VALUE) throw new RuntimeException("Int overflow: "+i);
		return (int) i;
	}

	public static boolean isCause(Throwable exception,
			Class<? extends Throwable> cause) {
		if (exception==null) return false;
		if (cause.isInstance(exception)) return true;
		return isCause(exception.getCause(), cause);
	}
}
