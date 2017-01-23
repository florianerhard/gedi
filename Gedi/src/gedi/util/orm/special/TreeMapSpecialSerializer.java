package gedi.util.orm.special;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;
import java.util.TreeMap;

public class TreeMapSpecialSerializer implements SpecialBinarySerializer<TreeMap<?,?>> {

	@Override
	public void serialize(OrmSerializer parent, BinaryWriter out, TreeMap<?,?> object) throws IOException {
		out.putCInt(object.size());
		for (Object k : object.keySet()) {
			parent.serialize(out, k);
			parent.serialize(out, object.get(k));
		}
	}

	@Override
	public TreeMap<?,?> deserialize(OrmSerializer parent, BinaryReader in) throws IOException {
		int c = in.getCInt();
		TreeMap re = new TreeMap();
		for (int i=0; i<c; i++)
			re.put(parent.deserialize(in),parent.deserialize(in));
		return re;
	}

}
