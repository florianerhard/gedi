package gedi.plot.renderables.legend;

import gedi.plot.aesthetics.Aesthetic;
import gedi.plot.aesthetics.DoubleAesthetic;
import gedi.plot.scale.DoubleScalingPreprocessed;

public interface DoubleLegend extends Legend<Double,DoubleScalingPreprocessed> {

	
	default void setAesthetic(Aesthetic<Double, DoubleScalingPreprocessed> aesthetic) {
		setDoubleAesthetic((DoubleAesthetic) aesthetic);
	}

	void setDoubleAesthetic(DoubleAesthetic aesthetic);

}
