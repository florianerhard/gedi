package gedi.util.functions;

@FunctionalInterface
public interface TriFunction<I1,I2,I3,O> {

	O apply(I1 a1, I2 a2, I3 a3);
	
}
