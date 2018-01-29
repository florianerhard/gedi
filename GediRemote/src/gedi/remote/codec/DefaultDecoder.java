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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gedi.app.classpath.ClassPathCache;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.orm.BinaryBlob;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;


/**
 * Decodes {@link BinarySerializable}, Numbers and Ascii Strings
 * @author erhard
 *
 */
public class DefaultDecoder extends ByteToMessageDecoder {

	
	private static final Logger log = Logger.getLogger( DefaultDecoder.class.getName() );

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		log.log(Level.SEVERE, "DefaultDecoder caught exception!", cause);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		
		if (in.readableBytes()<Integer.BYTES)
			return;
		
		in.markReaderIndex();
		int size = in.readInt();
		
		if (in.readableBytes()<size) {
			in.resetReaderIndex();
			return;
		}
		
		// everything has arrived, decode
		char[] classname = new char[in.readInt()];
		for (int i=0; i<classname.length; i++)
			classname[i] = (char) (in.readByte() & 255);
		
		String clsName = String.valueOf(classname);
		
		if (clsName.length()==1) {
			
			switch (clsName) {
			case "A":
				char[] re = new char[in.readInt()];
				for (int i=0; i<re.length; i++)
					re[i] = (char) (in.readByte() & 255);
				out.add(String.valueOf(re));
				break;
			case "B":
				out.add(in.readByte());
				break;
			case "S":
				out.add(in.readShort());
				break;
			case "I":
				out.add(in.readInt());
				break;
			case "L":
				out.add(in.readLong());
				break;
			case "F":
				out.add(in.readFloat());
				break;
			case "D":
				out.add(in.readDouble());
				break;
			}
			
		} else {
			
			if (!ClassPathCache.getInstance().existsClass(clsName)) {
				in.resetReaderIndex();
				return;
			}
			
			Class<?> cls = Class.forName(clsName);
			BinarySerializable re = (BinarySerializable) cls.newInstance();
			
			BinaryBlob buff = new BinaryBlob(size-Integer.BYTES-classname.length);
			in.readBytes(buff.getBuffer());
			buff.getBuffer().flip();
			
			re.deserialize(buff);
			
			out.add(re);
		}
	}
	
}
