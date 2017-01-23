package gedi.util.io.randomaccess.serialization;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;

import java.io.IOException;

public interface BinarySerializable {

	void serialize(BinaryWriter out) throws IOException;
	void deserialize(BinaryReader in) throws IOException;
	
}
