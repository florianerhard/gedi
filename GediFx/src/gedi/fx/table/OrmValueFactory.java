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
