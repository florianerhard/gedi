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
package gedi.fx.html;

import java.net.MalformedURLException;
import java.net.URI;

import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;

public class HtmlPane extends BorderPane {

	public HtmlPane(URI uri) {
		setPadding(new Insets(5, 0, 5, 0));
		setStyle("-fx-background-color: DAE6F3;");
		
		WebView view = new WebView();
		setCenter(view);
		
		try {
			view.getEngine().load(uri.toURL().toString());
		} catch (MalformedURLException e) {
		}
	}
	
}
