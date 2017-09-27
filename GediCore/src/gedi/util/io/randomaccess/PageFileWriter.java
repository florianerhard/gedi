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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.util.Arrays;

public class PageFileWriter implements AutoCloseable, BinaryWriter {

	private static final long PAGE_OVERLAP = Long.BYTES;
	
	protected RandomAccessFile file;
	protected FileChannel channel;
	protected long start;

	protected long position=0; // relative to start-end!
	protected ByteBuffer[] buffers = new ByteBuffer[1];
	
	protected long pageSize = 1<<28 ;//- PAGE_OVERLAP;

	protected FileLock lock;
	
	protected String path;
	protected long maxLength = 0;

	private ExtensionContext context;

	
	public PageFileWriter(String path) throws IOException  {
		this.path = path;
		file = new RandomAccessFile(path, "rw");
		this.channel = file.getChannel();
		lock = channel.lock();
		start = 0;
//		System.err.println("started writing "+getPath());
	}
	
	public PageFileWriter(String path, long start) throws IOException  {
		this.path = path;
		file = new RandomAccessFile(path, "rw");
		this.channel = file.getChannel();
		lock = channel.lock();
		this.start = start;
//		System.err.println("started writing "+getPath());
	}
	
	public void setPageSize(long pageSize) {
		this.pageSize = pageSize;
	}
	
	public long getPageSize() {
		return pageSize;
	}
	
	@Override
	public ExtensionContext getContext() {
		if (context==null) context = new ExtensionContext();
		return context;
	}
	
	public void open() throws IOException {
		file = new RandomAccessFile(path, "rw");
		this.channel = file.getChannel();
		lock = channel.lock();
	}
	
	public long getMaxLength() {
		return maxLength;
	}
	
	public String getPath() {
		return path;
	}
	
	public long getStart() {
		return start;
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
	
	public void truncate() throws IOException {
		file.setLength(maxLength);
		
		for (int i=0; i<buffers.length; i++) {
			if (buffers[i]!=null) {
				WeakReference<MappedByteBuffer> r = new WeakReference<MappedByteBuffer>((MappedByteBuffer) buffers[i]);
				buffers[i] = null;
				FileUtils.unmapSynchronous(r);
			}
		}
	}
	
	public void close() throws IOException {
		if (!isClosed()) {
			for (int i=0; i<buffers.length; i++) {
				if (buffers[i]!=null) {
					WeakReference<MappedByteBuffer> r = new WeakReference<MappedByteBuffer>((MappedByteBuffer) buffers[i]);
					buffers[i] = null;
					FileUtils.unmapSynchronous(r);
				}
			}
			file.setLength(maxLength);
			lock.release();
			lock = null;
			channel.close();
			file.close();
//			System.err.println("finished writing "+getPath());
		}
	}
	
	public boolean isClosed() {
		return lock==null;
	}

	public PageFileWriter put(long pos, byte[] dst, int offset, int length) throws IOException {
		int ol = length;
		int sindex = getIndex(pos);
		int eindex = getIndex(pos+length-1);
		int boffset = getOffset(pos);
		
		for (int index=sindex; index<=eindex; index++) {
			ByteBuffer buffer = getBuffer(index);
			buffer.position(boffset);
			int l = Math.min(length, (int)(pageSize-boffset));
			buffer.put(dst, offset, l);
			length-=l;
			offset+=l;
			boffset=0;
		}
		maxLength = Math.max(maxLength,pos+Byte.BYTES*ol);
        return this;
    }
	
	public PageFileWriter put(byte[] dst, int offset, int length) throws IOException {
       put(position,dst,offset,length);
       position+=Byte.BYTES*length;
       return this;
    }
	
	public PageFileWriter put(long pos, byte data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.put(offset,data);
		maxLength = Math.max(maxLength,pos+Byte.BYTES);
		return this;
	}

	public PageFileWriter put(byte data)
			throws IOException {
		put(position,data);
		position += Byte.BYTES;
		return this;
	}
	
	public PageFileWriter putByte(long pos, int data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.put(offset,(byte)data);
		maxLength = Math.max(maxLength,pos+Byte.BYTES);
		return this;
	}

	public PageFileWriter putByte(int data)
			throws IOException {
		putByte(position,data);
		position += Byte.BYTES;
		return this;
	}


	public PageFileWriter putChar(long pos, char data)
			throws IndexOutOfBoundsException, IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putChar(offset,data);
		maxLength = Math.max(maxLength,pos+Character.BYTES);
		return this;
	}

