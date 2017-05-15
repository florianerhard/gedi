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

package gedi.util.algorithm.rmq;

import java.io.IOException;

import cern.colt.Arrays;
import gedi.app.Config;
import gedi.util.datastructure.array.ByteArray;
import gedi.util.datastructure.array.DiskByteArray;
import gedi.util.datastructure.array.DiskIntegerArray;
import gedi.util.datastructure.array.DiskShortArray;
import gedi.util.datastructure.array.IntegerArray;
import gedi.util.datastructure.array.MemoryByteArray;
import gedi.util.datastructure.array.MemoryIntegerArray;
import gedi.util.datastructure.array.MemoryShortArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.ShortArray;
import gedi.util.datastructure.array.decorators.CumulatedNumericArray;
import gedi.util.datastructure.array.decorators.DecreasingNumericArray;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileView;
import gedi.util.io.randomaccess.PageFileWriter;

public class DiskMinMaxSumIndex {


	private BinaryReader file;
	
	private NumericArray inc;
	private NumericArray dec;

	// size of array a
	int n;
	// microblock size
	int s;
	// block size
	int sprime;
	// superblock size
	int sprimeprime;
	// depth of table M:
	int M_depth;
	// depth of table M':
	int Mprime_depth;


	boolean sum;
//	long[] minOffsets;
//	long[] maxOffsets;
	Index minIndex;
	Index maxIndex;


	// number of blocks (always n/sprime)
	int nb;
	// number of superblocks (always n/sprimeprime)
	int nsb;
	// number of microblocks (always n/s)
	int nmb;

	// because M just stores offsets (rel. to start of block), this method
	// re-calculates the true index:
	private final int m(long[] offsets, int k, int block) throws IOException { return getM(offsets,k,block)+(block*sprime); }
	private final int m(Index index, int k, int block) throws IOException { return getM(index,k,block)+(block*sprime); }

	// return microblock-number of entry i:
	private final int microblock(int i) { return i/s; }

	// return block-number of entry i:
	private final  int block(int i) { return i/sprime; }

	// return superblock-number of entry i:
	private final  int superblock(int i) { return i/sprimeprime; }

	// precomputed Catalan triangle (17 is enough for 64bit computing):
	static final int[][] Catalan ={
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
		{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16},
		{0,0,2,5,9,14,20,27,35,44,54,65,77,90,104,119,135},
		{0,0,0,5,14,28,48,75,110,154,208,273,350,440,544,663,798},
		{0,0,0,0,14,42,90,165,275,429,637,910,1260,1700,2244,2907,3705},
		{0,0,0,0,0,42,132,297,572,1001,1638,2548,3808,5508,7752,10659,14364},
		{0,0,0,0,0,0,132,429,1001,2002,3640,6188,9996,15504,23256,33915,48279},
		{0,0,0,0,0,0,0,429,1430,3432,7072,13260,23256,38760,62016,95931,144210},
		{0,0,0,0,0,0,0,0,1430,4862,11934,25194,48450,87210,149226,245157,389367},
		{0,0,0,0,0,0,0,0,0,4862,16796,41990,90440,177650,326876,572033,961400},
		{0,0,0,0,0,0,0,0,0,0,16796,58786,149226,326876,653752,1225785,2187185},
		{0,0,0,0,0,0,0,0,0,0,0,58786,208012,534888,1188640,2414425,4601610},
		{0,0,0,0,0,0,0,0,0,0,0,0,208012,742900,1931540,4345965,8947575},
		{0,0,0,0,0,0,0,0,0,0,0,0,0,742900,2674440,7020405,15967980},
		{0,0,0,0,0,0,0,0,0,0,0,0,0,0,2674440,9694845,25662825},
		{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9694845,35357670},
		{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,35357670}
	};;

	// minus infinity (change for 64bit version)
	static final double minus_infinity = Double.POSITIVE_INFINITY;

	// stuff for clearing the least significant x bits (change for 64-bit computing)
	static final byte[] HighestBitsSet = {~0, ~1, ~3, ~7, ~15, ~31, ~63, ~127};

	// Least Significant Bits for 8-bit-numbers:
	static final byte[] LSBTable256 = {
		0,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		6,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		7,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		6,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0
	};

