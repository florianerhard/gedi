package gedi.fx.html;

import gedi.core.workspace.action.WorkspaceItemAction;
import gedi.fx.FX;
import gedi.gui.WindowType;

import java.net.URI;

public class ShowHtmlAction implements WorkspaceItemAction<URI> {

	private String label = "HTML";
	private WindowType type = WindowType.Tab;
	
	public ShowHtmlAction(String label, WindowType type) {
		this.label = label;
		this.type = type;
	}

	public ShowHtmlAction(String label) {
		this.label = label;
	}

	public ShowHtmlAction(WindowType type) {
		this.type = type;
	}
	
	public ShowHtmlAction() {
	}


	
	@Override
	public void accept(URI image) {
		FX.html(image, label, type);
	}

	@Override
	public Class<URI> getItemClass() {
		return URI.class;
	}

}
