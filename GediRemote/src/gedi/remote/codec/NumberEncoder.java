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
public class NumberEncoder extends MessageToByteEncoder<Number> {

	private static final Logger log = Logger.getLogger( NumberEncoder.class.getName() );

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		log.log(Level.SEVERE, "NumberEncoder caught exception!", cause);
	}
	
	public NumberEncoder() {
		super(Number.class);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Number msg,
			ByteBuf out) throws Exception {
		if (msg instanceof Byte) {
			out.writeInt(Integer.BYTES+1+Byte.BYTES);
			out.writeInt(1);
			out.writeByte('B');
			out.writeByte(msg.byteValue());
		}
		else if (msg instanceof Short) {
			out.writeInt(Integer.BYTES+1+Short.BYTES);
			out.writeInt(1);
			out.writeByte('S');
			out.writeShort(msg.shortValue());
		}
		else if (msg instanceof Integer) {
			out.writeInt(Integer.BYTES+1+Integer.BYTES);
			out.writeInt(1);
			out.writeByte('I');
			out.writeInt(msg.intValue());
		}
		else if (msg instanceof Long) {
			out.writeInt(Integer.BYTES+1+Long.BYTES);
			out.writeInt(1);
			out.writeByte('L');
			out.writeLong(msg.longValue());
		}
		else if (msg instanceof Float) {
			out.writeInt(Integer.BYTES+1+Float.BYTES);
			out.writeInt(1);
			out.writeByte('F');
			out.writeFloat(msg.floatValue());
		}
		else if (msg instanceof Double) {
			out.writeInt(Integer.BYTES+1+Double.BYTES);
			out.writeInt(1);
			out.writeByte('D');
			out.writeDouble(msg.doubleValue());
		}
		else
			throw new RuntimeException("Could not encode number "+msg.getClass().getName());
		
	}
	
}
