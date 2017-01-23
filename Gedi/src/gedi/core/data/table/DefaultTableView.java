package gedi.core.data.table;

import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.functions.ExtendedIterator;
import gedi.util.orm.Orm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.logging.Level;

import net.sf.cglib.beans.BeanGenerator;

import org.h2.value.DataType;
import org.h2.value.Value;

public class DefaultTableView<T> implements TableView<T> {

	protected Connection conn;
	private Class<T> dataClass;
	private Table<?> table;

	private String select;
	private String where;
	private String order;
	private long limitFrom = -1;
	private long limitTo = -1;
	private long desiredPageSize = -1;

	
	DefaultTableView(Connection conn, Class<T> dataClass, Table<?> baseTable) {
		this.conn = conn;
		this.dataClass = dataClass;
		this.table = baseTable;
	}

	/**
	 * For superclasses that are tables
	 * @param conn
	 * @param dataClass
	 */
	DefaultTableView(Connection conn, Class<T> dataClass) {
		this.conn = conn;
		this.dataClass = dataClass;
		this.table = (Table<?>) this;
	}
	
	@Override
	public String getWhere() {
		return where;
	}
	
	@Override
	public String getSelect() {
		return select;
	}
	
	private String tableName(){
		return table.getMetaInfo().getTablename();
	}
	
	@Override
	public <A> Table<A> getTable() {
		return (Table<A>)table;
	}
	
	@Override
	public void close() throws Exception {
	}

	@Override
	public Class<T> getDataClass() {
		return dataClass;
	}

	private String getSql(String proj, String orderby, String limit) {
		return Tables.getSelectStatement(tableName(), proj, where, orderby, limit);
	}
	
	private String getSql(String limit) {

		if (select==null || select.length()==0) 
			return getSql("*", order,limit); 

		String sql = Tables.getSelectStatement(tableName(), H2Utils.resolveNestedAliases(select,Tables.getInstance().getSession(table.getType())), where, order, limit);
		return sql;
	}

	@Override
	public long size() {
		if (isPage())
			return getPageTo()-getPageFrom();
			
		try {
			Statement s = conn.createStatement();
			ResultSet rs = Tables.executeQuery(s, getSql("count(1)","",""));
			long re = -1;
			if (rs.next())
				re = rs.getLong(1);
			rs.close();
			s.close();
			
			return re;
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not fetch data", e);
			throw new RuntimeException("Could not fetch data", e);
		}
	}
	
	
	@Override
	public long delete() {
		try {
			Statement s = conn.createStatement();
			long re = Tables.executeUpdate(s,getDeleteSql());
			s.close();
			Tables.commit(conn);
			return re;
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not fetch data", e);
			throw new RuntimeException("Could not fetch data", e);
		}
	}


	
	private String getDeleteSql() {
		if (!hasOrder() || !hasLimit()) 
			return Tables.getDeleteStatement(tableName(), where, getLimitString());
		// with order and limit: create sub select query for ids and delete them
		return Tables.getDeleteStatement(tableName(),
				Tables.ID_NAME+" in ("+Tables.getSelectStatement(tableName(), Tables.ID_NAME, where, order, getLimitString())+")", 
				"");
	}

	@Override
	public T getFirst() {
		try {
			Statement s = conn.createStatement();
			ResultSet rs = Tables.executeQuery(s,getSql("1"));
			T re = null;
			if (rs.next())
				re = Orm.fromResultSet(rs, dataClass);
			rs.close();
			s.close();
			return re;

		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not fetch data", e);
			throw new RuntimeException("Could not fetch data", e);
		}
	}

	@Override
	public ExtendedIterator<T> iterate() {
		try {
			return new OrmResultSetIterator(getSql(getLimitString()));
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not fetch data", e);
			throw new RuntimeException("Could not fetch data", e);
		}
	}
	
	@Override
	public <A> ExtendedIterator<A> iterate(int column) {
		try {
			return FunctorUtils.mappedIterator(new OrmResultSetIterator(getSql(getLimitString())), FunctorUtils.ormExtractFunction(column));
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not fetch data", e);
			throw new RuntimeException("Could not fetch data", e);
		}
	}
	
	private boolean hasLimit() {
		return limitFrom>-1;
	}
	
	private boolean hasOrder() {
		return order!=null && order.length()>0;
	}
	private boolean hasWhere() {
		return where!=null && where.length()>0;
	}

	
	private String getLimitString() {
		if (!hasLimit()) return "";
		return limitFrom+","+(limitTo-limitFrom);
	}


	private class OrmResultSetIterator implements ExtendedIterator<T>, AutoCloseable {

		Statement s;
		ResultSet rs;
		boolean didNext = false;
		boolean hasNext = false;

		public OrmResultSetIterator(String sql) throws SQLException {
			s = conn.createStatement();
			rs = Tables.executeQuery(s,sql);
		}

		@Override
		public void close() throws Exception {
			rs.close();
			s.close();
		}

		@Override
		public boolean hasNext() {
			try {
				if (!didNext) {
					hasNext = rs.next();
					didNext = true;
				}
				return hasNext;
			} catch (SQLException e) {
				Tables.log.log(Level.SEVERE, "Could not fetch data", e);
				throw new RuntimeException("Could not fetch data", e);
			}
		}

		@Override
		public T next() {
			try {
				if (!didNext) {
					rs.next();
				}
				didNext = false;
				T re = Orm.fromResultSet(rs, dataClass);
				return re;
			} catch (SQLException e) {
				Tables.log.log(Level.SEVERE, "Could not fetch data", e);
				throw new RuntimeException("Could not fetch data", e);
			}
		}

	}
	

