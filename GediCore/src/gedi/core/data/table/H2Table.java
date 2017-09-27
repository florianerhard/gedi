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

import gedi.util.orm.Orm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class H2Table<T> extends DefaultTableView<T> implements Table<T> {
	
	private TableMetaInformation<T> meta;
	private TableType type;
	
	
	private PreparedStatement insertPrepared;
	private boolean insertBatch = false;

	private PreparedStatement updatePrepared;
	private boolean updateBatch = false;

	private PreparedStatement deletePrepared;
	private boolean deleteBatch = false;

	H2Table(Connection conn, TableMetaInformation<T> meta, TableType type) {
		super(conn,meta.getDataClass());
		this.meta = meta;
		this.type = type;
	}

	@Override
	public TableType getType() {
		return type;
	}
	
	private PreparedStatement getInsert() throws SQLException {
		if (insertPrepared==null) 
			insertPrepared = conn.prepareStatement(Tables.prepareInsertStatement(meta));
		return insertPrepared;
	}

	private PreparedStatement getUpdate() throws SQLException {
		if (insertPrepared==null) 
			insertPrepared = conn.prepareStatement(Tables.prepareUpdateStatement(meta));
		return insertPrepared;
	}
	
	private PreparedStatement getDelete() throws SQLException {
		if (deletePrepared==null) 
			deletePrepared = conn.prepareStatement(Tables.prepareDeleteStatement(meta));
		return deletePrepared;
	}
	
	@Override
	/**
	 * Close all prepared statments!
	 */
	public void close() throws Exception {
		super.close();
		if (insertPrepared!=null)
			insertPrepared.close();
		if (updatePrepared!=null)
			updatePrepared.close();
	}


	@Override
	public TableMetaInformation<T> getMetaInfo() {
		return meta;
	}

	@Override
	public void beginAddBatch() {
		insertBatch = true;
	}

	@Override
	public long add(T row, boolean getKey) {
		try {
			PreparedStatement insertPrepared = getInsert();
			Orm.toPreparedStatement(row, insertPrepared);
			if (insertBatch) {
				Tables.addBatch(insertPrepared);
				return -1;
			}
			Tables.executeUpdate(insertPrepared);
			Tables.commit(conn);
			if (!getKey) return -1;
			ResultSet rs = insertPrepared.getGeneratedKeys();
			if (!rs.next()) return -1;
			long re = rs.getLong(1);
			rs.close();
			return re;
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not add object to table: "+row, e);
			throw new RuntimeException("Could not add object to table: "+row, e);
		}
	}
	
	@Override
	public void endAddBatch() {
		try {
			insertBatch = false;
			Tables.executeBatch(getInsert());
			Tables.commit(conn);
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not add objects to table", e);
			throw new RuntimeException("Could not add objects to table", e);
		}
	}
	
	@Override
	public void beginUpdateBatch() {
		updateBatch = true;
	}
	@Override
	public void update(long id, T row) {
		try {
			PreparedStatement updatePrepared = getUpdate();
			
			int para = Orm.toPreparedStatement(row, updatePrepared);
			updatePrepared.setLong(para+1,id);
			
			if (updateBatch) {
				Tables.addBatch(updatePrepared);
				return;
			}
			Tables.executeUpdate(updatePrepared);
			Tables.commit(conn);
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not add object to table: "+row, e);
			throw new RuntimeException("Could not add object to table: "+row, e);
		}
	}
	
	@Override
	public void endUpdateBatch() {
		try {
			updateBatch = false;
			getUpdate().executeBatch();
			Tables.commit(conn);
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not update objects", e);
			throw new RuntimeException("Could not update objects", e);
		}
	}


	@Override
	public void beginDeleteBatch() {
		deleteBatch = true;
	}


	@Override
	public void delete(long id) {
		try {
			PreparedStatement deletePrepared = getDelete();
			deletePrepared.setLong(1, id);
			if (deleteBatch) {
				Tables.addBatch(deletePrepared);
				return;
			}
			Tables.executeUpdate(deletePrepared);
			Tables.commit(conn);
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not delete from table: "+id, e);
			throw new RuntimeException("Could not delete from table: "+id, e);
		}
	}


	@Override
	public void endDeleteBatch() {
		try {
			deleteBatch = false;
			getDelete().executeBatch();
			Tables.commit(conn);
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not delete objects", e);
			throw new RuntimeException("Could not delete objects", e);
		}
	}

	@Override
	public void drop() {
		try {
			Statement s = conn.createStatement();
			Tables.executeUpdate(s,"drop table "+getMetaInfo().getTablename());
			s.close();
			Table<TableMetaInformation> metaTable = Tables.getInstance().getMetaTable(getType());
			metaTable.where("tablename="+getMetaInfo().getTablename()).delete();
			
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not drop table", e);
			throw new RuntimeException("Could not drop table", e);
		}
	}
	
	
}
