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
package gedi.fx.dialog;

import com.sun.javafx.scene.control.skin.ButtonBarSkin;

import gedi.core.data.table.Table;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class DialogInsidePane extends StackPane {

	private Region disableRegion;
	private BorderPane dialog;
	
	public DialogInsidePane() {
		
		disableRegion = new Region();
		disableRegion.setBackground(new Background(new BackgroundFill(new Color(0, 0, 0, 0.4), new CornerRadii(0), new Insets(0))));
		
		dialog = new BorderPane();
		dialog.setBorder(new Border(new BorderStroke(new Color(0.3, 0.3, 0.3,1), BorderStrokeStyle.SOLID, new CornerRadii(0),new BorderWidths(2))));
		dialog.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		
		
		setAlignment(Pos.TOP_LEFT);
	
		dialog.getStyleClass().setAll("dialog");
        
//        dialog.getStylesheets().add(ButtonBarSkin.class.getResource("modena/dialog.css").toExternalForm());
        
	}

	protected InsideDialog dialog() {
		InsideDialog diaPane = new InsideDialog(this);
		
		dialog.setCenter(diaPane);
		return diaPane;
	}
	
	void showDialog(InsideDialog inside, Node anchor) {
		if (!getChildren().contains(disableRegion))
			getChildren().add(disableRegion);

		dialog.pseudoClassStateChanged(HEADER_PSEUDO_CLASS,      inside.hasHeader());
		dialog.pseudoClassStateChanged(NO_HEADER_PSEUDO_CLASS,   !inside.hasHeader());
	       
	        
		dialog.setTranslateX(anchor.getBoundsInParent().getMinX());
		dialog.setTranslateY(anchor.getBoundsInParent().getMaxY());
		if (!getChildren().contains(dialog))
			getChildren().add(dialog);
	}
	
	void hideDialog() {
		getChildren().remove(dialog);
		getChildren().remove(disableRegion);
	}
	
	
	/***************************************************************************
     *                                                                         
     * Stylesheet Handling                                                     
     *                                                                         
     **************************************************************************/
    private static final PseudoClass HEADER_PSEUDO_CLASS = 
            PseudoClass.getPseudoClass("header"); //$NON-NLS-1$
    private static final PseudoClass NO_HEADER_PSEUDO_CLASS = 
            PseudoClass.getPseudoClass("no-header"); //$NON-NLS-1$
}
