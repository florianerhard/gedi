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

package gedi.util.datastructure.tree.priorityQueue;



public class FibonacciHeap<T> implements PriorityQueue<T> {
	
	private static final double BOUND = Math.log(2)/Math.log((1+Math.sqrt(5))/2);

	protected FibonacciList<T> rootlist;
	protected FibonacciNode<T> min;
	protected int size;

	public FibonacciHeap () {
		size     = 0;
		rootlist = new FibonacciList<T>();
		min      = null;
	}

	@Override
	public FibonacciNode<T> insert(double key, T o) {
		FibonacciNode<T> q = new FibonacciNode<T>(key,o);    
		rootlist.insert(q);
		if (min==null || key<min.getKey())
			min = q;
		size++;

		return q;
	}


	public void decreaseKey(PriorityQueueEntry<T> e, double k) {
		FibonacciNode<T> v = (FibonacciNode<T>)e;
		if (k>v.getKey()) throw new IllegalArgumentException("May not increase key!");
		v.setKey(k);

		if (k<min.getKey()) min = v;
		if (v.parent==null || v.parent.getKey()<k) {
			return;
		}

		v.mark = true;
		while (v.parent != null && v.mark) {
			FibonacciNode<T> parent = v.parent;
			
			parent.childlist.remove(v);
			parent.rank--;
			v.mark = false;
			
			
			rootlist.insert(v);
			v.parent = null;
			
			v = parent;
		}

		if (v.parent != null) v.mark = true;
		
	}



	@Override
	public PriorityQueueEntry<T> getMin() {
		return min;
	}

	@Override
	public PriorityQueueEntry<T> deleteMin() {
		if (min == null) return null;

		rootlist.merge(min.childlist);
		rootlist.remove(min);
		
		FibonacciNode<T> p = min;
		if (rootlist.element==null)
			min = null;
		else
			consolidate ();

		size--;
		return p;
	}


	@Override
	public void delete(PriorityQueueEntry<T> e) {
		decreaseKey(e,Double.NEGATIVE_INFINITY);
		deleteMin();
	}

	public boolean isEmpty () { 
		return rootlist.element==null; 
	}

	public int size () {
		return size; 
	}
	
	
//	public void checkIntegrity() {
//		int n = countNodes(null,rootlist);
//		if (size!=n)
//			throw new RuntimeException();
//	}
//	
//	private int countNodes(FibonacciNode<T> parent, FibonacciList<T> list) {
//		if (list.element==null) return 0;
//		int re = 1+countNodes(list.element,list.element.childlist);;
//		for (FibonacciNode<T> n = list.element.next; n!=list.element; n=n.next) {
//			re+=1+countNodes(n,n.childlist);
//			if (n.parent!=parent)
//				throw new RuntimeException();
//		}
//		return re;
//	}
	

//	public void merge(FibonacciHeap<T> F) {
//		if (F==null) return;
//		rootlist.merge(F.rootlist);
//		size = size+F.size;
//		if (min == null)
//			min = F.min;
//		else if (F.min != null && F.min.getKey()<min.getKey())
//			min = F.min;
//	}

	private FibonacciNode<T> link(FibonacciNode<T> x, FibonacciNode<T> y) {
		if (x.getKey()>y.getKey())
			return link(y, x);

		x.childlist.insert(y);
		y.parent = x;
		x.rank++;
		return x;
	}


	private static int log2(long n) {
		int re;
		for (re=0; n>0; n>>>=1)
			re++;
		return re;
	}

	@SuppressWarnings("unchecked")
	private void consolidate () {
		int maxrank = 1+(int)Math.floor(BOUND*log2(size));
		FibonacciNode<T>[] A = new FibonacciNode[maxrank];

		while (rootlist.element!=null) {
			FibonacciNode<T> x = rootlist.element;
			rootlist.remove(x);
			while (A[x.rank]!=null) {
				FibonacciNode<T> y = A[x.rank];
				A[x.rank] = null;
				x = link (x, y);
			}
			A[x.rank] = x;
		}

		min = null;
		for (int i = 0; i < maxrank; i++) {
			if (A[i] != null) {
				A[i].mark   = false;
				A[i].parent = null; 
				rootlist.insert(A[i]);
				if (min==null || A[i].getKey()<min.getKey())
					min = A[i];
			}
		}
		
	}




	private static class FibonacciNode<T> extends PriorityQueueEntry<T> {

		private FibonacciNode<T> next;
		private FibonacciNode<T> prev;
		private boolean mark;
		private int rank;
		
		
		private FibonacciNode<T> parent;
		private FibonacciList<T> childlist;

		private FibonacciNode (double key, T element) {
			super(key, element);
			childlist = new FibonacciList<T>();
			parent = null;
			mark = false;
			rank = 0;
		}

		private void setKey(double key) {
			this.key = key;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Node k=");
			sb.append(key);
			sb.append(" rank=");
			sb.append(rank);
			if (mark)
				sb.append(" marked");
			sb.append(" ");
			sb.append(getObject().toString().replace('\n', ' '));
			return sb.toString();
		}

	}


	private static class FibonacciList<T>  {

		private FibonacciNode<T> element;

		public void insert (FibonacciNode<T> n) {
			if (element==null) {
				n.next = n;
				n.prev = n;
				element = n;
			}
			else {
				n.next = element;
				n.prev = element.prev;    
				element.prev.next = n;
				element.prev = n;
				element = n;
			}
		}
		

		public void remove (FibonacciNode<T> n) {
			if (n!=null) {
				n.prev.next = n.next; 
				n.next.prev = n.prev;

				if (n==element)
					element = element.next;

				if (n==element) {
					element = null;
				}
			}
		}

		public void merge (FibonacciList<T> L) {
			if (L == null || L.element == null) return;
			if (element==null) {
				element=L.element;
				return;
			}
			FibonacciNode<T> prev = element.prev;
			L.element.prev.next = element;
			element.prev = L.element.prev;
			prev.next = L.element;
			L.element.prev = prev;
		}
		
//		private void checkContains(FibonacciNode<T> n) {
//			checkConsistency();
//			if (n==element)return;
//			for (FibonacciNode<T> e=element.next; e!=element; e=e.next) 
//				if (n==e) return;
//			throw new RuntimeException();
//		}
//		
//		private void checkConsistency() {
//			if (element==null)
//				return;
//			
//			int x = size;
//			FibonacciNode<T> p = element;
//			for (FibonacciNode<T> e=p.next; e!=element; e=e.next) {
//				if (x--<0 || e.prev!=p)
//					throw new RuntimeException();
//				p=e;
//			}
//		}
		
		@Override
		public String toString() {
			if (element==null) return "Nodelist []: 0";
			StringBuilder sb = new StringBuilder();
			sb.append("Nodelist [");
			sb.append(element.toString());
			int n = 1;
			for (FibonacciNode<T> e=element.next; e!=element; e=e.next) {
				sb.append(",");
				sb.append(e.toString());
				n++;
			}
			sb.append("]: ");
			sb.append(n);
			return sb.toString();
		}

	}


}
