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

import java.io.IOException;

public abstract class AbstractBinarySerializer<T> implements BinarySerializer<T> {

	protected Class<T> type;
	protected boolean autoConfig;

	public AbstractBinarySerializer(Class<T> type, boolean autoConfig) {
		this.type = type;
		this.autoConfig = autoConfig;
	}

	@Override
	public Class<T> getType() {
		return type;
	}
	
	
	public void beginSerialize(BinaryWriter out) throws IOException {
		if (autoConfig) {
			out.getContext().add(new SerializerConfigInfo(out.position()));
			out.putLong(0);
		}
	}
	public void endSerialize(BinaryWriter out) throws IOException {
		if (autoConfig) {
			long pos = out.position();
			serializeConfig(out);
			out.putLong(out.getContext().get(SerializerConfigInfo.class).configOffsetOffset, pos);
		}
	}
	
	public void beginDeserialize(BinaryReader in) throws IOException {
		if (autoConfig) {
			long coo = in.getLong();
			long re = in.position();
			in.position(coo);
			deserializeConfig(in);
			in.position(re);
		}
	}
	
	
	@Override
	/**
	 * Configure an object that has been initialized with Unsafe.newInstance
	 */
	public void serializeConfig(BinaryWriter out) throws IOException {
		out.putString(type.getName());
	}
	
	@Override
	public void deserializeConfig(BinaryReader in) throws IOException {
		try {
			type = (Class<T>) Class.forName(in.getString());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Could not read serializer config!",e);
		}
	}
	
	private static class SerializerConfigInfo {

		long configOffsetOffset;
		
		public SerializerConfigInfo(long configOffsetOffset) {
			this.configOffsetOffset = configOffsetOffset;
		}

	}
	
}
