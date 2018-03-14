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
package gedi.util.datastructure.collections;

import gedi.util.mutable.MutableInteger;

import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Vector;

public class SetStack<E> extends Vector<E> {

	private HashMap<E,MutableInteger> contained = new HashMap<E, MutableInteger>(); 



	/**
	 * Creates an empty Stack.
	 */
	public SetStack() {
	}
	
	public SetStack(Collection<? extends E> initial) {
		for (E e : initial)
			push(e);
	}

	/**
	 * Pushes an item onto the top of this stack. This has exactly
	 * the same effect as:
	 * <blockquote><pre>
	 * addElement(item)</pre></blockquote>
	 *
	 * @param   item   the item to be pushed onto this stack.
	 * @return  the <code>item</code> argument.
	 * @see     java.util.Vector#addElement
	 */
	public E push(E item) {
		addElement(item);
		MutableInteger c = contained.get(item);
		if (c==null) contained.put(item, c = new MutableInteger());
		c.N++;

		return item;
	}
	
	public E pushIfNeverContained(E item) {
		if (contained(item)) return null;
		addElement(item);
		MutableInteger c = contained.get(item);
		if (c==null) contained.put(item, c = new MutableInteger());
		c.N++;

		return item;
	}
	
	public boolean contained(E item) {
		return contained.containsKey(item);
	}
	
	public boolean contains(Object item) {
		return contained.containsKey(item) && contained.get(item).N>0;
	}
	
	public int getCurrentNumber(E item) {
		return contained.containsKey(item)?contained.get(item).N:0;
	}

	/**
	 * Removes the object at the top of this stack and returns that
	 * object as the value of this function.
	 *
	 * @return  The object at the top of this stack (the last item
	 *          of the <tt>Vector</tt> object).
	 * @throws  EmptyStackException  if this stack is empty.
	 */
	public synchronized E pop() {
		E       obj;
		int     len = size();

		obj = peek();
		removeElementAt(len - 1);
		contained.get(obj).N--;

		return obj;
	}

	/**
	 * Looks at the object at the top of this stack without removing it
	 * from the stack.
	 *
	 * @return  the object at the top of this stack (the last item
	 *          of the <tt>Vector</tt> object).
	 * @throws  EmptyStackException  if this stack is empty.
	 */
	public synchronized E peek() {
		int     len = size();

		if (len == 0)
			throw new EmptyStackException();
		return elementAt(len - 1);
	}

	/**
	 * Tests if this stack is empty.
	 *
	 * @return  <code>true</code> if and only if this stack contains
	 *          no items; <code>false</code> otherwise.
	 */
	public boolean empty() {
		return size() == 0;
	}

	/**
	 * Returns the 1-based position where an object is on this stack.
	 * If the object <tt>o</tt> occurs as an item in this stack, this
	 * method returns the distance from the top of the stack of the
	 * occurrence nearest the top of the stack; the topmost item on the
	 * stack is considered to be at distance <tt>1</tt>. The <tt>equals</tt>
	 * method is used to compare <tt>o</tt> to the
	 * items in this stack.
	 *
	 * @param   o   the desired object.
	 * @return  the 1-based position from the top of the stack where
	 *          the object is located; the return value <code>-1</code>
	 *          indicates that the object is not on the stack.
	 */
	public synchronized int search(Object o) {
		int i = lastIndexOf(o);

		if (i >= 0) {
			return size() - i;
		}
		return -1;
	}

	/** use serialVersionUID from JDK 1.0.2 for interoperability */
	private static final long serialVersionUID = 1224463164541339165L;
}
