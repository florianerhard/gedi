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

import gedi.util.orm.Orm;
import gedi.util.orm.Orm.OrmInfo;
import sun.util.logging.PlatformLogger;
import sun.util.logging.PlatformLogger.Level;

import com.sun.javafx.property.PropertyReference;
import com.sun.javafx.scene.control.Logging;

import javafx.beans.NamedArg;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;

public class OrmValueFactory<T> implements Callback<CellDataFeatures<T, Object>, ObservableValue<Object>> {

	
	private int column;
	
	public OrmValueFactory(int column) {
		this.column = column;
	}

	@Override
	public ObservableValue<Object> call(CellDataFeatures<T, Object> param) {
		return new ObservableValueBase<Object>() {

			@Override
			public Object getValue() {
				return Orm.getField(param.getValue(), column);
			}
			
		};
		
	}

}
