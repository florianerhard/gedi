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

package gedi.remote;

import gedi.remote.codec.AsciiEncoder;
import gedi.remote.codec.DefaultDecoder;
import gedi.remote.codec.BinaryEncoder;
import gedi.remote.codec.NumberEncoder;
import io.netty.channel.ChannelPipeline;

public interface Protocol {

	String getName();
	void setCodecs(ChannelPipeline pipeline);
	int getDefaultPort();
	
	
	default void addDefaultCodecs(ChannelPipeline pipeline) {
		pipeline.addLast(new BinaryEncoder());
		pipeline.addLast(new NumberEncoder());
		pipeline.addLast(new AsciiEncoder());
		pipeline.addLast(new DefaultDecoder());
	}
	
}
