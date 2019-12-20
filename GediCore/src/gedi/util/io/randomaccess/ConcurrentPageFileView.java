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
import gedi.util.io.text.LineReader;
import gedi.util.mutable.MutableLong;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ConcurrentPageFileView implements BinaryReader, LineReader {

	private ConcurrentPageFile file;
	private long start;
	private long end;

	private ThreadLocal<MutableLong> position = ThreadLocal.withInitial(()->new MutableLong());
	//	private long position=0; // relative to start-end!

	public ConcurrentPageFileView(ConcurrentPageFile file) throws IOException  {
		this.file = file;
		this.start = 0;
		this.end = file.size();
	}

	public ConcurrentPageFileView(ConcurrentPageFileView fileView) throws IOException  {
		this.file = fileView.getFile();
		this.start = fileView.start;
		this.end = fileView.end;
	}

	public ConcurrentPageFileView(ConcurrentPageFile file, long start, long end) {
		this.file = file;
		this.start = start;
		this.end = end;
	}
	
	@Override
	public ExtensionContext getContext() {
		return file.getContext();
	}
	

	public ConcurrentPageFile getFile() {
		return file;
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	public long size() {
		return end-start;
	}


	/**
	 * Refers to logical position, i.e. if start>0, a position of 0 corresponds to a filepointer of start
	 * @return
	 */
	public long position() {
		return position.get().N;
	}

	public long relativePosition(long increment) {
		MutableLong position = this.position.get();
		if (position.N+increment<0||position.N+increment>end-start) 
			throw new IndexOutOfBoundsException("0<="+position+"+"+increment+"<"+(end-start));
		position.N += increment; 
		return position.N;
	}

	public long position(long position) {
		if (position<0||position>end-start) 
			throw new IndexOutOfBoundsException("0<="+position+"<"+(end-start));
		this.position.get().N = position;
		return position;
	}


	public ConcurrentPageFileView get(long pos, byte[] dst, int offset, int length) throws IOException {
		int sindex = getIndex(pos);
		int eindex = getIndex(pos+length-1);
		int boffset = getOffset(pos);

		synchronized (file) {
			// this must be synchronized, as the position may be overwritten!
			for (int index=sindex; index<=eindex; index++) {
				ByteBuffer buffer = file.getBuffer(index);
				buffer.position(boffset);
	//			int l = Math.min(length, buffer.capacity()-offset);
				int l = Math.min(length, (int)(ConcurrentPageFile.pageSize-boffset));// (int)(buffer.remaining()-PAGE_OVERLAP-offset));
				buffer.get(dst, offset, l);
				length-=l;
				offset+=l;
				boffset=0;
			}
		}
		return this;
	}

	public ConcurrentPageFileView get(byte[] dst, int offset, int length) throws IOException {
		MutableLong position = this.position.get();
		get(position.N,dst,offset,length);
		position.N+=Byte.BYTES*length;
		return this;
	}

	public byte get(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = file.getBuffer(index);
		return buffer.get(offset);
	}

	public byte get()
			throws IOException {
		MutableLong position = this.position.get();
		byte val = get(position.N);
		position.N += Byte.BYTES;
		return val;
	}

	public int getByte(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = file.getBuffer(index);
		return ((int)buffer.get(offset))&255;
	}

	public int getByte()
			throws IOException {
		MutableLong position = this.position.get();
		byte val = get(position.N);
		position.N += Byte.BYTES;
		return ((int)val) & 0xFF;
	}

	public char getChar(long pos)
			throws IndexOutOfBoundsException, IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = file.getBuffer(index);
		return buffer.getChar(offset);
	}

	public char getChar()
			throws IOException {
		MutableLong position = this.position.get();
		char val = getChar(position.N);
		position.N += Character.BYTES;
		return val;
	}

	public char getAsciiChar(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = file.getBuffer(index);
		return (char) buffer.get(offset);
	}

	public char getAsciiChar()
			throws IOException {
		MutableLong position = this.position.get();
		char val = getAsciiChar(position.N);
		position.N += Byte.BYTES;
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
		MutableLong position = this.position.get();
		StringBuilder s = new StringBuilder();
		char c = '\0';
		while (position.N<end && (c=getAsciiChar())!='\r' && c!='\n') 
			s.append(c);
		if (c=='\r' && position.N<end && (c=getAsciiChar())!='\n')
			position.N-=Byte.BYTES;
		return position.N==end && s.length()==0?null:s.toString(); 
	}

	public int readLine(char[] buffer) throws IOException {
		MutableLong position = this.position.get();
		char c = '\0';
		int ind = 0;
		while (ind<buffer.length && position.N<end && (c=getAsciiChar())!='\r' && c!='\n') 
			buffer[ind++] = c;
		if (c=='\r' && position.N<end && (c=getAsciiChar())!='\n')
			position.N-=Byte.BYTES;
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
		return getString(pos,new StringBuilder()).toString();
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
		return getString(new StringBuilder()).toString();
	}

	public double getDouble(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = file.getBuffer(index);
		return buffer.getDouble(offset);
	}

	public double getDouble()
			throws IOException {
		MutableLong position = this.position.get();
		double val = getDouble(position.N);
		position.N += Double.BYTES;
		return val;
	}


	public float getFloat(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = file.getBuffer(index);
		return buffer.getFloat(offset);
	}

	public float getFloat()
			throws IOException {
		MutableLong position = this.position.get();
		float val = getFloat(position.N);
		position.N += Float.BYTES;
		return val;
	}

	public int getInt(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = file.getBuffer(index);
		return buffer.getInt(offset);
	}

	public int getInt()
			throws IOException {
		MutableLong position = this.position.get();
		int val = getInt(position.N);
		position.N += Integer.BYTES;
		return val;
	}


	public long getLong(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = file.getBuffer(index);
		return buffer.getLong(offset);
	}

	public long getLong()
			throws IOException {
		MutableLong position = this.position.get();
		long val = getLong(position.N);
		position.N += Long.BYTES;
		return val;
	}


	public short getShort(long pos)
			throws IOException {
		int offset = getOffset(pos);
		int index = getIndex(pos);

		ByteBuffer buffer = file.getBuffer(index);
		return buffer.getShort(offset);
	}

	public short getShort()
			throws IOException {
		MutableLong position = this.position.get();
		short val = getShort(position.N);
		position.N += Short.BYTES;
		return val;
	}


	private int getOffset(long pos) {
		if (pos<0||pos>=end-start) 
			throw new IndexOutOfBoundsException(pos+"<0 or >="+(end-start));
		return (int) ((start+pos) % file.pageSize);
	}

	private int getIndex(long pos) {
		return (int) ((start+pos) / (long) file.pageSize);
	}



	public boolean eof() {
		return position()>=end-start;
	}

	@Override
	public String toString() {
		return file+" "+getStart()+"-"+getEnd();
	}

	@Override
	public void close() {
	}

	@Override
	public BinaryReader view(long start, long end) {
		return new ConcurrentPageFileView(file,this.start+start,this.start+end);
	}

}
