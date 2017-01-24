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

package gedi.core.data.reads;

import gedi.util.datastructure.array.NumericArray;

public enum ReadCountMode {

	
	All, Weight, Divide, Unique, CollapseAll, CollapseUnique;

	public double computeCount(int count, int multiplicity, double weight) {
		switch (this) {
		case All: return count;
		case Weight: return count*weight;
		case Divide: return (double)count/multiplicity;
		case Unique: return multiplicity<=1?count:0;
		case CollapseAll: return count>0?1:0;
		case CollapseUnique: return count>0&&multiplicity<=1?1:0;
		default: return Double.NaN;
		}
	}
	
	public int computeCountInt(int count, int multiplicity, double weight) {
		switch (this) {
		case All: return count;
		case Weight: throw new IllegalArgumentException("Mode weight produces real counts!");
		case Divide: throw new IllegalArgumentException("Mode divide produces real counts!");
		case Unique: return multiplicity<=1?count:0;
		case CollapseAll: return count>0?1:0;
		case CollapseUnique: return count>0&&multiplicity<=1?1:0;
		default: return Integer.MIN_VALUE;
		}
	}
	
	public int computeCountFloor(int count, int multiplicity, double weight) {
		switch (this) {
		case All: return count;
		case Weight: return (int)(count*weight);
		case Divide: return (int)((double)count/multiplicity);
		case Unique: return multiplicity<=1?count:0;
		case CollapseAll: return count>0?1:0;
		case CollapseUnique: return count>0&&multiplicity<=1?1:0;
		default: return Integer.MIN_VALUE;
		}
	}

	public void addCount(NumericArray dest, int destIndex, int count, int multiplicity, double weight) {
		switch (this) {
		case All: dest.add(destIndex,count); break;
		case Weight: dest.add(destIndex,count*weight); break;
		case Divide: dest.add(destIndex,(double)count/multiplicity); break;
		case Unique: dest.add(destIndex,multiplicity<=1?count:0); break;
		case CollapseAll: dest.add(destIndex,count>0?1:0); break;
		case CollapseUnique: dest.add(destIndex,count>0&&multiplicity<=1?1:0); break;
		}
	}
	
	public void getCount(NumericArray dest, int destIndex, int count, int multiplicity, double weight) {
		switch (this) {
		case All: dest.setInt(destIndex,count); break;
		case Weight: dest.setDouble(destIndex,count*weight); break;
		case Divide: dest.setDouble(destIndex,(double)count/multiplicity); break;
		case Unique: dest.setInt(destIndex,multiplicity<=1?count:0); break;
		case CollapseAll: dest.setInt(destIndex,count>0?1:0); break;
		case CollapseUnique: dest.setInt(destIndex,count>0&&multiplicity<=1?1:0); break;
		}
	}
	
	public Class<? extends Number> getType() {
		switch (this) {
		case All: return Integer.TYPE;
		case Weight: return Double.TYPE;
		case Divide: return Double.TYPE;
		case Unique: return Integer.TYPE;
		case CollapseAll: return Integer.TYPE;
		case CollapseUnique: return Integer.TYPE;
		default: return null;
		}
	}
	
}