	@Override
	public Spliterator<T> spliterate() {
		return Spliterators.spliterator(iterate(), size(), Spliterator.IMMUTABLE|Spliterator.NONNULL|Spliterator.ORDERED|Spliterator.DISTINCT);
	}


	
	@Override
	public <A> TableView<A> adapt(A dataObject) {
		DefaultTableView<A> re = (DefaultTableView<A>) copy();
		re.dataClass = (Class<A>) dataObject.getClass();
		return re;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <A> TableView<A> select(String... sql) {
		try {
				
			DefaultTableView<A> re = (DefaultTableView<A>) copy();
			
			re.select = ArrayUtils.concat(",", sql);
			
			Statement s = conn.createStatement();
			ResultSet rs = Tables.executeQuery(s,re.getSql("1"));
			ResultSetMetaData meta = rs.getMetaData();
			re.dataClass = null;
			
			if (meta.getColumnCount()==1) {
				int type = DataType.convertSQLTypeToValueType(meta.getColumnType(1));
				if (type==Value.INT) {
					re.dataClass = (Class<A>) Integer.class;
				}
				else if (type==Value.LONG) {
					re.dataClass = (Class<A>) Long.class;
				}
				else if (type==Value.DOUBLE) {
					re.dataClass = (Class<A>) Double.class;
				}
				else if (type==Value.BOOLEAN) {
					re.dataClass = (Class<A>) Boolean.class;
				}
				else if (type==Value.STRING) {
					re.dataClass = (Class<A>) String.class;
				}
			}
			if (re.dataClass==null) {
				BeanGenerator gen = new BeanGenerator();
				gen.setNamingPolicy(new GediNamingPolicy(getDataClass().getSimpleName()+"Projection"));
				for (int i=0; i<meta.getColumnCount(); i++) {
					int type = DataType.convertSQLTypeToValueType(meta.getColumnType(i+1));
					String lab = meta.getColumnLabel(i+1);
					
					if (!StringUtils.isJavaIdentifier(lab))
						throw new RuntimeException(lab+" is not a valid identifier! Use AS!");
					
					int index = Orm.getInfo(dataClass).getIndex(meta.getColumnName(i+1));
					if (index>=0)
						gen.addProperty(lab, Orm.getInfo(dataClass).getFields()[index].getType());
					else 
						gen.addProperty(lab, Orm.getClassFromH2Type(type));
				}
				re.dataClass = (Class<A>) gen.createClass();
			}
		
			rs.close();
			s.close();
			
			return re;
		} catch (SQLException e) {
			Tables.log.log(Level.SEVERE, "Could not do projection", e);
			throw new RuntimeException("Could not do projection", e);
		}
	}

//	@SuppressWarnings("unchecked")
//	@Override
//	public <A> TableView<A> selectColumn(String columnName) {
//		try {
//			int index = ArrayUtils.find(Orm.getOrmFieldNames(dataClass),columnName);
//			if (index==-1) throw new Exception("Column "+columnName+" unknown!");
//			
//			DefaultTableView<A> re = (DefaultTableView<A>) newView();
//			
//			re.selects = ArrayUtils.append(selects, columnName);
//			re.dataClass = (Class<A>) Orm.getOrmFieldClasses(dataClass)[index];
//			
//			return re;
//		} catch (Exception e) {
//			Tables.log.log(Level.SEVERE, "Could not do projection", e);
//			throw new RuntimeException("Could not do projection", e);
//		}
//	}
	
	@Override
	public DefaultTableView<T> copy() {
		DefaultTableView<T> re = new DefaultTableView<T>(conn, dataClass, table);
		
		re.select = select;
		re.where = where;
		re.order = order;
		re.limitFrom = limitFrom;
		re.limitTo = limitTo;
		
		return re;
	}
	
	@Override
	public TableView<T> where(String sqlCondition, ConditionOperator op) {
		DefaultTableView<T> re = copy();
		if (op==ConditionOperator.NEW)
			re.where = sqlCondition;
		else 
			re.where = where==null||where.length()==0?sqlCondition:("("+where+") "+op.toString()+" ("+sqlCondition+")");
		return re;
	}
	
	
	@Override
	public TableView<T> orderBy(String sqlOrderBy) {
		DefaultTableView<T> re = copy();
		re.order = sqlOrderBy;
		return re;
	}
	
	@Override
	public String getOrderBy() {
		return order;
	}

	@Override
	public TableView<T> nopage() {
		DefaultTableView<T> re = copy();
		re.limitFrom = -1;
		re.limitTo = -1;
		re.desiredPageSize = -1;
		return re;
	}
	
	@Override
	public TableView<T> page(long from, long to, long desiredSize) {
		DefaultTableView<T> re = copy();
		re.limitFrom = from;
		re.limitTo = to;
		re.desiredPageSize = desiredSize;
		return re;
	}
	
	@Override
	public long getDesiredPageSize() {
		return desiredPageSize;
	}

	@Override
	public long getPageFrom() {
		return limitFrom;
	}

	@Override
	public long getPageTo() {
		return limitTo;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Iterator<T> it = iterate();
		boolean useOrm = !Orm.hasToStringDeclared(dataClass);
		while (it.hasNext()) {
			T t = it.next();
			if (useOrm)
				sb.append(Orm.toString(t,",",true,true));
			else 
				sb.append((t.toString()));
			sb.append("\n");
		}
		return sb.toString();
	}


}
