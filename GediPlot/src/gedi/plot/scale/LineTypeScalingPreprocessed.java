package gedi.plot.scale;


import gedi.plot.primitives.LineType;

public class LineTypeScalingPreprocessed {

	private LineType[] types;
	
	public LineTypeScalingPreprocessed(LineType[] types) {
		this.types = types;
	}
	
	public LineType[] getLineTypes() {
		return types;
	}

	public LineType get(int index) {
		return types[index%types.length];
	}


}
