package com.ai.cre.ontology;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLProperty;

import com.ai.cre.representation.ConceptNode;
import com.ai.cre.representation.ConceptNodeSet;

/**
 * A class to store {@link ConceptNode} objects that represent existential and
 * universal restrictions each provided in separate collections
 */
public class RestrictionNodeCollection {

	/**
	 * An {@link OntologyHandler} representing the ontology from which the
	 * restrictions are collected
	 */
	public OntologyHandler ontologyHandler;

	/**
	 * Existential restrictions represented by {@link ConceptNode} objects stored in
	 * a {@link ConceptNodeSet} referring to {@link OWLObjectSomeValuesFrom}
	 * elements
	 */
	public ConceptNodeSet<OWLObjectSomeValuesFrom> exist_restrictions;

	/**
	 * Universal restrictions represented by {@link ConceptNode} separated by roles
	 * and stored in a {@link HashMap} that uses a {@link OWLProperty} as key to
	 * access a {@link ConceptNodeSet} referring to {@link OWLClassExpression}
	 * objects
	 */
	public HashMap<OWLProperty, ConceptNodeSet<OWLClassExpression>> univ_restrictions;

	/**
	 * Create a {@link RestrictionNodeCollection} with empty collections for both
	 * existential and universal restrictions represented by {@link ConceptNode}
	 * objects
	 * 
	 * @param ontologyHandler An {@link OntologyHandler} representing the ontology
	 *                        from which the restrictions should be collected
	 */
	public RestrictionNodeCollection(OntologyHandler ontologyHandler) {
		this.exist_restrictions = new ConceptNodeSet<OWLObjectSomeValuesFrom>();
		this.univ_restrictions = new HashMap<OWLProperty, ConceptNodeSet<OWLClassExpression>>();
		this.ontologyHandler = ontologyHandler;
	}

	/**
	 * Create a {@link ConceptNode} for the given existential restriction and add it
	 * to the collection {@link #exist_restrictions}
	 * 
	 * @param ex_rest A {@link OWLObjectSomeValuesFrom} object
	 */
	public void addExistRestriction(OWLObjectSomeValuesFrom ex_rest) {
		boolean no_equiv = true;
		Iterator<ConceptNode<OWLObjectSomeValuesFrom>> iterator = this.exist_restrictions.iterator();
		// add new ex_rest to an already present node if concepts are equivalent
		while (no_equiv && iterator.hasNext()) {
			ConceptNode<OWLObjectSomeValuesFrom> node = iterator.next();
			no_equiv = !ontologyHandler.addIfEquivalent(ex_rest, node.concepts);
		}
		if (no_equiv) {
			// create new node for ex_rest
			this.exist_restrictions.add(new ConceptNode<OWLObjectSomeValuesFrom>(ex_rest));
		}

	}

	/**
	 * Create a {@link ConceptNode} for a given universal restriction {@code ∀R.D}
	 * and add its inner concept {@code D} to the appropriate collection in
	 * {@link #univ_restrictions} based on its used role {@code R}
	 * 
	 * @param univ_rest A {@link OWLObjectAllValuesFrom} object
	 */
	public void addUnivRestriction(OWLObjectAllValuesFrom univ_rest) {
		// extract role from restriction
		OWLProperty role = univ_rest.getProperty().asOWLObjectProperty();
		// look for role-specific set of restrictions
		ConceptNodeSet<OWLClassExpression> role_rest_set = this.univ_restrictions.get(role);
		// create a new list if role not used before
		if (role_rest_set == null) {
			role_rest_set = new ConceptNodeSet<OWLClassExpression>();
		}
		// create node for restriction and add inner concept to role-specific collection
		role_rest_set.add(new ConceptNode<OWLClassExpression>(univ_rest.getFiller()));
		// add updated collection to hashmap
		this.univ_restrictions.put(role, role_rest_set);
	}

	/**
	 * Merge the object with the given {@link RestrictionNodeCollection} by adding
	 * the {@code other}'s restrictions if not {@code null}
	 * 
	 * @param other A {@link RestrictionNodeCollection}
	 */
	public void mergeWith(RestrictionNodeCollection other) {
		if (other != null) {
			// add existential restrictions
			this.exist_restrictions.addAll(other.exist_restrictions);
			// add every role-specific collection of universal restrictions
			other.univ_restrictions.forEach((role, col) -> this.mergeUnivRestsForRole(role, col));

		}
	}

	/**
	 * Add a collection of universal restrictions that share a given role to
	 * {@link #univ_restrictions}
	 * 
	 * @param role An {@link OWLProperty}
	 * @param col  A {@link Set} of {@link ConceptNode} objects for
	 *             {@link OWLClassExpression} instances
	 */
	private void mergeUnivRestsForRole(OWLProperty role, Set<ConceptNode<OWLClassExpression>> col) {
		// look for role-specific set of restrictions
		ConceptNodeSet<OWLClassExpression> role_rest_set = this.univ_restrictions.get(role);
		// create a new set if role not used before
		if (role_rest_set == null) {
			role_rest_set = new ConceptNodeSet<OWLClassExpression>();
		}
		// add nodes to role-specific collection
		role_rest_set.addAll(col);
		// add updated collection to hashmap
		this.univ_restrictions.put(role, role_rest_set);
	}

	/**
	 * Check if {@link RestrictionNodeCollection} object contains the given
	 * universal restriction with related {@code role}
	 * 
	 * @param role      The {@link OWLProperty} that appears as role in
	 *                  {@code univ_rest}
	 * @param univ_rest A {@link OWLObjectAllValuesFrom}
	 * @return {@code true} if the hashmap entry for the key {@code role} contains
	 *         inner concept of {@code univ_rest}
	 */
	public boolean containsUnivRest(OWLProperty role, OWLObjectAllValuesFrom univ_rest) {
		// check if same role used
		if (!univ_rest.getProperty().getNamedProperty().equals(role)) {
			return false;
		}
		// check if inner concept of used_rest given
		ConceptNodeSet<OWLClassExpression> role_univ_rests = this.univ_restrictions.get(role);
		if (role_univ_rests == null) {
			return false;
		} else {
			boolean found = false;
			Iterator<ConceptNode<OWLClassExpression>> iterator = role_univ_rests.iterator();
			while (!found && iterator.hasNext()) {
				if (iterator.next().concepts.contains(univ_rest.getFiller())) {
					found = true;
				}
			}
			return found;
		}

	}

	/**
	 * Check if {@link RestrictionNodeCollection} object contains the given
	 * existential restriction
	 * 
	 * @param exist_rest A {@link OWLObjectSomeValuesFrom}
	 * @return {@code true} if {@code exist_rest} in collection of existential
	 *         restrictions
	 */
	public boolean containsExistRest(OWLObjectSomeValuesFrom exist_rest) {
		boolean found = false;
		Iterator<ConceptNode<OWLObjectSomeValuesFrom>> iterator = this.exist_restrictions.iterator();
		while (!found && iterator.hasNext()) {
			if (iterator.next().concepts.contains(exist_rest)) {
				found = true;
			}
		}
		return found;
	}

}