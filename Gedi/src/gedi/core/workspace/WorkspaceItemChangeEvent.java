package gedi.core.workspace;


import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;

public class WorkspaceItemChangeEvent {

	public enum ChangeType {
		CREATED,DELETED,MODIFIED,UNKNOWN;

		public static ChangeType fromWatchKey(Kind<?> kind) {
			if (kind==StandardWatchEventKinds.ENTRY_CREATE)
				return CREATED;
			if (kind==StandardWatchEventKinds.ENTRY_DELETE)
				return DELETED;
			if (kind==StandardWatchEventKinds.ENTRY_MODIFY)
				return MODIFIED;
			return UNKNOWN;
		}
	}
	
	private WorkspaceItem item;
	private ChangeType type;

	public WorkspaceItemChangeEvent(WorkspaceItem item, ChangeType type) {
		this.item = item;
		this.type = type;
	}

	public WorkspaceItem getItem() {
		return item;
	}

	public ChangeType getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((item == null) ? 0 : item.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		WorkspaceItemChangeEvent other = (WorkspaceItemChangeEvent) obj;
		if (item == null) {
			if (other.item != null)
				return false;
		} else if (!item.equals(other.item))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "WorkspaceItemChangeEvent [item=" + item + ", type=" + type
				+ "]";
	}
	
}
