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

package gedi.core.reference;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.PageFileWriter;

import java.io.IOException;
import java.util.HashMap;

public class Chromosome implements ReferenceSequence {

	private String name;
	private Strand strand;
	
	private Chromosome(String name, Strand strand) {
		if (name.startsWith("chr")) name = name.substring(3);
		if (name.equals("M")) name = "MT";
		this.name = name;
		this.strand = strand;
	}
	
	
	public String getName() {
		return name;
	}
	
	public Strand getStrand() {
		return strand;
	}
	
	

	@Override
	public String toString() {
		return String.format("%s%s", getName(), getStrand());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Chromosome))
			return false;
		Chromosome chr = (Chromosome) obj;
		return chr.name.equals(name) && chr.strand==strand;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode()+(strand.ordinal()<<13);
	}
	
	
	public Chromosome getOppositeStrand() {
		if (strand==Strand.Independent) return this;
		if (strand==Strand.Plus) return obtain(name,Strand.Minus);
		return obtain(name,Strand.Plus);
	}
	
	
	
	
	/**
	 * Can parse chrX+
	 * @param name
	 * @return
	 */
	public static Chromosome obtain(String name) {
		if (name.endsWith("+") || name.endsWith("-"))
			return obtain(name.substring(0,name.length()-1),name.substring(name.length()-1));
		else 
			return obtain(name,Strand.Independent);
	}
	public static Chromosome obtain(String name, String strand) {
		return obtain(name,Strand.parse(strand));
	}
	public static Chromosome obtain(String name, boolean strand) {
		return obtain(name,strand?Strand.Plus:Strand.Minus);
	}
	public static Chromosome obtain(String name, Strand strand) {
		Chromosome re = cache[strand.ordinal()].get(name);
		if (re==null) cache[strand.ordinal()].put(name, re = new Chromosome(name,strand));
		return re;
	}
	
	public static Chromosome read(BinaryReader file) throws IOException {
		int l = file.getInt();
		char[] name = new char[l];
		for (int i=0; i<l; i++)
			name[i] = file.getAsciiChar();
		Strand s = Strand.values()[file.getInt()];
		return obtain(String.valueOf(name),s);
	}
	public static void write(Chromosome chr, PageFileWriter file) throws IOException {
		file.putInt(chr.getName().length());
		for (int i=0; i<chr.getName().length(); i++)
			file.putAsciiChar(chr.getName().charAt(i));
		file.putInt(chr.getStrand().ordinal());
	}
	
	
	@SuppressWarnings("unchecked")
	private final static HashMap<String,Chromosome>[] cache = new HashMap[Strand.values().length];
	static {
		for (int i=0; i<cache.length; i++)
			cache[i] = new HashMap<String,Chromosome>();
	}
	public static Chromosome UNMAPPED = new Chromosome("UNMAPPED",Strand.Independent);
	
	
	@Override
	public ReferenceSequence toChrStrippedReference() {
		return Chromosome.obtain(getChrStrippedName(),strand);
	}
}