	private final int log2fast(int v) {
		int c = 0;          // c will be lg(v)
		int tt,t;
		if ((tt = v >> 16)!=0)
			c = (t = v >> 24)!=0 ? 24 + LogTable256[t] : 16 + LogTable256[tt & 0xFF];
			else 
				c = (t = v >> 8)!=0 ? 8 + LogTable256[t] : LogTable256[v];
				return c;
	}

	// the following stuff is for fast base 2 logarithms:
	// (currently only implemented for 32 bit numbers)
	static final byte[] LogTable256 = {
		0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,
		4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
		5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
		5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7
	};;
	private final byte clearbits(byte n, int x) {
		return (byte) (n & HighestBitsSet[x]);
	}

	private boolean ARRAY_VERY_SMALL;


	/**
	 * Sum of a must fit in its type!! Overflows are not checked!
	 * @param writer
	 * @param a
	 * @param min
	 * @param max
	 * @throws IOException
	 * @returns position of the start of the array
	 */
	public static long create(PageFileWriter writer, NumericArray a, boolean min, boolean sum, boolean max) throws IOException {
		writer.putAsciiChars("MSS");
		writer.putByte(min?1:0);
		writer.putByte(sum?1:0);
		writer.putByte(max?1:0);
		
		if (min) createMinIndex(writer, a);
		if (max) createMinIndex(writer, new DecreasingNumericArray(a));
		
		long re = writer.position();
		if (sum)
			a.cumSum();
		NumericArray.write(writer, a);
		if (sum)
			a.deCumSum();
		return re;
	}
	public boolean hasSum() {
		return sum;
	}

