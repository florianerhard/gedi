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

package gedi.fx.docking;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;


public class DockGroup {


	public DockGroup() {
		this.detachable = new ReadOnlyBooleanWrapper(this, "detachable", true);
	}
	

	public DockGroup(boolean detachable) {
		this.detachable = new ReadOnlyBooleanWrapper(this, "detachable", detachable);
	}
	
	private ReadOnlyBooleanProperty detachable;

	public final ReadOnlyBooleanProperty detachableProperty() {
		return this.detachable;
	}

	public final boolean isDetachable() {
		return this.detachableProperty().get();
	}

	
}
