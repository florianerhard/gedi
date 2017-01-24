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

package gedi.util.r;


import gedi.core.data.table.Table;
import gedi.core.data.table.TableMetaInformation;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.math.stat.factor.Factor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;

import cern.colt.bitvector.BitMatrix;
import cern.colt.bitvector.BitVector;


public class RDataWriter {

	private interface RDataConn {

		void writeBytes(String s) throws IOException;
		void writeInt(int n) throws IOException;
		void writeLong(long n) throws IOException;
		void writeDouble(double d) throws IOException;
		void write(byte[] bytes) throws IOException;

		void flush() throws IOException;
		void close() throws IOException;
		void writeHead() throws IOException;
	}
	
	private class BinaryRDataConn implements RDataConn {
		private DataOutputStream out;
		public BinaryRDataConn(DataOutputStream dataOutputStream) {
			this.out = dataOutputStream;
		}
		@Override
		public void writeBytes(String string) throws IOException {
			out.writeBytes(string);
		}
		@Override
		public void writeInt(int i)  throws IOException{
			out.writeInt(i);
		}
		@Override
		public void writeLong(long n) throws IOException {
			out.writeLong(n);
		}
		@Override
		public void writeDouble(double d) throws IOException {
			out.writeDouble(d);
		}
		@Override
		public void write(byte[] bytes) throws IOException {
			out.write(bytes);
		}
		@Override
		public void flush() throws IOException {
			out.flush();
		}
		@Override
		public void close() throws IOException {
			out.flush();
		}
		@Override
		public void writeHead() throws IOException {
			writeBytes("RDX2\nX\n");			
		}
		
	}
	
	private class AsciiRDataConn implements RDataConn {
		
		private PrintStream out;
		
		public AsciiRDataConn(PrintStream out) {
			this.out = out;
		}

		@Override
		public void writeBytes(String s) throws IOException {
			out.println(s);
		}

		@Override
		public void writeInt(int n) throws IOException {
			out.println(n);
		}

		@Override
		public void writeLong(long n) throws IOException {
			out.println(n);
		}

		@Override
		public void writeDouble(double d) throws IOException {
			out.println(d);
		}

		@Override
		public void write(byte[] bytes) throws IOException {
			out.println(new String(bytes));
		}

		@Override
		public void flush() throws IOException {
			out.flush();
		}

		@Override
		public void close() throws IOException {
			out.close();
		}

		@Override
		public void writeHead() throws IOException {
			writeBytes("RDA2\nA");
		}
		
	}
	
	private class MultiplexRDataConn implements RDataConn {
		private ArrayList<RDataConn> list = new ArrayList<>();

		public MultiplexRDataConn(RDataConn... a) {
			list.addAll(Arrays.asList(a));
		}
		
		@Override
		public void writeBytes(String s) throws IOException {
			for (RDataConn r : list)
				r.writeBytes(s);
		}

		@Override
		public void writeInt(int n) throws IOException {
			for (RDataConn r : list)
				r.writeInt(n);			
		}

		@Override
		public void writeLong(long n) throws IOException {
			for (RDataConn r : list)
				r.writeLong(n);			
		}

		@Override
		public void writeDouble(double d) throws IOException {
			for (RDataConn r : list)
				r.writeDouble(d);			
		}

		@Override
		public void write(byte[] bytes) throws IOException {
			for (RDataConn r : list)
				r.write(bytes);
		}

		@Override
		public void flush() throws IOException {
			for (RDataConn r : list)
				r.flush();			
		}

		@Override
		public void close() throws IOException {
			for (RDataConn r : list)
				r.close();			
		}

		@Override
		public void writeHead() throws IOException {
			for (RDataConn r : list)
				r.writeHead();			
		}
	}

	
	private RDataConn conn;


	public RDataWriter(OutputStream out) {
		this.conn = 
//				new MultiplexRDataConn(
				new BinaryRDataConn(new DataOutputStream(out))
//				,
//				new AsciiRDataConn(System.out)
//				)
		;
	}
	
	public RDataWriter() {
		this.conn = new AsciiRDataConn(System.out);
	}


	public void writeHeader() throws IOException {
		conn.writeHead();
		conn.writeInt(2);
		conn.writeInt((2<<16)|(3<<8)); // todo: put true R version here...
		conn.writeInt((2<<16)|(3<<8));
	}
	
	private void writeEnvAndName(String name) throws IOException {
		if (name==null) return;
		conn.writeInt(1026); // Env
		conn.writeInt(1); // Symbol
		conn.writeInt((4<<16)|9); // flags
		conn.writeInt(name.length());
		conn.writeBytes(name);
	}
	
	public void finish() throws IOException {
		writeTerm();
		conn.flush();
		conn.close();
	}
	