	public static void createMinIndex(PageFileWriter writer, NumericArray a) throws IOException {

		int n = a.length();
		int s = 1 << 3;	         // microblock-size
		int sprime = 1 << 4;         // block-size
		int sprimeprime = 1 << 8;	 // superblock-size

		int nmb = (n-1)/s+1; // number of microblocks
		int nb = (n-1)/sprime+1;       // number of blocks
		int nsb = (n-1)/sprimeprime+1; // number of superblocks


		int M_depth = (int) Math.floor(Math.log(((double) sprimeprime / (double) sprime))/Math.log(2));
		int Mprime_depth = (int) Math.floor(Math.log(nsb)/Math.log(2)) + 1;


		// write header: MAGIX(RmaxQ),n, s,sprime,sprimeprime,M_depth,Mprime_depth
		writer.putInt(n);
		writer.putInt(s);
		writer.putInt(sprime);
		writer.putInt(sprimeprime);
		writer.putInt(M_depth);
		writer.putInt(Mprime_depth);



		// The following is necessary because we've fixed s, s' and s'' according to the computer's
		// word size and NOT according to the input size. This may cause the (super-)block-size
		// to be too big, or, in other words, the array too small. If this code is compiled on
		// a 32-bit computer, this happens iff n < 113. For such small instances it isn't 
		// advisable anyway to use this data structure, because simpler methods are faster and 
		// less space consuming.
		if (nb<sprimeprime/(2*sprime)) {
			// naive scanning is used, so no extra space needed
			return; 
		}

		// space for out-of-block- and out-of-superblock-queries:
		byte[][] M = new byte[M_depth][];
		M[0] = new byte[nb];
		int[][] Mprime = new int[Mprime_depth][];
		Mprime[0] = new int[nsb];

		// Type-calculation for the microblocks and pre-computation of in-microblock-queries:
		short[] type = new short[nmb];
		byte[][] Prec = new byte[Catalan[s][s]][];
		for (int i = 0; i < Catalan[s][s]; i++) {
			Prec[i] = new byte[s];
			Prec[i][0] = 1; // init with impossible value
		}

		NumericArray rp = NumericArray.createMemory(s+1, a.getType());   // rp: rightmost path in Cart. tree
		int z = 0;            // index in array a
		int start;            // start of current block
		int end;              // end of current block
		int q;                // position in Catalan triangle
		int p;                // --------- " ----------------

		// set a's infimum to rp[0]
		rp.copy(a, 0, 1); // save a[0] to rp[1]
		a.setInfimum(0); // set a infimum to a[0]
		rp.copy(a, 0, 0); // mv infimum to rp
		a.copy(rp, 1, 0); // mv back saved a[0]

		// prec[i]: the jth bit is 1 iff j is 1. pos. to the left of i where a[j] < a[i] 
    	int[] gstack = new int[s];
    	int gstacksize;
    	int g; // first position to the left of i where a[g[i]] < a[i]

    	for (int i = 0; i < nmb; i++) { // step through microblocks
    		start = z;            // init start
    		end = start + s;      // end of block (not inclusive!)
    		if (end > n) end = n; // last block could be smaller than s!

    		// compute block type as in Fischer/Heun CPM'06:
    		q = s;        // init q
    		p = s-1;      // init p
    		type[i] = 0;  // init type (will be increased!)
    		rp.copy(a, z, 1); // init rightmost path

    		while (++z < end) {   // step through current block:
    			p--;
    			while (a.compare(z, rp,q-p-1)<0) {
    				type[i] += Catalan[p][q]; // update type
    				q--;
    			}
    			rp.copy(a, z, q-p); // add last element to rightmost path
    		}

    		// precompute in-block-queries for this microblock (if necessary)
    		// as in Alstrup et al. SPAA'02:
    		if (Prec[type[i]][0] == 1) {
    			Prec[type[i]][0] = 0;
    			gstacksize = 0;
    			for (int j = start; j < end; j++) {
    				while(gstacksize > 0 && (a.compare(j,gstack[gstacksize-1])<0)) {
    					gstacksize--;
    				}
    				if(gstacksize > 0) {
    					g = gstack[gstacksize-1];
    					Prec[type[i]][j-start] = (byte) (Prec[type[i]][g-start] | (1 << (g % s)));
    				}
    				else Prec[type[i]][j-start] = 0;
    				gstack[gstacksize++] = j;
    			}
    		}
    	}
		
    	// space for out-of-block- and out-of-superblock-queries:
    	M_depth = (int) Math.floor(Math.log(((double) sprimeprime / (double) sprime))/Math.log(2));
    	M = new byte[M_depth][];
    	M[0] = new byte[nb];
    	Mprime_depth = (int) Math.floor(Math.log(nsb)/Math.log(2)) + 1;
    	Mprime = new int[Mprime_depth][];
    	Mprime[0] = new int[nsb];

    	// fill 0'th rows of M and Mprime:
    	z = 0; // minimum in current block
    	q = 0; // pos. of min in current superblock
    	g = 0; // number of current superblock
    	for (int i = 0; i < nb; i++) { // step through blocks
    		start = z;              // init start
    		p = start;              // init minimum
    		end = start + sprime;   // end of block (not inclusive!)
    		if (end > n) end = n;   // last block could be smaller than sprime!
    		if (a.compare(z, q)< 0) q = z; // update minimum in superblock

    		while (++z < end) { // step through current block:
    			if (a.compare(z, p)< 0) p = z; // update minimum in block
    			if (a.compare(z, q)< 0) q = z; // update minimum in superblock
    		}
    		M[0][i] = (byte) (p-start);                     // store index of block-minimum (offset!)
    		if (z % sprimeprime == 0 || z == n) {  // reached end of superblock?
    			Mprime[0][g++] = q;               // store index of superblock-minimum
    			q = z;
    		}
    	}

    	// fill M:
    	int dist = 1; // always 2^(j-1)
    	for (int j = 1; j < M_depth; j++) {
    		M[j] = new byte[nb];
    		for (int i = 0; i < nb - dist; i++) { // be careful: loop may go too far
    			int m1 = M[j-1][i]+(i*sprime);
				int m2 = M[j-1][i+dist]+(i+dist)*sprime;
				M[j][i] = (byte) (a.compare(m1, m2)<=0 ?
    				M[j-1][i] : M[j-1][i+dist] + (dist*sprime)); // add 'skipped' elements in a
    		}
    		for (int i = nb - dist; i < nb; i++) M[j][i] = M[j-1][i]; // fill overhang
    		dist *= 2;
    	}

    	// fill M':
    	dist = 1; // always 2^(j-1)
    	for (int j = 1; j < Mprime_depth; j++) {
    		Mprime[j] = new int[nsb];
    		for (int i = 0; i < nsb - dist; i++) {
    			Mprime[j][i] = a.compare(Mprime[j-1][i],Mprime[j-1][i+dist]) <= 0 ?
    				Mprime[j-1][i] : Mprime[j-1][i+dist];
    		}
    		for (int i = nsb - dist; i < nsb; i++) Mprime[j][i] = Mprime[j-1][i]; // overhang
    		dist *= 2;
    	}


		// write everything
		// first M
		for (int i=0; i<M_depth; i++)
			for (int j=0; j<nb; j++)
				writer.put(M[i][j]);
		// now Mprime
		for (int i=0; i<Mprime_depth; i++)
			for (int j=0; j<nsb; j++)
				writer.putInt(Mprime[i][j]);
		// type
		for (int i=0; i<nmb; i++)
			writer.putShort(type[i]);
		// finally, Prec
		for (int i=0; i<Catalan[s][s]; i++)
			for (int j=0; j<s; j++)
				writer.putByte(Prec[i][j]);
		
	}
	

