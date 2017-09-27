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

package gedi.util.io.randomaccess.diskarray;

import gedi.util.io.randomaccess.PageFileView;

import java.io.IOException;
import java.util.Arrays;

public class IntDiskArray  {

	private PageFileView data;
	private long size;

	public IntDiskArray(PageFileView data) throws IOException {
		this.data = data;
		this.size = data.size()/Integer.BYTES;
	}

	public PageFileView getView() {
		return data;
	}

	public int get(long index) throws IOException {
		long offset = index*Integer.BYTES;
		return data.getInt(offset);
	}

	public long size() {
		return size;
	}

	public long binarySearch(int key) throws IOException {
		return binarySearch(key, 0, size);
	}
	public long binarySearch(int key, long fromIndex, long toIndex) throws IOException {
		long low = fromIndex;
		long high = toIndex - 1;

		while (low <= high) {
			long mid = (low + high) >>> 1;
			int midVal = get(mid);

			if (midVal < key)
				low = mid + 1;
			else if (midVal > key)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1);  // key not found.
	}

}
