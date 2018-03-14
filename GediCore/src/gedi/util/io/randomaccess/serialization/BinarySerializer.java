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

public interface BinarySerializer<T> {

	
	Class<T> getType();
	default void beginSerialize(BinaryWriter out) throws IOException {}
	default void endSerialize(BinaryWriter out) throws IOException {}
	
	default void beginDeserialize(BinaryReader in) throws IOException {}
	default void endDeserialize(BinaryReader in) throws IOException {}
	
	void serialize(BinaryWriter out, T object) throws IOException;
	T deserialize(BinaryReader in) throws IOException;
	
	void serializeConfig(BinaryWriter out) throws IOException;
	void deserializeConfig(BinaryReader in) throws IOException;
	
}
