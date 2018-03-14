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
package gedi.core.data.table;

import gedi.app.extension.ExtensionContext;
import gedi.core.workspace.action.WorkspaceItemActionExtensionPoint;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.plotting.Aes;
import gedi.util.plotting.GGPlot;
import gedi.util.r.R;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Function;


/**
 * Purpose of this interface:
 * 
 * 1. Algorithms generate tables on the fly as their result
 * 2. Tables are visualized in the GUI (paging, sorting, filtering)
 * 3. They are automatically persisted (usually H2 database)
 * 4. Export to CSV, Excel(?)
 * 5. Import from CSV, Excel(?)
 * 6. Three types of tables: temporary (disappear after shutdown), project and gedi
 * 7. Table have versions, multiple tables with same name and distinct versions are allowed
 * 
 * Much simpler than full jdbc: Single tables, associated with one or an array of dataobjects, always an additional id field as primary key, API does automatic ORM mapping with a simple 
 * mechanism (only supports primitives, Strings, and {@link BinarySerializable}s).
 * 
 * It cannot be projected (i.e. fields selected) nor grouped nor joined with other tables. If you want to do this, simply use sql and jdbc (that's what it is for...)
 * 
 * Tables are not threadsafe, so do not access the same table from different threads (solved by using a {@link ThreadLocal} for connections in {@link Tables})).
 * 
 * @author erhard
 *
 */
public interface Table<T> extends TableView<T> {

	
	TableType getType();
	TableMetaInformation<T> getMetaInfo();

	void beginAddBatch();
	/**
	 * Can be called within {@link #beginAddBatch()} and {@link #endAddBatch()} or not.
	 * @param row
	 * @return a unique row id when not in batch mode!
	 */
	long add(T row, boolean getKey);
	default void add(T row) {
		add(row,false);
	}
	void endAddBatch();
	
	void beginUpdateBatch();
	/**
	 * Can be called within {@link #beginUpdateBatch()} and {@link #endUpdateBatch()} or not.
	 * @param the id form {@link #add(Object)}
	 * @param row
	 * @return a unique row id when not in batch mode!
	 */
	void update(long id, T row);
	void endUpdateBatch();
	
	
	void beginDeleteBatch();
	/**
	 * Can be called within {@link #beginDeleteBatch()} and {@link #endDeleteBatch()} or not.
	 * @param the id form {@link #add(Object)}
	 * @return a unique row id when not in batch mode!
	 */
	void delete(long id);
	void endDeleteBatch();
	
	void drop();
	
	@SuppressWarnings("unchecked")
	default void display() {
		WorkspaceItemActionExtensionPoint.getInstance().get(new ExtensionContext(), Table.class).accept(this);
	}
	
//	default GGPlot ggplot(Aes...aes) {
//		if (getType()!=TableType.R)
//			throw new RuntimeException("Not a table that is accessible to R!");
//		return new GGPlot(this, aes);
//	}
	
}
