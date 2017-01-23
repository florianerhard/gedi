package gedi.remote.codec;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gedi.app.classpath.ClassPathCache;
import gedi.util.FileUtils;
import gedi.util.io.randomaccess.BufferBinaryReaderWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;


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
			
			BufferBinaryReaderWriter buff = new BufferBinaryReaderWriter(size-Integer.BYTES-classname.length);
			in.readBytes(buff.getBuffer());
			buff.getBuffer().flip();
			
			re.deserialize(buff);
			
			out.add(re);
		}
	}
	
}
