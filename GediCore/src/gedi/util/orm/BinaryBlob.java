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

package gedi.util.orm;

import gedi.app.extension.ExtensionContext;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryReaderWriter;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.FixedSizeBinarySerializable;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Blob;
import java.sql.SQLException;

public class BinaryBlob implements Blob, BinaryReaderWriter {

	private ByteBuffer buffer;
	private ExtensionContext context;

	public BinaryBlob() {
		this(128);
	}

	public BinaryBlob(int size) {
		buffer = ByteBuffer.allocate(size);
		buffer.order(ByteOrder.BIG_ENDIAN);
	}
	
	public BinaryBlob(BinarySerializable data)  {
		this();
		try{
			data.serialize(this);
			buffer.flip();
		} catch(IOException e) {
			throw new RuntimeException("Cannot serialize object!",e);
		}
	}
	
	public BinaryBlob(FixedSizeBinarySerializable data)  {
		this(data.getFixedSize());
		try{
			data.serialize(this);
			buffer.flip();
		} catch(IOException e) {
			throw new RuntimeException("Cannot serialize object!",e);
		}
	}

    public BinaryBlob(Blob blob) throws SQLException {

        if (blob == null) {
            throw new SQLException("Cannot instantiate a BinaryBlob " +
                 "object with a null Blob object");
        }

        long len = blob.length();
        byte[] buf = blob.getBytes(1, (int)len );
        buffer = ByteBuffer.wrap(buf);
        
    }
    
    public ByteBuffer getBuffer() {
		return buffer;
	}
    
    @Override
    public ExtensionContext getContext() {
    	if (context==null)
    		context = new ExtensionContext();
    	return context;
    }
    
    private void checkSize(int bytes) {
    	checkSize(buffer.position(),bytes);
    }
    
    private void checkSize(long pos, int bytes) {
    	if (pos+bytes>=buffer.capacity()) {
    		int ns = buffer.capacity();
    		while (pos+bytes>=ns)
    			ns*=1.4;
    		ByteBuffer nb = ByteBuffer.allocate(ns);
    		buffer.flip();
    		nb.put(buffer);
    		buffer = nb;
    	}
    }
    
    @Override
    public boolean eof() {
    	return !buffer.hasRemaining();
    }
    
    /**
     *  call that to resize the buffer!
     */
    public void finish(boolean resize) {
    	if (resize) {
	    	ByteBuffer nb = ByteBuffer.allocate(buffer.position());
			nb.put(buffer);
			buffer = nb;
    	} else 
    		buffer.flip();
    }
    
    public void set(BinarySerializable data) throws IOException {
    	buffer.clear();
    	data.serialize(this);
		buffer.flip();
    }
    
    public ByteBuffer getByteBuffer() {
    	return buffer;
	}
    
    public byte[] toArray() {
    	byte[] re = new byte[buffer.limit()];
    	buffer.get(re);
    	return re;
    }

	@Override
	public long length() throws SQLException {
		return buffer.limit();
	}

	@Override
	public byte[] getBytes(long pos, int length) throws SQLException {
		byte[] re = new byte[length];
		buffer.position((int) pos-1);
		buffer.get(re);
		return re;
	}

	@Override
	public InputStream getBinaryStream(long pos, long length)
			throws SQLException {
		return new InputStream() {
			int posi = (int) pos;
			int end = (int) (pos+length);
			public int read() throws IOException {
				if (!buffer.hasRemaining() || buffer.position()>=end) {
					return -1;
				}
				return buffer.get(posi++) & 0xFF;
			}

			public int read(byte[] bytes, int off, int len)
					throws IOException {
				if (!buffer.hasRemaining()|| buffer.position()>=end) {
					return -1;
				}

				len = Math.min(len, buffer.remaining());
				if (posi+len>=end) len = end-posi;
				buffer.position(posi);
				buffer.get(bytes, off, len);
				posi+=len;
				return len;
			}
		};
	}


	@Override
	public InputStream getBinaryStream() throws SQLException {
		return new InputStream() {
			int pos = buffer.position();
			public int read() throws IOException {
				if (!buffer.hasRemaining()) {
					return -1;
				}
				return buffer.get(pos++) & 0xFF;
			}

			public int read(byte[] bytes, int off, int len)
					throws IOException {
				if (!buffer.hasRemaining()) {
					return -1;
				}

				len = Math.min(len, buffer.remaining());
				buffer.position(pos);
				buffer.get(bytes, off, len);
				pos+=len;
				return len;
			}
		};
	}

