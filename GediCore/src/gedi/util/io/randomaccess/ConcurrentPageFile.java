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
import gedi.util.mutable.MutableInteger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class ConcurrentPageFile implements BinaryReader, LineReader, AutoCloseable {

	static final long PAGE_OVERLAP = Long.BYTES;
	
	private RandomAccessFile file;
	final FileChannel channel;

	private long position=0; // relative to start-end!

	private MutableInteger[] working;
	private ReentrantLock[] locks;
	private ThreadLocal<MutableInteger> bufferIndex = new ThreadLocal<MutableInteger>();
	
	private ByteBuffer[] buffers;
	
	static long pageSize = Integer.MAX_VALUE - PAGE_OVERLAP;

	private ByteBuffer mem;

	private String path;
	
	long size;
	
	private boolean unmap = true;
	
	private ExtensionContext context;

//	public PageFile(PageFile file) throws IOException  {
//		this(file.getPath(),file.getStart(),file.getEnd());
//	}
	
	/**
	 * From its start to its max length!
	 * @param writerToReadFrom
	 * @throws IOException
	 */
	public ConcurrentPageFile(PageFileWriter writerToReadFrom) throws IOException  {
		this.path = writerToReadFrom.getPath();
		file = new RandomAccessFile(path, "r");
		this.channel = file.getChannel();
//		start = writerToReadFrom.getStart();
//		end = writerToReadFrom.position();
		size = writerToReadFrom.getMaxLength();
		
		buffers = new ByteBuffer[(int) Math.ceil(size()/(double)pageSize)];
		working = new MutableInteger[buffers.length];
		for (int i=0; i<working.length; i++)
			working[i] = new MutableInteger();
		locks = new ReentrantLock[buffers.length];
		for (int i=0; i<working.length; i++)
			locks[i] = new ReentrantLock();
//		System.err.println("started reading "+getPath());
	}
	
	
	
	public ConcurrentPageFile(String path) throws IOException  {
		this.path = path;
		file = new RandomAccessFile(path, "r");
		this.channel = file.getChannel();
		size = file.length();
//		start = 0;
//		end = file.length();
		buffers = new ByteBuffer[(int) Math.ceil(size()/(double)pageSize)];
		working = new MutableInteger[buffers.length];
		for (int i=0; i<working.length; i++)
			working[i] = new MutableInteger();
		locks = new ReentrantLock[buffers.length];
		for (int i=0; i<working.length; i++)
			locks[i] = new ReentrantLock();
//		System.err.println("started reading "+getPath());
	}
	
//	private PageFile(String path, long start, long end) throws IOException  {
//		this.path = path;
//		file = new RandomAccessFile(path, "r");
//		if (end>file.length()) throw new IOException("Cannot read out of bounds of file!");
//		this.channel = file.getChannel();
//		this.start = start;
//		this.end = end;
////		buffers = new ByteBuffer[(int) Math.ceil(size()/(double)pageSize)];
//	}
	
	
	public boolean isUnmap() {
		return unmap;
	}
	
	/**
	 * Set whether other buffers are immediately unmapped when a different page is requested; even if threaded, constantly mapping and unmapping
	 * may be a huge performance bottleneck
	 * @param unmap
	 */
	public void setUnmap(boolean unmap) {
		this.unmap = unmap;
	}
	
	public String getPath() {
		return path;
	}
	
	public ConcurrentPageFileView view(long start, long end) {
		return new ConcurrentPageFileView(this, start, end);
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
					n.deserialize(ConcurrentPageFile.this);
				} catch (IOException e) {
					throw new RuntimeException("Could not iterate objects in "+ConcurrentPageFile.this.getPath());
				}
				return n;
			}
			
		};
	}
	
