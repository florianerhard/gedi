package gedi.util.datastructure.collections;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class BinarySerializableArrayList<E> extends ArrayList<E> implements BinarySerializable {
	
	public BinarySerializableArrayList() {
		super(0);
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(size());
		OrmSerializer orm = new OrmSerializer(true, true);
		orm.serializeAll(out, iterator());
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		int size = in.getCInt();
		ensureCapacity(size);
		OrmSerializer orm = new OrmSerializer(true,true);
		orm.deserializeAll(in).toCollection((Collection)this);
	}

}
