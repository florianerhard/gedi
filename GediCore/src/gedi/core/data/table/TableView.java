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


import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableFactory;
import gedi.util.mutable.MutableHeptuple;
import gedi.util.mutable.MutableMonad;
import gedi.util.mutable.MutableOctuple;
import gedi.util.mutable.MutablePair;
import gedi.util.mutable.MutableQuadruple;
import gedi.util.mutable.MutableQuintuple;
import gedi.util.mutable.MutableSextuple;
import gedi.util.mutable.MutableTriple;

import java.util.HashSet;
import java.util.Spliterator;
import java.util.function.LongPredicate;



public interface TableView<T> extends AutoCloseable {
	
	<A> Table<A> getTable();

	Class<T> getDataClass();
	

	T getFirst();
	ExtendedIterator<T> iterate();
	<A> ExtendedIterator<A> iterate(int column);
	
	Spliterator<T> spliterate();

	/**
	 * New Projection! use either one string with commas separated or multiple columns
	 * @param sql
	 * @return
	 */
	<A> TableView<A> select(String... sql);
	
	
	default <T1> TableView<MutableMonad<T1>> selectToMutable(Class<T1> cls, String sql) {
		return select(sql+" AS Item1").adapt(
				MutableFactory.create(cls)
				);
	}
	default <T1,T2> TableView<MutablePair<T1,T2>> selectToMutable(Class<T1> cls1, String sql1, Class<T2> cls2, String sql2) {
		return select(sql1+" AS Item1",sql2+" AS Item2").adapt(
				MutableFactory.create(cls1,cls2)
				);
	}
	default <T1,T2,T3> TableView<MutableTriple<T1,T2,T3>> selectToMutable(Class<T1> cls1, String sql1, Class<T2> cls2, String sql2, Class<T3> cls3, String sql3) {
		return select(sql1+" AS Item1",sql2+" AS Item2",sql3+" AS Item3").adapt(
				MutableFactory.create(cls1,cls2,cls3)
				);
	}
	default <T1,T2,T3,T4> TableView<MutableQuadruple<T1,T2,T3,T4>> selectToMutable(Class<T1> cls1, String sql1, Class<T2> cls2, String sql2, Class<T3> cls3, String sql3, Class<T4> cls4, String sql4) {
		return select(sql1+" AS Item1",sql2+" AS Item2",sql3+" AS Item3",sql4+" AS Item4").adapt(
				MutableFactory.create(cls1,cls2,cls3,cls4)
				);
	}
	default <T1,T2,T3,T4,T5> TableView<MutableQuintuple<T1,T2,T3,T4,T5>> selectToMutable(Class<T1> cls1, String sql1, Class<T2> cls2, String sql2, Class<T3> cls3, String sql3, Class<T4> cls4, String sql4, Class<T5> cls5, String sql5) {
		return select(sql1+" AS Item1",sql2+" AS Item2",sql3+" AS Item3",sql4+" AS Item4",sql5+" AS Item5").adapt(
				MutableFactory.create(cls1,cls2,cls3,cls4,cls5)
				);
	}
	default <T1,T2,T3,T4,T5,T6> TableView<MutableSextuple<T1,T2,T3,T4,T5,T6>> selectToMutable(Class<T1> cls1, String sql1, Class<T2> cls2, String sql2, Class<T3> cls3, String sql3, Class<T4> cls4, String sql4, Class<T5> cls5, String sql5, Class<T6> cls6, String sql6) {
		return select(sql1+" AS Item1",sql2+" AS Item2",sql3+" AS Item3",sql4+" AS Item4",sql5+" AS Item5",sql6+" AS Item6").adapt(
				MutableFactory.create(cls1,cls2,cls3,cls4,cls5,cls6)
				);
	}
	default <T1,T2,T3,T4,T5,T6,T7> TableView<MutableHeptuple<T1,T2,T3,T4,T5,T6,T7>> selectToMutable(Class<T1> cls1, String sql1, Class<T2> cls2, String sql2, Class<T3> cls3, String sql3, Class<T4> cls4, String sql4, Class<T5> cls5, String sql5, Class<T6> cls6, String sql6, Class<T7> cls7, String sql7) {
		return select(sql1+" AS Item1",sql2+" AS Item2",sql3+" AS Item3",sql4+" AS Item4",sql5+" AS Item5",sql6+" AS Item6",sql7+" AS Item7").adapt(
				MutableFactory.create(cls1,cls2,cls3,cls4,cls5,cls6,cls7)
				);
	}
	default <T1,T2,T3,T4,T5,T6,T7,T8> TableView<MutableOctuple<T1,T2,T3,T4,T5,T6,T7,T8>> selectToMutable(Class<T1> cls1, String sql1, Class<T2> cls2, String sql2, Class<T3> cls3, String sql3, Class<T4> cls4, String sql4, Class<T5> cls5, String sql5, Class<T6> cls6, String sql6, Class<T7> cls7, String sql7, Class<T8> cls8, String sql8) {
		return select(sql1+" AS Item1",sql2+" AS Item2",sql3+" AS Item3",sql4+" AS Item4",sql5+" AS Item5",sql6+" AS Item6",sql7+" AS Item7",sql8+" AS Item8").adapt(
				MutableFactory.create(cls1,cls2,cls3,cls4,cls5,cls6,cls7,cls8)
				);
	}
	
	<A> TableView<A> adapt(A dataObject);
	
	/**
	 * connect to current filter
	 * @param sql
	 * @return
	 */
	TableView<T> where(String sql, ConditionOperator op);
	