	public DiskMinMaxSumIndex(String file) throws IOException {
		this(new PageFileView(new PageFile(file)));
	}
	
	
	public DiskMinMaxSumIndex(BinaryReader file) throws IOException {
		this.file = file;
		checkLoad(); // necessary for multi dimensional input, as the loader expects the file pointer to be after the array
	}
	
	private void checkLoad() throws IOException{
		if (inc==null) {
			file.position(0);
			if (!file.getAsciiChars(3).equals("MSS")) throw new IOException("Illegal file!");
			
//			if (file.getByte()==1) minOffsets = new long[5];
//			sum = file.getByte()==1;
//			if (file.getByte()==1) maxOffsets = new long[5];
			if (file.getByte()==1) minIndex = new Index();
			sum = file.getByte()==1;
			if (file.getByte()==1) maxIndex = new Index();
			
			if (minIndex!=null) readIndex(file,minIndex);
			if (maxIndex!=null) readIndex(file,maxIndex);
			
			inc = NumericArray.readDisk(file);
			if (sum)
				inc = new CumulatedNumericArray(inc);
			dec = new DecreasingNumericArray(inc);
		}
	}
	
	public int getMinIndex(int start, int stop) throws IOException {
		checkLoad();
		return query(inc, minIndex, start, stop);
	}
	
	public double getSum(int start, int stop) throws IOException {
		checkLoad();
		if (!sum) {
			double re = 0;
			for (int x = start; x <= stop; x++) re+=getValue(x);
			return re;
		}
		
		if (start==0) return inc.getDouble(stop);
		return inc.getDouble(stop)-inc.getDouble(start-1);
	}
	
	public int getMaxIndex(int start, int stop) throws IOException {
		checkLoad();
		return query(dec, maxIndex, start, stop);
	}
	
	public void loadToMemory() throws IOException {
		checkLoad();
		if (minIndex!=null) minIndex.mem();
		if (maxIndex!=null) maxIndex.mem();
		
		NumericArray old = inc;
		if (old instanceof CumulatedNumericArray) old = ((CumulatedNumericArray)old).getParent();
		
		inc = NumericArray.createMemory(old.length(), old.getType());
		old.copyRange(0, inc, 0, inc.length());
		
		
		if (sum)
			inc = new CumulatedNumericArray(inc);
		dec = new DecreasingNumericArray(inc);
	}
	
	private void readIndex(BinaryReader file, long[] offsets) throws IOException {
		this.n = file.getInt();
		this.s = file.getInt();
		this.sprime = file.getInt();
		this.sprimeprime = file.getInt();
		this.M_depth = file.getInt();;
		this.Mprime_depth = file.getInt();
		

		nmb = (n-1)/s+1; // number of microblocks
		nb = (n-1)/sprime+1;       // number of blocks
		nsb = (n-1)/sprimeprime+1; // number of superblocks

		if (nb<sprimeprime/(2*sprime)) {
			ARRAY_VERY_SMALL = true;
			return; 
		}
		
		offsets[0] = file.position();
		offsets[1] = offsets[0] + Byte.BYTES*M_depth*nb;
		offsets[2] = offsets[1] + Integer.BYTES*Mprime_depth*nsb;
		offsets[3] = offsets[2] + Short.BYTES*nmb;
		offsets[4] = offsets[3] + Byte.BYTES*Catalan[s][s]*s;
		
		file.position(offsets[4]);
		
	}
	
