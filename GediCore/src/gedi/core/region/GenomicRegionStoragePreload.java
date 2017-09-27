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

package gedi.core.region;

import gedi.util.dynamic.DynamicObject;

public class GenomicRegionStoragePreload<T> {

	private Class<T> type;
	private T example;
	private DynamicObject metaData;

	public GenomicRegionStoragePreload(Class<T> type, T example, DynamicObject metaData) {
		this.type = type;
		this.example = example;
		this.metaData = metaData;
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public T getExample() {
		return example;
	}
	
	public DynamicObject getMetaData() {
		return metaData;
	}
	
}