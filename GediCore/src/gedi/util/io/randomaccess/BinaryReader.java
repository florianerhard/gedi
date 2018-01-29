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

import java.io.IOException;

public interface BinaryReader {

	long position() throws IOException;
	long position(long position) throws IOException;

	short getShort() throws IOException;

	long getLong() throws IOException;

	int getInt() throws IOException;

	float getFloat() throws IOException;

	double getDouble() throws IOException;

	String getString() throws IOException;

	StringBuilder getString(StringBuilder re) throws IOException;

	char getAsciiChar() throws IOException;

	default String getAsciiChars(int l)
			throws IOException {
		char[] re = new char[l];
		for (int i=0; i<re.length; i++)
			re[i] = getAsciiChar();
		return String.valueOf(re);
	}

	char getChar() throws IOException;

	int getByte() throws IOException;

	byte get() throws IOException;
	
	boolean eof();
	 
	ExtensionContext getContext();

	BinaryReader get(byte[] dst, int offset, int length)
			throws IOException;

	default byte[] getBuffer(byte[] dst)
			throws IOException {
		get(dst,0,dst.length);
		return dst;
	}


	short getShort(long position) throws IOException;

	long getLong(long position) throws IOException;

	int getInt(long position) throws IOException;

	float getFloat(long position) throws IOException;

	double getDouble(long position) throws IOException;

	String getString(long position) throws IOException;

	StringBuilder getString(long position, StringBuilder re) throws IOException;

	char getAsciiChar(long position) throws IOException;

	char getChar(long position) throws IOException;

	int getByte(long position) throws IOException;

	byte get(long position) throws IOException;

	BinaryReader get(long position, byte[] dst, int offset, int length)
			throws IOException;

	static final int SHORT_MASK = 127;
	static final int INTLONG_MASK = 63;
	default short getCShort()  throws IOException{
		int b = getByte();
		int c = (b>>7)&1;
		if (c==0) return (short)b;
		return (short) ((b&SHORT_MASK)<<8 | getByte());
	}

	default int getCInt()  throws IOException{
		int b = getByte();
		int c = (b>>6)&3;
		if (c==0) return b;
		if (c==1) return (b&INTLONG_MASK)<<8 | getByte();
		return (b&INTLONG_MASK)<<24 |
				getByte()<<16 |
				getByte()<<8 |
				getByte();
	}

	default long getCLong()  throws IOException{
		int b = getByte();
		int c = (b>>6)&3;
		if (c==0) return b;
		if (c==1) return (b&INTLONG_MASK)<<8 | getByte();
		if (c==2) return (b&INTLONG_MASK)<<24 |
				getByte()<<16 |
				getByte()<<8 |
				getByte();
		long l = b;
		return (l&INTLONG_MASK)<<56 |
				((long)getByte())<<48 |
				((long)getByte())<<40 |
				((long)getByte())<<32 |
				((long)getByte())<<24 |
				((long)getByte())<<16 |
				((long)getByte())<<8 |
				getByte();
	}


}
