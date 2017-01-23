package gedi.util.orm.special;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;

public class StringSpecialSerializer implements SpecialBinarySerializer<String> {

	@Override
	public void serialize(OrmSerializer parent, BinaryWriter out, String object) throws IOException {
		out.putString(object);
	}

	@Override
	public String deserialize(OrmSerializer parent, BinaryReader in) throws IOException {
		return in.getString();
	}

}
