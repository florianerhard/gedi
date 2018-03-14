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

import gedi.util.ArrayUtils;
import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;
import gedi.util.algorithm.graph.DepthFirstSearch;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.logging.Level;


import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;


import org.h2.api.ErrorCode;
import org.h2.command.Parser;
import org.h2.command.dml.Select;
import org.h2.engine.Database;
import org.h2.engine.FunctionAlias;
import org.h2.engine.FunctionAlias.JavaMethod;
import org.h2.engine.Session;
import org.h2.expression.Comparison;
import org.h2.expression.ConditionAndOr;
import org.h2.expression.ConditionExists;
import org.h2.expression.ConditionInSelect;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionColumn;
import org.h2.expression.Function;
import org.h2.expression.JavaFunction;
import org.h2.expression.Operation;
import org.h2.expression.ValueExpression;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.Column;
import org.h2.table.ColumnResolver;
import org.h2.table.SingleColumnResolver;
import org.h2.table.TableFilter;
import org.h2.value.Value;


import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MultiHashtable;


import sun.reflect.MethodAccessor;

public class H2Utils {

	
	private static int ALIAS_INDEX = 0;
	
	
	public static class WrapLongPredicate implements LongPredicate {
		private LongPredicate p;
		
		public WrapLongPredicate(LongPredicate p) {
			this.p = p;
		}

		@Override
		public boolean test(long value) {
			return p.test(value);
		}
		
	}
	
	public static String createAlias(LongPredicate p, Session session, Schema schema) {
	
		try {
			LongPredicate predicate = new WrapLongPredicate(p);
			Method method = predicate.getClass().getMethod("test", long.class);
			return createAlias(predicate, method, session, schema);
		} catch (Exception e) {
			Tables.log.log(Level.SEVERE, "Could not create alias!",e);
			throw new RuntimeException("Could not create alias!",e);
		}
		
	}
	
	private static String createAlias(final Object o, Method method, Session session, Schema schema) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException, SecurityException, InstantiationException {
		
		String aliasName = "H2UtilsAlias"+ALIAS_INDEX++;
		
		String javaClassMethod = method.getClass().getName()+"."+method.getName();
		
		session.commit(true);
	    session.getUser().checkAdmin();
	    Database db = session.getDatabase();
	    if (schema.findFunction(aliasName) != null) 
	        throw DbException.get(
	                ErrorCode.FUNCTION_ALIAS_ALREADY_EXISTS_1, aliasName);
	    
	    
	    int id = session.getDatabase().allocateObjectId();
	    FunctionAlias functionAlias = FunctionAlias.newInstance(schema, id,
	            aliasName, javaClassMethod, true,
	            false);
	    functionAlias.setDeterministic(true);
	    db.addSchemaObject(session, functionAlias);
	    
	    
	    // hack: copy the method object, replace its MethodAccessor by one that ignores the object 
	    // (first argument of invoke, in the FunctionAlias always null) and uses the predicate instead
		method = ReflectionUtils.invoke(method,"copy");
		final MethodAccessor ma = ReflectionUtils.invoke(method, "acquireMethodAccessor");
		ReflectionUtils.set(method, "methodAccessor", new MethodAccessor() {
			@Override
			public Object invoke(Object arg0, Object[] arg1)
					throws IllegalArgumentException, InvocationTargetException {
				return ma.invoke(o, arg1);
			}
			});
		
		JavaMethod[] a = {ReflectionUtils.newInstance(JavaMethod.class, method, 0)};
	 	ReflectionUtils.set(functionAlias, "javaMethods", a);
		
	    return aliasName;
		
		
	}

