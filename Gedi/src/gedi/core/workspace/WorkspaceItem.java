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

package gedi.core.workspace;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public interface WorkspaceItem {

	public static final int LOADABLE = 1<<0;
	
	
	int getOptions();
	
	<T> T load() throws IOException;
	
	<T> Class<T> getItemClass();
	
	void forEachChild(Consumer<WorkspaceItem> consumer);
	
	
	default boolean isLoadable() {
		return (getOptions()&LOADABLE)!=0;
	}
	
	default boolean hasChildren() {
		boolean[] re = {false};
		forEachChild(c->re[0]=true);
		return re[0];
	}

	WorkspaceItem getParent();
	
	String getName();
	

}
