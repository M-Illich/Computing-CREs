package com.ai.cre.representation;

import java.util.LinkedList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * A node representing a list of equivalent concepts which may furthermore have
 * some sub- and super-nodes
 *
 * @param <C> the concept type represented by the node
 */
public class ConceptNode<C extends OWLClassExpression> {

	/**
	 * A {@link List} of equivalent {@link OWLClassExpression} concepts being
	 * represented by the node
	 */
	public List<C> concepts;

	/**
	 * A {@link ConceptNodeSet} for sub-elements of the node w.r.t. some order
	 */
	public ConceptNodeSet<C> subs;

	/**
	 * A {@link ConceptNodeSet<C>} for super-elements of the node w.r.t. some order
	 */
	public ConceptNodeSet<C> supers;

	public ConceptNode(C con) {
		this.concepts = new LinkedList<>();
		this.concepts.add(con);
		this.subs = new ConceptNodeSet<C>();
		this.supers = new ConceptNodeSet<C>();
	}

	public ConceptNode(C con, ConceptNodeSet<C> sub_nodes) {
		this.concepts = new LinkedList<>();
		this.concepts.add(con);
		this.subs = sub_nodes;
		this.supers = new ConceptNodeSet<C>();
	}

	/**
	 * Get a concept represented by the node
	 * 
	 * @return An element from {@link #concepts} or {@code null} if none available
	 */
	public C getConcept() {
		if (!this.concepts.isEmpty()) {
			return this.concepts.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Add a {@link ConceptNode} element to the {@link #subs} list
	 * 
	 * @param sub A {@link ConceptNode} considered as sub-element of the node w.r.t.
	 *            to some order
	 */
	public void addSubNode(ConceptNode<C> sub) {
		this.subs.add(sub);
	}

	/**
	 * Create a subsumption relation to another node
	 * 
	 * @param sub A {@link ConceptNode} considered as sub-element of the node w.r.t.
	 *            to some order
	 */
	public void addRelationToSub(ConceptNode<C> sub) {
		this.subs.add(sub);
		sub.supers.add(this);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o instanceof ConceptNode<?>) {
			ConceptNode<?> c = (ConceptNode<?>) o;
			return this.concepts.equals(c.concepts);
		} else {
			return false;
		}
	}

}
