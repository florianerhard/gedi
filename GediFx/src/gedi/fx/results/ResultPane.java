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
package gedi.fx.results;


import gedi.fx.image.ImagePane;
import gedi.util.userInteraction.results.ImageResult;
import gedi.util.userInteraction.results.Result;
import gedi.util.userInteraction.results.ResultConsumer;
import gedi.util.userInteraction.results.ResultProducer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class ResultPane extends BorderPane implements ResultConsumer {


	public ResultPane() {
		setPadding(new Insets(5, 0, 5, 0));
		setStyle("-fx-background-color: DAE6F3;");
	}

	@Override
	public void newResult(ResultProducer producer) {
		Platform.runLater(()->{


			setTop(new Label(producer.getName()));
			setBottom(producer.getDescription()==null?null:new Label(producer.getDescription()));

			Result result = producer.getCurrentResult();


			if (result.is(ImageResult.class)) {
				ImageResult img = result.as(ImageResult.class);
				setCenter(new ImagePane(img.getImage()));
			}


		});
	}





}
