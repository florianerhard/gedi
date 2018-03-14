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
package gedi.fx.test;

import gedi.fx.docking.DockGroup;
import gedi.fx.docking.Dockable;
import gedi.fx.docking.DockingPane;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


public class Test extends Application {

	public void start(final Stage stage) throws Exception
	{
		
		DockingPane center = new DockingPane(createDockable(Color.RED,"1",true,true));
		center.add(createDockable(Color.GREEN, "2",true,false),true);
		center.split(createDockable(Color.BLUE,"3",false,true),Orientation.VERTICAL);
		
		
		DockingPane left = new DockingPane(createDockable(Color.RED,"A",true,true), new DockGroup(false));
		left.add(createDockable(Color.GREEN, "B",true,true),true);
		left.add(createDockable(Color.GREEN, "C",true,true),true);
		
		
		BorderPane rootPane = new BorderPane(center);
		rootPane.setLeft(left);
		
		Scene scene = new Scene(rootPane, 300, 300);
        stage.setScene(scene);
        stage.setTitle("Test");
        stage.show();
        
        
        
//        ScenicView.show(scene);
	}

	private Dockable createDockable(Color color, String label, boolean closable, boolean draggable) {
		Pane re = new Pane();
		re.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));

		return new Dockable(re,label,closable,draggable);
	}
	

    public static void main(String[] args)
    {
        launch(args);
    }

}
