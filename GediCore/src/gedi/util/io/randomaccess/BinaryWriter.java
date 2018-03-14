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

public interface BinaryWriter {
	
	
	ExtensionContext getContext();
	
	long position() throws IOException;
	long position(long position) throws IOException;
	
	
	BinaryWriter putShort(short data) throws IOException;

	BinaryWriter putLong(long data) throws IOException;

	BinaryWriter putInt(int data) throws IOException;

	BinaryWriter putFloat(float data) throws IOException;

	BinaryWriter putDouble(double data) throws IOException;

	default BinaryWriter putFloat(double data) throws IOException {
		return putFloat((float)data);
	}

	/**
	 * Writes first the string length and then the chars (as ascii)
	 * @param line
	 * @return
	 * @throws IOException
	 */
	BinaryWriter putString(CharSequence line) throws IOException;

	BinaryWriter putAsciiChars(CharSequence data) throws IOException;

	BinaryWriter putChars(CharSequence data) throws IOException;

	BinaryWriter putAsciiChar(char data) throws IOException;

	BinaryWriter putChar(char data) throws IOException;

	BinaryWriter putByte(int data) throws IOException;

	BinaryWriter put(byte data) throws IOException;

	BinaryWriter put(byte[] dst, int offset, int length)
			throws IOException;
	
	
	BinaryWriter putShort(long position, short data) throws IOException;

	BinaryWriter putLong(long position, long data) throws IOException;

	BinaryWriter putInt(long position, int data) throws IOException;

	BinaryWriter putFloat(long position, float data) throws IOException;

	BinaryWriter putDouble(long position, double data) throws IOException;

	/**
	 * Writes first the string length and then the chars (as ascii)
	 * @param line
	 * @return
	 * @throws IOException
	 */
	BinaryWriter putString(long position, CharSequence line) throws IOException;

	BinaryWriter putAsciiChars(long position, CharSequence data) throws IOException;

	BinaryWriter putChars(long position, CharSequence data) throws IOException;

	BinaryWriter putAsciiChar(long position, char data) throws IOException;

	BinaryWriter putChar(long position, char data) throws IOException;

	BinaryWriter putByte(long position, int data) throws IOException;

	BinaryWriter put(long position, byte data) throws IOException;

	BinaryWriter put(long position, byte[] dst, int offset, int length)
			throws IOException;
	
	
	/**
	 * Must be >0!
	 * @param data
	 * @return
	 * @throws IOException
	 */
	 default BinaryWriter putCShort(short data)  throws IOException{
		 if (data>Short.MAX_VALUE || data<0) 
			 throw new IndexOutOfBoundsException(data+" should be >=0 and <="+Short.MAX_VALUE/2);
		 if (data>Byte.MAX_VALUE) {
			 putByte(1<<7|data>>8);
			 putByte(data&0xFF);
//			 System.out.print(" "+data+"(CShort:2)");
		 }else {
			 putByte(data);
//			 System.out.print(" "+data+"(CShort:1)");
		 }
		 return this;
	 }
	 
	 default BinaryWriter putCInt(int data)  throws IOException{
		 if (data>Integer.MAX_VALUE/2 || data<0) 
			 throw new IndexOutOfBoundsException(data+" should be >=0 and <="+Integer.MAX_VALUE/2);
		 if (data>Short.MAX_VALUE/2) {
			 putByte(2<<6|data>>24);
			 putByte(data>>16 &0xFF);
			 putByte(data>>8 &0xFF);
			 putByte(data&0xFF);
//			 System.out.print(" "+data+"(CInt:3)");
		 }else if (data>Byte.MAX_VALUE/2) {
			 putByte(1<<6|data>>8);
			 putByte(data&0xFF);
//			 System.out.print(" "+data+"(CInt:2)");
		 }else {
			 putByte(data);
//			 System.out.print(" "+data+"(CInt:1)");
		 }
		 return this;
	 }
	 
	 default BinaryWriter putCLong(long data)  throws IOException{
		 if (data>Long.MAX_VALUE/2 || data<0) 
			 throw new IndexOutOfBoundsException(data+" should be >=0 and <="+Long.MAX_VALUE/2);
		 if (data>Integer.MAX_VALUE/2) {
			 putByte((int) ((3<<6|data>>56)) & 0xFF);
			 putByte((int) (data>>48 & 0xFF));
			 putByte((int) (data>>40 & 0xFF));
			 putByte((int) (data>>32 & 0xFF));
			 putByte((int) (data>>24 & 0xFF));
			 putByte((int) (data>>16 & 0xFF));
			 putByte((int) (data>>8 & 0xFF));
			 putByte((int) (data    & 0xFF));
//			 System.out.print(" "+data+"(CLong:8)");
		 } else if (data>Short.MAX_VALUE/2) {
			 putByte((int) (2<<6|data>>24));
			 putByte((int) (data>>16 &0xFF));
			 putByte((int) (data>>8 &0xFF));
			 putByte((int) (data&0xFF));
//			 System.out.print(" "+data+"(CLong:4)");
		 }else if (data>Byte.MAX_VALUE/2) {
			 putByte((int) (1<<6|data>>8));
			 putByte((int) (data&0xFF));
//			 System.out.print(" "+data+"(CLong:2)");
		 }else {
			 putByte((int) data);
//			 System.out.print(" "+data+"(CLong:1)");
		 }
		 return this;
	 }
	 
}