	private void writeTerm() throws IOException {
		conn.writeInt(254);
	}

	public static final int  LOGICAL	   = 10	;  /* logical vectors */
	public static final int  INT	   = 13;	  /* integer vectors */
	public static final int  REAL	 =   14	;  /* real variables */
	public static final int  STRING	   = 16	;  /* string vectors */
	public static final int VECTOR	= 19; /* generic vectors */
	public static final int  CHAR	  =   9;	  /* "scalar" string type (internal only)*/
	public static final int UTF8_MASK = (1<<3);
	public static final int  DATAFRAME	   = 1023;  /* string vectors */
	
	private static final int IS_CLASS = 1<<8;
	private static final int HAS_ATTRIBUTES = 1<<9;
	
	
	private HashMap<Class<?>,Method> allMethods;
	private Method[] noarrayMethods;
	
	
	public void write(String name, Object val) throws IOException {
		if (allMethods==null) {
			allMethods = new HashMap<>();
			ArrayList<Method> others = new ArrayList<>();
			for (Method m : getClass().getDeclaredMethods())
				if (m.getName().equals("write") && m.getParameterCount()==2 && m.getParameters()[0].getType()==String.class) {
					Class cls = m.getParameters()[1].getType();
					if (!cls.isArray()) 
						others.add(m);
					allMethods.put(cls, m);
				}
			noarrayMethods = others.toArray(new Method[0]);
		}
		
		Method m = allMethods.get(val.getClass());
		if (m==null) 
			for (Method o : noarrayMethods)
				if (o.getParameters()[1].getType().isAssignableFrom(val.getClass()) && o.getParameters()[1].getType()!=Object.class) {
					m = o;
					break;
				}
		
		if (m==null) throw new IOException("No write method found for class "+val.getClass());
		
		try {
			m.invoke(this, name, val);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IOException("Could not write object!",e);
		}
	}

