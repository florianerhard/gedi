package gedi.core.workspace.action;

import java.util.function.Consumer;

public interface WorkspaceItemAction<T> extends Consumer<T> {

	Class<T> getItemClass();

}
