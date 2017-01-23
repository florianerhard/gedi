package gedi.jdbc;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.FunctorUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.orm.BinaryBlob;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class JdbcStorage<D extends BinarySerializable> implements GenomicRegionStorage<D>, Closeable {


	private Connection connection;
	private HashMap<ReferenceSequence,TreeParameters> references;
	private PreparedStatement refInserter;
	private PreparedStatement refUpdater;
	private HashMap<ReferenceSequence,PreparedStatement> idQuery = new HashMap<ReferenceSequence, PreparedStatement>();
	private HashMap<ReferenceSequence,PreparedStatement> dataQuery = new HashMap<ReferenceSequence, PreparedStatement>();
	private HashMap<ReferenceSequence,PreparedStatement> intersectingQuery = new HashMap<ReferenceSequence, PreparedStatement>();
	private HashMap<ReferenceSequence,PreparedStatement> intersectingDataQuery = new HashMap<ReferenceSequence, PreparedStatement>();
	private HashMap<ReferenceSequence,PreparedStatement> regionInserter = new HashMap<ReferenceSequence, PreparedStatement>();
	private HashMap<ReferenceSequence,PreparedStatement> regionDataInserter = new HashMap<ReferenceSequence, PreparedStatement>();
	private HashMap<ReferenceSequence,PreparedStatement> regionDataUpdater = new HashMap<ReferenceSequence, PreparedStatement>();
	
	private Class<D> cls;
	private PreparedStatement leftPrepared;
	private PreparedStatement rightPrepared;
	private String prefix;

	private static class TreeParameters {
		long entries = -1;
		int offset = -1;
		int leftRoot = 0;
		int rightRoot = 0;
		public TreeParameters(int offset, int leftRoot, int rightRoot) {
			this.offset = offset;
			this.leftRoot = leftRoot;
			this.rightRoot = rightRoot;
		}
		boolean dirty = false;
		public TreeParameters update() {
			dirty = true;
			return this;
		}
		public void writeIfNecessary(ReferenceSequence reference,
				PreparedStatement refUpdater) throws SQLException {
			if (dirty) {
				refUpdater.setInt(1, offset);
				refUpdater.setInt(2, leftRoot);
				refUpdater.setInt(3, rightRoot);
				refUpdater.setString(4, reference.getName());
				refUpdater.setInt(5, reference.getStrand().ordinal());
				refUpdater.execute();
				dirty = false;
			}
		}
		
	}
	
	@Override
	public Class<D> getType() {
		return cls;
	}
	
	@Override
	public DynamicObject getMetaData() {
		return DynamicObject.getEmpty();
	}


	public JdbcStorage(Connection connection, String prefix, Class<D> cls) throws SQLException {
		this.connection = connection;
		this.prefix = prefix;
		this.cls = cls;
		Runtime.getRuntime().addShutdownHook( new Thread() {
			@Override public void run() {
				try {
					close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} );

		try (Statement s = connection.createStatement()) {
			s.execute("create table if not exists `"+getReferenceTableName()+"`(`name` VARCHAR(128),`strand` INT, `offset` INT DEFAULT -1, `leftRoot` INT DEFAULT 0, `rightRoot` INT DEFAULT 0, PRIMARY KEY(name,strand))");
		} 
		
		try (Statement s = connection.createStatement()) {
			references = new HashMap<ReferenceSequence,TreeParameters>();
			ResultSet rs = s.executeQuery("select * from "+getReferenceTableName());
			while (rs.next()) {
				references.put(Chromosome.obtain(rs.getString(1),Strand.values()[rs.getInt(2)]),new TreeParameters(rs.getInt(3),rs.getInt(4),rs.getInt(5)));
			}
		}
		
		refInserter = connection.prepareStatement("insert into `"+getReferenceTableName()+"` (`name`,`strand`) values(?,?)");
		refUpdater = connection.prepareStatement("update `"+getReferenceTableName()+"` set `offset`=?, `leftRoot`=?,`rightRoot`=? where `name`=? and `strand`=?");
		
	}

	private Supplier<D> supplier;
	
	
	public void setSupplier(Supplier<D> supplier) {
		this.supplier = supplier;
	}
	
	public Supplier<D> getSupplier() {
		if (supplier==null)
			supplier = FunctorUtils.newInstanceSupplier(getDataClass());
		return supplier;
	}
	
	@Override
	public void close() throws IOException {
		try {
			if (connection!=null && !connection.isClosed())
				connection.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	public Class<D> getDataClass() {
		return cls;
	}

	@Override
	public Set<ReferenceSequence> getReferenceSequences() {
		return references.keySet();
	}
	
	private void ensureReference(ReferenceSequence reference) {
		if (!references.containsKey(reference)) {
			try {
				refInserter.setString(1, reference.getName());
				refInserter.setInt(2, reference.getStrand().ordinal());
				refInserter.execute();
				
				try (Statement s = connection.createStatement()) {
					s.execute("create table `"+getTableName(reference)+"` (`node` INT,`lower` INT,`upper` INT, `id` BIGINT, `region` VARBINARY(255))");
					s.execute("create index `lowerIndex"+getTableName(reference)+"` on `"+getTableName(reference)+"` (`node`,`lower`)");
					s.execute("create index `upperIndex"+getTableName(reference)+"` on `"+getTableName(reference)+"` (`node`,`upper`)");
					s.execute("create index `id"+getTableName(reference)+"` on `"+getTableName(reference)+"` (`id`)");
					s.execute("create index `coord"+getTableName(reference)+"` on `"+getTableName(reference)+"` (`region`)");
					s.execute("create table `"+getDataTableName(reference)+"` (`id` BIGINT primary key, `data` BLOB)");
					
				}
				
				references.put(reference, new TreeParameters(-1, 0, 0));
				
			} catch (SQLException e) {
				throw new RuntimeException("Could not insert reference sequence!",e);
			}
		}
	}
	
	public long countRegions(ReferenceSequence reference) throws SQLException {
		ensureReference(reference);
		TreeParameters mi = references.get(reference);
		if (mi.entries==-1) {
			try (Statement s = connection.createStatement()) {
				ResultSet rs = s.executeQuery("select count(*) from `"+getDataTableName(reference)+"`");
				rs.next();
				mi.entries=rs.getLong(1);
				rs.close();
			}
		}
		return mi.entries;
	}
	
	public boolean insert(ReferenceSequence reference, GenomicRegion region) throws SQLException {
		return insert(reference,region,null);
	}
	
	/**
	 * Does nothing, if region already present
	 * @param reference
	 * @param region
	 * @param data
	 * @return
	 * @throws SQLException
	 */
	public boolean insert(ReferenceSequence reference, GenomicRegion region, D data) throws SQLException {
		if (contains(reference, region))
			return update(reference,region,data);
		ensureReference(reference);
		countRegions(reference);
		TreeParameters count = references.get(reference);
		long id = count.entries++;
		
		PreparedStatement regionStatement = getRegionInserterStatement(reference);
		regionStatement.setLong(4, id);
		regionStatement.setBytes(5, encodeRegion(region));
		for (int p=0; p<region.getNumParts(); p++) {
			regionStatement.setInt(1, computeNode(reference,region.getStart(p),region.getStop(p)));
			regionStatement.setInt(2, region.getStart(p));
			regionStatement.setInt(3, region.getStop(p));
			if (regionStatement.executeUpdate()!=1) {
				if (p==0)
					return false;
				else throw new RuntimeException("Cannot be!");
			}
		}
		
		PreparedStatement regionDataStatement = getRegionDataInserterStatement(reference);
		regionDataStatement.setLong(1, id);
		regionDataStatement.setBlob(2, data==null?null:new BinaryBlob(data));
		regionDataStatement.execute();
		
		return true;
	} 
	
	/**
	 * Does nothing if not present!
	 * @param reference
	 * @param region
	 * @param data
	 * @return
	 * @throws SQLException
	 */
	public boolean update(ReferenceSequence reference, GenomicRegion region, D data) throws SQLException {
		ensureReference(reference);
		countRegions(reference);
		
		long id = getId(reference, region);
		if (id<0) return false;
		
		PreparedStatement regionDataStatement = getRegionDataUpdaterStatement(reference);
		regionDataStatement.setLong(2, id);
		regionDataStatement.setBlob(1, data==null?null:new BinaryBlob(data));
		if (regionDataStatement.executeUpdate()!=1) throw new RuntimeException("Cannot be!");
		
		return true;
	} 
	
	@Override
	public void fill(GenomicRegionStorage<D> storage) {
		
		try {
			BinaryBlob blob = new BinaryBlob();
			
			for (ReferenceSequence reference : storage.getReferenceSequences()) {
			
				ensureReference(reference);
				
				if (countRegions(reference)>0) throw new RuntimeException("Not supported to batch add elements into a non-empty table!");
				
				connection.setAutoCommit(false);
				PreparedStatement regionStatement = getRegionInserterStatement(reference);
				PreparedStatement regionDataStatement = getRegionDataInserterStatement(reference);
				TreeParameters count = references.get(reference);
		
				storage.iterateMutableReferenceGenomicRegions(reference).forEachRemaining(mrgr->{
					
					try{
						long id = count.entries++;
						
						GenomicRegion region = mrgr.getRegion();
						regionStatement.setLong(4, id);
						regionStatement.setBytes(5, encodeRegion(region));
						for (int p=0; p<region.getNumParts(); p++) {
							regionStatement.setInt(1, computeNode(reference,region.getStart(p),region.getStop(p)));
							regionStatement.setInt(2, region.getStart(p));
							regionStatement.setInt(3, region.getStop(p));
							regionStatement.addBatch();
						}
						D d = mrgr.getData();;
						if (d!=null) 
							blob.set(d);
						regionDataStatement.setLong(1, id);
						regionDataStatement.setBlob(2, d==null?null:blob);
						regionDataStatement.addBatch();
					} catch (Exception e) {
						throw new RuntimeException("Cannot insert!",e);
					}
					
				});
				regionStatement.executeBatch();
				regionDataStatement.executeBatch();
			
			}
			
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			throw new RuntimeException("Could not fill database!",e);
		}
	}
	
	public D getData(ReferenceSequence reference, GenomicRegion region) {
		try {
			
			ensureReference(reference);
			long id = getId(reference, region);
			if (id<0) return null;
			
			PreparedStatement query = getDataQuery(reference);
			query.setLong(1, id);
			ResultSet rs = query.executeQuery();
			if (!rs.next()) throw new RuntimeException("Cannot be!");
			Blob ori = rs.getBlob(1);
			rs.close();
			if (ori==null) return null;
			
			BinaryBlob blob = new BinaryBlob(ori);
			D re = getSupplier().get();
			re.deserialize(blob);
			return re;
		} catch (SQLException | IOException e) {
			throw new RuntimeException("Could not fill database!",e);
		}
	}
	
	public long getId(ReferenceSequence reference, GenomicRegion region) throws SQLException {
		if (region instanceof IdGenomicRegion)
			return ((IdGenomicRegion)region).getId();

		ensureReference(reference);
		PreparedStatement query = getIdQuery(reference);
		query.setBytes(1, encodeRegion(region));
		ResultSet rs = query.executeQuery();
		if (!rs.next()) return -1;
		long re = rs.getLong(1);
		rs.close();
		return re;
	}
	
	public boolean contains(ReferenceSequence reference, GenomicRegion region) {
		try {
			ensureReference(reference);
			PreparedStatement query = getIdQuery(reference);
			query.setBytes(1, encodeRegion(region));
			ResultSet rs = query.executeQuery();
			boolean re = rs.next();
			rs.close();
			return re;
		} catch (SQLException e) {
			throw new RuntimeException("Could not fill database!",e);
		}
	}
	
	public <C extends Collection<GenomicRegion>> C getIntersectingRegions(ReferenceSequence reference, int start, int stop, C re) throws SQLException {
		ensureReference(reference);
		prepareTransient();
		computeNodeCreateTransient(reference, start, stop);
		PreparedStatement query = getIntersectingQuery(reference);
		query.setInt(1, start);
		query.setInt(2, stop);
		ResultSet rs = query.executeQuery();
		while (rs.next()) {
			IdGenomicRegion region = decodeRegion(rs.getBytes(2), rs.getLong(1));
			re.add(region);
		}
		rs.close();
		finishTransient();
		return re;
	}

	public <C extends Map<GenomicRegion,D>> C getIntersectingRegions(ReferenceSequence reference, int start, int stop, C re) throws SQLException, IOException {
		ensureReference(reference);
		prepareTransient();
		computeNodeCreateTransient(reference, start, stop);
		PreparedStatement query = getIntersectingDataQuery(reference);
		query.setInt(1, start);
		query.setInt(2, stop);
		ResultSet rs = query.executeQuery();
		while (rs.next()) {
			IdGenomicRegion region = decodeRegion(rs.getBytes(2), rs.getLong(1));
			D data = null;
			Blob ori = rs.getBlob(3);
			if (ori!=null) {
				BinaryBlob blob = new BinaryBlob(ori);
				data = getSupplier().get();
				data.deserialize(blob);
			}
			re.put(region, data);
		}
		rs.close();
		finishTransient();
		return re;
	}
	
	public <C extends Map<GenomicRegion,D>> C getIntersectingRegions(ReferenceSequence reference, GenomicRegion reg, C re) throws SQLException, IOException {
		ensureReference(reference);
		HashSet<Long> ids = new HashSet<Long>();
		
		for (int p=0; p<reg.getNumParts(); p++) {
			int start = reg.getStart(p);
			int stop = reg.getStop(p);
			
			prepareTransient();
			computeNodeCreateTransient(reference, start, stop);
			PreparedStatement query = getIntersectingDataQuery(reference);
			query.setInt(1, start);
			query.setInt(2, stop);
			ResultSet rs = query.executeQuery();
			while (rs.next()) {
				IdGenomicRegion region = decodeRegion(rs.getBytes(2), rs.getLong(1));
				if (ids.add(region.getId())){
					D data = null;
					Blob ori = rs.getBlob(3);
					if (ori!=null) {
						BinaryBlob blob = new BinaryBlob(ori);
						data = getSupplier().get();
						data.deserialize(blob);
					}
					re.put(region, data);
				}
			}
			rs.close();
			finishTransient();
		}
		return re;
	}
	
	public <C extends Collection<MutableReferenceGenomicRegion<D>>> C getIntersectingReferenceRegions(ReferenceSequence reference, GenomicRegion reg, C re) throws SQLException, IOException {
		ensureReference(reference);
		HashSet<Long> ids = new HashSet<Long>();
		
		for (int p=0; p<reg.getNumParts(); p++) {
			int start = reg.getStart(p);
			int stop = reg.getStop(p);
			
			prepareTransient();
			computeNodeCreateTransient(reference, start, stop);
			PreparedStatement query = getIntersectingDataQuery(reference);
			query.setInt(1, start);
			query.setInt(2, stop);
			ResultSet rs = query.executeQuery();
			while (rs.next()) {
				IdGenomicRegion region = decodeRegion(rs.getBytes(2), rs.getLong(1));
				if (ids.add(region.getId())){
					D data = null;
					Blob ori = rs.getBlob(3);
					if (ori!=null) {
						BinaryBlob blob = new BinaryBlob(ori);
						data = getSupplier().get();
						data.deserialize(blob);
					}
					re.add(new MutableReferenceGenomicRegion<D>().set(reference, region, data));
				}
			}
			rs.close();
			finishTransient();
		}
		return re;
	}
	

	private byte[] encodeRegion(GenomicRegion region) {
		byte[] re = new byte[region.getNumBoundaries()*4];
		for (int i=0; i<re.length; i+=4) {
			int b = region.getBoundary(i/4);
			re[i] = (byte) ((b>>24) & 0xff);
			re[i+1] = (byte) ((b>>16) & 0xff);
			re[i+2] = (byte) ((b>>8) & 0xff);
			re[i+3] = (byte) ((b   ) & 0xff);
		}
		return re;
	}
	
	private IdGenomicRegion decodeRegion(byte[] region, long id) {
		int[] re = new int[region.length/4];
		for (int i=0; i<region.length; i+=4) {
			int a = region[i] & 0xff;
			int b = region[i+1] & 0xff;
			int c = region[i+2] & 0xff;
			int d = region[i+3] & 0xff;
			re[i/4] = a<<24 | b<<16 | c<<8 | d;
		}
		return new IdGenomicRegion(re,id);
	}
	
	

	private int computeNode(ReferenceSequence reference, int lower, int upper) throws SQLException {
		TreeParameters para = references.get(reference);
		if (para.offset<0) para.update().offset = lower;
		int l = lower-para.offset;
		int u = upper-para.offset;
		
		if (u<0 && l<=2*para.leftRoot) para.update().leftRoot = (int) -Math.pow(2, Math.floor(Math.log(-l)/Math.log(2)));
		if (0<l && u>=2*para.rightRoot) para.update().rightRoot = (int) Math.pow(2, Math.floor(Math.log(u)/Math.log(2)));
		
		int node,step;
		if (u<0) node = para.leftRoot;
		else if (0<l) node = para.rightRoot;
		else node = 0;
		
		for (step=Math.abs(node/2); step>=1; step/=2) {
			if (u<node)node-=step;
			else if (node<l)node+=step;
			else break;
		}
		
		para.writeIfNecessary(reference,refUpdater);
		return node;
	}

	private int computeNodeCreateTransient(ReferenceSequence reference, int lower, int upper) throws SQLException {
		connection.setAutoCommit(false);
		
		TreeParameters para = references.get(reference);
		int l = lower-para.offset;
		int u = upper-para.offset;
		
		int node,step;
		if (u<0) node = para.leftRoot;
		else if (0<l) node = para.rightRoot;
		else node = 0;
		
		for (step=Math.abs(node/2); step>=1; step/=2) {
			if (u<node){
				insertTransientRight(node);
				node-=step;
			}
			else if (node<l) {
				insertTransientLeft(node,node);
				node+=step;
			}
			else break;
				
		}
		int lnode = node;
		int rnode = node;
		for (; (lnode!=l || rnode!=u) && step>=1; step/=2) {
			if (l<lnode){
				lnode-=step;
			}
			else if (lnode<l) {
				insertTransientLeft(lnode,lnode);
				lnode+=step;
			}
			if (u<rnode){
				insertTransientRight(rnode);
				rnode-=step;
			}
			else if (rnode<u) {
				rnode+=step;
			}
		}
		
		insertTransientLeft(l,u);
		
		leftPrepared.executeBatch();
		rightPrepared.executeBatch();
		connection.setAutoCommit(true);
		
		return node;
	}
	
	private void prepareTransient() throws SQLException {
		try(Statement s = connection.createStatement()) {
			s.execute("CREATE "+getTemporaryKeyword()+" TABLE `leftNodes` (`min` int, `max` int) "+getTemporaryModifier());
			s.execute("CREATE "+getTemporaryKeyword()+" TABLE `rightNodes` (`node` int) "+getTemporaryModifier());
		}
		leftPrepared = connection.prepareStatement("INSERT INTO `leftNodes` (`min`,`max`) VALUES (?,?)");
		rightPrepared = connection.prepareStatement("INSERT INTO `rightNodes` (`node`) VALUES (?)");
	}
	
	protected String getTemporaryKeyword() {
		return "TEMPORARY";
	}
	
	protected String getTemporaryModifier() {
		return "";
	}

	private void insertTransientRight(int node) throws SQLException {
		rightPrepared.setInt(1, node);
		rightPrepared.addBatch();
	}

	private void insertTransientLeft(int l, int u) throws SQLException {
		leftPrepared.setInt(1, l);
		leftPrepared.setInt(2, u);
		leftPrepared.addBatch();
	}

	private void finishTransient() throws SQLException {
		try(Statement s = connection.createStatement()) {
			s.execute("DROP TABLE leftNodes");
			s.execute("DROP TABLE rightNodes");
		}
		leftPrepared.close();
		rightPrepared.close();
		leftPrepared = rightPrepared = null;
	}
	private PreparedStatement getRegionInserterStatement(
			ReferenceSequence reference) throws SQLException {
		PreparedStatement re = regionInserter.get(reference);
		if (re==null) regionInserter.put(reference, re = connection.prepareStatement("insert into `"+getTableName(reference)+"` (`node`,`lower`,`upper`,`id`,`region`) values(?,?,?,?,?)"));
		return re;
	}
	
	private PreparedStatement getRegionDataUpdaterStatement(
			ReferenceSequence reference) throws SQLException {
		PreparedStatement re = regionDataUpdater.get(reference);
		if (re==null) regionDataUpdater.put(reference, re = connection.prepareStatement("update `"+getDataTableName(reference)+"` set `data`=? where `id`=?"));
		return re;
	}
	
	private PreparedStatement getRegionDataInserterStatement(
			ReferenceSequence reference) throws SQLException {
		PreparedStatement re = regionDataInserter.get(reference);
		if (re==null) regionDataInserter.put(reference, re = connection.prepareStatement("insert into `"+getDataTableName(reference)+"` (`id`,`data`) values(?,?)"));
		return re;
	}
	
	private PreparedStatement getIdQuery(
			ReferenceSequence reference) throws SQLException {
		ensureReference(reference);
		PreparedStatement re = idQuery.get(reference);
		if (re==null) idQuery.put(reference, re = connection.prepareStatement("select `id` from `"+getTableName(reference)+"` where `region`=?"));
		return re;
	}
	private PreparedStatement getDataQuery(
			ReferenceSequence reference) throws SQLException {
		ensureReference(reference);
		PreparedStatement re = dataQuery.get(reference);
		if (re==null) dataQuery.put(reference, re = connection.prepareStatement("select `data` from `"+getDataTableName(reference)+"` where `id`=?"));
		return re;
	}
	private PreparedStatement getIntersectingQuery(
			ReferenceSequence reference) throws SQLException {
		PreparedStatement re = intersectingQuery.get(reference);
		if (re==null) intersectingQuery.put(reference, re = connection.prepareStatement(
				"select distinct `id`,`region` from `"+getTableName(reference)+"` i, `leftNodes"+getTableName(reference)+"` l"
						+ " where i.node between l.min and l.max and i.upper>=?"
						+ " union "
						+ "select distinct `id`,`region`,`data` from `"+getTableName(reference)+"` i, `rightNodes"+getTableName(reference)+"` r"
						+ " where i.node = r.node and i.lower <=?"
				));
		return re;
	}
	
	private PreparedStatement getIntersectingDataQuery(
			ReferenceSequence reference) throws SQLException {
		PreparedStatement re = intersectingDataQuery.get(reference);
		if (re==null) intersectingDataQuery.put(reference, re = connection.prepareStatement(
				"select distinct i.`id`,`region`,`data` from `"+getTableName(reference)+"` i, `leftNodes` l,`"+getDataTableName(reference)+"` d"
						+ " where i.node between l.min and l.max and i.upper>=? and d.id=i.id"
						+ " union "
						+ "select distinct i.`id`,`region`,`data` from "+getTableName(reference)+" i, `rightNodes` r,`"+getDataTableName(reference)+"` d"
						+ " where i.node = r.node and i.lower <=? and d.id=i.id"
				));
		return re;
	}
	
	private PreparedStatement getIntersectingDataQuerySimple(
			ReferenceSequence reference) throws SQLException {
		PreparedStatement re = intersectingDataQuery.get(reference);
		if (re==null) intersectingDataQuery.put(reference, re = connection.prepareStatement(
				"select i.`id`,`region`,`data` from `"+getTableName(reference)+"` i, `"+getDataTableName(reference)+"` d"
						+ " where i.upper >= ? and ?>=i.lower and i.id=d.id"
				));
		return re;
	}

	private String getTableName(ReferenceSequence reference) {
		return prefix+reference.getName()+reference.getStrand().ordinal();
	}
	
	private String getDataTableName(ReferenceSequence reference) {
		return prefix+reference.getName()+reference.getStrand().ordinal()+"Data";
	}
	
	private String getReferenceTableName() {
		return prefix+"ReferenceSequences";
	}

	private class GenomicRegionSpliterator implements Spliterator<MutableReferenceGenomicRegion<D>>, AutoCloseable {

		private int minEntriesForSplit=1000;
		
		private ReferenceSequence ref;
		private long pos;
		private long end;
		private Statement s;
		private ResultSet rs;
		private MutableReferenceGenomicRegion<D> re = new MutableReferenceGenomicRegion<D>();
		
		public GenomicRegionSpliterator(ReferenceSequence ref) throws SQLException {
			this(ref,0,countRegions(ref));
		}
		
		public GenomicRegionSpliterator(ReferenceSequence ref, long start, long end) throws SQLException {
			this.ref = ref;
			pos = start;
			this.end = end;
			
			s = connection.createStatement();
			rs = s.executeQuery("select distinct `id`,`region` from `"+getTableName(ref)+"` order by `lower`,`upper` limit "+start+","+(end-start));
		}
		
		@Override
		public Comparator<? super MutableReferenceGenomicRegion<D>> getComparator() {
			return (MutableReferenceGenomicRegion<D> a, MutableReferenceGenomicRegion<D> b) -> a.getRegion().compareTo(b.getRegion());
		}

		
		@Override
		public boolean tryAdvance(Consumer<? super MutableReferenceGenomicRegion<D>> action) {
			try {
				if (pos++==end) {
					close();
					return false;
				}
				if (!rs.next()) throw new RuntimeException("Cannot be!");
				
//				IntArrayList build = new IntArrayList();
//				int id = rs.getInt(1);
//				build.add(rs.getInt(2));
//				build.add(rs.getInt(3));
//				
//				while (pos++<end) {
//					rs.next();
//					if (rs.getInt(1)!=id) {
//						rs.previous();
//						pos--;
//						break;
//					}
//					build.add(rs.getInt(2));
//					build.add(rs.getInt(3));
//				}
//				
//				ArrayGenomicRegion re = new ArrayGenomicRegion(build);
//				action.accept(re);
//				return true;
				IdGenomicRegion region = decodeRegion(rs.getBytes(2),rs.getLong(1));
				action.accept(re.set(ref,region,getData(ref, region)));
				return true;
			} catch (Exception e) {
				throw new RuntimeException("Cannot fetch database entries!",e);
			}
		}
		
		@Override
		public void close() throws Exception {
			rs.close();
			s.close();
		}

		@Override
		public Spliterator<MutableReferenceGenomicRegion<D>> trySplit() {
			if (end-pos<minEntriesForSplit) return null;
			
			long center = (pos+end)/2;
			if (center==pos) return null;
			
			try{
				GenomicRegionSpliterator re = new GenomicRegionSpliterator(ref,center,end);
				end = center;
				return re;
			} catch (Exception e) {
				throw new RuntimeException("Cannot fetch database entries!",e);
			}
		}

		@Override
		public long estimateSize() {
			return end-pos;
		}

		@Override
		public int characteristics() {
			return DISTINCT|NONNULL|SORTED|ORDERED|IMMUTABLE|SIZED;
		}
		
	}

	@Override
	public Spliterator<MutableReferenceGenomicRegion<D>> iterateMutableReferenceGenomicRegions(
			ReferenceSequence ref) {
		ensureReference(ref);
		try{
			return new GenomicRegionSpliterator(ref);
		} catch (Exception e) {
			throw new RuntimeException("Cannot fetch database entries!",e);
		}
	}

	@Override
	public Spliterator<MutableReferenceGenomicRegion<D>> iterateIntersectingMutableReferenceGenomicRegions(
			ReferenceSequence reference, GenomicRegion region) {
		try {
			return getIntersectingReferenceRegions(reference,region,new ArrayList<MutableReferenceGenomicRegion<D>>()).spliterator();
		} catch (SQLException | IOException e) {
			throw new RuntimeException("Could not fetch data!",e);
		}
	}

	@Override
	public boolean add(ReferenceSequence reference, GenomicRegion region, D data) {
		try {
			return insert(reference,region,data);
		} catch (SQLException e) {
			throw new RuntimeException("Could not insert into database!",e);
		}
	}

	@Override
	public boolean remove(ReferenceSequence reference, GenomicRegion region) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long size(ReferenceSequence reference) {
		try {
			return countRegions(reference);
		} catch (SQLException e) {
			throw new RuntimeException("Could not get size!",e);
		}
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setMetaData(DynamicObject meta) {
	}

	

	
}
