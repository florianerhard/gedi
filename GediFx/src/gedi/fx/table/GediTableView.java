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

package gedi.fx.table;


import gedi.core.data.table.ConditionOperator;
import gedi.core.data.table.H2Utils;
import gedi.core.data.table.TableMetaInformation;
import gedi.core.data.table.Tables;
import gedi.fx.FX;
import gedi.fx.dialog.DialogInsidePane;
import gedi.fx.dialog.InsideDialog;
import gedi.fx.utils.AutocompleteListener;
import gedi.fx.utils.AutocompleteListener.AutoCompleteData;
import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;
import gedi.util.io.text.LineIterator;
import gedi.util.orm.Orm;
import gedi.util.orm.Orm.OrmInfo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.controlsfx.control.textfield.CustomTextField;
import org.h2.value.DataType;
import org.h2.value.Value;

import com.sun.javafx.scene.control.TableColumnComparatorBase.TableColumnComparator;

import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;

public class GediTableView<T> extends DialogInsidePane {


	private TableView<T> tableView;

	public GediTableView(gedi.core.data.table.TableView<T> view) {
		setView(view);

		BorderPane bp = new BorderPane();

		bp.setTop(createIconBar());
		bp.setCenter(createTable());
		getChildren().add(bp);


		updateView();
	}


	private AwesomeIcon SEQ = AwesomeIcon.ARROWS_V;
	private AwesomeIcon ONE = AwesomeIcon.ARROWS_H;
	private AwesomeIcon mode = SEQ;

