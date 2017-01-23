package gedi.util.io.randomaccess.serialization;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;

import java.io.IOException;

public interface BinarySerializer<T> {

	
	Class<T> getType();
	default void beginSerialize(BinaryWriter out) throws IOException {}
	default void endSerialize(BinaryWriter out) throws IOException {}
	
	default void beginDeserialize(BinaryReader in) throws IOException {}
	default void endDeserialize(BinaryReader in) throws IOException {}
	
	void serialize(BinaryWriter out, T object) throws IOException;
	T deserialize(BinaryReader in) throws IOException;
	
	void serializeConfig(BinaryWriter out) throws IOException;
	void deserializeConfig(BinaryReader in) throws IOException;
	
}
