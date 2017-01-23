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
