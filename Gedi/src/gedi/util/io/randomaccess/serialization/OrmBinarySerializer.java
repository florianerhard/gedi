package gedi.util.io.randomaccess.serialization;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;

public class OrmBinarySerializer<T> extends AbstractBinarySerializer<T> {

	private OrmSerializer orm = new OrmSerializer();

	public OrmBinarySerializer(Class<T> cls) {
		super(cls, false);
	}


	@Override
	public void serialize(BinaryWriter out, T object) throws IOException {
		orm.serialize(out, object);
		orm.clearObjectCache();
	}

	@Override
	public T deserialize(BinaryReader in) throws IOException {
		T re = orm.deserialize(in);
		orm.clearObjectCache();
		return re;
	}
	
	
	
	
}
