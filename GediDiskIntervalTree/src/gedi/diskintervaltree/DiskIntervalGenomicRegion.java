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
package gedi.diskintervaltree;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;

public class DiskIntervalGenomicRegion<T> extends ArrayGenomicRegion {

	private T data;
	
	public DiskIntervalGenomicRegion(int start, int end, T data) {
		super(start,end);
		this.data = data;
	}

	public DiskIntervalGenomicRegion(int[] coord, T data) {
		super(coord);
		this.data=data;
	}

	public T getData() {
		return data;
	}
	

}
