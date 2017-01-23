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
public class BinaryEncoder extends MessageToByteEncoder<BinarySerializable> {

	
	private static final Logger log = Logger.getLogger( BinaryEncoder.class.getName() );

	
	public BinaryEncoder() {
		super(BinarySerializable.class);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, BinarySerializable msg,
			ByteBuf out) throws Exception {
		
		BufferBinaryReaderWriter buff = new BufferBinaryReaderWriter(msg);
		String n = msg.getClass().getName();
		out.writeInt(Integer.BYTES+n.length()+buff.getBuffer().limit());
		out.writeInt(n.length());
		for (int i=0; i<n.length(); i++)
			out.writeByte(n.charAt(i));
		out.writeBytes(buff.getBuffer());
	}
	
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		log.log(Level.SEVERE, "BinaryEncoder caught exception!", cause);
	}
	
	
}
