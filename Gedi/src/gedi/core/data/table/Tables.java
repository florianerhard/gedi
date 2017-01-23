package gedi.core.data.table;

import gedi.app.Config;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.workspace.Workspace;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.FileUtils;
import gedi.util.ReflectionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.command.CommandInterface;
import org.h2.engine.Session;
import org.h2.jdbc.JdbcConnection;
import org.h2.message.Trace;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;
import org.h2.util.StatementBuilder;
import org.h2.value.DataType;
import org.h2.value.Value;

@SuppressWarnings({"unchecked","rawtypes"})
public class Tables implements AutoCloseable {
	
	public static final Logger log = Logger.getLogger( Tables.class.getName() );


	public static final String ID_NAME = "gediID";
	private static final String METADATA_NAME = "metadata";
	
	private static ThreadLocal<Tables> instances = new ThreadLocal<Tables>(){
		protected Tables initialValue() {
			return new Tables();
		}
	};
	
	public static Tables getInstance() {
		return instances.get();
	}
	
	private Connection createConnection(String pathOrName,String user, String pw, boolean deleteFirst, boolean server) throws SQLException {
		try {
			if (deleteFirst)
				DeleteDbFiles.execute(new File(pathOrName).getParent(), new File(pathOrName).getName(), true);
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
		}
		String dbstring = "jdbc:h2:"+pathOrName+(server?";AUTO_SERVER=TRUE":"")+";DATABASE_TO_UPPER=FALSE";
//		System.out.println(dbstring);
		return DriverManager.getConnection(dbstring, user, pw);
	}
	
	/**
	 * If a name is given, also start a TCP server to allow connections from outside.
	 * @param name
	 * @return
	 * @throws SQLException
	 */
	private Connection createTempConnection(String name) throws SQLException {
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
		}
		if (name==null)
			return DriverManager.getConnection("jdbc:h2:mem:"+";DATABASE_TO_UPPER=FALSE");
		
