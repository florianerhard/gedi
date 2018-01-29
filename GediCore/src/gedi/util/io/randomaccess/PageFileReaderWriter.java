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
import gedi.util.FileUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.util.Arrays;

public class PageFileReaderWriter implements AutoCloseable, BinaryReaderWriter {

	private static final long PAGE_OVERLAP = Long.BYTES;
	
	private RandomAccessFile file;
	private final FileChannel channel;
	private long start;

	private long position=0; // relative to start-end!
	private ByteBuffer[] buffers = new ByteBuffer[1];
	
	private long pageSize = 1<<24 - PAGE_OVERLAP;

	private FileLock lock;
	
	private String path;
	private long maxLength = 0;

	private ExtensionContext context;

	
	public PageFileReaderWriter(String path) throws IOException  {
		this.path = path;
		file = new RandomAccessFile(path, "rw");
		this.channel = file.getChannel();
		lock = channel.lock();
		start = 0;
	}
	
	public PageFileReaderWriter(String path, long start) throws IOException  {
		this.path = path;
		file = new RandomAccessFile(path, "rw");
		this.channel = file.getChannel();
		lock = channel.lock();
		this.start = start;
	}
	
	@Override
	public ExtensionContext getContext() {
		if (context==null) context = new ExtensionContext();
		return context;
	}
	
	
	public String getPath() {
		return path;
	}
	
	public long getStart() {
		return start;
	}
	
	public boolean eof() {
		try {
			return position()>=file.length();
		} catch (IOException e) {
			return true;
		}
	}
	
	
	public long position() {
		return position;
	}
	
	public long relativePosition(long increment) {
		if (position+increment<0) throw new IndexOutOfBoundsException();
		this.position += increment; 
		maxLength = Math.max(maxLength,position);
		return position();
	}
	
	public long position(long position) {
		if (position<0) throw new IndexOutOfBoundsException();
		this.position = position; 
		maxLength = Math.max(maxLength,position);
		return position();
	}
	
	public void close() throws IOException {
		for (int i=0; i<buffers.length; i++) {
			if (buffers[i]!=null) {
				WeakReference<MappedByteBuffer> r = new WeakReference<MappedByteBuffer>((MappedByteBuffer) buffers[i]);
				buffers[i] = null;
				FileUtils.unmap(r);
			}
		}
		file.setLength(maxLength);
		lock.release();
		lock = null;
		channel.close();
		file.close();
	}
	
	public PageFileReaderWriter get(long pos, byte[] dst, int offset, int length) throws IOException {
		int sindex = getIndex(pos);
		int eindex = getIndex(pos+length-1);
		int boffset = getOffset(pos);
		
		for (int index=sindex; index<=eindex; index++) {
			ByteBuffer buffer = getBuffer(index);
			buffer.position(boffset);
			int l = Math.min(length, (int)(PageFile.pageSize-boffset));// (int)(buffer.remaining()-PAGE_OVERLAP-offset));
//			int l = Math.min(length, buffer.capacity()-offset);
			buffer.get(dst, offset, l);
			length-=l;
			offset+=l;
			boffset=0;
		}
        return this;
    }
	
	public PageFileReaderWriter get(byte[] dst, int offset, int length) throws IOException {
       get(position,dst,offset,length);
       position+=Byte.BYTES*length;
       return this;
    }
	
	public byte get(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		return buffer.get(offset);
	}

	public byte get()
			throws IOException {
		byte val = get(position);
		position += Byte.BYTES;
		return val;
	}
	
	public int getByte(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		return ((int)buffer.get(offset))&255;
	}

	public int getByte()
			throws IOException {
		byte val = get(position);
		position += Byte.BYTES;
		return ((int)val) & 0xFF;
	}

