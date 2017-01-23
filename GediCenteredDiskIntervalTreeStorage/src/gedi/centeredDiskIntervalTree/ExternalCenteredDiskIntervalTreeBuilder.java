package gedi.centeredDiskIntervalTree;

import gedi.util.io.randomaccess.serialization.BinarySerializable;

public class ExternalCenteredDiskIntervalTreeBuilder<D extends BinarySerializable> extends CenteredDiskIntervalTreeBuilder<D> {

	public static final String MAGIC = "CITE";
	
	
	public ExternalCenteredDiskIntervalTreeBuilder() {
		super(false,MAGIC,"",System.getProperty("java.io.tmpdir"));
	}
	
	
}
