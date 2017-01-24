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

package gedi.remote.codec;

import java.util.logging.Level;
import java.util.logging.Logger;

import gedi.util.FileUtils;
import gedi.util.io.randomaccess.BufferBinaryReaderWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Encoded as follows:
 * int (total size in bytes)
 * int (length of classname)
 * byte[] (classname)
 * buffer data
 * @author erhard
 *
 */
public class AsciiEncoder extends MessageToByteEncoder<CharSequence> {

	
	private static final Logger log = Logger.getLogger( AsciiEncoder.class.getName() );

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		log.log(Level.SEVERE, "AsciiEncoder caught exception!", cause);
	}
	
	public AsciiEncoder() {
		super(String.class);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, CharSequence msg,
			ByteBuf out) throws Exception {
		out.writeInt(Integer.BYTES+1+Integer.BYTES+msg.length());
		out.writeInt(1);
		out.writeByte('A');
		out.writeInt(msg.length());
		for (int i=0; i<msg.length(); i++)
			out.writeByte(msg.charAt(i));
	}
	
}
