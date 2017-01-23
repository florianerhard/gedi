package gedi.util.orm.special;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;
import java.util.HashMap;

public class HashMapSpecialSerializer implements SpecialBinarySerializer<HashMap<?,?>> {

	@Override
	public void serialize(OrmSerializer parent, BinaryWriter out, HashMap<?,?> object) throws IOException {
		out.putCInt(object.size());
		for (Object k : object.keySet()) {
			parent.serialize(out, k);
			parent.serialize(out, object.get(k));
		}
	}

	@Override
	public HashMap<?,?> deserialize(OrmSerializer parent, BinaryReader in) throws IOException {
		int c = in.getCInt();
		HashMap re = new HashMap();
		for (int i=0; i<c; i++)
			re.put(parent.deserialize(in),parent.deserialize(in));
		return re;
	}

}
