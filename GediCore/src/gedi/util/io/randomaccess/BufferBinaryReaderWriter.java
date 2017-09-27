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

package gedi.util.io.randomaccess;

import gedi.app.extension.ExtensionContext;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;
import java.nio.ByteBuffer;

public class BufferBinaryReaderWriter implements BinaryReaderWriter {

	private ByteBuffer buffer;
	private ExtensionContext context;


	public BufferBinaryReaderWriter() {
		this(1024*64);
	}

	public BufferBinaryReaderWriter(int size) {
		buffer = ByteBuffer.allocate(size);
	}
	
	public BufferBinaryReaderWriter(BinarySerializable data)  {
		this();
		try{
			data.serialize(this);
			buffer.flip();
		} catch(IOException e) {
			throw new RuntimeException("Cannot serialize object!",e);
		}
	}
	
	public BufferBinaryReaderWriter(FixedSizeBinarySerializable data)  {
		this(data.getFixedSize());
		try{
			data.serialize(this);
			buffer.flip();
		} catch(IOException e) {
			throw new RuntimeException("Cannot serialize object!",e);
		}
	}
	
	public ByteBuffer getBuffer() {
		return buffer;
	}

	@Override
	public ExtensionContext getContext() {
		if (context==null) context = new ExtensionContext();
		return context;
	}
	
    
    public void set(BinarySerializable data) throws IOException  {
    	buffer.clear();
    	data.serialize(this);
		buffer.flip();
    }
    
    public byte[] toArray() {
    	byte[] re = new byte[buffer.limit()];
    	buffer.get(re);
    	return re;
    }


    private void ensureSize(int len) {
    	ensureSize(position(), len);
    }
    
    private void ensureSize(long pos, int len) {
    	if (buffer.capacity()<pos+len) {
    		int ns = buffer.capacity()+1;
    		for (; ns<pos+len; ns*=1.4);
    		
    		ByteBuffer nbuffer = ByteBuffer.allocate(ns);
    		buffer.flip();
    		nbuffer.put(buffer);
    		nbuffer.position(buffer.position());
    		buffer = nbuffer;
    	}
    }
    
    
	@Override
	public BinaryWriter putShort(short data)  {
		ensureSize(Short.BYTES);
		buffer.putShort(data);
		return this;
	}

	@Override
	public BinaryWriter putLong(long data)  {
		ensureSize(Long.BYTES);
		buffer.putLong(data);
		return this;
	}

	@Override
	public BinaryWriter putInt(int data)  {
		ensureSize(Integer.BYTES);
		buffer.putInt(data);
		return this;
	}

	@Override
	public BinaryWriter putFloat(float data)  {
		ensureSize(Float.BYTES);
		buffer.putFloat(data);
		return this;
	}

	@Override
	public BinaryWriter putDouble(double data)  {
		ensureSize(Double.BYTES);
		buffer.putDouble(data);
		return this;
	}

