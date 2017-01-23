package gedi.core.data.table;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.orm.Orm;
import gedi.util.orm.OrmField;
import gedi.util.orm.OrmObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;


public class TableMetaInformation<T> implements OrmObject {
	
	@OrmField private String tablename;
	@OrmField private String creator;
	@OrmField long versionTimestamp;
	@OrmField private int versionNumber;
	@OrmField private String versionDescription;
	@OrmField private String name;
	@OrmField private String className;
	
	private Class<T> cls;
	private String[] names;
	private Class[] classes;
	
	private Table<T> table;
	
	private HashMap<String,Integer> nameToIndex;
	
	@Override
	public void postOrmAction() {
		try {
			cls = (Class<T>) Class.forName(className);
		} catch (ClassNotFoundException e) {
			Tables.log.log(Level.SEVERE, "Cannot perform ORM for class "+className, e);
			throw new RuntimeException("Cannot perform ORM for class "+className, e);
		}
		names = Orm.getOrmFieldNames(cls);
		classes = Orm.getOrmFieldClasses(cls);
		nameToIndex = ArrayUtils.createIndexMap(names);
	}
	
	TableMetaInformation(String tablename,String name, String creator, int versionNumber, String versionDescription, Class<T> cls) {
		this.tablename = tablename;
		this.name = name;
		this.creator = creator;
		this.versionNumber = versionNumber;
		this.versionDescription = versionDescription;
		this.cls = cls;
		this.className = cls.getName();
		names = Orm.getOrmFieldNames(cls);
		classes = Orm.getOrmFieldClasses(cls);
		nameToIndex = ArrayUtils.createIndexMap(names);
	}
	
	void setTable(Table<T> table) {
		this.table = table;
	}
	
	public Table<T> getTable() {
		return table;
	}

	public int getNumColumns() {
		return names.length;
	}
	
	public String getColumnName(int c) {
		return names[c];
	}

	public Class<?> getColumnClass(int c) {
		return classes[c];
	}
	
	public Class<?> getColumnClass(String name) {
		Integer index = nameToIndex.get(name);
		if (index==null) return null;
		return classes[index];
	}
	
	public Class<T> getDataClass() {
		return cls;
	}
	
	
	public String getTablename() {
		return tablename;
	}
	
	public String getUnquotedTablename() {
		return StringUtils.trim(tablename,'"');
	}

	public String getCreator() {
		return creator;
	}

	public long getVersionTimestamp() {
		return versionTimestamp;
	}

	public int getVersionNumber() {
		return versionNumber;
	}

	public String getVersionDescription() {
		return versionDescription;
	}

	public String getName() {
		return name;
	}
	@Override
	public String toString() {
		return "TableMetaInformation [tablename=" + tablename + ", creator="
				+ creator + ", versionTimestamp=" + versionTimestamp
				+ ", versionNumber=" + versionNumber + ", versionDescription="
				+ versionDescription + ", name=" + name + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + versionNumber;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TableMetaInformation other = (TableMetaInformation) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (versionNumber != other.versionNumber)
			return false;
		return true;
	}

	
	
	
}
