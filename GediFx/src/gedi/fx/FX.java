package gedi.fx;


import gedi.fx.docking.Dockable;
import gedi.fx.html.HtmlPane;
import gedi.fx.image.ImagePane;
import gedi.fx.inspector.InspectorPane;
import gedi.fx.results.ResultPane;
import gedi.gui.WindowType;
import gedi.util.r.R;
import gedi.util.userInteraction.results.ResultProducer;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;

public class FX extends Application {

	public static final Logger log = Logger.getLogger( FX.class.getName() );

	public static StackPane getSceneStackPane(Scene scene) {
		Parent root = scene.getRoot();
		if (root instanceof StackPane) return (StackPane) root;
		StackPane re = new StackPane();
		scene.setRoot(re);
		re.getChildren().add(root);
		return re;
	}
	
	private static Object lock = new Object();
	private static FX instance;
	private static ArrayList<Consumer<FX>> beforeStartCommands = new ArrayList<Consumer<FX>>(); 
	private static boolean started = false;
	
	public static void startup(String...args) {
		synchronized (lock) {
			if (started) return;
			started = true;
			new Thread("FXlauncher") {
				@Override
				public void run() {
					launch(FX.class,args);
					synchronized (lock) {
						started = false;
					}
				}
			}.start();
			final Thread launchedFrom = Thread.currentThread();
			new Thread("FXdestroyer") {
				@Override
				public void run() {
					try {
						launchedFrom.join();
					} catch (InterruptedException e) {
					} finally {
						close();
					}
				}
			}.start();
			
			
		}
	}
	
	public static void close(){
		synchronized (lock) {
			if (instance!=null)
				sendCommand(fx->{
					Platform.exit();
				});
		}
	}
	
	public static void sendCommand(Consumer<FX> command) {
		startup();
		synchronized (lock) {
			if (instance==null)
				beforeStartCommands.add(command);
			else instance.command(command);
		}
	}
	

	private FXWindow main;
	private Stage stage;

	
	
	@Override
	public void start(Stage stage) throws Exception {
		synchronized (lock) {
			Platform.setImplicitExit(false);
			instance = this;
			this.stage = stage;	
			for (Consumer<FX> cmd : beforeStartCommands)
				command(cmd);
			beforeStartCommands = null;
		}
	}
	private void command(Consumer<FX> command) {
		Platform.runLater(()->command.accept(this));
	}

	public Stage getPrimaryStage() {
		return stage;
	}
	
	
	public FXWindow getMainOrNew() {
		return main==null?new FXWindow():main;
	}
	
	public FXWindow showDockable(Dockable last, Consumer<Dockable> lastSetter, Dockable dockable, WindowType type) {
		
		if (last==null || last.getScene()==null || !(last.getScene().getWindow() instanceof FXWindow) || ((FXWindow) last.getScene().getWindow()).isClosed() || type==WindowType.New) {
			log.log(Level.FINE, "Create new window for "+dockable.getLabel());
			FXWindow re = new FXWindow();
			re.add(dockable);
			lastSetter.accept(dockable);
			return re;
		}
		
		
		FXWindow re = (FXWindow) last.getScene().getWindow();
		if(type==WindowType.Tab){
			log.log(Level.FINE, "Reuse window for "+dockable.getLabel());
			re.add(dockable);
			lastSetter.accept(dockable);
		} else {
			log.log(Level.FINE, "Replace "+dockable.getLabel());
			last.set(dockable);
			lastSetter.accept(last);
		}
		
		return re;
	}
	
	
	private HashMap<String,Dockable> lastWindowDisplay = new HashMap<String,Dockable>();
	public static void image(BufferedImage image, String label, WindowType type) {
		
		sendCommand(fx->{
			Dockable dockable = new Dockable(new ImagePane(image), label, true, true, AwesomeDude.createIconLabel(AwesomeIcon.IMAGE));
			fx.showDockable(fx.lastWindowDisplay.get(label), d->fx.lastWindowDisplay.put(label, d), dockable, type);
		});
	}
	
	private HashMap<String,Dockable> lastWindowHtml = new HashMap<String,Dockable>();
	public static void html(URI uri, String label, WindowType type) {
		sendCommand(fx->{
			Dockable dockable = new Dockable(new HtmlPane(uri), label, true, true, AwesomeDude.createIconLabel(AwesomeIcon.IMAGE));
			fx.showDockable(fx.lastWindowHtml.get(label), d->fx.lastWindowHtml.put(label, d), dockable, type);
		});
	}
	
	public static void observeResults(ArrayList<ResultProducer> producers) {
		sendCommand(fx->{
			FXWindow win = fx.getMainOrNew();
			
			for (ResultProducer p : producers) {
				ResultPane pane = new ResultPane();
				p.registerConsumer(pane);
				win.add(new Dockable(pane, p.getName(), true, true, AwesomeDude.createIconLabel(AwesomeIcon.TABLE)));
			}
		});
	}
	
	public static void inspect(Object o) {
		sendCommand(fx->
			fx.getMainOrNew().add(new Dockable(new InspectorPane(o),"Inspect "+o.toString(), true,true,AwesomeDude.createIconLabel(AwesomeIcon.SEARCH)))
		);
	}

	
	
}
