package gedi.util.io.randomaccess;

import gedi.app.extension.ExtensionContext;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.io.text.LineReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PageFileView implements BinaryReader, LineReader {

	private PageFile file;
	private long start;
	private long end;

	private long position=0; // relative to start-end!
	
	public PageFileView(PageFile file)   {
		this.file = file;
		this.start = 0;
		this.end = file.size();
	}
	
	public PageFileView(PageFileView fileView)  {
		this.file = fileView.getFile();
		this.start = fileView.start;
		this.end = fileView.end;
	}
	
	public PageFileView(PageFile file, long start, long end)   {
		this.file = file;
		this.start = start;
		this.end = end;
	}

	@Override
	public ExtensionContext getContext() {
		return file.getContext();
	}
	
	public PageFile getFile() {
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
					n.deserialize(PageFileView.this);
				} catch (IOException e) {
					throw new RuntimeException("Could not iterate objects in "+getFile().getPath()+"("+getStart()+"-"+getEnd()+")");
				}
				return n;
			}
			
		};
	}
	
	public <T>  ExtendedIterator<T> iterator(Function<BinaryReader,T> supplier) {
		return new ExtendedIterator<T>() {

			@Override
			public boolean hasNext() {
				return !eof();
			}

			@Override
			public T next() {
				return supplier.apply(PageFileView.this);
			}
			
		};
	}
	
	/**
	 * Refers to logical position, i.e. if start>0, a position of 0 corresponds to a filepointer of start
	 * @return
	 */
	public long position() {
		return position;
	}
	
	public long relativePosition(long increment) {
		if (position+increment<0||position+increment>end-start) 
			throw new IndexOutOfBoundsException("0<="+position+"+"+increment+"<"+(end-start));
		this.position += increment; 
		return position();
	}
	
	public long position(long position) {
		if (position<0||position>end-start) 
			throw new IndexOutOfBoundsException("0<="+position+"<"+(end-start));
		this.position = position; 
		return position();
	}
	

	public PageFileView get(long pos, byte[] dst, int offset, int length) throws IOException {
		int sindex = getIndex(pos);
		int eindex = getIndex(pos+length-1);
		int boffset = getOffset(pos);
		
		for (int index=sindex; index<=eindex; index++) {
			ByteBuffer buffer = file.getBuffer(index);
			buffer.position(boffset);
			int l = Math.min(length, (int)(PageFile.pageSize-boffset));//(int)(buffer.remaining()-PageFile.PAGE_OVERLAP-offset));
			buffer.get(dst, offset, l);
			length-=l;
			offset+=l;
			boffset=0;
		}
        return this;
        
    }
	
	public PageFileView get(byte[] dst, int offset, int length) throws IOException {
       get(position,dst,offset,length);
       position+=Byte.BYTES*length;
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
		byte val = get(position);
		position += Byte.BYTES;
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
		byte val = get(position);
		position += Byte.BYTES;
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
		char val = getChar(position);
		position += Character.BYTES;
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
		StringBuilder s = new StringBuilder();
		char c = '\0';
		while (position<end && (c=getAsciiChar())!='\r' && c!='\n') 
			s.append(c);
		if (c=='\r' && position<end && (c=getAsciiChar())!='\n')
			position-=Byte.BYTES;
		return position==end && s.length()==0?null:s.toString(); 
	}

	public int readLine(char[] buffer) throws IOException {
		char c = '\0';
		int ind = 0;
		while (ind<buffer.length && position<end && (c=getAsciiChar())!='\r' && c!='\n') 
			buffer[ind++] = c;
		if (c=='\r' && position<end && (c=getAsciiChar())!='\n')
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
		double val = getDouble(position);
		position += Double.BYTES;
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
		float val = getFloat(position);
		position += Float.BYTES;
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
		int val = getInt(position);
		position += Integer.BYTES;
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
		long val = getLong(position);
		position += Long.BYTES;
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
		short val = getShort(position);
		position += Short.BYTES;
		return val;
	}


	private int getOffset(long pos) {
		if (pos<0||pos>=end-start) throw new IndexOutOfBoundsException(pos+"<0 or >="+(end-start));
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
	
}
