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

package gedi.util.math.stat.counting;

import java.util.Arrays;

public class ItemCount<T> {

	private T item;
	private int[] count;
	public ItemCount(T item, int[] count) {
		this.item = item;
		this.count = count;
	}
	public T getItem() {
		return item;
	}
	public int[] getCount() {
		return count;
	}
	
	public int getCount(int d) {
		return count[d];
	}
	
	public int getDimensions() {
		return count.length;
	}
	@Override
	public String toString() {
		return "ItemCount [item=" + item + ", count=" + Arrays.toString(count)
				+ "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(count);
		result = prime * result + ((item == null) ? 0 : item.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemCount other = (ItemCount) obj;
		if (!Arrays.equals(count, other.count))
			return false;
		if (item == null) {
			if (other.item != null)
				return false;
		} else if (!item.equals(other.item))
			return false;
		return true;
	}

	
	
	
}