	private static class Index {
		ByteArray M;
		IntegerArray Mprime;
		ShortArray type;
		ByteArray Prec;
		public void mem() {
			M = new MemoryByteArray(M.toByteArray());
			Mprime = new MemoryIntegerArray(Mprime.toIntArray());
			type = new MemoryShortArray(type.toShortArray());
			Prec = new MemoryByteArray(Prec.toByteArray());
		}
	}

	private void readIndex(BinaryReader file, Index index) throws IOException {
		this.n = file.getInt();
		this.s = file.getInt();
		this.sprime = file.getInt();
		this.sprimeprime = file.getInt();
		this.M_depth = file.getInt();;
		this.Mprime_depth = file.getInt();
		

		nmb = (n-1)/s+1; // number of microblocks
		nb = (n-1)/sprime+1;       // number of blocks
		nsb = (n-1)/sprimeprime+1; // number of superblocks

		if (nb<sprimeprime/(2*sprime)) {
			ARRAY_VERY_SMALL = true;
			return; 
		}
		
		long off = file.position();
		int l = 0;
		index.M = new DiskByteArray(file,null,off,l=Byte.BYTES*M_depth*nb);
		off+=l;
		index.Mprime = new DiskIntegerArray(file, null, off, l=Integer.BYTES*Mprime_depth*nsb);
		off+=l;
		index.type = new DiskShortArray(file, null, off, l=Short.BYTES*nmb);
		off+=l;
		index.Prec = new DiskByteArray(file, null, off, l=Byte.BYTES*Catalan[s][s]*s);
		off+=l;
		
		file.position(off);
		
	}

	private static final int Mp = 0;
	private static final int Mprimep = 1;
	private static final int typep = 2;
	private static final int Precp = 3;
	
	
	
	private final byte getM(long[] offsets, int i, int j) throws IOException {
		byte re = file.get(offsets[Mp]+Byte.BYTES*(i*nb+j));
		return re;
	}
	private final int getMprime(long[] offsets, int i, int j) throws IOException {
		int re = file.getInt(offsets[Mprimep]+Integer.BYTES*(i*nsb+j));
		return re;
	}
	private final short gettype(long[] offsets, int i) throws IOException {
		short re = file.getShort(offsets[typep]+Short.BYTES*i);
		return re;
	}
	private final byte getPrec(long[] offsets, int i, int j) throws IOException {
		byte re = file.get(offsets[Precp]+Byte.BYTES*(i*s+j));
		return re;
	}
	
	private final byte getM(Index index, int i, int j) throws IOException {
		return index.M.getByte(i*nb+j);
	}
	private final int getMprime(Index index, int i, int j) throws IOException {
		return index.Mprime.getInt(i*nsb+j);
	}
	private final short gettype(Index index, int i) throws IOException {
		return index.type.getShort(i);
	}
	private final byte getPrec(Index index, int i, int j) throws IOException {
		return index.Prec.getByte(i*s+j);
	}

