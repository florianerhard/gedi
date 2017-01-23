package gedi.util.datastructure.tree.priorityQueue;

public class PriorityQueueEntry<T> {

	protected double key;
	protected T o;
	
	public PriorityQueueEntry(double key, T o) {
		this.key = key;
		this.o = o;
	}
	public double getKey() {
		return key;
	}
	public T getObject() {
		return o;
	}
	
	
	
}