		Server server = Server.createTcpServer("-tcpDaemon").start();
		servers.add(server);
		return DriverManager.getConnection("jdbc:h2:mem:"+name+";DATABASE_TO_UPPER=FALSE");
	}
	
	
	/**
	 * Are all not autocommit!
	 */
	private Connection[] connections = new Connection[TableType.values().length];
	private HashMap<Table<?>,TableIntervalIndex> intervalIndices = new HashMap<Table<?>, TableIntervalIndex>();
	private HashMap<String,Reference<Table<?>>>[] tables = new HashMap[TableType.values().length];
	private Table<TableMetaInformation>[] metaTables = new Table[TableType.values().length];
	private ArrayList<Server> servers = new ArrayList<Server>();
	
	
	public Session getSession(TableType type) {
		JdbcConnection c = (JdbcConnection) connections[getConnection(type)];
        Session session = (Session) c.getSession();
        return session;
	}
	
	public Schema getSchema(TableType type) {
		Session session = getSession(type);
		return session.getDatabase().getSchema(session.getCurrentSchemaName());
	}
	
	private int getConnection(TableType type) {
		if (connections[type.ordinal()]==null) {
			Connection conn; 
			
			try {
				switch (type) {
				case Gedi: conn =  createConnection(
						Config.getInstance().getGediDatabasePath(), 
						Config.getInstance().getGediDatabaseName(), 
						Config.getInstance().getGediDatabasePassword(), 
						false, true);
				break;
				case Workspace: conn = createConnection(Workspace.getCurrent().getWorkspaceTableFile().toString(), "gedi", "gedi", false, false);
					break;
				case Temporary: conn = createTempConnection(null);
//				case R: conn = createTempConnection("R");
				break;
				default: throw new RuntimeException("Table type "+type+" unknown!");
				}
				conn.setAutoCommit(false);
			} catch (SQLException e) {
				log.log(Level.SEVERE, "Could not connect to database!", e);
				throw new RuntimeException("Could not connect to database!",e);
			}
			
			connections[type.ordinal()] = conn;
			tables[type.ordinal()] = new HashMap<String, Reference<Table<?>>>();
			metaTables[type.ordinal()] = getOrCreateMeta(type.ordinal());
		}
		return type.ordinal();
	}

	public <T> TableMetaInformation<T> buildMeta(String name, String creator, Class<T> cls) {
		return new TableMetaInformation<>(getH2TableName(name, 0),name, creator, 0,"",cls);
	}
	
	
	public <T> TableMetaInformation<T> buildMeta(String name, String creator, int version, String versionDescription, Class<T> cls) {
		return new TableMetaInformation<>(getH2TableName(name, version),name, creator, version,versionDescription,cls);
	}
	
	public <T> TableMetaInformation<T> buildMeta(String creator, Class<T> cls) {
		return buildMeta(cls.getSimpleName(),creator, cls);
	}
	
	
	public <T> TableMetaInformation<T> buildMeta(String creator, int version, String versionDescription, Class<T> cls) {
		return buildMeta(cls.getSimpleName(), creator, version, versionDescription, cls);
	}
	@Override
	public void close() throws Exception {
		for (int i=0; i<connections.length; i++)
			if (connections[i]!=null) {
				for (Reference<Table<?>> t : tables[i].values()) {
					Table<?> tab = t.get();
					if (tab!=null)
						tab.close();
				}
				tables[i] = null;
				
				metaTables[i].close();
				metaTables[i] = null;

				connections[i].close();
				connections[i] = null;
			}
		for (Server s : servers)
			s.shutdown();
		servers.clear();
		
	}
	
	
	private TableMetaInformation<TableMetaInformation> meta;


	private TableMetaInformation<TableMetaInformation> getMetaTableInformation() {
		if (meta==null)
			meta = buildMeta(METADATA_NAME, "gedi", TableMetaInformation.class);
		return meta;
	}
	
	
	Table<TableMetaInformation> getMetaTable(TableType type) {
		return metaTables[type.ordinal()];
	}

	@SuppressWarnings("incomplete-switch")
	public TableIntervalIndex getIntervalIndex(Table<?> table) {
		TableIntervalIndex re = (TableIntervalIndex) intervalIndices.get(table);
		if (re==null) {
			
			String path = null;
			switch (table.getType()) {
			case Gedi: 
				path = Config.getInstance().getGediDatabasePath()+"."+table.getMetaInfo().getTablename()+".*"; 
				break;
			case Workspace:
				path = Workspace.getCurrent().getWorkspaceTableFile()+"."+table.getMetaInfo().getTablename()+".*"; 
				break;
			}
			if (path!=null) {
				try {
					Optional<Path> file = Files.find(Paths.get(path), 0, (p, a)->true).findAny();
					if (file.isPresent()) {
						String column = FileUtils.getFullNameWithoutExtension(file.get().getName(file.get().getNameCount()-1));
						column = column.substring(column.lastIndexOf('.')+1);
						GenomicRegionStorage<Long> index = (GenomicRegionStorage<Long>) WorkspaceItemLoaderExtensionPoint.getInstance().get(file.get()).load(file.get());
						re = new TableIntervalIndex(index,table, column);
					}
					intervalIndices.put(table, re);
				} catch (IOException e) {
					log.log(Level.SEVERE, "Could not search for index in "+path, e);
				} 
				
			}
		}
		return re;
	}
	
	public TableIntervalIndex buildIntervalIndex(Table<?> table) {
		for (int i=0; i<table.getMetaInfo().getNumColumns(); i++) {
			if (table.getMetaInfo().getColumnClass(i)==MutableReferenceGenomicRegion.class)
				return buildIntervalIndex(table, table.getMetaInfo().getColumnName(i));
		}
		return null;
	}
	
	@SuppressWarnings("incomplete-switch")
	public TableIntervalIndex buildIntervalIndex(Table<?> table, String column) {
		if (table.getMetaInfo().getColumnClass(column)!=MutableReferenceGenomicRegion.class)
			throw new IllegalArgumentException("Column "+column+" must be of class MutableReferenceGenomicRegion!");
			
		String path = null;
		switch (table.getType()) {
		case Gedi: 
			path = Config.getInstance().getGediDatabasePath()+"."+table.getMetaInfo().getUnquotedTablename()+"."+column; 
			break;
		case Workspace:
			path = Workspace.getCurrent().getWorkspaceTableFile()+"."+table.getMetaInfo().getUnquotedTablename()+"."+column; 
			break;
		}
		GenomicRegionStorage<Long> storage = null;
		if (path!=null) 
			storage = GenomicRegionStorageExtensionPoint.getInstance().get(Long.class,path,GenomicRegionStorageCapabilities.Disk,GenomicRegionStorageCapabilities.Fill);
		else
			storage = GenomicRegionStorageExtensionPoint.getInstance().get(Long.class,null,GenomicRegionStorageCapabilities.Memory,GenomicRegionStorageCapabilities.Fill);
		
		Iterator<MutableReferenceGenomicRegion<Long>> mit =
				table.selectToMutable(Long.class, ID_NAME,MutableReferenceGenomicRegion.class, column).iterate()
				.map(p->p.Item2.setData(p.Item1));
		
		storage.fill(mit);
		
		TableIntervalIndex re = new TableIntervalIndex(storage, table, column);
		
		intervalIndices.put(table, re);
		return re;
	}
	
	
	
	
	private <T> Table<T> getTableFromDbOrCache(int connId, String name, int version) {
		String tn = getH2TableName(name, version);
		Reference<Table<?>> reference = tables[connId].get(tn);
		if (reference==null) {
			Table tab = getTableFromDb(connId,name,version);
			if (tab==null) return null;
			tables[connId].put(tn, createReference(tab));
			return tab;
		}
		Table tab = reference.get();
		if (tab==null) {
			tab = getTableFromDb(connId,name,version);
			if (tab==null) return null;
			tables[connId].put(tn, createReference(tab));
			return tab;
		}
		return tab;
	}
	
	private ReferenceQueue<Table> tempDeleteQueue;
	private Reference createReference(Table tab) {
		if (tab.getType().isPersistent())
			return new StrongReference(tab);
		if (tempDeleteQueue==null) {
			 tempDeleteQueue = new ReferenceQueue<Table>();
			new CleanupThread().start();
		}
		return new TableReference(tab,tempDeleteQueue);
	}
	
	private static class StrongReference<T> extends WeakReference<T> {

		public StrongReference(T referent) {
			super(referent);
		}

	}

	
	private static class TableReference extends WeakReference<Table> {

		private String tablename;
		private TableType type;
		
		public TableReference(Table referent, ReferenceQueue<? super Table> q) {
			super(referent, q);
			tablename = referent.getMetaInfo().getTablename();
			type = referent.getType();
		}
		
		private void clean(Connection[] connections, Table<TableMetaInformation>[] metaTables) {
			try {
				Statement s = connections[type.ordinal()].createStatement();
				Tables.executeUpdate(s,"drop table "+tablename);
				s.close();
				Table<TableMetaInformation> metaTable = metaTables[type.ordinal()];
				metaTable.where("tablename='"+tablename+"'").delete();
				log.log(Level.FINE, "Removed temporary table "+tablename);
			} catch (SQLException e) {
				Tables.log.log(Level.SEVERE, "Could not drop table", e);
				throw new RuntimeException("Could not drop table", e);
			}
		}

		
	}
	
	
	private class CleanupThread extends Thread {
        CleanupThread() {
            setPriority(Thread.MAX_PRIORITY);
            setName("Tables-cleanupthread");
            setDaemon(true);
        }

        public void run() {
            while (true) {
                try {
                	TableReference ref = (TableReference) tempDeleteQueue.remove();
                    while (true) {
                        ref.clean(connections, metaTables);
                        ref = (TableReference) tempDeleteQueue.remove();
                    }
                } catch (InterruptedException e) {
                }
            }
        }
    }
	

	private <T> Table<T> getTableFromDb(int connId, String name, int version) {
		TableView<TableMetaInformation> tv = metaTables[connId].where("name='"+name+"' and versionNumber="+version);
		
		if (tv.size()==0) return null;
		if (tv.size()!=1) throw new RuntimeException("Not unique!");
		
		TableMetaInformation<T> metaInfo = tv.getFirst();
		H2Table<T> re = new H2Table<T>(connections[connId],metaInfo,TableType.values()[connId]);
		metaInfo.setTable(re);
		return re;
	}
	
	private <T> Table<T> getMostRecentVersionFromDbOrCache(int connId, String name) {
		TableView<TableMetaInformation> tv = metaTables[connId].where("name='"+name+"'").orderBy("versionNumber DESC");
		
		if (tv.size()==0) return null;
		
		TableMetaInformation<T> metaInfo = tv.getFirst();
		return getTableFromDbOrCache(connId, name, metaInfo.getVersionNumber());
	}

	/**
	 * May not contain a table with the given name and version number
	 * @param type
	 * @param meta
	 * @param strategy
	 * @return
	 */
	public <T> Table<T> create(TableType type, TableMetaInformation<T> meta) {
		int connId = getConnection(type);
		
		Table<T> table = getTableFromDbOrCache(connId,meta.getName(),meta.getVersionNumber());
		if (table!=null) 
			throw new RuntimeException("Table "+meta.getName()+" already exists!");
		
		
		Connection conn = connections[connId];
		try {
			Statement s = conn.createStatement();
			meta.versionTimestamp = System.currentTimeMillis();
			String sql = getCreateStatement(meta);
			log.log(Level.CONFIG, sql);
			s.execute(sql);
			conn.commit();
			s.close();
			
			metaTables[connId].add(meta);
			
			H2Table<T> re = new H2Table<T>(conn,meta,type);
			tables[connId].put(meta.getTablename(), createReference(re));
			
			return re;
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Could not create table "+meta.getName()+" v"+meta.getVersionNumber(), e);
			throw new RuntimeException("Could not create table "+meta.getName()+" v"+meta.getVersionNumber(),e);
		}
	}
	
	public <T> Table<T> newVersion(Table<T> table, String versionDescription) {
		int connId = getConnection(table.getType());
		
		TableMetaInformation<T> meta = table.getMetaInfo();
		meta = buildMeta(meta.getName(), meta.getCreator(), meta.getVersionNumber()+1, versionDescription, meta.getDataClass());
		
		Table<T> re = getTableFromDbOrCache(connId,meta.getName(),meta.getVersionNumber());
		if (re!=null) 
			throw new RuntimeException("Table "+meta.getName()+" already exists!");
		
		Connection conn = connections[connId];
		try {
			Statement s = conn.createStatement();
			meta.versionTimestamp = System.currentTimeMillis();
			s.execute(getCreateStatement(meta));
			conn.commit();
			s.close();
			
			metaTables[connId].add(meta);
			
			re = new H2Table<T>(conn,meta,table.getType());
			tables[connId].put(meta.getTablename(), createReference(re));
			
			return re;
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Could not create table "+meta.getName()+" v"+meta.getVersionNumber(), e);
			throw new RuntimeException("Could not create table "+meta.getName()+" v"+meta.getVersionNumber(),e);
		}
	}
	
	private Table<TableMetaInformation> getOrCreateMeta(int connId) {
		Connection conn = connections[connId];
		try {
			Statement s = conn.createStatement();
			String sql = getCreateMeta();
//			System.out.println(sql);
			s.execute(sql);
			conn.commit();
			s.close();
			
			return new H2Table<TableMetaInformation>(conn,getMetaTableInformation(),TableType.values()[connId]);
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Could not create table "+meta.getName()+" v"+meta.getVersionNumber(), e);
			throw new RuntimeException("Could not create table "+meta.getName()+" v"+meta.getVersionNumber(),e);
		}
	}

	/**
	 * If present, all meta information are ignored!
	 * @param type
	 * @param meta
	 * @return
	 */
	public <T> Table<T> createOrOpen(TableType type, TableMetaInformation<T> meta) {
		int connId = getConnection(type);
		
		Table<T> table = getTableFromDbOrCache(connId,meta.getName(),meta.getVersionNumber());
		if (table!=null) 
			return table;
		return create(type, meta);
	}
	
	

	public <T> Table<T> open(TableType type, String name, int versionNumber) {
		int connId = getConnection(type);
		return getTableFromDbOrCache(connId, name, versionNumber);
	}
	
	public boolean contains(TableType type, String name, int versionNumber) {
		int connId = getConnection(type);
		return getTableFromDbOrCache(connId, name, versionNumber)!=null;
	}
	
	public int getMostRecentVersion(TableType type, String name) {
		int connId = getConnection(type);
		Table t = getMostRecentVersionFromDbOrCache(connId, name);
		if (t==null) return -1;
		return t.getMetaInfo().getVersionNumber();
	}

	static String getH2TableName(TableMetaInformation<?> meta) {
		return getH2TableName(meta.getName(), meta.getVersionNumber());
	}

	static String getH2TableName(String name, int version) {
		return "\""+name+"$"+version+"\"";
	}
	
	
	private String getCreateMeta() {
		StatementBuilder buff = new StatementBuilder("CREATE ");
        buff.append("TABLE IF NOT EXISTS ");
        buff.append(getH2TableName(getMetaTableInformation()));
        buff.append("( ");
        
        Column id = new Column(ID_NAME, Value.INT);
        id.setPrimaryKey(true);
        buff.appendExceptFirst(",");
        buff.append(id.getCreateSQL());
        buff.append(" auto_increment");
        
        for (int i=0; i<meta.getNumColumns(); i++) {
        	Column column = new Column(meta.getColumnName(i), DataType.getTypeFromClass(meta.getColumnClass(i)));
            buff.appendExceptFirst(",");
            buff.append(column.getCreateSQL());
        }
        
        buff.append(")");
        return buff.toString();
	}
	
	static String getCreateStatement(TableMetaInformation<?> meta) {
		StatementBuilder buff = new StatementBuilder("CREATE ");
        buff.append("TABLE ");
        buff.append(getH2TableName(meta));
        buff.append("( ");
        
        Column id = new Column(ID_NAME, Value.INT);
        id.setPrimaryKey(true);
        buff.appendExceptFirst(",");
        buff.append(id.getCreateSQL());
        buff.append(" auto_increment");
        
        for (int i=0; i<meta.getNumColumns(); i++) {
        	Column column = new Column(meta.getColumnName(i), DataType.getTypeFromClass(meta.getColumnClass(i)));
            buff.appendExceptFirst(",");
            buff.append(column.getCreateSQL());
        }
        
        buff.append(")");
        
        return buff.toString();
	}

	static String prepareInsertStatement(TableMetaInformation<?> meta) {
		StatementBuilder buff = new StatementBuilder("INSERT INTO ");
        buff.append(meta.getTablename());
        buff.append(" ( ");
        
        for (int i=0; i<meta.getNumColumns(); i++) {
        	Column column = new Column(meta.getColumnName(i), DataType.getTypeFromClass(meta.getColumnClass(i)));
            buff.appendExceptFirst(",");
            buff.append(column.getSQL());
        }
        
        buff.append(") VALUES (").resetCount();
        
        for (int i=0; i<meta.getNumColumns(); i++) {
            buff.appendExceptFirst(",");
            buff.append("?");
        }
        
        buff.append(")");
		return buff.toString();
	}
	
	static String prepareDeleteStatement(TableMetaInformation<?> meta) {
		StatementBuilder buff = new StatementBuilder("DELETE FROM ");
        buff.append(meta.getTablename());
        buff.append(" WHERE ");
        buff.append(ID_NAME);
        buff.append("=?");
        buff.append(")");
		return buff.toString();
	}
	
	static String prepareUpdateStatement(TableMetaInformation<?> meta) {
		StatementBuilder buff = new StatementBuilder("UPDATE ");
        buff.append(meta.getTablename());
        buff.append(" SET ");
        
        for (int i=0; i<meta.getNumColumns(); i++) {
        	Column column = new Column(meta.getColumnName(i), DataType.getTypeFromClass(meta.getColumnClass(i)));
            buff.appendExceptFirst(",");
            buff.append(column.getSQL());
            buff.append("=?");
        }
        
        buff.append(" WHERE ");
        buff.append(ID_NAME);
        buff.append("=?");
        
		return buff.toString();
	}
	
	static String getSelectStatement(String table, String fields, String where, String order, String limit) {
		StatementBuilder buff = new StatementBuilder("SELECT ");
		buff.append(fields);
		buff.append(" FROM ");
        buff.append(table);
        if (where!=null && where.length()>0) 
        	buff.append(" WHERE "+where);
        
        if (order!=null && order.length()>0) 
        	buff.append(" ORDER BY "+order);
        
        if (limit!=null && limit.length()>0) 
        	buff.append(" LIMIT "+limit);
        
		return buff.toString();
	}
	
	static String getDeleteStatement(String table, String where, String limit) {
		StatementBuilder buff = new StatementBuilder("DELETE FROM ");
        buff.append(table);
        if (where!=null && where.length()>0) 
        	buff.append(" WHERE "+where);
        
        if (limit!=null && limit.length()>0) 
        	buff.append(" LIMIT "+limit);
        
		return buff.toString();
	}
	
	
	
	static int executeUpdate(Statement s, String sql) throws SQLException {
		log.log(Level.FINE, sql);
		return s.executeUpdate(sql);
	}
	
	static boolean execute(Statement s, String sql) throws SQLException {
		log.log(Level.FINE, sql);
		return s.execute(sql);
	}
	
	static ResultSet executeQuery(Statement s, String sql) throws SQLException {
		log.log(Level.FINE, sql);
		return s.executeQuery(sql);
	}
	
	static void addBatch(PreparedStatement s) throws SQLException {
			try {
				CommandInterface cmd = ReflectionUtils.getPrivate(s,"command");
				log.log(Level.FINER, Trace.formatParams(cmd.getParameters())+" [BATCH]");
			} catch (NoSuchFieldException | SecurityException e) {
				log.log(Level.SEVERE, "Cannot get parameters from prepared statement!",e);
				throw new RuntimeException("Cannot get parameters from prepared statement!",e);
			}
		s.addBatch();
	}
	
	static int[] executeBatch(PreparedStatement s) throws SQLException {
			try {
				log.log(Level.FINER, ReflectionUtils.getPrivate(s,"sqlStatement")+" [BATCH n="+((List) ReflectionUtils.getPrivate(s,"batchParameters")).size()+"]");
			} catch (NoSuchFieldException | SecurityException e) {
				log.log(Level.SEVERE, "Cannot get SQL command from prepared statement!",e);
				throw new RuntimeException("Cannot get SQL command from prepared statement!",e);
			}
		return s.executeBatch();
	}
	
	static int executeUpdate(PreparedStatement s) throws SQLException {
		log.log(Level.FINE, s.toString());
		return s.executeUpdate();
	}
	
	static boolean execute(PreparedStatement s) throws SQLException {
		log.log(Level.FINE, s.toString());
		return s.execute();
	}
	
	static ResultSet executeQuery(PreparedStatement s) throws SQLException {
		log.log(Level.FINE, s.toString());
		return s.executeQuery();
	}
	
	static void commit(Connection conn) throws SQLException {
		log.log(Level.FINE, "[COMMIT]");
		conn.commit();
	}

	
	
}
