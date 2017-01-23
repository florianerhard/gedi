package gedi.util.math.stat.kernel;

import java.util.LinkedHashMap;
import java.util.function.DoubleUnaryOperator;

public interface Kernel extends DoubleUnaryOperator {
	double halfSize();
	default String name() {
		String n = getClass().getSimpleName();
		if (n.endsWith("Kernel")) n = n.substring(0, n.length()-"Kernel".length());
		return n;
	}
	default void setParameter(String name, String value){throw new RuntimeException("No parameters!");}
	default void setParameter(String name, double value){throw new RuntimeException("No parameters!");}
	default void setParameter(String name, int value){throw new RuntimeException("No parameters!");}
	
	default String[] parameterNames() { return new String[0];}
	default String getParameter(String name) { return null; }
	
	default PreparedIntKernel prepare() {
		return new PreparedIntKernel(this);
	}
	
	default String toKernelString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name()).append(" [");
		for (int i=0; i<parameterNames().length; i++) {
			if (i>0) sb.append(",");
			sb.append(parameterNames()[i]).append("=").append(getParameter(parameterNames()[i]));
		}
		sb.append("]");
		return sb.toString();
	}
	
	
}