//	public long getStart() {
//		return start;
//	}
//	
//	public long getEnd() {
//		return end;
//	}
	
	public long size() {
		return size;
	}
	
	/**
	 * Refers to logical position, i.e. if start>0, a position of 0 corresponds to a filepointer of start
	 * @return
	 */
	public long position() {
		return position;
	}
	
	public long relativePosition(long increment) {
		if (position+increment<0||position+increment>size) 
			throw new IndexOutOfBoundsException("0<="+position+"+"+increment+"<"+(size));
		this.position += increment; 
		return position();
	}
	
	public long position(long position) {
		if (position<0||position>size) 
			throw new IndexOutOfBoundsException("0<="+position+"<"+(size));
		this.position = position; 
		return position();
	}
	
	public void close() {
		for (int i=0; i<buffers.length; i++) {
			if (buffers[i]!=null) {
				WeakReference<MappedByteBuffer> r = new WeakReference<MappedByteBuffer>((MappedByteBuffer) buffers[i]);
				buffers[i] = null;
				FileUtils.unmap(r);
			}
		}
		try {
			channel.close();
			file.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
//		System.err.println("fininshed reading "+getPath());
	}

	public ConcurrentPageFile get(long pos, byte[] dst, int offset, int length) throws IOException {
		int sindex = getIndex(pos);
		int eindex = getIndex(pos+length-1);
		int boffset = getOffset(pos);
		
		for (int index=sindex; index<=eindex; index++) {
			ByteBuffer buffer = getBuffer(index);
			buffer.position(boffset);
			int l = Math.min(length, (int)(ConcurrentPageFile.pageSize-boffset));// (int)(buffer.remaining()-PAGE_OVERLAP-offset));
			buffer.get(dst, offset, l);
			length-=l;
			offset+=l;
			boffset=0;
		}
        return this;
    }
	
	public ConcurrentPageFile get(byte[] dst, int offset, int length) throws IOException {
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
	
	
	public String readLine() throws IOException {
		StringBuilder s = new StringBuilder(80);
		
		char c = '\0';
		while (position<size && (c=getAsciiChar())!='\r' && c!='\n') 
			s.append(c);
		if (c=='\r' && position<size && (c=getAsciiChar())!='\n')
			position-=Byte.BYTES;
		return position==size && s.length()==0?null:s.toString(); 
	}

	public int readLine(char[] buffer) throws IOException {
		char c = '\0';
		int ind = 0;
		while (ind<buffer.length && position<size && (c=getAsciiChar())!='\r' && c!='\n') 
			buffer[ind++] = c;
		if (c=='\r' && position<size && (c=getAsciiChar())!='\n')
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


	private int getOffset(long pos) {
		if (mem!=null) return (int) pos;
		if (pos<0||pos>=size) throw new IndexOutOfBoundsException(pos+"<0 or >="+(size));
		return (int) ((pos) % pageSize);
	}

	private int getIndex(long pos) {
		if (mem!=null) return 0;
		return (int) ((pos) / (long) pageSize);
	}

	ByteBuffer getBuffer(int index)
			throws IOException {
//		if(buffers[index]==null) {
//			long offset = pageSize * index;
//			buffers[index] = channel.map(MapMode.READ_ONLY,offset,Math.min(size-offset,pageSize+PAGE_OVERLAP));
//		}
//
//		return buffers[index];
		
		
		ByteBuffer buf = buffers[index];
		
		
		if (buf==null) {
			
			if (bufferIndex.get()==null) bufferIndex.set(new MutableInteger(-1));
			int bi = bufferIndex.get().N;
			
			if (bi!=-1 && buffers[bi]!=null) {
				locks[bi].lock();
				
				if (--working[bi].N==0 && buffers[bi]!=null) {
					if (unmap) {
						WeakReference<MappedByteBuffer> r = new WeakReference<MappedByteBuffer>((MappedByteBuffer) buffers[bi]);
						buffers[bi] = null;
						FileUtils.unmap(r);
					} else {
						buffers[bi] = null;
					}
				}
				locks[bi].unlock();
			}
			
			locks[index].lock();
			
			working[index].N++;
			buf = buffers[index];
			if (buf==null) {
				long offset = pageSize * index;
//				System.err.printf("Creating buffer with index %d at %d len %d in %s\n",index,offset,Math.min(size-offset,pageSize+PAGE_OVERLAP),this.toString());
				buf = buffers[index] = channel.map(MapMode.READ_ONLY,offset,Math.min(size-offset,pageSize+PAGE_OVERLAP));
				bufferIndex.get().N = index;
			}
			
			
			locks[index].unlock();
		}
		
		return buf;
	}
	

	public boolean eof() {
		return position()>=size;
	}

	@Override
	public String toString() {
		return path;
	}


	
	
}
