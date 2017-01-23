package gedi.util.orm.special;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;
import java.util.ArrayList;

public class ArrayListSpecialSerializer implements SpecialBinarySerializer<ArrayList<?>> {

	@Override
	public void serialize(OrmSerializer parent, BinaryWriter out, ArrayList<?> object) throws IOException {
		out.putCInt(object.size());
		for (int i=0; i<object.size(); i++)
			parent.serialize(out, object.get(i));
	}

	@Override
	public ArrayList<?> deserialize(OrmSerializer parent, BinaryReader in) throws IOException {
		int c = in.getCInt();
		ArrayList re = new ArrayList(c);
		for (int i=0; i<c; i++)
			re.add(parent.deserialize(in));
		return re;
	}

}