	@Override
	public long position(byte[] pattern, long start) throws SQLException {
		if (start < 1 || start > buffer.capacity()) {
			return -1;
		}

		int pos = (int)start-1; // internally Blobs are stored as arrays.
		int i = 0;
		long patlen = pattern.length;

		while (pos < buffer.capacity()) {
			if (pattern[i] == buffer.get(pos)) {
				if (i + 1 == patlen) {
					return (pos + 1) - (patlen - 1);
				}
				i++; pos++; // increment pos, and i
			} else if (pattern[i] != buffer.get(pos)) {
				pos++; // increment pos only
			}
		}
		return -1; // not found
	}

	@Override
	public long position(Blob pattern, long start) throws SQLException {
		return position(pattern.getBytes(1, (int)(pattern.length())), start);
	}

	@Override
	public int setBytes(long pos, byte[] bytes) throws SQLException {
		checkSize(pos, bytes.length);
		buffer.position((int) pos);
		buffer.put(bytes);
		return bytes.length;
	}

	@Override
	public int setBytes(long pos, byte[] bytes, int offset, int len)
			throws SQLException {
		checkSize(pos, len);
		buffer.position((int) pos);
		buffer.put(bytes,offset,len);
		return len;
	}

	@Override
	public OutputStream setBinaryStream(long pos) throws SQLException {
		return new OutputStream() {
			int posi = (int) pos;
			public void write(int b) throws IOException {
				checkSize(pos, Byte.BYTES);
				buffer.put(posi++,(byte) b);
			}

			public void write(byte[] bytes, int off, int len)
					throws IOException {
				checkSize(posi, len);
				
				buffer.position(posi);
				buffer.put(bytes, off, len);
				posi+=len;
			}

		};
	}

	@Override
	public void truncate(long len) {
		buffer.limit((int) len);
	}

	@Override
	public void free() {
		buffer.clear();
	}


	@Override
	public BinaryWriter putShort(short data) throws IOException {
		checkSize(Short.BYTES);
		buffer.putShort(data);
		return this;
	}

	@Override
	public BinaryWriter putLong(long data) throws IOException {
		checkSize(Long.BYTES);
		buffer.putLong(data);
		return this;
	}

	@Override
	public BinaryWriter putInt(int data) throws IOException {
		checkSize(Integer.BYTES);
		buffer.putInt(data);
		return this;
	}

	@Override
	public BinaryWriter putFloat(float data) throws IOException {
		checkSize(Float.BYTES);
		buffer.putFloat(data);
		return this;
	}

	@Override
	public BinaryWriter putDouble(double data) throws IOException {
		checkSize(Double.BYTES);
		buffer.putDouble(data);
		return this;
	}

