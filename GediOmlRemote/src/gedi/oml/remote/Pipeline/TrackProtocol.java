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
package gedi.oml.remote.Pipeline;

import gedi.remote.Protocol;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

public class TrackProtocol implements Protocol {

	
	
	@Override
	public String getName() {
		return "tracks";
	}

	@Override
	public void setCodecs(ChannelPipeline pipeline) {
		pipeline.addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
		pipeline.addLast(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
		addDefaultCodecs(pipeline);
	}

	@Override
	public int getDefaultPort() {
		return 58315;
	}

	
	@Override
	public String toString() {
		return getName();
	}
	
}

