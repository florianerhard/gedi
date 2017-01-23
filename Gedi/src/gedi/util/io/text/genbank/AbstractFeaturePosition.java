package gedi.util.io.text.genbank;

public abstract class AbstractFeaturePosition implements GenbankFeaturePosition {

	private String descriptor;
	private GenbankFeature feature;
	protected int leftMost;
	protected int rightMost;
	
	public AbstractFeaturePosition(GenbankFeature feature, String descriptor) {
		this.feature = feature;
		this.descriptor = descriptor;
	}
	

	@Override
	public String getDescriptor() {
		return descriptor;
	}

	@Override
	public GenbankFeature getFeature() {
		return feature;
	}
	
	@Override
	public String toString() {
		return getDescriptor();
	}

	@Override
	public int getStartInSource() {
		return leftMost;
	}
	
	@Override
	public int getEndInSource() {
		return rightMost;
	}
}