	/**
	 * AND connect to current filter
	 * @param sql
	 * @return
	 */
	default TableView<T> whereIds(LongPredicate where, long min, long max, ConditionOperator op) {
		
		String name = H2Utils.createAlias(where, 
				Tables.getInstance().getSession(getTable().getType()),
				Tables.getInstance().getSchema(getTable().getType()));
		
//		String name = "WHERE"+ALIAS_INDEX++;
//		try (Statement s = conn.createStatement()){
//			s.execute("CREATE ALIAS "+name+" FOR "+where.getClass().getMethod("test", long.class).toString());
//			s.close();
//		} catch (Exception e) {
//			Tables.log.log(Level.SEVERE, "Could not create method alias!", e);
//			throw new RuntimeException("Could not create method alias!", e);
//		}
		
		StringBuilder sb = new StringBuilder();
		if (min>Long.MIN_VALUE) {
			String sqlCondition = Tables.ID_NAME+">="+min;
			if (sb.length()>0) sb.append(" AND ");
			sb.append(sqlCondition);
		}
		if (max<Long.MAX_VALUE) {
			String sqlCondition = Tables.ID_NAME+"<="+max;
			if (sb.length()>0) sb.append(" AND ");
			sb.append(sqlCondition);
		}
		String sqlCondition = name+"("+Tables.ID_NAME+")";
		if (sb.length()>0) sb.append(" AND ");
		sb.append(sqlCondition);
		
		
		return where(sb.toString(),op);
	}
	
	/**
	 * AND connect to current filter
	 * @param sql
	 * @return
	 */
	default TableView<T> whereIds(LongPredicate where, ConditionOperator op) {
		return whereIds(where, Long.MIN_VALUE, Long.MAX_VALUE,op);
	}
	
	/**
	 * AND connect to current filter
	 * @param sql
	 * @return
	 */
	default TableView<T> whereIds(HashSet<Long> ids, ConditionOperator op) {
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		for (Long l : ids) {
			if (l<min) min = l;
			if (l>max) max = l;
		}
		
		return whereIds(l->ids.contains(l), min,max, op);
	}
	
	default TableView<T> whereIntersects(GenomicRegionStorage<? extends Number> index, ReferenceSequence reference, GenomicRegion region,ConditionOperator op) {
		HashSet<Long> indices = new HashSet<Long>();
		index.iterateIntersectingMutableReferenceGenomicRegions(reference, region).forEachRemaining(mrgr->indices.add(mrgr.getData().longValue()));
		return whereIds(indices,op);
	}
	
	default TableView<T> whereIntersects(ReferenceSequence reference, GenomicRegion region,ConditionOperator op) {
		return whereIntersects(Tables.getInstance().getIntervalIndex(getTable()).getIndex(), reference, region, op);
	}
	
	
	/**
	 * AND connect to current filter
	 * @param sql
	 * @return
	 */
	default TableView<T> where(String sql) {
		return where(sql,ConditionOperator.AND);
	}
	
	/**
	 * AND connect to current filter
	 * @param sql
	 * @return
	 */
	default TableView<T> whereIds(LongPredicate where, long min, long max) {
		return whereIds(where, min, max,ConditionOperator.AND);
	}
	
	
	/**
	 * AND connect to current filter
	 * @param sql
	 * @return
	 */
	default TableView<T> whereIds(LongPredicate where) {
		return whereIds(where,ConditionOperator.AND);
	}
	
	/**
	 * AND connect to current filter
	 * @param sql
	 * @return
	 */
	default TableView<T> whereIds(HashSet<Long> ids) {
		return whereIds(ids,ConditionOperator.AND);
	}
	
	default TableView<T> whereIntersects(GenomicRegionStorage<Long> index, ReferenceSequence reference, GenomicRegion region) {
		return whereIntersects(index, reference, region, ConditionOperator.AND);
	}
	
	
	
	
	
	/**
	 * Overwrites the current sorting (if any)
	 * @param sql
	 * @return
	 */
	TableView<T> orderBy(String sql);
	
	String getOrderBy();
	
	
	TableView<T> nopage();

	/**
	 * Inclusive exclusive starting from 0; always refers to the parent table (i.e. if it is already paged, it is not "sub-paged")
	 * Do not supply illegal values!
	 * @param from
	 * @param to
	 * @return
	 */
	TableView<T> page(long from, long to, long desiredSize);
	
	default TableView<T> pageChecked(long from, long to, long desiredSize) {
		long totalSize = getTable().size();
		return page(Math.max(0, from),Math.min(totalSize, to),desiredSize);
	}
	
	long getPageFrom();
	long getPageTo();
	long getDesiredPageSize();
	default boolean isPage() {
		return getPageTo()!=-1;
	}
	
	default TableView<T> firstPage() {
		if (!isPage()) return this;
		long size = getDesiredPageSize();
		return page(0, size,size);
	}
	
	default TableView<T> prevPage() {
		if (!isPage()) return this;
		long from = getPageFrom();
		long size = getDesiredPageSize();
		long nfrom = Math.max(0, from-size);
		return page(nfrom, nfrom+size,size);
	}
	
	default TableView<T> nextPage() {
		if (!isPage()) return this;
		long to = getPageTo();
		long size = getDesiredPageSize();
		long totalSize = getTable().size();
		if (to==totalSize) return this;
		return page(to, Math.min(totalSize,to+size),size);
	}
	
	default TableView<T> lastPage() {
		if (!isPage()) return this;
		long size = getDesiredPageSize();
		long totalSize = getTable().size();
		long pages = totalSize/size+(totalSize%size==0?0:1);
		return page(size*(pages-1), totalSize,size);
	}
	

	
	long size();

	
	/**
	 * Removes all entries from this view from the underlying table
	 * @return
	 */
	long delete();

	TableView<T> copy();

	String getSelect();

	String getWhere();

	
	
	
}

