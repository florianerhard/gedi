package gedi.core.data.table;

public enum TableType {

	Temporary,Workspace,Gedi;

	public boolean isPersistent() {
		return this==Workspace || this==Gedi;
	}
}
