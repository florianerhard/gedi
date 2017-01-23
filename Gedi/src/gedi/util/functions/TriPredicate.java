package gedi.util.functions;

@FunctionalInterface
public interface TriPredicate<I1,I2,I3> {

	boolean test(I1 a1, I2 a2, I3 a3);
	
}
