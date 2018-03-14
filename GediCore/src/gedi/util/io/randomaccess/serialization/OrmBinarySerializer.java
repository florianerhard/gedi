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