	@Override
	public BinaryWriter putString(CharSequence line)  {
		ensureSize(Integer.BYTES+line.length());
		buffer.putInt(line.length());
		for (int i=0; i<line.length(); i++)
			buffer.put((byte)line.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putAsciiChars(CharSequence data)  {
		ensureSize(data.length());
		for (int i=0; i<data.length(); i++)
			buffer.put((byte)data.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putChars(CharSequence data)  {
		ensureSize(data.length()*Character.BYTES);
		for (int i=0; i<data.length(); i++)
			buffer.putChar(data.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putAsciiChar(char data)  {
		ensureSize(1);
		buffer.put((byte)data);
		return this;
	}

	@Override
	public BinaryWriter putChar(char data)  {
		ensureSize(Character.BYTES);
		buffer.putChar(data);
		return this;
	}

	@Override
	public BinaryWriter putByte(int data)  {
		ensureSize(1);
		buffer.put((byte)data);
		return this;
	}

	@Override
	public BinaryWriter put(byte data)  {
		ensureSize(1);
		buffer.put(data);
		return this;
	}

	@Override
	public BinaryWriter put(byte[] dst, int offset, int length)
			 {
		ensureSize(length);
		buffer.put(dst, offset, length);
		return this;
	}

	@Override
	public short getShort()  {
		return buffer.getShort();
	}

	@Override
	public long getLong()  {
		return buffer.getLong();
	}

	@Override
	public int getInt()  {
		return buffer.getInt();
	}

	@Override
	public float getFloat()  {
		return buffer.getFloat();
	}

	@Override
	public double getDouble()  {
		return buffer.getDouble();
	}

	@Override
	public String getString()  {
		return getString(new StringBuilder()).toString();
	}

	@Override
	public StringBuilder getString(StringBuilder re)  {
		int l = getInt();
		for (int i=0; i<l; i++)
			re.append(getAsciiChar());
		return re;
	}

	@Override
	public char getAsciiChar()  {
		return (char)buffer.get();
	}

	@Override
	public char getChar()  {
		return buffer.getChar();
	}

	@Override
	public int getByte()  {
		return ((int)buffer.get())&0xFF;
	}

	@Override
	public byte get()  {
		return buffer.get();
	}

	@Override
	public BufferBinaryReaderWriter get(byte[] dst, int offset, int length)  {
		buffer.get(dst, offset, length);
		return this;
	}

	@Override
	public long position() {
		return buffer.position();
	}
	
	@Override
	public long position(long position)  {
		ensureSize(position,0);
		
		buffer.position((int) position);
		return position;
	}

	@Override
	public short getShort(long position)  {
		return buffer.getShort((int) position);
	}

	@Override
	public long getLong(long position)  {
		return buffer.getLong((int) position);
	}

	@Override
	public int getInt(long position)  {
		return buffer.getInt((int) position);
	}

	@Override
	public float getFloat(long position)  {
		return buffer.getFloat((int) position);
	}

	@Override
	public double getDouble(long position)  {
		return buffer.getDouble((int) position);
	}

	@Override
	public String getString(long position)  {
		return getString(position,new StringBuilder()).toString();
	}

	@Override
	public StringBuilder getString(long position, StringBuilder re)  {
		int l = getInt(position);
		position+=Integer.BYTES;
		for (int i=0; i<l; i++){
			re.append(getAsciiChar(position));
			position+=Byte.BYTES;
		}
		return re;
	}

	@Override
	public char getAsciiChar(long position)  {
		return (char)buffer.get((int)position);
	}

	@Override
	public char getChar(long position)  {
		return buffer.getChar((int) position);
	}

	@Override
	public int getByte(long position)  {
		return ((int)buffer.get((int) position))&0xFF;
	}

	@Override
	public byte get(long position)  {
		return buffer.get((int) position);
	}

	@Override
	public BinaryReader get(long position, byte[] dst, int offset, int length)
			 {
		int old = buffer.position();
		buffer.position((int)position);
		buffer.get(dst, offset, length);
		buffer.position(old);
		return this;
	}

	@Override
	public BinaryWriter putShort(long position, short data)  {
		ensureSize(position,Short.BYTES);
		buffer.putShort((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putLong(long position, long data)  {
		ensureSize(position,Long.BYTES);
		buffer.putLong((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putInt(long position, int data)  {
		ensureSize(position,Integer.BYTES);
		buffer.putInt((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putFloat(long position, float data)  {
		ensureSize(position,Float.BYTES);
		buffer.putFloat((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putDouble(long position, double data)
			 {
		ensureSize(position,Double.BYTES);
		buffer.putDouble((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putString(long position, CharSequence line)
			 {
		ensureSize(position,Integer.BYTES+line.length());
		buffer.putInt(line.length());
		for (int i=0; i<line.length(); i++)
			buffer.put((int)position+i,(byte)line.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putAsciiChars(long position, CharSequence data)
			 {
		ensureSize(position,data.length());
		for (int i=0; i<data.length(); i++)
			buffer.put((int)position+i,(byte)data.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putChars(long position, CharSequence data)
			 {
		ensureSize(position,data.length()*Character.BYTES);
		for (int i=0; i<data.length(); i++)
			buffer.putChar((int)position+i,data.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putAsciiChar(long position, char data)
			 {
		ensureSize(position,1);
		buffer.put((int)position,(byte)data);
		return this;
	}

	@Override
	public BinaryWriter putChar(long position, char data)  {
		ensureSize(position,Character.BYTES);
		buffer.putChar((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putByte(long position, int data)  {
		ensureSize(position,1);
		buffer.put((int)position, (byte)data);
		return this;
	}

	@Override
	public BinaryWriter put(long position, byte data)  {
		ensureSize(position,1);
		buffer.put((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter put(long position, byte[] dst, int offset, int length)
			 {
		ensureSize(position,length);
		long pos = position();
		position(position);
		buffer.put(dst, offset, length);
		position(pos);
		return this;
	}

}
