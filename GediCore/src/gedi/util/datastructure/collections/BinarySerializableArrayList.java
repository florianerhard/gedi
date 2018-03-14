/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
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
