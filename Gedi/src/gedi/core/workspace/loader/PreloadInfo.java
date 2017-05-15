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

package gedi.core.workspace.loader;

public class PreloadInfo<T,P> {
	private Class<T> itemClass;
	private P info;

	public PreloadInfo(Class<T> itemClass, P info) {
		this.itemClass = itemClass;
		this.info = info;
	}

	public Class<T> getItemClass() {
		return itemClass;
	}

	public P getInfo() {
		return info;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((info == null) ? 0 : info.hashCode());
		result = prime * result + ((itemClass == null) ? 0 : itemClass.hashCode());
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
		PreloadInfo other = (PreloadInfo) obj;
		if (info == null) {
			if (other.info != null)
				return false;
		} else if (!info.equals(other.info))
			return false;
		if (itemClass == null) {
			if (other.itemClass != null)
				return false;
		} else if (!itemClass.equals(other.itemClass))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + itemClass + ", " + info + "]";
	}

	
	
}
