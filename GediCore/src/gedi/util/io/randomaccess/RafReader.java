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
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.io.text.LineReader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.function.Supplier;

public class RafReader implements BinaryReader, LineReader, AutoCloseable {

	
	private RandomAccessFile file;

	private String path;
	long size;
	long offset;
	
	private ExtensionContext context;
	/**
	 * From its start to its max length!
	 * @param writerToReadFrom
	 * @throws IOException
	 */
	public RafReader(PageFileWriter writerToReadFrom) throws IOException  {
		this.path = writerToReadFrom.getPath();
		file = new RandomAccessFile(path, "r");
		size = writerToReadFrom.getMaxLength()-offset;
	}
	
	
	
	public RafReader(String path) throws IOException  {
		this.path = path;
		file = new RandomAccessFile(path, "r");
		size = file.length()-offset;
	}
	
	public RafReader(String path, long start, long end) throws IOException  {
		this.path = path;
		this.offset = start;
		file = new RandomAccessFile(path, "r");
		file.seek(offset);
		size = end-start;
	}
	
	
	public String getPath() {
		return path;
	}
	
	public RafReader view(long start, long end) {
		try {
			return new RafReader(path, start, end);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public ExtensionContext getContext() {
		if (context==null) context = new ExtensionContext();
		return context;
	}
	
	
	public <T extends BinarySerializable>  ExtendedIterator<T> iterator(Supplier<T> supplier) {
		return new ExtendedIterator<T>() {

			@Override
			public boolean hasNext() {
				return !eof();
			}

			@Override
			public T next() {
				T n = supplier.get();
				try {
					n.deserialize(RafReader.this);
				} catch (IOException e) {
					throw new RuntimeException("Could not iterate objects in "+RafReader.this.getPath());
				}
				return n;
			}
			
		};
	}
	
	@Override
	public long getStart() {
		return offset;
	}
	
	@Override
	public long getEnd() {
		return offset+size();
	}
	
	public long size() {
		return size;
	}
	
	/**
	 * Refers to logical position, i.e. if start>0, a position of 0 corresponds to a filepointer of start
	 * @return
	 */
	public long position() {
		try {
			return file.getFilePointer()-offset;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public long relativePosition(long increment) {
		long position = position();
		if (position+increment<0||position+increment>size) 
			throw new IndexOutOfBoundsException("0<="+position+"+"+increment+"<"+(size));
		try {
			file.seek(position+increment);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return position+increment;
	}
	
	public long position(long position) {
		if (position<0||position>size) 
			throw new IndexOutOfBoundsException("0<="+position+"<"+(size));
		try {
			file.seek(position+offset);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return position;
	}
	
	public void close() {
		try {
			file.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public RafReader get(long pos, byte[] dst, int offset, int length) throws IOException {
		position(pos);
		file.read(dst, offset, length);
        return this;
    }
	
	public RafReader get(byte[] dst, int offset, int length) throws IOException {
       file.read(dst,offset,length);
       return this;
    }
	
	public byte get(long pos)
			throws IOException {
		position(pos);
		return file.readByte();
	}

	public byte get()
			throws IOException {
		return file.readByte();
	}
	
	public int getByte(long pos)
			throws IOException {
		position(pos);
		return file.read();
	}

	public int getByte()
			throws IOException {
		return file.read();
	}

	public char getChar(long pos)
			throws IndexOutOfBoundsException, IOException {
		position(pos);
		return file.readChar();
	}

	public char getChar()
			throws IOException {
		return file.readChar();
	}
	
	public char getAsciiChar(long pos)
			throws IOException {
		position(pos);
		return (char)file.readByte();
	}

	public char getAsciiChar()
			throws IOException {
		return (char)file.readByte();
	}
	
	
	public String readLine() throws IOException {
		return file.readLine();
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
		position(pos);
		return file.readDouble();
	}

	public double getDouble()
			throws IOException {
		return file.readDouble();
	}


	public float getFloat(long pos)
			throws IOException {
		position(pos);
		return file.readFloat();
	}

	public float getFloat()
			throws IOException {
		return file.readFloat();
	}

	public int getInt(long pos)
			throws IOException {
		position(pos);
		return file.readInt();
	}

	public int getInt()
			throws IOException {
		return file.readInt();
	}


	public long getLong(long pos)
			throws IOException {
		position(pos);
		return file.readLong();
	}

	public long getLong()
			throws IOException {
		return file.readLong();
	}


	public short getShort(long pos)
			throws IOException {
		position(pos);
		return file.readShort();
	}

	public short getShort()
			throws IOException {
		return file.readShort();
	}


	

	public boolean eof() {
		return position()>=size;
	}

	@Override
	public String toString() {
		return path;
	}

	public ExtendedIterator<RafReader> ei() {
		return new ExtendedIterator<RafReader>() {

			@Override
			public boolean hasNext() {
				return !eof();
			}

			@Override
			public RafReader next() {
				return RafReader.this;
			}
			
		};
	}
	
	
}
