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

package gedi.fx.utils;

import gedi.fx.utils.AutocompleteListener.AutoCompleteData;
import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.tree.Trie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebView;
import javafx.stage.Popup;


/**
 * How to use it: simply attach as {@link KeyEvent} to any {@link TextInputControl} and add data to it.
 * It reacts to ctrl-space and shows a popup list for the word at the text caret. Once the user chooses one of the suggestions,
 * a {@link #AUTOCOMPLETE_TYPE} event is fired at the {@link TextInputControl}
 * @author erhard
 *
 */
public class AutocompleteListener implements EventHandler<KeyEvent> {
	
	
	private Trie<AutoCompleteData> prefix = new Trie<AutoCompleteData>();
	private Comparator<AutoCompleteData> comp = FunctorUtils.naturalComparator();
	private AutocompletePopup popup;
	
	
	public void addAll(Collection<AutoCompleteData> data) {
		for (AutoCompleteData d : data)
			prefix.put(d.getWord().toUpperCase(),d);
	}

	public void add(AutoCompleteData data) {
		prefix.put(data.getWord().toUpperCase(),data);
	}
	
	public void remove(AutoCompleteData data) {
		prefix.remove(data);
	}

	@Override
	public void handle(KeyEvent event) {
		TextInputControl tx = (TextInputControl) event.getSource();
		String word = readWord(tx);
		
		boolean force = event.isControlDown() && event.getCode()==KeyCode.SPACE;
		if (force || (popup!=null && !popup.word.equals(word))) {
			
			ArrayList<AutoCompleteData> sug = prefix.getByPrefix(word.toUpperCase(),new ArrayList<AutoCompleteData>());
			Collections.sort(sug, comp);
			
			Bounds pos = tx.localToScreen(tx.getBoundsInLocal());
			if (popup!=null) popup.hide();
			if (word.length()>0 || force)
				popup = new AutocompletePopup(word,tx,pos.getMinX(),pos.getMaxY(),sug);
			
		}
		else if (event.getCode()==KeyCode.ESCAPE && popup!=null)
			popup.hide();
		else if (event.getCode()==KeyCode.ENTER && popup!=null && popup.suggestList.getSelectionModel().getSelectedItem()!=null) {
			tx.fireEvent(new AutocompleteEvent(tx,popup.suggestList.getSelectionModel().getSelectedItem()));
			if (isAutoInsert())
				insert(tx,popup.suggestList.getSelectionModel().getSelectedItem());
			popup.hide();
		}
			
	}
	
	
	
	public void insert(TextInputControl tx, AutoCompleteData data) {
		int from;
		for (from=tx.getCaretPosition()-1;from>=0 && Character.isJavaIdentifierPart(tx.getText().charAt(from)); from--);
		from++;
		int to = tx.getCaretPosition();
		
		int newCaret = data.getSuggestion().length();
		int ncl = 0;
		if (data.getSuggestion().contains("(")) {
			newCaret = data.getSuggestion().indexOf('(')+1;
			String within = data.getSuggestion().substring(newCaret);
			int c1 = within.indexOf(',');
			int c2 = within.indexOf(')');
			ncl = c1>0&&c2>0?Math.min(c1, c2):Math.max(c1, c2); 
			if (ncl<0) ncl = 0;
		}
		
		newCaret+=from;
		tx.setText(tx.getText().substring(0, from)+data.getSuggestion()+tx.getText().substring(to));
		tx.selectRange(newCaret, newCaret+ncl);
	}

	private String readWord(TextInputControl tx) {
		int from;
		for (from=tx.getCaretPosition()-1;from>=0 && Character.isJavaIdentifierPart(tx.getText().charAt(from)); from--);
		from++;
		
		return tx.getText().substring(from, tx.getCaretPosition());
	}



	private class AutocompletePopup extends Popup  {

		private ListView<AutoCompleteData> suggestList;
		private HBox ctrls;
		private WebView help;
		
		private String word;
		
