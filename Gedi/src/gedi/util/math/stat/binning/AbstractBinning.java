package gedi.util.math.stat.binning;

import gedi.util.math.stat.factor.Factor;
import gedi.util.math.stat.factor.IntervalFactor;

public abstract class AbstractBinning implements Binning {

	private Factor factor;
	private boolean isint = false;
	
	public AbstractBinning(boolean isint) {
		this.isint = isint;
	}
	
	@Override
	public boolean isInteger() {
		return isint;
	}

	@Override
	public Factor getFactor(int index) {
		if (factor==null) {
			synchronized (this) {
				if (factor==null)
					factor = IntervalFactor.create(this);	
			}
		}
		return factor.get(index);
	}
	
}
