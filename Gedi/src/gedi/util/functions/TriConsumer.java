package gedi.util.functions;

@FunctionalInterface
public interface TriConsumer<A,B,C> {

	void accept(A a, B b, C c);
	
}
