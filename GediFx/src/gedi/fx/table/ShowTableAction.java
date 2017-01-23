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
