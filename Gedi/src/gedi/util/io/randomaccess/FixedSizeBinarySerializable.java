package gedi.util.io.randomaccess;

import gedi.util.io.randomaccess.serialization.BinarySerializable;

public interface FixedSizeBinarySerializable extends BinarySerializable {

	/**
	 * In byte
	 * @return
	 */
	int getFixedSize();
	
}
