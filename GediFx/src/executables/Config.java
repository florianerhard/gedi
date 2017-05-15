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

package executables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executors;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.tools.ValueExtractor;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationMessage;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.decoration.ValidationDecoration;

import com.sun.corba.se.spi.orbutil.threadpool.ThreadPool;
import com.sun.corba.se.spi.orbutil.threadpool.ThreadPoolManager;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import gedi.app.Gedi;
import gedi.app.config.Configurator;
import gedi.app.config.ConfiguratorExtensionPoint;
import gedi.app.extension.ExtensionContext;
import gedi.fx.FX;
import gedi.util.mutable.MutableTriple;
import gedi.util.mutable.MutableQuadruple;
import gedi.util.mutable.MutableQuintuple;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class Config  {

	public static void main(String[] args) throws InterruptedException {
		Gedi.startup(false);

		FX.startup(args);
		FX.sendCommand((fx)->{

			ValidationSupport valid = new ValidationSupport();
			AdditionalDecorator deco = new AdditionalDecorator();
			valid.setValidationDecorator(deco);
			Dialog dlg = new Dialog();
			String header = "Specific functions may not work properly, when there is any error in the configuration.\nCheck each tab below to correct these errors!";
			dlg.setHeaderText(header);
			dlg.setTitle("Gedi configuration");


			ArrayList<Runnable> okActions = new ArrayList<>();

			TabPane tabPane = new TabPane();
			HashMap<String,MutableTriple<Tab,GridPane, Integer>> tabMap = new HashMap<>();


			Comparator<Configurator> comp = (a,b)->{
				String as = a.getSection();
				String bs = b.getSection();
				if (as.equals(bs)) return 0;
				if (as.equals("Gedi")) return -1;
				if (bs.equals("Gedi")) return 1;
				return as.compareTo(bs);
			};
			comp = comp.thenComparing((a,b)->a.getLabel().compareTo(b.getLabel()));
			
			
			for (Configurator conf : ConfiguratorExtensionPoint.getInstance().getExtensions(new ExtensionContext()).sort(comp).loop()) {

				MutableTriple<Tab,GridPane,Integer> pair = tabMap.computeIfAbsent(conf.getSection(), sec->createTab(tabPane,sec));
				Tab tab = pair.Item1;
				GridPane grid = pair.Item2;
				int index = pair.Item3;
				
				Label lab = new Label(conf.getLabel()+":");
				lab.setMinWidth(Region.USE_PREF_SIZE);
				grid.add(lab, 0, index);

				Label icon = AwesomeDude.createIconLabel(AwesomeIcon.QUESTION_CIRCLE);
				icon.addEventFilter(MouseEvent.MOUSE_PRESSED, me->{
					PopOver po = new PopOver(new Label(conf.getHelp()));
					po.setArrowLocation(ArrowLocation.BOTTOM_RIGHT);
					po.show(icon);
				});
				grid.add(icon, 2, index);
				int sindex = index;
				
				Control ctrl = conf.getControl();
				
				grid.add(ctrl, 1, index++);

				valid.registerValidator((Control)ctrl, (c,v)->{
					String val = conf.validate(v);
					return ValidationResult.fromErrorIf((Control)ctrl, val, val!=null);
				});

				okActions.add(()->{
					Object v = ValueExtractor.getValue(ctrl);
					if (v!=null && conf.validate(v)==null)
						conf.set(v);
				});

				TextArea msg = new TextArea();
				msg.setEditable(false);
				msg.setPrefColumnCount(50);
				msg.setWrapText(true);
				index++;

				Node additional = conf.getAdditionalNode(ctrl);
				if (additional!=null)
					grid.add(additional, 1, index++);

				
				deco.add(ctrl, grid,sindex, tab, msg);
				
				pair.Item3 = index;
			}

			valid.initInitialDecoration();


			// Do some validation (using the Java 8 lambda syntax).
			//	    username.textProperty().addListener((observable, oldValue, newValue) -> {
			//	        actionLogin.disabledProperty().set(newValue.trim().isEmpty());
			//	    });

			dlg.getDialogPane().setContent(tabPane);

			dlg.getDialogPane().getButtonTypes().add(new ButtonType("Ok", ButtonData.OK_DONE));
			dlg.getDialogPane().getButtonTypes().add(new ButtonType("Cancel", ButtonData.CANCEL_CLOSE));


			ButtonType res = (ButtonType) dlg.showAndWait().get();
			if(res.getButtonData()==ButtonData.OK_DONE) {
				for (Runnable r : okActions)
					r.run();
			}
			FX.close();
		});

	}

	private static class AdditionalDecorator implements ValidationDecoration {

		private HashSet<Control> errors = new HashSet<>();
		private HashMap<Control,MutableQuintuple<GridPane,Integer,Label,Label,TextArea>> map = new HashMap<>();
		private HashMap<GridPane,MutableQuadruple<ArrayList<Control>,Tab,Label,Label>> map2 = new HashMap<>();


		public void add(Control ctrl, GridPane grid, int row, Tab tab, TextArea msg) {
			map.put(ctrl, new MutableQuintuple<GridPane, Integer, Label, Label, TextArea>(grid, row, createOk(), createError(), msg));
			map2.computeIfAbsent(grid, g->new MutableQuadruple<>(new ArrayList<>(), tab, createOk(), createError())).Item1.add(ctrl);
		}

		private Label createError() {
			Label error = AwesomeDude.createIconLabel(AwesomeIcon.EXCLAMATION_TRIANGLE);
			error.setStyle("-icons-color: red; -fx-text-fill: linear-gradient(to bottom, derive(-icons-color,20%) 10%, derive(-icons-color,-40%) 80%);"+error.getStyle());
			error.setMinWidth(Region.USE_PREF_SIZE);
			return error;
		}

		private Label createOk() {
			Label ok = AwesomeDude.createIconLabel(AwesomeIcon.CHECK_CIRCLE);
			ok.setStyle("-icons-color: green; -fx-text-fill: linear-gradient(to bottom, derive(-icons-color,20%) 10%, derive(-icons-color,-40%) 80%);"+ok.getStyle());
			ok.setMinWidth(Region.USE_PREF_SIZE);
			return ok;
		}

		@Override
		public void removeDecorations(Control target) {
			MutableQuintuple<GridPane,Integer,Label,Label,TextArea> t = map.get(target);
			t.Item1.getChildren().remove(t.Item3);
			t.Item1.getChildren().remove(t.Item4);
			t.Item1.add(t.Item3,3,t.Item2);
			t.Item1.getChildren().remove(t.Item5);
			errors.remove(target);
			checkGrid(t.Item1);
		}

		@Override
		public void applyValidationDecoration(ValidationMessage message) {
			if (message.getSeverity()==Severity.ERROR) {
				MutableQuintuple<GridPane,Integer,Label,Label,TextArea> t = map.get(message.getTarget());
				t.Item1.getChildren().remove(t.Item3);
				t.Item1.getChildren().remove(t.Item4);
				t.Item1.add(t.Item4,3,t.Item2);
				t.Item5.setText(message.getText());
				t.Item1.add(t.Item5,1,t.Item2+1);
				errors.add(message.getTarget());
				checkGrid(t.Item1);
			}
		}

		private void checkGrid(GridPane grid) {
			MutableQuadruple<ArrayList<Control>, Tab, Label, Label> t = map2.get(grid);
			if (t.Item1.stream().anyMatch(errors::contains)) 
				t.Item2.setGraphic(t.Item4);
			else
				t.Item2.setGraphic(t.Item3);
		}

		@Override
		public void applyRequiredDecoration(Control target) {
		}

	}

	private static MutableTriple<Tab,GridPane,Integer> createTab(TabPane tp, String section) {
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(10, 10, 10, 10));
		grid.setPrefHeight(600);
		grid.setPrefWidth(800);

		ScrollPane scroller = new ScrollPane(grid);
		scroller.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		scroller.setHbarPolicy(ScrollBarPolicy.NEVER);
		
		Tab tab = new Tab();
		tab.setText(section);
		tab.setContent(scroller);
		tp.getTabs().add(tab);
		tab.setClosable(false);
		
		return new MutableTriple<>(tab,grid,0);
	}

}