	public PageFileWriter putChar(char data)
			throws IOException {
		putChar(position,data);
		position += Character.BYTES;
		return this;
	}
	
	public PageFileWriter putAsciiChar(long pos, char data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.put(offset,(byte)data);
		maxLength = Math.max(maxLength,pos+Byte.BYTES);
		return this;
	}

	public PageFileWriter putAsciiChar(char data)
			throws IOException {
		putAsciiChar(position,data);
		position += Byte.BYTES;
		return this;
	}
	
	public PageFileWriter putChars(long pos, CharSequence data)
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

	public PageFileWriter putChars(CharSequence data)
			throws IOException {
		putChars(position,data);
		position += Character.BYTES*data.length();
		return this;
	}
	
	public PageFileWriter putAsciiChars(long pos, CharSequence data)
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

	public PageFileWriter putAsciiChars(CharSequence data)
			throws IOException {
		putAsciiChars(position,data);
		position += Byte.BYTES*data.length();
		return this;
	}
	
	public PageFileWriter putString(CharSequence line) throws IOException {
		if (line==null)
			putInt(-1);
		else {
			putInt(line.length());
			for (int i=0; i<line.length(); i++)
				putAsciiChar(line.charAt(i));
		}
		return this;
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


	
	public PageFileWriter writeLine(CharSequence line) throws IOException {
		for (int i=0; i<line.length(); i++)
			putAsciiChar(line.charAt(i));
		putAsciiChar('\n');
		return this;
	}
	
	public PageFileWriter writeLine(char[] line) throws IOException {
		for (int i=0; i<line.length; i++)
			putAsciiChar(line[i]);
		putAsciiChar('\n');
		return this;
	}

	public PageFileWriter putDouble(long pos, double data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putDouble(offset,data);
		maxLength = Math.max(maxLength,pos+Double.BYTES);
		return this;
	}

	public PageFileWriter putDouble(double data)
			throws IOException {
		putDouble(position,data);
		position += Double.BYTES;
		return this;
	}


	public PageFileWriter putFloat(long pos,float data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putFloat(offset,data);
		maxLength = Math.max(maxLength,pos+Float.BYTES);
		return this;
	}

	public PageFileWriter putFloat(float data)
			throws IOException {
		putFloat(position,data);
		position += Float.BYTES;
		return this;
	}

	public PageFileWriter putInt(long pos, int data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putInt(offset,data);
		maxLength = Math.max(maxLength,pos+Integer.BYTES);
		return this;
	}

	public PageFileWriter putInt(int data)
			throws IOException {
		putInt(position,data);
		position += Integer.BYTES;
		return this;
	}


	public PageFileWriter putLong(long pos, long data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putLong(offset,data);
		maxLength = Math.max(maxLength,pos+Long.BYTES);
		return this;
	}

	public PageFileWriter putLong(long data)
			throws IOException {
		putLong(position,data);
		position += Long.BYTES;
		return this;
	}


	public PageFileWriter putShort(long pos,short data)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = getBuffer(index);
		buffer.putShort(offset,data);
		maxLength = Math.max(maxLength,pos+Short.BYTES);
		return this;
	}

	public PageFileWriter putShort(short data)
			throws IOException {
		putShort(position,data);
		position += Short.BYTES;
		return this;
	}


	protected int getOffset(long pos) {
		if (pos<0) throw new IndexOutOfBoundsException();
		return (int) ((start+pos) % pageSize);
	}

	protected int getIndex(long pos) {
		return (int) ((start+pos) / (long) pageSize);
	}

	
	protected ByteBuffer getBuffer(int index)
			throws IOException {
		
		if (index>=buffers.length) {
			int ns = buffers.length;
			for (;index>=ns; ns*=2);
			buffers = Arrays.copyOf(buffers, ns);
		}
		
		if(buffers[index]==null) {
			long offset = pageSize * index;
			file.setLength(Math.max(file.length(),offset+pageSize+PAGE_OVERLAP ));
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

	public PageFileWriter createTempWriter(String suffix) throws IOException {
		File f = File.createTempFile(new File(path).getName(), suffix, new File(path).getParentFile());
		f.deleteOnExit();
		return new PageFileWriter(f.getPath());
	}

	public PageFile read(boolean close) throws IOException {
		if (close) close();
		return new PageFile(this);
	}
	
	public ConcurrentPageFile readConcurrently(boolean close) throws IOException {
		if (close) close();
		return new ConcurrentPageFile(this);
	}


	public File getFile() {
		return new File(path);
	}


}