	/**
	 * Select N,N*2 AS D,-D AS A from table is supposed to work, but not supported by H2
	 * @param select
	 * @return
	 */
	public static String resolveNestedAliases(String select, Session session) {
		Parser p = new Parser(session);
		
		String[] parts = StringUtils.split(select, ',');
		Expression[] expr = new Expression[parts.length];
		String[] alias = new String[parts.length];
		
		for (int i=0; i<expr.length; i++) {
			int AS = parts[i].toUpperCase().indexOf(" AS ");
			if (AS==-1) {
				expr[i] = p.parseExpression(parts[i]);
				alias[i] = null;
			} else {
				expr[i] = p.parseExpression(parts[i].substring(0, AS));
				alias[i] = StringUtils.trim(parts[i].substring(AS+4));
			}
		}
		HashMap<String, Integer> index = ArrayUtils.createIndexMap(alias);
		
		
		// which field to set for this Identifier
//		HashMap<ExpressionColumn,Integer> toset = new HashMap<ExpressionColumn, Integer>();
		ArrayList<ExpressionColumn>[] tosetre = new ArrayList[expr.length];
		for (int i=0; i<tosetre.length; i++) tosetre[i] = new ArrayList<ExpressionColumn>();
		// in which field is this Identifier
		HashMap<ExpressionColumn,Integer> infield = new HashMap<ExpressionColumn, Integer>();
		
		for (int i=0; i<expr.length; i++) {
			LinkedList<Expression> queue = new LinkedList<Expression>(Arrays.asList(expr[i]));
			while (!queue.isEmpty()) {
				Expression e = queue.poll();
				// ExpressionColumn, Function, JavaFunction,Operation,
				if (e instanceof ExpressionColumn) {
					ExpressionColumn c = (ExpressionColumn)e;
					if (index.containsKey(c.getColumnName())) {
						tosetre[index.get(c.getColumnName())].add(c);
//						toset.put(c, index.get(c.getColumnName()));
						infield.put(c, i);
					}
				} else if (e instanceof Function) {
					Function f = (Function) e;
					queue.addAll(Arrays.asList(f.getArgs()));
				} else if (e instanceof JavaFunction) {
					JavaFunction f = (JavaFunction) e;
					queue.addAll(Arrays.asList(f.getArgs()));
				} else if (e instanceof Operation) {
					Operation f = (Operation) e;
					try {
						queue.addAll(Arrays.asList(ReflectionUtils.get(f, "left"),ReflectionUtils.get(f, "right")));
					} catch (NoSuchFieldException | SecurityException
							| IllegalArgumentException | IllegalAccessException e1) {
						throw new RuntimeException("Could not resolve!",e1);
					}
				} 
			}
		}
		
		// determine the order of replacement: for each (Identifier e-> Field f) in toset: f has to be processed before infield[e]
		Comparator<Integer> order = (a,b)->{
			int re = 0;
			for (ExpressionColumn c : tosetre[a])
				if (infield.get(c)==b) re =-1;
			for (ExpressionColumn c : tosetre[b])
				if (infield.get(c)==a) {
					if (re==-1) throw new RuntimeException("Circle detected!");
					re = -1;
				}
			return re;
		};
		Integer[] fields = new Integer[expr.length];
		for (int i=0; i<fields.length; i++)
			fields[i] = i;
		Arrays.sort(fields,order);
		
		for (int i=1; i<fields.length; i++)
			if (order.compare(fields[i-1], fields[i])>0) 
				throw new RuntimeException("Circle found!");
		
		for (Integer f : fields) 
			for (ExpressionColumn c : tosetre[f])
				try {
					ReflectionUtils.set(c, "columnName", expr[f].getSQL());
				} catch (NoSuchFieldException | SecurityException
						| IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Could not resolve!",e);
				}
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<expr.length; i++) {
			if (i>0) sb.append(", ");
			sb.append(expr[i].getSQL());
			if (alias[i]!=null) 
				sb.append(" AS ").append(alias[i]);
		}
		