	private int query(NumericArray a, Index offsets, int start, int stop) throws IOException {
		int i=start;
		int j = stop;
		int min, min_tmp;             // min: to be returned
		
		if (offsets==null || ARRAY_VERY_SMALL) { // scan naively
			min = i;
			for (int x = i+1; x <= j; x++) if (a.compare(x, min)<0) min = x;
			return min;
		}
		
		int mb_i = microblock(i);     // i's microblock
		int mb_j = microblock(j);     // j's microblock
		int s_mi = mb_i * s;          // start of i's microblock
		int i_pos = i - s_mi;         // pos. of i in its microblock

		
		if (mb_i == mb_j) { // only one in-microblock-query
			min_tmp = clearbits(getPrec(offsets,gettype(offsets,mb_i),j-s_mi), i_pos);
			min = min_tmp == 0 ? j : s_mi + LSBTable256[min_tmp];
		}
		else { 
			int b_i = block(i);      // i's block
			int b_j = block(j);      // j's block
			int s_mj = mb_j * s;     // start of j's microblock
			int j_pos = j - s_mj;    // position of j in its microblock
			min_tmp = clearbits(getPrec(offsets,gettype(offsets,mb_i),s-1), i_pos);
			min = min_tmp == 0 ? s_mi + s - 1 : s_mi + LSBTable256[min_tmp]; // left in-microblock-query

			if (mb_j > mb_i + 1) { // otherwise only 2 in-microblock-queries
				int s_bi = b_i * sprime;      // start of i's block
				int s_bj = b_j * sprime;      // start of j's block
				if (s_bi+s > i) { // do another microblock-query to compensate for missing block-layer
					mb_i++;   // go one microblock to the right
					min_tmp = getPrec(offsets,gettype(offsets,mb_i),s-1) == 0 ?
							s_bi + sprime - 1 : s_mi + s + LSBTable256[getPrec(offsets,gettype(offsets,mb_i),s-1)];
					if (a.compare(min_tmp,min)<0) min = min_tmp;
				}

				if (b_j > b_i + 1) { // otherwise no out-of-block-queries
					int k, t, b;  // temporary variables
					b_i++; // block where out-of-block-query starts
					if (s_bj - s_bi - sprime <= sprimeprime) { // just one out-of-block-query
						k = log2fast(b_j - b_i - 1);
						t = 1 << k; // 2^k
						i = m(offsets,k, b_i); b = m(offsets,k, b_j-t); // i can be overwritten!
						min_tmp = a.compare(i,b)<=0 ? i : b;
						if (a.compare(min_tmp, min)<0) min = min_tmp;
					}
					else { // here we have two out-of-block-queries:
						int sb_i = superblock(i); // i's superblock
						int sb_j = superblock(j); // j's superblock

						b = block((sb_i+1)*sprimeprime); // end of left out-of-block-query
						k = log2fast(b - b_i);
						t = 1 << k; // 2^k
						i = m(offsets,k, b_i); i_pos = m(offsets,k, b+1-t); // i & i_pos can be overwritten!
						min_tmp = a.compare(i, i_pos)<=0 ? i : i_pos;
						if (a.compare(min_tmp, min)<0) min = min_tmp;

						if (sb_j > sb_i + 1) { // the superblock-query
							k = log2fast(sb_j - sb_i - 2);
							t = 1 << k;
							i = getMprime(offsets,k,sb_i+1); i_pos = getMprime(offsets,k,sb_j-t);
							min_tmp = a.compare(i, i_pos)<=0 ? i : i_pos;
							if (a.compare(min_tmp, min)<0) min = min_tmp;
						}

						b = block(sb_j*sprimeprime); // start of right out-of-block-query
						k = log2fast(b_j - b);
						t = 1 << k; // 2^k
						b--; // going one block to the left doesn't harm and saves some tests
						i = m(offsets,k, b); i_pos = m(offsets,k, b_j-t);
						min_tmp = a.compare(i, i_pos)<=0 ? i : i_pos;
						if (a.compare(min_tmp,min)<0) min = min_tmp;
					}
				}

				if (j >= s_bj+s) { // another microblock-query to compensate for missing block-layer
					min_tmp = getPrec(offsets,gettype(offsets,mb_j-1),s-1) == 0 ?
							s_mj - 1 : s_bj + LSBTable256[getPrec(offsets,gettype(offsets,mb_j-1),s-1)];
					if (a.compare(min_tmp,min)<0) min = min_tmp;
				}
			}

			min_tmp = getPrec(offsets,gettype(offsets,mb_j),j_pos) == 0 ?
					j : s_mj + LSBTable256[getPrec(offsets,gettype(offsets,mb_j),j_pos)];     // right in-microblock-query
			if (a.compare(min_tmp,min)<0) min = min_tmp;

		}

		return min;
	}

	public double getValue(int index)  {
		try {
			checkLoad();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (sum){
			if (index==0) return inc.getDouble(0);
			if (inc.isIntegral()){
				return inc.getLong(index)-inc.getLong(index-1);
			}
			return inc.getDouble(index)-inc.getDouble(index-1);
		}
		return inc.getDouble(index);
	}
	
	public int length() {
		try {
			checkLoad();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return inc.length();
	}
	
	
	public String format(int index)  {
		try {
			checkLoad();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (sum){
			if (index==0) return String.format(Config.getInstance().getRealFormat(),inc.getDouble(0));
			if (inc.isIntegral()){
				return inc.getLong(index)-inc.getLong(index-1)+"";
			}
			return String.format(Config.getInstance().getRealFormat(),inc.getDouble(index)-inc.getDouble(index-1));
		}
		return String.format(Config.getInstance().getRealFormat(),inc.getDouble(index));
	}


}