		public AutocompletePopup(String word, TextInputControl tx, double x, double y, ArrayList<AutoCompleteData> sug) {
			this.word = word;
			
			setAnchorLocation(AnchorLocation.WINDOW_TOP_LEFT);
			setAnchorX(x);
			setAnchorY(y);
			setAutoFix(false);
			
			ChangeListener<? super Boolean> focusList = (e,o,n)->{
				if (!n) 
					hide();
			};
			tx.focusedProperty().addListener(focusList);
					
			
			suggestList = new ListView<AutoCompleteData>(FXCollections.observableArrayList(sug));
			suggestList.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
			
			ctrls = new HBox();
			ctrls.getChildren().add(suggestList);
			getContent().add(ctrls);

			help = new WebView();
			help.maxHeightProperty().bind(suggestList.heightProperty());
			help.setFocusTraversable(false);
			help.getEngine().setJavaScriptEnabled(false);
			help.setContextMenuEnabled(false);

			suggestList.getSelectionModel().selectedItemProperty().addListener((c,o,n)->{
				if (n!=null) {
					String html = "<body style='background : FFFFE0;'>"+n.getHelpText()+"</body>";
					help.getEngine().loadContent(html);
				}
				if (o==null && n!=null) ctrls.getChildren().add(help);
				if (o!=null && n==null) ctrls.getChildren().remove(help);
			});
			
			
			setOnHiding(e->{
				tx.focusedProperty().removeListener(focusList);
				popup = null;
			});
			suggestList.setOnMouseClicked(e->{
				if (e.getClickCount()>1){
					tx.fireEvent(new AutocompleteEvent(tx,suggestList.getSelectionModel().getSelectedItem()));
					if (isAutoInsert())
						insert(tx,popup.suggestList.getSelectionModel().getSelectedItem());
					hide();
				}
			});
			
			
			show(tx.getScene().getWindow());
			
		}
		
		
	}
	
	
	 private static final EventType<? extends AutocompleteEvent> AUTOCOMPLETE_TYPE =
	            new EventType<>(Event.ANY, "AUTOCOMPLETE");
    public static class AutocompleteEvent extends Event {
        private final AutoCompleteData data;
        
        private static final long serialVersionUID = 2013074324L;

        public static final EventType<?> AUTOCOMPLETE = AUTOCOMPLETE_TYPE;

        /**
         * Creates a new EditEvent instance to represent an edit event. This 
         * event is used for {@link #EDIT_START_EVENT}, 
         * {@link #EDIT_COMMIT_EVENT} and {@link #EDIT_CANCEL_EVENT} types.
         */
        public AutocompleteEvent(TextInputControl source,
                         AutoCompleteData data) {
            super(source, Event.NULL_SOURCE_TARGET, AUTOCOMPLETE);
            this.data = data;
        }
        
        @Override
        public TextInputControl getSource() {
        	return (TextInputControl) super.getSource();
        }


        public AutoCompleteData getData() {
			return data;
		}

        @Override public String toString() {
            return "AutocompleteEvent [ data: " + data.toString() + " ]";
        }
    }
	public static class AutoCompleteData implements Comparable<AutoCompleteData> {
		private String word;
		private String suggestion;
		private String helpText;
		private String category;
		public AutoCompleteData(String word, String suggestion,
				String helpText, String category) {
			this.word = word;
			this.suggestion = suggestion;
			this.helpText = helpText;
			this.category = category;
		}
		public String getWord() {
			return word;
		}
		public String getSuggestion() {
			return suggestion;
		}
		public String getHelpText() {
			return helpText;
		}
		public String getCategory() {
			return category;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((category == null) ? 0 : category.hashCode());
			result = prime * result
					+ ((helpText == null) ? 0 : helpText.hashCode());
			result = prime * result
					+ ((suggestion == null) ? 0 : suggestion.hashCode());
			result = prime * result + ((word == null) ? 0 : word.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AutoCompleteData other = (AutoCompleteData) obj;
			if (category == null) {
				if (other.category != null)
					return false;
			} else if (!category.equals(other.category))
				return false;
			if (helpText == null) {
				if (other.helpText != null)
					return false;
			} else if (!helpText.equals(other.helpText))
				return false;
			if (suggestion == null) {
				if (other.suggestion != null)
					return false;
			} else if (!suggestion.equals(other.suggestion))
				return false;
			if (word == null) {
				if (other.word != null)
					return false;
			} else if (!word.equals(other.word))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return suggestion;
		}
		
		@Override
		public int compareTo(AutoCompleteData o) {
			int re = category.compareTo(o.category);
			if (re==0)
				re = suggestion.compareTo(o.suggestion);
			return re;
		}
		
	}
	
	
	private BooleanProperty autoInsert = new SimpleBooleanProperty(this, "autoInsert", true);
	public final BooleanProperty autoInsertProperty() {
		return this.autoInsert;
	}
	public final boolean isAutoInsert() {
		return this.autoInsertProperty().get();
	}
	public final void setAutoInsert(final boolean autoInsert) {
		this.autoInsertProperty().set(autoInsert);
	}


	

}