		return sb.toString();
	}

	public static String normalizeExpression(String expression, Session session) {
		if (expression.length()==0) return "";
		Parser par = new Parser(session);
		try {
			Expression tree = par.parseExpression(expression);
			String re = tree.getSQL();
			re=re.replace('\n', ' ');
			re=re.replaceAll("\\s+"," ");
			return re;
		} catch (Throwable e) {
			return expression;
		}
	}
	
	public static String checkParseField(String expression, HashSet<String> aliasesAndColumns, Session session) {
		return checkParseField(expression, aliasesAndColumns, null, session);
	}
	public static String checkParseField(String expression, HashSet<String> aliasesAndColumns, IntFunction<String> typeChecker,
			Session session) {
		
		
		if (expression.length()==0) return "Empty expression!";
		Parser p = new Parser(session);
		ArrayList<String> re = new ArrayList<String>();
		try {
			Expression expr = p.parseExpression(expression);
			
			LinkedList<Expression> queue = new LinkedList<Expression>(Arrays.asList(expr));
			while (!queue.isEmpty()) {
				Expression e = queue.poll();
				// ExpressionColumn, Function, JavaFunction,Operation,all conditions
				if (e instanceof ExpressionColumn) {
					ExpressionColumn c = (ExpressionColumn)e;
					if (typeChecker!=null)
						c.mapColumns(ReflectionUtils.newInstance(SingleColumnResolver.class,new Column(c.getColumnName(), Value.NULL)), 0);
					if (!aliasesAndColumns.contains(c.getColumnName())) {
						re.add("Column or alias "+c.getColumnName()+" unknown!");
					}
				} else if (e instanceof Function) {
					Function f = (Function) e;
					queue.addAll(Arrays.asList(f.getArgs()));
				} else if (e instanceof JavaFunction) {
					JavaFunction f = (JavaFunction) e;
					queue.addAll(Arrays.asList(f.getArgs()));
				} else if (e instanceof ValueExpression) {
				} else if (ReflectionUtils.has(e, "left") && ReflectionUtils.has(e, "right")){
					// Operation, CompareLike,Comparison,ConditionAndOr
					try {
						queue.addAll(Arrays.asList(ReflectionUtils.get(e, "left"),ReflectionUtils.get(e, "right")));
					} catch (NoSuchFieldException | SecurityException
							| IllegalArgumentException | IllegalAccessException e1) {
						throw new RuntimeException("Could not resolve!",e1);
					}
				} else if (ReflectionUtils.has(e, "left") && ReflectionUtils.has(e, "valueList")) {
					// ConditionIn, ConditionInConstantSet
					try {
						queue.add(ReflectionUtils.get(e, "left"));
						queue.addAll(ReflectionUtils.get(e, "valueList"));
					} catch (NoSuchFieldException | SecurityException
							| IllegalArgumentException | IllegalAccessException e1) {
						throw new RuntimeException("Could not resolve!",e1);
					}
				} else if (ReflectionUtils.has(e, "condition")) {
					// ConditionIn, ConditionInConstantSet
					try {
						queue.add(ReflectionUtils.get(e, "condition"));
					} catch (NoSuchFieldException | SecurityException
							| IllegalArgumentException | IllegalAccessException e1) {
						throw new RuntimeException("Could not resolve!",e1);
					}
				} else if (e instanceof ConditionExists) { // will not check the select statements in ConditionExists and ConditionInSelect
				} else if (e instanceof ConditionInSelect) { // will not check the select statements in ConditionExists and ConditionInSelect
					try {
						queue.add(ReflectionUtils.get(e, "left"));
					} catch (NoSuchFieldException | SecurityException
							| IllegalArgumentException | IllegalAccessException e1) {
						throw new RuntimeException("Could not resolve!",e1);
					}
				} else {
					re.add(e.getClass().getName()+" not allowed!");
				}
			}
			
			if (typeChecker!=null) {
				
				expr.optimize(session);
				String te = typeChecker.apply(expr.getType());
				if (te!=null)
					re.add(te);
			}
		} catch (DbException e) {
			String r = e.getMessage();
			r = r.replace(" in SQL statement", ":");
			r = r.replaceAll("\\[[0-9\\-]+\\]", "");
			return r;
		} catch (Throwable e) {
			return e.getClass().getSimpleName()+": "+e.getMessage();
		}
		
		if (re.size()>1) {
			StringBuilder sb = new StringBuilder();
			sb.append("Multiple errors:");
			for (int i=0; i<re.size(); i++) 
				sb.append("\n -").append(re.get(i));
			return sb.toString();
		}
		if (re.size()==1)
			return re.get(0);
		
		return null;
			
		
	}

	
	/**
	 * Assemlbes a sequence of conditions successively applied from an expression tree. This is done by traversing from the left most
	 * leave (which is a {@link ConditionAndOr) to the root. The expression tree is left-rotated, where possible, to minimize the 
	 * complexity of each expression in the sequence.
	 * @param where
	 * @param conditions
	 * @param conjunctions
	 * @param session
	 */
	public static void parseConditionSequence(String where,
			LinkedList<String> conditions,
			LinkedList<ConditionOperator> conjunctions, Session session) {
		
		if (where==null || where.length()==0) return;
		
		Parser p = new Parser(session);
		Expression tree = p.parseExpression(where);
		
		// traverse the and/or tree and do all left-rotations that are possible
		Stack<ConditionAndOr> dfs = new Stack<ConditionAndOr>();
		HashMap<ConditionAndOr,ConditionAndOr> toParent = new HashMap<ConditionAndOr, ConditionAndOr>();
		if (tree instanceof ConditionAndOr)
			dfs.push((ConditionAndOr) tree);
		while (!dfs.isEmpty()) {
			try {
				ConditionAndOr root = dfs.pop(); 
				Expression l = ReflectionUtils.get(root, "left");
				Expression r = ReflectionUtils.get(root, "right");
				
				if (l instanceof ConditionAndOr) {
					toParent.put((ConditionAndOr)l, root);
					dfs.push((ConditionAndOr) l);
				}
				if (r instanceof ConditionAndOr) {
					toParent.put((ConditionAndOr)r, root);
					dfs.push((ConditionAndOr) r);
				}
				
				if (r instanceof ConditionAndOr && ReflectionUtils.get(r, "andOrType").equals(ReflectionUtils.get(root, "andOrType"))) {
					// do the rotation
					ReflectionUtils.set(root, "right", ReflectionUtils.get(r, "left"));
					ReflectionUtils.set(r, "left", root);
					ConditionAndOr parent = toParent.get(root);
					if (parent==null) tree = r;
					else if (ReflectionUtils.get(parent, "right")==root) ReflectionUtils.set(parent, "right", r);
					else ReflectionUtils.set(parent, "left", r);
					toParent.put(root,(ConditionAndOr)r);
					toParent.put((ConditionAndOr)r, parent);
				}
				
			} catch (NoSuchFieldException | SecurityException
					| IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("Could not resolve!",e);
			}
			
		}
		
		try {
			// now extract the left-path bottom up of and and ors!
			while (tree instanceof ConditionAndOr) {
				conditions.addFirst(ReflectionUtils.<Expression,Expression>get(tree,"right").getSQL());
				conjunctions.addFirst(ConditionOperator.fromConditionAndOrType(ReflectionUtils.get(tree,"andOrType")));
				tree = ReflectionUtils.get(tree,"left");
			}
			conditions.addFirst(tree.getSQL());
			
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("Could not resolve!",e);
		}
		
	}

}
