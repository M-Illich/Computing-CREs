package com.ai.cre.representation;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * An alternative implementation of {@link HashSet} for storing
 * {@link ConceptNode} elements being identified by the concepts they represent
 *
 * @param <E> the type of the concepts represented by stored nodes
 */
public class ConceptNodeSet<E extends OWLClassExpression> extends AbstractSet<ConceptNode<E>> {

	private HashMap<List<E>, ConceptNode<E>> map;

	/**
	 * Constructs a new, empty set; the backing {@code HashMap} instance has default
	 * initial capacity (16) and load factor (0.75).
	 */
	public ConceptNodeSet() {
		map = new HashMap<>();
	}

	/**
	 * Constructs a new set containing the elements in the specified collection. The
	 * {@code HashMap} is created with default load factor (0.75) and an initial
	 * capacity sufficient to contain the elements in the specified collection.
	 *
	 * @param c the collection whose elements are to be placed into this set
	 * @throws NullPointerException if the specified collection is null
	 */
	public ConceptNodeSet(Collection<? extends ConceptNode<E>> c) {
		map = new HashMap<>(Math.max((int) (c.size() / .75f) + 1, 16));
		for (ConceptNode<E> e : c) {
			add(e);
		}
	}

	/**
	 * Constructs a new, empty set; the backing {@code HashMap} instance has the
	 * specified initial capacity and the specified load factor.
	 *
	 * @param initialCapacity the initial capacity of the hash map
	 * @param loadFactor      the load factor of the hash map
	 * @throws IllegalArgumentException if the initial capacity is less than zero,
	 *                                  or if the load factor is nonpositive
	 */
	public ConceptNodeSet(int initialCapacity, float loadFactor) {
		map = new HashMap<>(initialCapacity, loadFactor);
	}

	/**
	 * Constructs a new, empty set; the backing {@code HashMap} instance has the
	 * specified initial capacity and default load factor (0.75).
	 *
	 * @param initialCapacity the initial capacity of the hash table
	 * @throws IllegalArgumentException if the initial capacity is less than zero
	 */
	public ConceptNodeSet(int initialCapacity) {
		map = new HashMap<>(initialCapacity);
	}

	/**
	 * Returns an iterator over the elements in this set. The elements are returned
	 * in no particular order.
	 *
	 * @return an Iterator over the elements in this set
	 * @see ConcurrentModificationException
	 */
	public Iterator<ConceptNode<E>> iterator() {
		return map.values().iterator();
	}

	/**
	 * Returns the number of elements in this set (its cardinality).
	 *
	 * @return the number of elements in this set (its cardinality)
	 */
	public int size() {
		return map.size();
	}

	/**
	 * Returns {@code true} if this set contains no elements.
	 *
	 * @return {@code true} if this set contains no elements
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Returns {@code true} if this set contains the specified element. More
	 * formally, returns {@code true} if and only if this set contains an element
	 * {@code e} such that {@code Objects.equals(o, e)}.
	 *
	 * @param node element whose presence in this set is to be tested
	 * @return {@code true} if this set contains the specified element
	 */
	public boolean contains(ConceptNode<E> node) {
		return map.containsKey(node.concepts);
	}

	/**
	 * Adds the specified element to this set if it is not already present. More
	 * formally, adds the specified element {@code e} to this set if this set
	 * contains no element {@code e2} such that {@code Objects.equals(e, e2)}. If
	 * this set already contains the element, the call leaves the set unchanged and
	 * returns {@code false}.
	 *
	 * @param node element to be added to this set
	 * @return {@code true} if this set did not already contain the specified
	 *         element
	 */
	public boolean add(ConceptNode<E> node) {
		return map.putIfAbsent(node.concepts, node) == null;
	}

	/**
	 * Removes the specified element from this set if it is present. More formally,
	 * removes an element {@code e} such that {@code Objects.equals(o, e)}, if this
	 * set contains such an element. Returns {@code true} if this set contained the
	 * element (or equivalently, if this set changed as a result of the call). (This
	 * set will not contain the element once the call returns.)
	 *
	 * @param o object to be removed from this set, if present
	 * @return {@code true} if the set contained the specified element
	 */
	public boolean remove(ConceptNode<E> node) {
		return map.remove(node.concepts) != null;
	}

	/**
	 * Removes all of the elements from this set. The set will be empty after this
	 * call returns.
	 */
	public void clear() {
		map.clear();
	}

}