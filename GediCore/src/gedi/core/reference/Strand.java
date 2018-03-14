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

public enum Strand {
	Plus,Minus,Independent;
	
	public boolean isPlus() {
		return this.equals(Plus);
	}
	
	public boolean isMinus() {
		return this.equals(Minus);
	}
	
	public boolean isIndependent() {
		return this.equals(Independent);
	}
	
	public static Strand parse(String s) {
		if (s==null || s.length()==0 || s.equals("0") || s.equals(".")) return Strand.Independent;
		if (s.equals("+") || s.equals("1")) return Strand.Plus;
		if (s.equals("-") || s.equals("-1")) return Strand.Minus;
		return valueOf(s);
	}
	
	public String toString() {
		if (this==Plus) return "+";
		if (this==Minus) return "-";
		return "";
	}

	public String getGff() {
		if (this==Plus) return "+";
		if (this==Minus) return "-";
		return ".";
	}
	public String getEnsembl() {
		if (this==Plus) return "1";
		if (this==Minus) return "-1";
		return "0";
	}

	public static int compare(Strand a, Strand b) {
		return a.ordinal()-b.ordinal();
	}

	public Strand toOpposite() {
		if (this==Plus) return Minus;
		if (this==Minus) return Plus;
		return Independent;
	}
	
}