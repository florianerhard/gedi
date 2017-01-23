package gedi.util.io.randomaccess.serialization;

import gedi.util.dynamic.DynamicObject;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.Orm;

import java.io.IOException;

public class BinarySerializableSerializer<T extends BinarySerializable> extends AbstractBinarySerializer<T> {


	public BinarySerializableSerializer(Class<T> cls) {
		super(cls, false);
	}


	@Override
	public void serialize(BinaryWriter out, T object) throws IOException {
		object.serialize(out);
	}

	@Override
	public T deserialize(BinaryReader in) throws IOException {
		T re = Orm.create(getType());
		re.deserialize(in);
		return re;
	}
	
	@Override
	public void serializeConfig(BinaryWriter out) throws IOException {
		super.serializeConfig(out);
		out.putString(out.getContext().getGlobalInfo().toJson());
	}
	
	@Override
	public void deserializeConfig(BinaryReader in) throws IOException {
		super.deserializeConfig(in);
		in.getContext().setGlobalInfo(DynamicObject.parseJson(in.getString()));
	}
	
	
}
