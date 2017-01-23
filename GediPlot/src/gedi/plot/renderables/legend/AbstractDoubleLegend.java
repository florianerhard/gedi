package gedi.plot.renderables.legend;

import gedi.plot.aesthetics.Aesthetic;
import gedi.plot.aesthetics.DoubleAesthetic;
import gedi.plot.scale.DoubleScalingPreprocessed;

public abstract class AbstractDoubleLegend extends AbstractLegend<Double,DoubleScalingPreprocessed> implements DoubleLegend {

	
	protected DoubleAesthetic aes;
	
	@Override
	public void setDoubleAesthetic(DoubleAesthetic aesthetic) {
		aes = aesthetic;
		super.setAesthetic(aesthetic);
	}
	
	@Override
	public void setAesthetic(Aesthetic<Double, DoubleScalingPreprocessed> aesthetic) {
		setDoubleAesthetic((DoubleAesthetic) aesthetic);
	}
}
