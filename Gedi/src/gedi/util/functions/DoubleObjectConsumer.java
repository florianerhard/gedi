package gedi.util.functions;

@FunctionalInterface
public interface DoubleObjectConsumer<T> {

	void accept(double n, T o);
	
}
