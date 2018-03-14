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
package gedi.fx;

import gedi.fx.docking.Dockable;
import gedi.fx.docking.DockingPane;
import javafx.scene.Scene;
import javafx.stage.Stage;
import de.jensd.fx.fontawesome.AwesomeStyle;

public class FXWindow extends Stage {

	private DockingPane center;
	
	
	
	public FXWindow() {
		
		center = new DockingPane();
		
		Scene scene = new Scene(center);
		scene.getStylesheets().addAll(AwesomeStyle.BLUE.getStylePath());

		setScene(scene);
		setWidth(800);
		setHeight(600);
		show();
	}
	
	public boolean isClosed() {
		return !isShowing();
	}
	
	public void add(Dockable dockable) {
		center.add(dockable,true);
	}
	

//extends Application {

//	public static void main(String[] args) throws Exception
//	{
//
//		Gedi.startup();
//		launch(args);
////		H2Utils.resolveNestedAliases("N,-D as A,A*2 as D", Tables.getInstance().getSession(TableType.Temporary));
//	}
//
//	private DockingPane center;
//
//	public void start(final Stage stage) throws Exception
//	{
//
//		Dockable ws = new Dockable(new WorkspaceTree(this), "Workspace", false, true);
//		ws.setImage(AwesomeDude.createIconLabel(AwesomeIcon.SITEMAP));
//		DockingPane left = new DockingPane(ws);
//		
//		center = new DockingPane();
//
//
//		Workspace.open("/home/proj/Herpes/Data/RibosomalProfiling/combined/gedi");
//
//		BorderPane rootPane = new BorderPane(null);
//		rootPane.setLeft(left);
//		rootPane.setCenter(center);
//
//		Scene scene = new Scene(rootPane);
//		scene.getStylesheets().addAll(AwesomeStyle.BLUE.getStylePath());
//
//		stage.setMaximized(true);
//		stage.setScene(scene);
//		stage.setTitle("Test");
//		stage.show();
//		
//		Table<MutableDouble> tab = Tables.getInstance().create(TableType.Temporary, Tables.getInstance().buildMeta("", MutableDouble.class));
//		RandomNumbers rnd = new RandomNumbers();
//		for (int i=0; i<200; i++)
//			tab.add(new MutableDouble(rnd.getNormal()));
//
//		spawnDockable(new GediTableView<MutableDouble>(tab.where("(N>1 and N<10) or (N<-1 and N>-10)").page(0, 50,50)), "Test", null);
//		
//	}
//	
//	public void spawnDockable(Node node, String label, Node image) {
//		Dockable dockable = new Dockable(node, label, true, true);
//		dockable.setImage(image);
//		center.add(dockable);
//	}


}
