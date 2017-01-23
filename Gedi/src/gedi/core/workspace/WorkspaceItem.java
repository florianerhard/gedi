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