	public char getChar(long pos)
			throws IndexOutOfBoundsException, IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		return buffer.getChar(offset);
	}

	public char getChar()
			throws IOException {
		char val = getChar(position);
		position += Character.BYTES;
		return val;
	}
	
	public char getAsciiChar(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		return (char) buffer.get(offset);
	}

	public char getAsciiChar()
			throws IOException {
		char val = getAsciiChar(position);
		position += Byte.BYTES;
		return val;
	}
	
	public String getAsciiChars(int l)
			throws IOException {
		char[] re = new char[l];
		for (int i=0; i<re.length; i++)
			re[i] = getAsciiChar();
		return String.valueOf(re);
	}
	
	
	public String readLine() throws IOException {
		StringBuilder s = new StringBuilder(80);
		
		char c = '\0';
		while (position<maxLength && (c=getAsciiChar())!='\r' && c!='\n') 
			s.append(c);
		if (c=='\r' && position<maxLength && (c=getAsciiChar())!='\n')
			position-=Byte.BYTES;
		return position==maxLength && s.length()==0?null:s.toString(); 
	}

	public int readLine(char[] buffer) throws IOException {
		char c = '\0';
		int ind = 0;
		while (ind<buffer.length && position<maxLength && (c=getAsciiChar())!='\r' && c!='\n') 
			buffer[ind++] = c;
		if (c=='\r' && position<maxLength && (c=getAsciiChar())!='\n')
			position-=Byte.BYTES;
		return ind; 
	}
	
	/**
	 * as written by {@link PageFileWriter#putString(CharSequence)}
	 * @param pos
	 * @param re
	 * @return
	 * @throws IOException
	 */
	public StringBuilder getString(long pos, StringBuilder re) throws IOException {
		position(pos);
		return getString(re);
	}

	/**
	 * as written by {@link PageFileWriter#putString(CharSequence)}
	 * @param pos
	 * @param re
	 * @return
	 * @throws IOException
	 */
	public String getString(long pos) throws IOException {
		StringBuilder re = getString(pos,new StringBuilder());
		if (re==null) return null;
		return re.toString();
	}	
	
	/**
	 * as written by {@link PageFileWriter#putString(CharSequence)}
	 * @param pos
	 * @param re
	 * @return
	 * @throws IOException
	 */
	public StringBuilder getString(StringBuilder re) throws IOException {
		int l = getInt();
		if (l==-1) return null;
		for (int i=0; i<l; i++)
			re.append(getAsciiChar());
		return re;
	}

	/**
	 * as written by {@link PageFileWriter#putString(CharSequence)}
	 * @param pos
	 * @param re
	 * @return
	 * @throws IOException
	 */
	public String getString() throws IOException {
		StringBuilder re = getString(new StringBuilder());
		if (re==null) return null;
		return re.toString();
	}
	
	public double getDouble(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		return buffer.getDouble(offset);
	}

	public double getDouble()
			throws IOException {
		double val = getDouble(position);
		position += Double.BYTES;
		return val;
	}


	public float getFloat(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		return buffer.getFloat(offset);
	}

	public float getFloat()
			throws IOException {
		float val = getFloat(position);
		position += Float.BYTES;
		return val;
	}

	public int getInt(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		return buffer.getInt(offset);
	}

	public int getInt()
			throws IOException {
		int val = getInt(position);
		position += Integer.BYTES;
		return val;
	}


	public long getLong(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		return buffer.getLong(offset);
	}

	public long getLong()
			throws IOException {
		long val = getLong(position);
		position += Long.BYTES;
		return val;
	}


	public short getShort(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		return buffer.getShort(offset);
	}

	public short getShort()
			throws IOException {
		short val = getShort(position);
		position += Short.BYTES;
		return val;
	}

	public PageFileReaderWriter put(long pos, byte[] dst, int offset, int length) throws IOException {
		int sindex = getIndex(pos);
		int eindex = getIndex(pos+length-1);
		int boffset = getOffset(pos);
		
		for (int index=sindex; index<=eindex; index++) {
			ByteBuffer buffer = getBuffer(index);
			buffer.position(boffset);
//			int l = Math.min(length, buffer.capacity()-offset);
			int l = Math.min(length, (int)(pageSize-boffset));
			buffer.put(dst, offset, l);
			length-=l;
			offset+=l;
			boffset=0;
		}
		maxLength = Math.max(maxLength,pos+Byte.BYTES*length);
        return this;
    }
	
	public PageFileReaderWriter put(byte[] dst, int offset, int length) throws IOException {
       put(position,dst,offset,length);
       position+=Byte.BYTES*length;
       return this;
    }
	
	public PageFileReaderWriter put(long pos, byte data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.put(offset,data);
		maxLength = Math.max(maxLength,pos+Byte.BYTES);
		return this;
	}

	public PageFileReaderWriter put(byte data)
			throws IOException {
		put(position,data);
		position += Byte.BYTES;
		return this;
	}
	
	public PageFileReaderWriter putByte(long pos, int data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.put(offset,(byte)data);
		maxLength = Math.max(maxLength,pos+Byte.BYTES);
		return this;
	}

	public PageFileReaderWriter putByte(int data)
			throws IOException {
		putByte(position,data);
		position += Byte.BYTES;
		return this;
	}


	public PageFileReaderWriter putChar(long pos, char data)
			throws IndexOutOfBoundsException, IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putChar(offset,data);
		maxLength = Math.max(maxLength,pos+Character.BYTES);
		return this;
	}

	public PageFileReaderWriter putChar(char data)
			throws IOException {
		putChar(position,data);
		position += Character.BYTES;
		return this;
	}
	
	public PageFileReaderWriter putAsciiChar(long pos, char data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.put(offset,(byte)data);
		maxLength = Math.max(maxLength,pos+Byte.BYTES);
		return this;
	}

	public PageFileReaderWriter putAsciiChar(char data)
			throws IOException {
		putAsciiChar(position,data);
		position += Byte.BYTES;
		return this;
	}
	
	public PageFileReaderWriter putChars(long pos, CharSequence data)
			throws IndexOutOfBoundsException, IOException {
		
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.position(offset);
		for (int i=0; i<data.length(); i++)
			buffer.putChar(data.charAt(i));
		maxLength = Math.max(maxLength,pos+Character.BYTES*data.length());
		return this;
	}

	public PageFileReaderWriter putChars(CharSequence data)
			throws IOException {
		putChars(position,data);
		position += Character.BYTES*data.length();
		return this;
	}
	
	public PageFileReaderWriter putAsciiChars(long pos, CharSequence data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.position(offset);
		for (int i=0; i<data.length(); i++)
			buffer.put((byte)data.charAt(i));
		maxLength = Math.max(maxLength,pos+Byte.BYTES*data.length());
		return this;
	}

	public PageFileReaderWriter putAsciiChars(CharSequence data)
			throws IOException {
		putAsciiChars(position,data);
		position += Byte.BYTES*data.length();
		return this;
	}
	
	public PageFileReaderWriter putString(CharSequence line) throws IOException {
		if (line==null)
			putInt(-1);
		else {
			putInt(line.length());
			for (int i=0; i<line.length(); i++)
				putAsciiChar(line.charAt(i));
		}
		return this;
	}
	
	public PageFileReaderWriter writeLine(CharSequence line) throws IOException {
		for (int i=0; i<line.length(); i++)
			putAsciiChar(line.charAt(i));
		putAsciiChar('\n');
		return this;
	}
	
	public PageFileReaderWriter writeLine(char[] line) throws IOException {
		for (int i=0; i<line.length; i++)
			putAsciiChar(line[i]);
		putAsciiChar('\n');
		return this;
	}

	public PageFileReaderWriter putDouble(long pos, double data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putDouble(offset,data);
		maxLength = Math.max(maxLength,pos+Double.BYTES);
		return this;
	}

	public PageFileReaderWriter putDouble(double data)
			throws IOException {
		putDouble(position,data);
		position += Double.BYTES;
		return this;
	}


	public PageFileReaderWriter putFloat(long pos,float data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putFloat(offset,data);
		maxLength = Math.max(maxLength,pos+Float.BYTES);
		return this;
	}

	public PageFileReaderWriter putFloat(float data)
			throws IOException {
		putFloat(position,data);
		position += Float.BYTES;
		return this;
	}

	public PageFileReaderWriter putInt(long pos, int data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putInt(offset,data);
		maxLength = Math.max(maxLength,pos+Integer.BYTES);
		return this;
	}

	public PageFileReaderWriter putInt(int data)
			throws IOException {
		putInt(position,data);
		position += Integer.BYTES;
		return this;
	}


	public PageFileReaderWriter putLong(long pos, long data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putLong(offset,data);
		maxLength = Math.max(maxLength,pos+Long.BYTES);
		return this;
	}

	public PageFileReaderWriter putLong(long data)
			throws IOException {
		putLong(position,data);
		position += Long.BYTES;
		return this;
	}


	public PageFileReaderWriter putShort(long pos,short data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putShort(offset,data);
		maxLength = Math.max(maxLength,pos+Short.BYTES);
		return this;
	}

	public PageFileReaderWriter putShort(short data)
			throws IOException {
		putShort(position,data);
		position += Short.BYTES;
		return this;
	}


	private int getOffset(long pos) {
		if (pos<0) throw new IndexOutOfBoundsException();
		return (int) ((start+pos) % pageSize);
	}

	private int getIndex(long pos) {
		return (int) ((start+pos) / (long) pageSize);
	}

	
	private ByteBuffer getBuffer(int index)
			throws IOException {
		
		if (index>=buffers.length) {
			int ns = buffers.length;
			for (;index>=ns; ns*=2);
			buffers = Arrays.copyOf(buffers, ns);
		}
		
		if(buffers[index]==null) {
			long offset = pageSize * index;
			buffers[index] = channel.map(MapMode.READ_WRITE,offset,pageSize+PAGE_OVERLAP);
			for (int i=0; i<index; i++) {
				if (buffers[i]!=null) {
					WeakReference<MappedByteBuffer> r = new WeakReference<MappedByteBuffer>((MappedByteBuffer) buffers[i]);
					buffers[i] = null;
					FileUtils.unmap(r);
				}
			}
		}

		
		
		return buffers[index];
	}

	@Override
	public BinaryWriter putString(long position, CharSequence line)
			throws IOException {
		if (line==null) {
			putInt(position,-1);
			position+=Integer.BYTES;
		} else {
			putInt(position, line.length());
			position+=Integer.BYTES;
			for (int i=0; i<line.length(); i++) {
				putAsciiChar(position,line.charAt(i));
				position+=Byte.BYTES;
			}
		}
		return this;
	}

}