	@Override
	public BinaryWriter putString(CharSequence line) throws IOException {
		checkSize(Integer.BYTES+line.length());
		buffer.putInt(line.length());
		for (int i=0; i<line.length(); i++)
			buffer.put((byte)line.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putAsciiChars(CharSequence data) throws IOException {
		checkSize(data.length());
		for (int i=0; i<data.length(); i++)
			buffer.put((byte)data.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putChars(CharSequence data) throws IOException {
		checkSize(data.length()*Character.BYTES);
		for (int i=0; i<data.length(); i++)
			buffer.putChar(data.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putAsciiChar(char data) throws IOException {
		checkSize(Byte.BYTES);
		buffer.put((byte)data);
		return this;
	}

	@Override
	public BinaryWriter putChar(char data) throws IOException {
		checkSize(Character.BYTES);
		buffer.putChar(data);
		return this;
	}

	@Override
	public BinaryWriter putByte(int data) throws IOException {
		checkSize(Byte.BYTES);
		buffer.put((byte)data);
		return this;
	}

	@Override
	public BinaryWriter put(byte data) throws IOException {
		checkSize(Byte.BYTES);
		buffer.put(data);
		return this;
	}

	@Override
	public BinaryWriter put(byte[] dst, int offset, int length)
			throws IOException {
		checkSize(length);
		buffer.put(dst, offset, length);
		return this;
	}

	@Override
	public short getShort() throws IOException {
		return buffer.getShort();
	}

	@Override
	public long getLong() throws IOException {
		return buffer.getLong();
	}

	@Override
	public int getInt() throws IOException {
		return buffer.getInt();
	}

	@Override
	public float getFloat() throws IOException {
		return buffer.getFloat();
	}

	@Override
	public double getDouble() throws IOException {
		return buffer.getDouble();
	}

	@Override
	public String getString() throws IOException {
		return getString(new StringBuilder()).toString();
	}

	@Override
	public StringBuilder getString(StringBuilder re) throws IOException {
		int l = getInt();
		for (int i=0; i<l; i++)
			re.append(getAsciiChar());
		return re;
	}

	@Override
	public char getAsciiChar() throws IOException {
		return (char)buffer.get();
	}

	@Override
	public char getChar() throws IOException {
		return buffer.getChar();
	}

	@Override
	public int getByte() throws IOException {
		return ((int)buffer.get())&0xFF;
	}

	@Override
	public byte get() throws IOException {
		return buffer.get();
	}

	@Override
	public BinaryBlob get(byte[] dst, int offset, int length) throws IOException {
		buffer.get(dst, offset, length);
		return this;
	}

	@Override
	public long position() throws IOException {
		return buffer.position();
	}
	
	@Override
	public long position(long position) throws IOException {
		buffer.position((int) position);
		return position;
	}

	@Override
	public short getShort(long position) throws IOException {
		return buffer.getShort((int) position);
	}

	@Override
	public long getLong(long position) throws IOException {
		return buffer.getLong((int) position);
	}

	@Override
	public int getInt(long position) throws IOException {
		return buffer.getInt((int) position);
	}

	@Override
	public float getFloat(long position) throws IOException {
		return buffer.getFloat((int) position);
	}

	@Override
	public double getDouble(long position) throws IOException {
		return buffer.getDouble((int) position);
	}

	@Override
	public String getString(long position) throws IOException {
		return getString(position,new StringBuilder()).toString();
	}

	@Override
	public StringBuilder getString(long position, StringBuilder re) throws IOException {
		int l = getInt(position);
		position+=Integer.BYTES;
		for (int i=0; i<l; i++){
			re.append(getAsciiChar(position));
			position+=Byte.BYTES;
		}
		return re;
	}

	@Override
	public char getAsciiChar(long position) throws IOException {
		return (char)buffer.get((int)position);
	}

	@Override
	public char getChar(long position) throws IOException {
		return buffer.getChar((int) position);
	}

	@Override
	public int getByte(long position) throws IOException {
		return ((int)buffer.get((int) position))&0xFF;
	}

	@Override
	public byte get(long position) throws IOException {
		return buffer.get((int) position);
	}

	@Override
	public BinaryReader get(long position, byte[] dst, int offset, int length)
			throws IOException {
		int old = buffer.position();
		buffer.position((int)position);
		buffer.get(dst, offset, length);
		buffer.position(old);
		return this;
	}

	@Override
	public BinaryWriter putShort(long position, short data) throws IOException {
		checkSize(position,Short.BYTES);
		buffer.putShort((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putLong(long position, long data) throws IOException {
		checkSize(position,Long.BYTES);
		buffer.putLong((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putInt(long position, int data) throws IOException {
		checkSize(position,Integer.BYTES);
		buffer.putInt((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putFloat(long position, float data) throws IOException {
		checkSize(position,Float.BYTES);
		buffer.putFloat((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putDouble(long position, double data)
			throws IOException {
		checkSize(position,Double.BYTES);
		buffer.putDouble((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putString(long position, CharSequence line)
			throws IOException {
		checkSize(position,Integer.BYTES+line.length());
		buffer.putInt(line.length());
		for (int i=0; i<line.length(); i++)
			buffer.put((int)position+i,(byte)line.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putAsciiChars(long position, CharSequence data)
			throws IOException {
		checkSize(position,data.length());
		for (int i=0; i<data.length(); i++)
			buffer.put((int)position+i,(byte)data.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putChars(long position, CharSequence data)
			throws IOException {
		checkSize(position,data.length()*Character.BYTES);
		for (int i=0; i<data.length(); i++)
			buffer.putChar((int)position+i,data.charAt(i));
		return this;
	}

	@Override
	public BinaryWriter putAsciiChar(long position, char data)
			throws IOException {
		checkSize(position,Byte.BYTES);
		buffer.put((int)position,(byte)data);
		return this;
	}

	@Override
	public BinaryWriter putChar(long position, char data) throws IOException {
		checkSize(position,Character.BYTES);
		buffer.putChar((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter putByte(long position, int data) throws IOException {
		checkSize(position,Byte.BYTES);
		buffer.put((int)position, (byte)data);
		return this;
	}

	@Override
	public BinaryWriter put(long position, byte data) throws IOException {
		checkSize(position,Byte.BYTES);
		buffer.put((int)position, data);
		return this;
	}

	@Override
	public BinaryWriter put(long position, byte[] dst, int offset, int length)
			throws IOException {
		checkSize(position,length);
		long pos = position();
		position(position);
		buffer.put(dst, offset, length);
		position(pos);
		return this;
	}


}
