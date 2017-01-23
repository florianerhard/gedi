package gedi.util.io.text.jph;


public class JhpParameterException extends RuntimeException {

	private static final long serialVersionUID = 227393818068359180L;
	private String variableName;

	public JhpParameterException(String variableName) {
		super("Unknown variable: "+variableName);
		this.variableName = variableName;
	}

	public String getVariableName() {
		return variableName;
	}
	
	
}
