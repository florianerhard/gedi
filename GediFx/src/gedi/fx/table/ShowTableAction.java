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
package gedi.fx.table;

import gedi.core.data.table.Table;
import gedi.core.data.table.TableView;
import gedi.core.workspace.action.WorkspaceItemAction;
import gedi.fx.FX;
import gedi.fx.FXWindow;
import gedi.fx.docking.Dockable;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;

public class ShowTableAction implements WorkspaceItemAction<TableView<?>> {

	
	@Override
	public void accept(TableView<?> t) {
		FX.sendCommand(fx->{
			FXWindow win = fx.getMainOrNew();
			win.add(new Dockable(new GediTableView(t), t.getTable().getMetaInfo().getName(), true,true, AwesomeDude.createIconLabel(AwesomeIcon.TABLE)));
		});
	}

	@Override
	public Class<TableView<?>> getItemClass() {
		return (Class)Table.class;
	}

}
