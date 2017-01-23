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
