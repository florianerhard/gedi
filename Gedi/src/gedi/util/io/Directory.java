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

package gedi.util.io;

import java.io.File;

public class Directory extends File {

	private static final long serialVersionUID = -82127428023950989L;

	public Directory(Directory parent, String name) {
		super(parent,name);
		if (exists() && !isDirectory())
			throw new IllegalArgumentException("Path does not point to a valid directory!");
	}
	
	public Directory(String path) {
		super(path);
		if (exists() && !isDirectory())
			throw new IllegalArgumentException("Path does not point to a valid directory!");
	}

	public Directory getParentDirectory() {
		return new Directory(getParent());
	}

}
