package gedi.util.job;

import java.util.function.Function;

import gedi.util.StringUtils;
import gedi.util.mutable.MutableTuple;

public abstract class JobAdapter<TO> implements Job<TO> {

	
	private Class[] input;
	private Class<TO> output;
	protected String id = null;
	

	public JobAdapter(Class[] input, Class<TO> output) {
		this.input = input;
		this.output = output;
	}
	

	public JobAdapter(Class input, Class<TO> output) {
		this.input = new Class[] {input};;
		this.output = output;
	}

	@Override
	public Class[] getInputClasses() {
		return input;
	}

	@Override
	public Class getOutputClass() {
		return output;
	}
	
	@Override
	public String getId() {
		return id;
	}

	
	@Override
	public boolean isDisabled() {
		return false;
	}

}