	private Node createIconBar() {


		HBox left = new HBox(3);
		left.setPadding(new Insets(5));
		HBox right = new HBox(3);
		right.setAlignment(Pos.CENTER_RIGHT);
		right.setPadding(new Insets(5));

		Button filter = AwesomeDude.createIconButton(AwesomeIcon.FILTER, "Filter");
		left.getChildren().add(filter);
		filter.setOnAction(e->{
			InsideDialog dia = dialog();
			dia.getButtonTypes().add(ButtonType.APPLY);
			dia.getButtonTypes().add(ButtonType.CANCEL);
			ConditionPane condPane = new ConditionPane(dia,getView().getWhere(),mode);
			dia.setContent(condPane);

			dia.setHeaderText("Filter table");
			dia.setGraphic(AwesomeDude.createIconLabel(AwesomeIcon.FILTER, "32.0"));
			dia.resultProperty().addListener((e1,o,n)->{
				if (n==ButtonType.APPLY) 
					createView(getView().where(((ConditionPane) dia.getContent()).getWhere(),ConditionOperator.NEW));
			});
			
			dia.show(filter);
		});


		Button cols = AwesomeDude.createIconButton(AwesomeIcon.COLUMNS, "Columns");
		left.getChildren().add(cols);

		cols.setOnAction(e->{

			InsideDialog dia = dialog();
			dia.getButtonTypes().add(ButtonType.APPLY);
			dia.getButtonTypes().add(ButtonType.CANCEL);
			ColumnPane colPane = new ColumnPane(dia);
			dia.setContent(colPane);

			dia.setHeaderText("Choose columns");
			dia.setGraphic(AwesomeDude.createIconLabel(AwesomeIcon.COLUMNS, "32.0"));
			dia.resultProperty().addListener((e1,o,n)->{
				if (n==ButtonType.APPLY) 
					createView(getView().select(colPane.getColumns()));
			});
			dia.show(cols);
		});

		Button export = AwesomeDude.createIconButton(AwesomeIcon.SAVE, "Export");
		left.getChildren().add(export);
		export.setOnAction(e->{

		});


		Button astart = AwesomeDude.createIconButton(AwesomeIcon.STEP_BACKWARD);
		Button aleft = AwesomeDude.createIconButton(AwesomeIcon.ARROW_LEFT);

		TextField fromTo = new TextField(getView().getPageFrom()+"-"+getView().getPageTo());
		fromTo.setPrefColumnCount(fromTo.getText().length());
		fromTo.setPadding(new Insets(2));
		fromTo.setAlignment(Pos.CENTER);
		fromTo.addEventFilter(KeyEvent.KEY_RELEASED, e->{
			if (!checkSetFromTo(fromTo.getText(),null)) 
				fromTo.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, new CornerRadii(2),BorderWidths.DEFAULT)));
			else
				fromTo.setBorder(null);
		});
		fromTo.setOnAction(e->{
			long[] re = new long[2];
			if (checkSetFromTo(fromTo.getText(),re)) {
				createView(getView().pageChecked(re[0],re[1],re[1]-re[0]));
			}else {
				fromTo.setText(getView().getPageFrom()+"-"+getView().getPageTo());
				fromTo.setPrefColumnCount(fromTo.getText().length());
			}
		});

		Label totalCount = new Label(" / "+getView().nopage().size());
		viewProperty().addListener((e,o,n)-> {
			if (n==null) totalCount.setText(" / ?");
			else totalCount.setText(" / "+n.nopage().size());
			fromTo.setText(getView().getPageFrom()+"-"+getView().getPageTo());
			fromTo.setPrefColumnCount(fromTo.getText().length());
		});

		Button aright = AwesomeDude.createIconButton(AwesomeIcon.ARROW_RIGHT);
		Button aend = AwesomeDude.createIconButton(AwesomeIcon.STEP_FORWARD);
		right.getChildren().addAll(astart,aleft,fromTo,totalCount,aright,aend);

		astart.setOnAction(e->createView(getView().firstPage()));
		aleft.setOnAction(e->createView(getView().prevPage()));
		aright.setOnAction(e->createView(getView().nextPage()));
		aend.setOnAction(e->createView(getView().lastPage()));

		BorderPane re = new BorderPane();
		re.setLeft(left);
		re.setRight(right);
		return re;
	}



	private boolean checkSetFromTo(String value,long[] parsed) {
		int dash = value.indexOf('-');
		if (dash==-1) return false;
		String from = StringUtils.trim(value.substring(0,dash));
		String to = StringUtils.trim(value.substring(dash+1));
		if (!StringUtils.isInt(from) || !StringUtils.isInt(to)) return false;
		if (parsed!=null) {
			parsed[0] = Long.parseLong(from);
			parsed[1] = Long.parseLong(to);
		}
		return true;
	}

	private TableView<T> createTable() {
		tableView = new TableView<T>();

		tableView.setOnSort(se->{
			TableColumnComparator<T,Object> comp = (TableColumnComparator<T, Object>) tableView.getComparator();

			StringBuilder sb = new StringBuilder();
			if (comp!=null)
				for (int i=0; i<comp.getColumns().size(); i++) {
					if (sb.length()>0) sb.append(", ");
					TableColumn col = (TableColumn)comp.getColumns().get(i);
					sb.append(col.getId());
					if (col.getSortType()==SortType.DESCENDING) 
						sb.append(" DESC");
				}
			createView(getView().orderBy(sb.toString()));
			updateView();
			se.consume();
		});

		viewProperty().addListener((e,n,o)->{if (n!=o) updateView();});


		return tableView;
	}

	private gedi.core.data.table.TableView<T> createView(
			gedi.core.data.table.TableView<T> view) {
		setView(view);
		return view;
	}


	private Class<T> dataClass = null;
	private void updateView() {

		Class<T> cls = getView().getDataClass();
		if (cls!=dataClass) {

			tableView.getColumns().clear();

			OrmInfo info = Orm.getInfo(cls);

			for (int i=0; i<info.getFields().length; i++) {
				TableColumn<T, Object> col = new TableColumn<T, Object>(info.getNames()[i]);
				tableView.getColumns().add(col);
				col.setId(info.getFields()[i].getName());
				col.setCellValueFactory(new OrmValueFactory<T>(i));
			}
			dataClass = cls;
		}

		tableView.getItems().clear();
		tableView.getItems().addAll(getView().iterate().toList());
	}


	private ObjectProperty<gedi.core.data.table.TableView<T>> view = new SimpleObjectProperty<gedi.core.data.table.TableView<T>>(this, "view");


	public final ObjectProperty<gedi.core.data.table.TableView<T>> viewProperty() {
		return this.view;
	}

	public final gedi.core.data.table.TableView<T> getView() {
		return this.viewProperty().get();
	}

	public final void setView(final gedi.core.data.table.TableView<T> view) {
		this.viewProperty().set(view);
	}

	private enum AutocompleteCategories {
		Alias,SQLFunction
	}

	private static final DataFormat TAB_MOVE = new DataFormat("GediTable:ColMove");
	private class ColumnPane extends GridPane {

		private AutocompleteListener autoComplete;
		private Insets margin = new Insets(5);
		private HashSet<String> columns = new HashSet<String>();
		private HashMap<CustomTextField,CustomTextField> textToLabel = new HashMap<CustomTextField, CustomTextField>();

		private int errors = 0;
		
		private InsideDialog dia;
		
		public ColumnPane(InsideDialog dia) {
			this.dia = dia;
			setAlignment(Pos.BASELINE_LEFT);

			TableMetaInformation<Object> meta = getView().getTable().getMetaInfo();
			for (int m=0; m<meta.getNumColumns(); m++)
				columns.add(meta.getColumnName(m));

			autoComplete = new AutocompleteListener();
			for (String s : columns)
				autoComplete.add(new AutoCompleteData(s, s, s+" is a table column", AutocompleteCategories.Alias.toString()));
			
			autoComplete.addAll(getH2Help().values());
			
			String[] select = null;
			if (getView().getSelect()==null || getView().getSelect().length()==0) {
				select = new String[meta.getNumColumns()];
				for (int i=0; i<meta.getNumColumns(); i++)
					select[i] = getView().getTable().getMetaInfo().getColumnName(i);
			} else
				select = StringUtils.split(getView().getSelect(),',');


			Label name = new Label("Name");
			name.setFont(Font.font(null, FontWeight.EXTRA_BOLD, -1));
			add(name,2,0);
			GridPane.setMargin(name, margin);

			Label label = new Label("Label");
			label.setFont(Font.font(null, FontWeight.EXTRA_BOLD, -1));
			add(label,3,0);
			GridPane.setMargin(label, margin);

			int row = 1;
			for (int i=0; i<select.length; i++) {
				select[i] = StringUtils.trim(select[i]);
				int AS = select[i].toUpperCase().indexOf(" AS ");

				if (select[i].equals("*")) {
					for (int m=0; m<meta.getNumColumns(); m++)
						createColumn(meta.getColumnName(m), null, row++);
				} else if (AS>=0) {
					createColumn(select[i].substring(0, AS), select[i].substring(AS+4), row++);
				} else
					createColumn(select[i], null, row++);
			}

			createAdd(row);

			setOnDragOver(this::handleDragOver);
			
			
			
		}

		private int DRAGGED_INDEX = -1;
		private void dragDetected(Event event) {
			Node node = (Node) event.getSource();
			Dragboard db = node.startDragAndDrop(TransferMode.MOVE);

			WritableImage snapShot = node.snapshot(new SnapshotParameters(), null);

			db.setDragView(snapShot, snapShot.getWidth()*2, snapShot.getHeight() * 1);

			ClipboardContent content = new ClipboardContent();
			String data = System.identityHashCode(node) + "";
			if (data != null) {
				content.put(TAB_MOVE, data);
			}
			db.setContent(content);
			DRAGGED_INDEX = getRowIndex(node);
		}


		@SuppressWarnings("all")
		void handleDragOver(DragEvent event) {
			event.consume();

			double y = event.getY() ;

			int dropRow = DRAGGED_INDEX;
			for (Node n : getChildren()) {
				if (n instanceof Label && n.getUserData()==n) {
					if (getRowIndex(n)==DRAGGED_INDEX-1) {
						Bounds b = n.getBoundsInParent();
						double by = b.getMinY()+b.getHeight()/2;
						if (y<by)
							dropRow = DRAGGED_INDEX-1;
					}
					if (getRowIndex(n)==DRAGGED_INDEX+1) {
						Bounds b = n.getBoundsInParent();
						double by = b.getMinY()+b.getHeight()/2;
						if (y>by)
							dropRow = DRAGGED_INDEX+1;
					}
				}
			}
			if (dropRow != -1 && DRAGGED_INDEX!=dropRow) {
				moveRow(DRAGGED_INDEX,dropRow);
				DRAGGED_INDEX = dropRow;
				event.acceptTransferModes(TransferMode.MOVE);
			}
		}


		private void moveRow(int from, int to) {
			Iterator<Node> it = getChildren().iterator();
			while (it.hasNext()) {
				Node ch = it.next();
				int row = getRowIndex(ch);
				if (row==from)
					setRowIndex(ch, to);
				else if (from<to && row>from && row<=to)
					setRowIndex(ch,row-1);
				else if (from>to && row<from && row>=to)
					setRowIndex(ch,row+1);
			}
		}

		private HashMap<CustomTextField,AutoCompleteData> autoCompleteAliases = new HashMap<CustomTextField, AutoCompleteData>();

		private boolean validateField(CustomTextField field,CustomTextField label) {
			if (autoCompleteAliases.containsKey(label))
				autoComplete.remove(autoCompleteAliases.remove(label));
			autoCompleteAliases.put(label, new AutoCompleteData(label.getText(), label.getText(), label.getText()+" is a table column", AutocompleteCategories.Alias.toString()));
			
			HashSet<String> aliases = new HashSet<String>(columns);
			Iterator<Node> it = getChildren().iterator();
			while (it.hasNext()) {
				Node n = it.next();
				if (n!=label && n instanceof CustomTextField) {
					CustomTextField t = (CustomTextField) n;
					ErrorField indi = (ErrorField) t.getRight();
					if (!indi.isField && indi.error==null && t.getText().length()>0) {
						aliases.add(t.getText());
					}
				}
			}
			
			boolean wasValid = ((ErrorField) field.getRight()).getError()==null && ((ErrorField) label.getRight()).getError()==null;
			
			
			String error = H2Utils.checkParseField(field.getText(), aliases, Tables.getInstance().getSession(getView().getTable().getType()));
			((ErrorField) field.getRight()).setError(error);
			
			if (!StringUtils.isJavaIdentifier(label.getText()) && !(label.getText().length()==0 && StringUtils.isJavaIdentifier(field.getText()))) 
				((ErrorField) label.getRight()).setError("Not a valid identifier!");
			else if (aliases.contains(label.getText())) {
				((ErrorField) label.getRight()).setError("Ambiguous label!");
			} else 
				((ErrorField) label.getRight()).removeError();
				
			
			boolean valid = ((ErrorField) field.getRight()).getError()==null && ((ErrorField) label.getRight()).getError()==null;
			
			if (!valid) {
				autoComplete.remove(autoCompleteAliases.get(label));
				if (wasValid) {
					errors++;
					updateApply();
				}
				
				return false;
			}
			else {
				autoComplete.add(autoCompleteAliases.get(label));
				if (!wasValid) {
					errors--;
					updateApply();
				}	
				return true;
			}
		}

		/**
		 * Returns number of invalids!
		 * @return
		 */
		private int validateAll() {
			int re = 0;
			for (CustomTextField text : textToLabel.keySet())
				if (!validateField(text, textToLabel.get(text)))
					re++;
			errors = re;
			updateApply();
			return re;
		}


		private void updateApply() {
			dia.lookupButton(ButtonType.APPLY).setDisable(errors>0);
		}


		private CustomTextField createColumn(String name, String label, int row) {

			Label mover = AwesomeDude.createIconLabel(AwesomeIcon.ARROWS_V);
			mover.setUserData(mover);
			add(mover,0,row);
			GridPane.setMargin(mover, margin);
			mover.setOnDragDetected(this::dragDetected);
			mover.setCursor(Cursor.MOVE);

			Button but = AwesomeDude.createIconButton(AwesomeIcon.MINUS_CIRCLE);
			but.setStyle("-icons-color: rgb(255,0,0);");
			add(but,1,row);
			GridPane.setMargin(but, margin);

//			if (name==null) {
				CustomTextField text = new CustomTextField();
				if (name!=null)
					text.setText(name);
				text.setRight(new ErrorField(text,true));
				text.setPrefColumnCount(20);
				text.setOnKeyReleased(autoComplete);
				add(text,2,row);
				GridPane.setMargin(text, margin);

				CustomTextField labelF = new CustomTextField();
				if (label!=null)
					labelF.setText(label);
				labelF.setRight(new ErrorField(labelF,false));
				labelF.setPrefColumnCount(10);
				add(labelF,3,row);
				GridPane.setMargin(labelF, margin);

				text.textProperty().addListener((e,o,n)->validateField(text,labelF));
				labelF.textProperty().addListener((e,o,n)->validateField(text,labelF));
//				text.setOnKeyReleased(e->validateField(text,labelF));
//				labelF.setOnKeyReleased(e->validateLabel(labelF,text));
				validateField(text,labelF);

//			} else {
//				Label text = new Label(name);
//				add(text,2,row);
//				GridPane.setMargin(text, margin);
//
//				Label labelF = new Label(label);
//				add(labelF,3,row);
//				GridPane.setMargin(labelF, margin);
//			}

			but.setOnAction(e->{
				int index = getRowIndex(mover);
				Iterator<Node> it = getChildren().iterator();
				while (it.hasNext()) {
					Node ch = it.next();
					if (getRowIndex(ch)==index) {
						it.remove();
						if (autoCompleteAliases.containsKey(ch))
							autoComplete.remove(autoCompleteAliases.remove(ch));
						textToLabel.remove(ch);
					} else if (getRowIndex(ch)>index)
						setRowIndex(ch,getRowIndex(ch)-1);
				}
				validateAll();
			});

			textToLabel.put(text, labelF);
			return text;

		}

		private void createAdd(int row) {

			Button but = AwesomeDude.createIconButton(AwesomeIcon.PLUS_CIRCLE);
			but.setStyle("-icons-color: rgb(0,255,0);");
			add(but,1,row);
			GridPane.setMargin(but, margin);


			but.setOnAction(e->{
				getChildren().remove(but);
				createColumn(null,null, row).requestFocus();
				createAdd(row+1);
			});
		}

		public String[] getColumns() {
			ArrayList<String> columns = new ArrayList<String>();
			ArrayList<String> labels = new ArrayList<String>();

			Iterator<Node> it = getChildren().iterator();
			while (it.hasNext()) {
				Node ch = it.next();
				if (getRowIndex(ch)==0) continue;
				
				if (getColumnIndex(ch)==2)
					setList(columns,getRowIndex(ch)-1,ch);
				else if (getColumnIndex(ch)==3)
					setList(labels,getRowIndex(ch)-1,ch);
			}

			String[] re = new String[columns.size()];
			for (int i=0; i<re.length; i++)
				if (labels.get(i).length()>0)
					re[i] = columns.get(i)+" AS "+labels.get(i);
				else
					re[i] = columns.get(i);
			
			return re;
		}

		private void setList(ArrayList<String> l, int index,
				Node ch) {

			try {
				while (index>=l.size()) l.add("");
				String text = ReflectionUtils.invoke(ch, "getText");
				l.set(index,text);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

	}
	
	
	private class ConditionPane extends GridPane {

		private AutocompleteListener autoComplete;
		private Insets margin = new Insets(5);
		private int errors = 0;
		private HashSet<String> columns = new HashSet<String>();
		
		private InsideDialog dia;
		
		private HashSet<CustomTextField> fields = new HashSet<CustomTextField>();
		private IntFunction<String> typeChecker = t->t==Value.BOOLEAN?null:"Type must be boolean, but is "+DataType.getDataType(t).name;
		
		public ConditionPane(InsideDialog dia, String where, AwesomeIcon mode) {
			this.dia = dia;
			setAlignment(Pos.BASELINE_LEFT);

			TableMetaInformation<Object> meta = getView().getTable().getMetaInfo();
			for (int m=0; m<meta.getNumColumns(); m++)
				columns.add(meta.getColumnName(m));
			
			String[] select = null;
			if (getView().getSelect()==null || getView().getSelect().length()==0) {
				select = new String[meta.getNumColumns()];
				for (int i=0; i<meta.getNumColumns(); i++)
					select[i] = getView().getTable().getMetaInfo().getColumnName(i);
			} else
				select = StringUtils.split(getView().getSelect(),',');
			
			for (int i=0; i<select.length; i++) {
				select[i] = StringUtils.trim(select[i]);
				int AS = select[i].toUpperCase().indexOf(" AS ");

				if (AS>=0) 
					columns.add(select[i].substring(AS+4));
			}

			autoComplete = new AutocompleteListener();
			for (String s : columns)
				autoComplete.add(new AutoCompleteData(s, s, s+" is a table column or alias", AutocompleteCategories.Alias.toString()));
			autoComplete.addAll(getH2Help().values());
			
			if (mode==SEQ) {
				Label name = new Label("Sequence of filters");
				name.setFont(Font.font(null, FontWeight.EXTRA_BOLD, -1));
				add(name,2,0);
				GridPane.setMargin(name, margin);
				
				LinkedList<String> conditionsList = new LinkedList<String>();
				LinkedList<ConditionOperator> conjunctionsList = new LinkedList<ConditionOperator>();
				H2Utils.parseConditionSequence(where,conditionsList,conjunctionsList, Tables.getInstance().getSession(getView().getTable().getType()));
				String[] conditions = conditionsList.toArray(new String[0]);
				ConditionOperator[] conjunctions = conjunctionsList.toArray(new ConditionOperator[0]);
				
				for (int i=0; i<conditions.length; i++) {
					createCondition(i==0?ConditionOperator.NEW:conjunctions[i-1],conditions[i], i+1);
				}
				createAdd(conditions.length+1);
				updateDelete();
			} else {
				Label name = new Label("Filter");
				name.setFont(Font.font(null, FontWeight.EXTRA_BOLD, -1));
				add(name,2,0);
				GridPane.setMargin(name, margin);
				
				createCondition(ConditionOperator.NEW,where, 1);
			}
		}


		private boolean validateField(CustomTextField field) {
			
			
			boolean wasValid = ((ErrorField) field.getRight()).getError()==null;
			String error = H2Utils.checkParseField(field.getText(), columns, typeChecker ,Tables.getInstance().getSession(getView().getTable().getType()));
			((ErrorField) field.getRight()).setError(error);
			boolean valid = ((ErrorField) field.getRight()).getError()==null;
			
			if (!valid) {
				if (wasValid) {
					errors++;
					updateApply();
				}
				
				return false;
			}
			else {
				if (!wasValid) {
					errors--;
					updateApply();
				}	
				return true;
			}
		}

		/**
		 * Returns number of invalids!
		 * @return
		 */
		private int validateAll() {
			int re = 0;
			for (CustomTextField text : fields)
				if (!validateField(text))
					re++;
			errors = re;
			updateApply();
			return re;
		}
		
		private void updateDelete() {
			Iterator<Node> it = getChildren().iterator();
			Button bottom = null;
			while (it.hasNext()) {
				Node ch = it.next();
				if (ch.getUserData()==removeButton) {
					if (bottom==null || getRowIndex(ch)>getRowIndex(bottom))
						bottom = (Button) ch;
					ch.setVisible(false);
				}
			}
			if (bottom!=null) bottom.setVisible(true);
		}


		private void updateApply() {
			dia.lookupButton(ButtonType.APPLY).setDisable(errors>0);
		}

		private AwesomeIcon AND = AwesomeIcon.CHEVRON_CIRCLE_UP;
		private AwesomeIcon OR = AwesomeIcon.CHEVRON_CIRCLE_DOWN;

		private Object removeButton = new Object();

		
		private CustomTextField createCondition(ConditionOperator op, String cond, int row) {

			if (op!=ConditionOperator.NEW) {
				Button but = AwesomeDude.createIconButton(AwesomeIcon.MINUS_CIRCLE);
				but.setStyle("-icons-color: rgb(255,0,0);");
				add(but,0,row);
				GridPane.setMargin(but, margin);
				but.setVisible(false);
				but.setUserData(removeButton);
				
				but.setOnAction(e->{
					int index = getRowIndex(but);
					Iterator<Node> it = getChildren().iterator();
					while (it.hasNext()) {
						Node ch = it.next();
						if (getRowIndex(ch)==index) {
							it.remove();
						} else if (getRowIndex(ch)>index)
							setRowIndex(ch,getRowIndex(ch)-1);
					}
					validateAll();
					updateDelete();
				});
				
				Button junc = AwesomeDude.createIconButton(ConditionOperator.AND==op?AND:OR);
				add(junc,1,row);
				junc.setUserData(ConditionOperator.AND==op?AND:OR);
				GridPane.setMargin(junc, margin);
				junc.setOnAction(e->{
					AwesomeIcon set = junc.getUserData()==AND?OR:AND;
					junc.setGraphic(AwesomeDude.createIconLabel(set));
					junc.setUserData(set);
				});
			} else {
				Button seqOne = AwesomeDude.createIconButton(mode);
				add(seqOne,0,row);
				GridPane.setMargin(seqOne, margin);
				seqOne.setOnAction(e->{
					mode = mode==ONE?SEQ:ONE;
					dia.setContent(new ConditionPane(dia,getWhere(),mode));
				});
			}

			
			CustomTextField text = new CustomTextField();
			if (cond!=null)
				text.setText(cond);
			text.setRight(new ErrorField(text,true));
			text.setPrefColumnCount(20);
			text.setOnKeyReleased(autoComplete);
			add(text,2,row);
			GridPane.setMargin(text, margin);


			text.textProperty().addListener((e,o,n)->validateField(text));
			validateField(text);


			
			

			return text;

		}

		private void createAdd(int row) {

			Button but = AwesomeDude.createIconButton(AwesomeIcon.PLUS_CIRCLE);
			but.setStyle("-icons-color: rgb(0,255,0);");
			add(but,1,row);
			GridPane.setMargin(but, margin);


			but.setOnAction(e->{
				getChildren().remove(but);
				createCondition(row==1?ConditionOperator.NEW:ConditionOperator.AND,null, row).requestFocus();
				createAdd(row+1);
				updateDelete();
			});
		}

		public String getWhere() {
			ArrayList<String> conjunctions = new ArrayList<String>();
			ArrayList<String> conditions = new ArrayList<String>();

			Iterator<Node> it = getChildren().iterator();
			while (it.hasNext()) {
				Node ch = it.next();
				if (getRowIndex(ch)==0) continue;
				
				if (getColumnIndex(ch)==2)
					setList(conditions,getRowIndex(ch)-1,(TextField)ch);
				else if (getColumnIndex(ch)==1)
					setList(conjunctions,getRowIndex(ch)-1,(Button)ch);
			}

			StringBuilder sb = new StringBuilder();
			for (int i=0; i<conditions.size()-1; i++)
				sb.append("(");
			for (int i=0; i<conditions.size(); i++) {
				if (i>0) sb.append(" ").append(conjunctions.get(i)).append(" ");
				sb.append(conditions.get(i));
				if (i>0) sb.append(")");
			}
			return H2Utils.normalizeExpression(sb.toString(), Tables.getInstance().getSession(getView().getTable().getType()));
		}

		private void setList(ArrayList<String> l, int index,
				TextField ch) {
			while (index>=l.size()) l.add("");
			l.set(index,ch.getText());
		}

		private void setList(ArrayList<String> l, int index,
				Button ch) {
			while (index>=l.size()) l.add("");
			l.set(index,ch.getUserData()==AND?"AND":"OR");
		}
	}

	private static class ErrorField extends Label {
		private CustomTextField parent;
		private boolean isField;
		public ErrorField(CustomTextField parent, boolean isField) {
			this.parent = parent;
			this.isField = isField;
			getStyleClass().add("awesome");
			setStyle("-fx-font-family: FontAwesome; -fx-font-size: " + 16.0 + ";");
			update();
		}
		private String error = null;
		public void setError(String error) {
			this.error = error;
			update();
		}
		public String getError() {
			return error;
		}
		public void removeError() {
			error = null;
			update();
		}
		private void update() {
			if (error==null) {
				parent.setTooltip(null);
				setText(AwesomeIcon.CHECK_CIRCLE.toString());
				setStyle("-fx-font-family: FontAwesome; -fx-font-size: " + 16.0 + "; -icons-color: rgb(0,255,0);");
				
			} else {
				parent.setTooltip(new Tooltip(error));
				
				setText(AwesomeIcon.EXCLAMATION_CIRCLE.toString());	
				setStyle("-fx-font-family: FontAwesome; -fx-font-size: " + 16.0 + "; -icons-color: rgb(255,0,0);");
			}
			
		}
		
	}

	
	private static HashMap<String,AutoCompleteData> h2help = null;
	private static HashMap<String,AutoCompleteData> getH2Help() {
		if (h2help==null) {
			h2help = new HashMap<String, AutoCompleteData>();
			HashSet<String> allowedCategories = new HashSet<String>(Arrays.asList("Numeric Functions","String Functions","Time and Date Functions"));
			try (LineIterator it = new LineIterator(GediTableView.class.getResourceAsStream("/help/h2help.html"))) {
//			try (LineIterator it = new LineIterator(WorkspaceTreeNode.class.getResourceAsStream("/icons.properties"), "#")){
				
				String fname = "";
				String bnf = "";
				StringBuilder help = new StringBuilder();
				while (it.hasNext()) {
					String l = StringUtils.trim(it.next());
					if (l.startsWith("<h3")) {fname = extractInnerHtml(l); help.delete(0, help.length());}
					else if (l.startsWith("<a href=") && l.endsWith("</a><br />") && allowedCategories.contains(fname))
						h2help.put(extractInnerHtml(l), null);
					else if (l.equals("<pre name=\"bnf\" style=\"display: none\">")) bnf = correctBnf(it.next().replaceAll("<.*?>", ""));
					else if (l.startsWith("<p ") || l.startsWith("<p>")) help.append(l+"\n");
					else if (l.length()==0 && h2help.containsKey(fname))
						h2help.put(fname, new AutoCompleteData(fname, bnf, help.toString(), AutocompleteCategories.SQLFunction.toString()));
				}
			} catch (IOException e) {
				FX.log.log(Level.SEVERE, "Could not read H2 database help file!", e);
			}
			
		}
		return h2help;
	}

	private static String correctBnf(String bnf) {
		bnf = bnf.replaceFirst("\\{\\s+(.*?)\\s+\\|.*\\}\\s+", "$1");
		bnf = bnf.replaceFirst("\\(\\s+", "(");
		bnf = bnf.replaceFirst("\\s+\\(", "(");
		bnf = bnf.replaceFirst("\\s+\\)", ")");
		bnf = bnf.replaceFirst("\\)\\s+", ")");
		bnf = bnf.replaceFirst("\\s+,", ",");
		bnf = bnf.replaceFirst(",\\s+", ", ");
		return bnf;
	}


	private static Pattern tag = Pattern.compile("<.*>(.*)</.*>");
	private static String extractInnerHtml(String h) {
		Matcher m = tag.matcher(h);
		if (m.find()) return m.group(1);
		return "";
	}

}
