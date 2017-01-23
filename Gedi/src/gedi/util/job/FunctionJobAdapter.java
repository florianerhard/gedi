package gedi.util.job;

import java.util.function.Function;

import gedi.util.StringUtils;
import gedi.util.mutable.MutableTuple;

public class FunctionJobAdapter<TO> extends JobAdapter<TO> {

	private Function<MutableTuple,TO> function;

	public FunctionJobAdapter(Class[] input, Class<TO> output, Function<MutableTuple,TO> function) {
		super(input,output);
		this.function = function;
	}
	

	public FunctionJobAdapter(Class input, Class<TO> output, Function<MutableTuple,TO> function) {
		super(input,output);
		this.function = function;
	}

	@Override
	public TO execute(ExecutionContext context, MutableTuple input) {
		return function.apply(input);
	}
	

}
