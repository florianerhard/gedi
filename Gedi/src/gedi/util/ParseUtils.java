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

package gedi.util;

import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.Trie;
import gedi.util.io.text.HeaderLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cern.colt.bitvector.BitVector;

public class ParseUtils {

	
	public static BitVector parseRangeBv(String s, int count) {
		if (s.equals("all")) s = "0:-1";
		BitVector re = new BitVector(count);
		String[] c = StringUtils.split(s, ',');
		for (String x : c) {
			String[] i = StringUtils.split(x, ':');
			if (i.length==1)
				re.putQuick(Integer.parseInt(i[0]),true);
			else  if (i.length!=2) throw new RuntimeException("Could not parse range");
			else {
				int from = i[0].length()>0 ? Integer.parseInt(i[0]) : 0;
				int to = i[1].length()>0 ? Integer.parseInt(i[1]) : -1;
				if (from<0) from = count+from;
				if (to<0) to = count+to;
				for (int j=from; j<=to; j++)
					re.putQuick(j, true);
			}
		}
		return re;
	}
	
	public static IntArrayList parseRangePositions(String s, int count, IntArrayList positions) {
		return parseRangePositions(s, count, positions, null, false);
	}
	/**
	 * 
	 * @param s
	 * @param count
	 * @param positions
	 * @param header
	 * @param onebased are the numbers in s onebased?
	 * @return
	 */
	public static IntArrayList parseRangePositions(String s, int count, IntArrayList positions, HeaderLine header, boolean onebased) {
		return parseRangePositions(s, count, positions, header, ':', onebased);
	}
	
	/**
	 * 
	 * @param s
	 * @param count
	 * @param positions
	 * @param header
	 * @param onebased are the numbers in s onebased?
	 * @return
	 */
	public static IntArrayList parseRangePositions(String s, int count, IntArrayList positions, HeaderLine header, char rangeDelimiter, boolean onebased) {
		positions.clear();
		if (s.equals("all") || s.equals("*")) s = onebased?"1"+rangeDelimiter+"-1":"0"+rangeDelimiter+"-1";
		
		String[] c = StringUtils.split(s, ',');
		for (String x : c) {
			String[] i = StringUtils.split(x, rangeDelimiter);
			if (i.length==1)
				positions.add(getSaveFromHeader(i[0],header, onebased));
			else  if (i.length!=2) throw new RuntimeException("Could not parse range");
			else {
				int from = i[0].length()>0 ? getSaveFromHeader(i[0],header, onebased) : 0;
				int to = i[1].length()>0 ? getSaveFromHeader(i[1],header, onebased) : -1;
				if (from<0) from = count+from;
				if (to<0) to = count+to;
				for (int j=from; j<=to; j++)
					positions.add(j);
			}
		}
		return positions;
	}
	
	private static int getSaveFromHeader(String f, HeaderLine header, boolean onebased) {
		if (StringUtils.isInt(f)) {
			int re = Integer.parseInt(f);
			if (onebased && re>0) re--;
			return re;
		}
		if (header==null) throw new IllegalArgumentException(f+" is not a number and no header is given!");
		return header.get(f);
	}

	public static long parseBitstring(String s) {
		if (s.length()>=63 || s.length()==0)
			throw new RuntimeException("Bitstring has invalid length!",null);

		long re = 0;
		for (int i=s.length()-1; i>=0; i--) {
			char c = s.charAt(i);
			if (c=='1')
				re |= (1L<<s.length()-1L-i);
			else if (c!='0')
				throw new RuntimeException("Bitstring contains invalid characters!",null);
		}
		return re;
	}

	public static String parseDelimiter(String option) {
		return option.replace("\\t", "\t");
	}

	/**
	 * all even entries contain text, all odd contain variable names.
	 * Variables must be given as ${Name}!
	 * @param s
	 * @return
	 */
	public static String[] parseVariableFormatString(String s) {
		Matcher m = Pattern.compile("\\$\\{(.*?)\\}").matcher(s);
		ArrayList<String> re = new ArrayList<String>();
		int e = 0;
		while (m.find()) {
			int st = m.start();
			re.add(s.substring(e,st));
			re.add(m.group(1));
			e = m.end();
		}
		re.add(s.substring(e));
		return re.toArray(new String[0]);
	}

	public static int[] parseNumbers(String commaSeparated) {
		String[] c = StringUtils.split(commaSeparated, ',');
		int[] re = new int[c.length];
		for (int i=0; i<c.length; i++)
			re[i] = Integer.parseInt(c[i]);
		return re;
	}

	/**
	 * Parses by prefix; if name is null or the empty string, null is returned
	 * @param name
	 * @param ignoreCase
	 * @param enumClass
	 * @return
	 */
	public static <E> E parseEnumNameByPrefix(String name,
			boolean ignoreCase, Class<E> enumClass) {
		if (name==null || name.length()==0) return null;
		E[] el = enumClass.getEnumConstants();
		if (el==null) throw new IllegalArgumentException("Not an enum class!");
		
		Trie<E> trie = new Trie<E>();
		for (E e : el)
			trie.put(ignoreCase?e.toString().toLowerCase():e.toString(),e);
		
		E direct = trie.get(ignoreCase?name.toLowerCase():name);
		if (direct!=null) return direct;
		return trie.getUniqueWithPrefix(ignoreCase?name.toLowerCase():name);
	}

	public static double[] parseFunctionParameter(String f) {
		int o = f.indexOf('(');
		int c = f.indexOf(')', o+1);
		if (o>=0 && c>=0) f = f.substring(o+1,c);
		return StringUtils.parseDouble(StringUtils.split(f, ','));
	}

	public static HashMap<String, String> parseOptions(String s) {
		HashMap<String,String> re = new HashMap<String, String>();
		for (String p : s.split("\\s+")) {
			String[] ps = StringUtils.split(p, '=');
			if (ps.length!=2) throw new RuntimeException("Invalid option string!");
			re.put(ps[0],ps[1]);
		}
		return re;
			
	}


}