	public void write(String name, boolean[] vector) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(LOGICAL);
		conn.writeInt(vector.length);
		for(int i=0;i!=vector.length;++i) {
			conn.writeInt(vector[i]?1:0);
		}
	}
	
	public void write(String name, Boolean[] vector) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(LOGICAL);
		conn.writeInt(vector.length);
		for(int i=0;i!=vector.length;++i) {
			conn.writeInt(vector[i]?1:0);
		}
	}

	public void write(String name, BitSet vector) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(LOGICAL);
		conn.writeInt(vector.length());
		for(int i=0;i!=vector.length();++i) {
			conn.writeInt(vector.get(i)?1:0);
		}
	}
	
	public void write(String name, BitVector vector) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(LOGICAL);
		conn.writeInt(vector.size());
		for(int i=0;i!=vector.size();++i) {
			conn.writeInt(vector.getQuick(i)?1:0);
		}
	}
	
	public void write(String name, boolean[][] matrix) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(LOGICAL | HAS_ATTRIBUTES);
		conn.writeInt(matrix.length*matrix[0].length);
		for (int c=0; c<matrix[0].length; c++)
			for (int r=0; r<matrix.length; r++)
					conn.writeInt(matrix[r][c]?1:0);				
		
		write("dim", new int[] {matrix.length, matrix[0].length});
		writeTerm();
	}
	
	public void write(String name, BitMatrix matrix) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(LOGICAL | HAS_ATTRIBUTES);
		conn.writeInt(matrix.size());
		for (int r=0; r<matrix.rows(); r++)
			for (int c=0; c<matrix.columns(); c++)
				conn.writeInt(matrix.getQuick(c, r)?1:0);				
		
		write("dim", new int[] {matrix.rows(), matrix.columns()});
		writeTerm();
	}
	

	
	public void write(String name, int[] vector) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(INT);
		conn.writeInt(vector.length);
		for(int i=0;i!=vector.length;++i) 
			conn.writeInt(vector[i]);
	}
	
	public void write(String name, Integer[] vector) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(INT);
		conn.writeInt(vector.length);
		for(int i=0;i!=vector.length;++i) 
			conn.writeInt(vector[i]);
	}
	
	public void write(String name, int[][] matrix) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(INT | HAS_ATTRIBUTES);
		conn.writeInt(matrix.length*matrix[0].length);
		for (int r=0; r<matrix.length; r++)
			for (int c=0; c<matrix[0].length; c++)
				conn.writeInt(matrix[r][c]);				
		
		write("dim", new int[] {matrix.length, matrix[0].length});
		writeTerm();
	}

	public void write(String name, NumericArray vector) throws IOException {
		if (vector.isIntegral() && vector.getType()!=NumericArrayType.Long)
			write(name,vector.toIntArray());
		else
			write(name,vector.toDoubleArray());
	}

	
	 public static final long NA_BITS = 0x7FF00000000007A2L;
	public void write(String name, double[] vector) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(REAL);
		conn.writeInt(vector.length);
		for(int i=0;i!=vector.length;++i) { 
			if(Double.isNaN(vector[i])) {
				conn.writeLong(NA_BITS);
			} else {
				conn.writeDouble(vector[i]);
			}
		}
	}

	public void write(String name, Double[] vector) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(REAL);
		conn.writeInt(vector.length);
		for(int i=0;i!=vector.length;++i) { 
			if(Double.isNaN(vector[i])) {
				conn.writeLong(NA_BITS);
			} else {
				conn.writeDouble(vector[i]);
			}
		}
	}
	
	public void write(String name, double[][] matrix) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(REAL | HAS_ATTRIBUTES);
		conn.writeInt(matrix.length*matrix[0].length);
		for (int c=0; c<matrix[0].length; c++)
			for (int r=0; r<matrix.length; r++)
				if(Double.isNaN(matrix[r][c])) {
					conn.writeLong(NA_BITS);
				} else {
					conn.writeDouble(matrix[r][c]);
				};				
		
		write("dim", new int[] {matrix.length, matrix[0].length});
		writeTerm();
	}


	public void write(String name, String[] vector) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(STRING);
		conn.writeInt(vector.length);
		for(int i=0;i!=vector.length;++i) {
			writeCharExp(vector[i]);
		}
	}
	
	public void write(String name, String[][] matrix) throws IOException {
		writeEnvAndName(name);
		conn.writeInt(STRING | HAS_ATTRIBUTES);
		conn.writeInt(matrix.length*matrix[0].length);
		for (int c=0; c<matrix[0].length; c++)
			for (int r=0; r<matrix.length; r++)
				writeCharExp(matrix[r][c]);				
		
		write("dim", new int[] {matrix.length, matrix[0].length});
		writeTerm();
	}

	public void write(String name, Factor[] vector) throws IOException {
		if (vector.length==0) return;
		
		writeEnvAndName(name);
		conn.writeInt(INT | HAS_ATTRIBUTES | IS_CLASS);
		conn.writeInt(vector.length);
		for(int i=0;i!=vector.length;++i) 
			conn.writeInt(vector[i].getIndex()+1);

		write("levels", vector[0].getNames());
		write("class", new String[] {"factor"});
		writeTerm();
	}
	
	public void write(String name, Enum<?>[] vector) throws IOException {
		if (vector.length==0) return;
		
		writeEnvAndName(name);
		conn.writeInt(INT | HAS_ATTRIBUTES | IS_CLASS);
		conn.writeInt(vector.length);
		for(int i=0;i!=vector.length;++i) 
			conn.writeInt(vector[i].ordinal());

		Enum<?>[] levels=vector[0].getClass().getEnumConstants();
		String[] names = new String[levels.length];
		for (int i=0; i<names.length; i++)
			names[i] = levels[i].name();
		
		write("levels", names);
		write("class", new String[] {"factor"});
		writeTerm();
	}
	
	public void write(String name, Table<?> df) throws IOException {
		TableMetaInformation<?> meta = df.getMetaInfo();
		
		writeEnvAndName(name);
		conn.writeInt(VECTOR | HAS_ATTRIBUTES | IS_CLASS);
		conn.writeInt(meta.getNumColumns());
		
		for (int c=0; c<meta.getNumColumns(); c++)
			write(null,df.iterate(c).toArray());
		
		String[] names = new String[meta.getNumColumns()];
		for (int i=0; i< names.length; i++) 
			names[i] = meta.getColumnName(i);
		
		write("names", names);
		write("row.names", new int[] {Integer.MIN_VALUE,(int)-df.size()});
//		conn.writeInt(DATAFRAME);
		write("class", new String[] {"data.frame"});
		writeTerm();
	}
	
	public void write(String name, DataFrame df) throws IOException {
		
		writeEnvAndName(name);
		conn.writeInt(VECTOR | HAS_ATTRIBUTES | IS_CLASS);
		conn.writeInt(df.columns());
		
		for (int c=0; c<df.columns(); c++)
			write(null,df.getColumn(c).getRaw());
		
		String[] names = new String[df.columns()];
		for (int i=0; i< names.length; i++) 
			names[i] =df.getColumn(i).name();
		
		write("names", names);
		write("row.names", new int[] {Integer.MIN_VALUE,(int)-df.rows()});
//		conn.writeInt(DATAFRAME);
		write("class", new String[] {"data.frame"});
		writeTerm();
	}


	private void writeCharExp(String string) throws IOException {
		conn.writeInt( CHAR | UTF8_MASK );
		byte[] bytes = string.getBytes("UTF8");
		conn.writeInt(bytes.length);
		conn.write(bytes);
	}

}
