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

import java.io.IOException;
import java.nio.file.Path;

public interface WorkspaceItemLoader<T,P> {

	String[] getExtensions();

	T load(Path path) throws IOException;
	P preload(Path path) throws IOException;
	
	default PreloadInfo<T, P> getPreloadInfo(Path path) throws IOException {
		return new PreloadInfo<>(getItemClass(),preload(path));
	}

	Class<T> getItemClass();
	
	
	boolean hasOptions();
	
	void updateOptions(Path path);
	
}
