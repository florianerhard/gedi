package gedi.util.datastructure.tree.priorityQueue;

public interface PriorityQueue<T> {

  boolean isEmpty();
  int size();

  PriorityQueueEntry<T> insert(double key, T o);

  PriorityQueueEntry<T> deleteMin();
  void delete(PriorityQueueEntry<T> o);

  void decreaseKey (PriorityQueueEntry<T> e, double k);

  PriorityQueueEntry<T> getMin();

  
  
}