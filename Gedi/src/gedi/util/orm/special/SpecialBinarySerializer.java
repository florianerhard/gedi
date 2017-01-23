package gedi.util.orm.special;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;


public interface SpecialBinarySerializer<T> {

	
	void serialize(OrmSerializer parent, BinaryWriter out, T object) throws IOException;
	T deserialize(OrmSerializer parent, BinaryReader in) throws IOException;
	
}
