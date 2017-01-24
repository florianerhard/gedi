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

/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package gedi.fx.dialog;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;


/**
 * Add buttons and (extended) content as for normal {@link DialogPane}s. After that, call show (the anchor determines, where the dialog is shown)
 * Once it is closed, the result property changes, so simply install a listener to it.
 * @author erhard
 *
 */
public class InsideDialog extends DialogPane {

	private DialogInsidePane parent;
    
    /**************************************************************************
     * 
     * Constructors
     * 
     **************************************************************************/
    
    /**
     * Creates a new DialogPane instance with a style class of 'dialog-pane'.
     */
    public InsideDialog(DialogInsidePane parent) {
    	this.parent = parent;
    }
    
    
    public void show(Node anchor) {
    	parent.showDialog(this,anchor);
    }
    
    boolean hasHeader() {
           return getHeader() != null || isTextHeader();
    }
    private boolean isTextHeader() {
        String headerText = getHeaderText();
        return headerText != null && !headerText.isEmpty();
    }

    public void hide() {
    	parent.hideDialog();
    }
    
    /**************************************************************************
     * 
     * Properties
     * 
     **************************************************************************/

    
    
    private ObjectProperty<ButtonType> result = new SimpleObjectProperty<ButtonType>(this,"result");
    public final ObjectProperty<ButtonType> resultProperty() {
		return this.result;
	}

	public final javafx.scene.control.ButtonType getResult() {
		return this.resultProperty().get();
	}

	public final void setResult(final javafx.scene.control.ButtonType result) {
		this.resultProperty().set(result);
	}
    

    
    /**
     * This method can be overridden by subclasses to create a custom button that
     * will subsequently inserted into the DialogPane button area (created via
     * the {@link #createButtonBar()} method, but mostly commonly it is an instance
     * of {@link ButtonBar}.
     * 
     * @param buttonType The {@link ButtonType} to create a button from.
     * @return A JavaFX {@link Node} that represents the given {@link ButtonType},
     *         most commonly an instance of {@link Button}.
     */
    protected Node createButton(ButtonType buttonType) {
        final Button button = new Button(buttonType.getText());
        final ButtonData buttonData = buttonType.getButtonData();
        ButtonBar.setButtonData(button, buttonData);
        button.setDefaultButton(buttonType != null && buttonData.isDefaultButton());
        button.setCancelButton(buttonType != null && buttonData.isCancelButton());
        button.addEventHandler(ActionEvent.ACTION, ae -> {
            if (ae.isConsumed()) return;
            resultProperty().set(buttonType);
            parent.hideDialog();
        });
        
        return button;
    }
    

	
}
