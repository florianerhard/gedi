package gedi.fx.image;

import gedi.core.workspace.action.WorkspaceItemAction;
import gedi.fx.FX;
import gedi.gui.WindowType;

import java.awt.image.BufferedImage;

public class ShowImageAction implements WorkspaceItemAction<BufferedImage> {

	private String label = "Image";
	private WindowType type = WindowType.Tab;
	
	public ShowImageAction(String label, WindowType type) {
		this.label = label;
		this.type = type;
	}

	public ShowImageAction(String label) {
		this.label = label;
	}

	public ShowImageAction(WindowType type) {
		this.type = type;
	}
	
	public ShowImageAction() {
	}


	
	@Override
	public void accept(BufferedImage image) {
		FX.image(image, label, type);
	}

	@Override
	public Class<BufferedImage> getItemClass() {
		return BufferedImage.class;
	}

}
