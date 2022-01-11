package com.ai.cre.algo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.ai.cre.ontology.OntologyHandler;
import com.ai.cre.ontology.RestrictionNodeCollection;
import com.ai.cre.representation.ConRefExpPart;
import com.ai.cre.representation.ConceptNode;
import com.ai.cre.representation.ConceptNodeSet;
import com.ai.cre.representation.ConceptReferringExpression;
import com.ai.cre.representation.ProcessConRefExpression;

/**
 * Main algorithm to get all concept referring expressions that constitute a
 * correct, singular and unique answer to a generalized instance retrieval query
 * on a Horn-ALC ontology
 *
 */
public class RefExpRetrieval {

	/**
	 * state if sorting of restrictions in form of subsumption hierarchies should
	 * not be applied
	 */
	boolean no_sorting;

	public RefExpRetrieval() {
		this.no_sorting = false;
	}

	/**
	 * 
	 * @param apply_sort A {@code boolean} stating if the collected existential and
	 *                   universal restrictions should be sorted by subsumption or
	 *                   not
	 */
	public RefExpRetrieval(boolean apply_sort) {
		this.no_sorting = !apply_sort;
	}

	/**
	 * Get every correct answer for a instance retrieval query on a Horn-ALC
	 * ontology in form of singular, unique concept referring expressions
	 * 
	 * @param ontology An {@link OWLOntology}
	 * @param reasoner An {@link OWLReasoner} for the related ontology
	 * @param query    An {@link OWLClassExpression}
	 * @return A {@link Collection} of {@link ConceptReferringExpression} objects
	 */
	public Collection<ConceptReferringExpression> getInstanceReferringExpressions(OWLOntology ontology,
			OWLReasoner reasoner, OWLClassExpression query) {
		// create OntologyHandler for ontology
		OntologyHandler ontologyHandler = new OntologyHandler(ontology, reasoner);
		return getInstanceReferringExpressions(ontologyHandler, query);
	}

	/**
	 * Get every correct answer for a instance retrieval query on a Horn-ALC
	 * ontology in form of singular, unique concept referring expressions
	 * 
	 * @param ontology An {@link OWLOntology}
	 * @param query    An {@link OWLClassExpression}
	 * @return A {@link Collection} of {@link ConceptReferringExpression} objects
	 */
	public Collection<ConceptReferringExpression> getInstanceReferringExpressions(OWLOntology ontology,
			OWLClassExpression query) {
		// create OntologyHandler for ontology
		OntologyHandler ontologyHandler = new OntologyHandler(ontology);
		return getInstanceReferringExpressions(ontologyHandler, query);
	}

	/**
	 * Get every correct answer for a instance retrieval query on a Horn-ALC
	 * ontology in form of singular, unique concept referring expressions
	 * 
	 * @param ontologyHandler An {@link OntologyHandler} maintaining the considered
	 *                        ontology and reasoner
	 * @param query           An {@link OWLClassExpression}
	 * @return A {@link Collection} of {@link ConceptReferringExpression} objects
	 */
	public Collection<ConceptReferringExpression> getInstanceReferringExpressions(OntologyHandler ontologyHandler,
			OWLClassExpression query) {
		// get all appropriate restrictions from ontology as nodes
		RestrictionNodeCollection restNodeCol = ontologyHandler.getPotentialRightSideRestrictionNodes();
		if (!no_sorting) {
			// sort existential restriction nodes
			restNodeCol.exist_restrictions = new ConceptNodeSet<>(NodeSorter.sortBySubsumption(ontologyHandler,
					new ConceptNodeSet<>(restNodeCol.exist_restrictions)));
			// sort universal restriction nodes
			restNodeCol.univ_restrictions.forEach((role, nodes) -> restNodeCol.univ_restrictions.put(role,
					new ConceptNodeSet<>(NodeSorter.sortBySubsumption(ontologyHandler, new ConceptNodeSet<>(nodes)))));
		}

		// call actual method
		return getInstanceReferringExpressions(ontologyHandler, query, restNodeCol);
	}

	/**
	 * Get every correct answer for a instance retrieval query on a Horn-ALC
	 * ontology represented by {@code ontologyHandler} in form of singular, unique
	 * concept referring expressions
	 * 
	 * @param ontologyHandler An {@link OntologyHandler}
	 * @param query           An {@link OWLClassExpression}
	 * @param restNodeCol     A {@link RestrictionNodeCollection} with sorted
	 *                        elements
	 * @return A {@link Collection} of {@link ConceptReferringExpression} objects
	 */
	public Collection<ConceptReferringExpression> getInstanceReferringExpressions(OntologyHandler ontologyHandler,
			OWLClassExpression query, RestrictionNodeCollection restNodeCol) {
		return getInstanceReferringExpressions(ontologyHandler, query, restNodeCol, false);
	}

	/**
	 * Get every correct answer for a instance retrieval query on a Horn-ALC
	 * ontology represented by {@code ontologyHandler} in form of singular, unique
	 * concept referring expressions
	 * 
	 * @param ontologyHandler An {@link OntologyHandler}
	 * @param query           An {@link OWLClassExpression}
	 * @param restNodeCol     A {@link RestrictionNodeCollection} with sorted
	 *                        elements w.r.t. subsumption hierarchy
	 * @param showIndStats    A {@code boolean} stating if statistics about number
	 *                        of individuals and size of combined individual sets
	 *                        should be printed out
	 * @return A {@link Collection} of {@link ConceptReferringExpression} objects
	 */
	public Collection<ConceptReferringExpression> getInstanceReferringExpressions(OntologyHandler ontologyHandler,
			OWLClassExpression query, RestrictionNodeCollection restNodeCol, boolean showIndStats) {

		// variables for statistics about combined individual sets
		int numProcessedInds = 0;
		int numCombIndSets = 0;
		int maxIndSetSize = 0;

		// get all individuals appearing in ontology
		Set<OWLIndividual> individuals = ontologyHandler.getIndividuals();

		// get most specific concepts based on concept and role assertions for
		// individuals
		HashMap<Set<OWLClassExpression>, Set<OWLIndividual>> consForInds = new HashMap<>();
		ontologyHandler.getMostSpecificConceptsForIndividuals(individuals, restNodeCol.univ_restrictions)
				.forEach((ind, concepts) -> {
					// combine individuals to one group if they share the same concepts
					Set<OWLIndividual> stored_inds = consForInds.get(concepts);
					if (stored_inds == null) {
						stored_inds = new HashSet<OWLIndividual>();
					}
					stored_inds.add(ind);
					consForInds.put(concepts, stored_inds);
				});

		Collection<ConceptReferringExpression> answerRefExps = new HashSet<>();

		// call algorithm to construct referring expressions for each group of
		// individuals
		Set<Set<OWLClassExpression>> ind_concepts = consForInds.keySet();
		for (Set<OWLClassExpression> ind_con : ind_concepts) {
			// combine found (most specific) concepts to conjunction
			OWLClassExpression conj_cons = ontologyHandler.createConjunction(ind_con);

			// further separate combined individuals based on their shared role assertions
			// in order to block the same restrictions for the initial construction call
			HashMap<Set<RoleIndPair>, Set<OWLIndividual>> roleAssertForInds = new HashMap<>();
			for (OWLIndividual ind : consForInds.get(ind_con)) {
				Set<RoleIndPair> roleIndPairs = getRoleIndPairs(ontologyHandler.getRoleAssertions(ind));
				Set<OWLIndividual> stored_inds = roleAssertForInds.get(roleIndPairs);
				if (stored_inds == null) {
					stored_inds = new HashSet<OWLIndividual>();
				}
				stored_inds.add(ind);
				// extract pairs of role R and object b from role assertions R(a,b)
				roleAssertForInds.put(roleIndPairs, stored_inds);
			}

			for (Set<OWLIndividual> inds : roleAssertForInds.values()) {

				// collect data about combined individual sets
				if (showIndStats) {
					numCombIndSets += 1;
					numProcessedInds += inds.size();
					if (inds.size() > maxIndSetSize) {
						maxIndSetSize = inds.size();
					}
				}

				// get every concept referring expression starting with base individuals of
				// current concepts ind_con
				answerRefExps.addAll(constructRefExpAnswers(ontologyHandler, restNodeCol.exist_restrictions,
						restNodeCol.univ_restrictions, query, conj_cons, new ProcessConRefExpression(inds),
						new HashSet<OWLObjectSomeValuesFrom>()));
			}
		}

		// print out statistics
		if (showIndStats) {
			System.out.println("individuals: " + individuals.size());
			System.out.println("combined ind sets: " + numCombIndSets);
			System.out.println(" average set size: " + ((float) numProcessedInds / (float) numCombIndSets));
			System.out.println(" max set size: " + maxIndSetSize);
		}

		return answerRefExps;
	}

	/**
	 * Recursively construct concept referring expressions (which may contain
	 * cycles) that serve as correct, singular answers for a given instance
	 * retrieval query towards a considered ontology by selecting suitable
	 * restrictions from provided collections for a currently regarded concept in
	 * order to extend a given (possibly incomplete) referring expression
	 * 
	 * @param ontologyHandler      An {@link OntologyHandler} instance representing
	 *                             the considered ontology
	 * @param pos_exist_rests      A {@link Set} of {@link ConceptNode} elements for
	 *                             {@link OWLObjectSomeValuesFrom} instances
	 *                             representing the existential restrictions that
	 *                             can be used to build referring expressions by
	 *                             relating the current concept to another one
	 * @param pos_univ_rests_table A {@link HashMap} using roles in form of
	 *                             {@link OWLProperty} objects as keys to access
	 *                             {@link ConceptNodeSet} for
	 *                             {@link OWLClassExpression} instances representing
	 *                             universal restrictions with same role which may
	 *                             be used to constrain the concepts to which the
	 *                             current concept can be linked to by the selected
	 *                             existential restrictions
	 * @param query                An {@link OWLClassExpression} object forming an
	 *                             instance retrieval query
	 * @param current_concept      An {@link OWLClassExpression} object
	 * @param ref_exp              A {@link ProcessConRefExpression}
	 * @param used_ex_rests        A {@link Set} of {@link OWLObjectSomeValuesFrom}
	 *                             instances representing existential restrictions
	 *                             that have been used before for the construction
	 *                             of {@code ref_exp}
	 * @return A {@link Set} of {@link ConceptReferringExpression} elements
	 */
	protected Set<ConceptReferringExpression> constructRefExpAnswers(OntologyHandler ontologyHandler,
			ConceptNodeSet<OWLObjectSomeValuesFrom> pos_exist_rests,
			HashMap<OWLProperty, ConceptNodeSet<OWLClassExpression>> pos_univ_rests_table, OWLClassExpression query,
			OWLClassExpression current_concept, ProcessConRefExpression ref_exp,
			Set<OWLObjectSomeValuesFrom> used_ex_rests) {

		// set to collect completed referring expressions
		Set<ConceptReferringExpression> completed_ref_exps = new HashSet<>();

		/*
		 * find suitable existential restriction that may be applied for current concept
		 * to further construct the referring expression
		 */
		// collect possible existential restrictions
		Set<OWLObjectSomeValuesFrom> possible_rests = considerSubExistRestrictions(ontologyHandler, current_concept,
				pos_exist_rests, new HashSet<>());
		Set<OWLObjectProperty> roles = new HashSet<>();
		// get roles occurring in found existential restrictions
		for (OWLObjectSomeValuesFrom ex_rest : possible_rests) {
			roles.add(ex_rest.getProperty().asOWLObjectProperty());
		}

		// HashMap for possible universal restrictions that use the collected roles
		HashMap<OWLObjectProperty, OWLObjectAllValuesFrom> role_constraints = new HashMap<>();
		for (OWLObjectProperty role : roles) {
			// get inner concepts of universal restrictions for considered role
			ConceptNodeSet<OWLClassExpression> nodes_for_role = pos_univ_rests_table.get(role);
			if (nodes_for_role != null && !nodes_for_role.isEmpty()) {
				// get one universal restriction that combines every suitable universal
				// restriction with same role
				role_constraints.put(role, getRoleConstraint(ontologyHandler, current_concept, nodes_for_role, role));
			}
		}

		// set for restrictions applied for further construction of referring expression
		Set<OWLObjectSomeValuesFrom> next_rests = new HashSet<>();
		// consider every existential restriction ∃R.D from possible_rests
		for (OWLObjectSomeValuesFrom ex_rest : possible_rests) {
			// get role constraint ∀R.E for role R of current existential restriction ∃R.D
			OWLObjectAllValuesFrom role_con = role_constraints.get(ex_rest.getProperty().asOWLObjectProperty());
			// combine ∃R.D and ∀R.E to new restriction ∃R.(D ⊓ E), i.e., ∀R.E constraints
			// the related concept D of ∃R.D
			OWLObjectSomeValuesFrom combined_rest = ontologyHandler.combineToExistRestriction(ex_rest, role_con);
			// check if no semantically equivalent restriction already chosen for further
			// construction of referring expression
			if (ontologyHandler.checkIfNoEquivalent(combined_rest, next_rests)) {
				/*
				 * for initial call with 'empty' referring expression, check if role assertions
				 * in ABox that may lead to duplicate answers
				 */
				if (ref_exp.refExpParts.isEmpty()) {
					// get role and related concept of combined restriction
					OWLObjectProperty com_role = combined_rest.getProperty().asOWLObjectProperty();
					OWLClassExpression com_concept = combined_rest.getFiller();
					// only required to consider one base individual a as they possess equal
					// role assertions (see getInstanceReferringExpressions method)
					OWLIndividual ind = ref_exp.baseIndividuals.iterator().next();
					// check if there exists a role assertion R(a,b) with role R from combined_rest
					// and current individual a for which the related individual b is an instance of
					// the filler concept C from combined_rest, i.e. C(b) holds
					if (!ontologyHandler.checkIfRoleAssertionPresent(com_role, ind, com_concept)) {
						next_rests.add(combined_rest);
					}

				} else {
					/*
					 * look for cycle in current referring expression
					 */
					if (used_ex_rests.contains(combined_rest)) {
						// look for part of current referring expression that was constructed when
						// combined_rest was processed
						ConRefExpPart part = ref_exp.findPart(combined_rest);
						// mark cycle
						ref_exp.markCycle(part);
					} else {
						next_rests.add(combined_rest);
					}
				}
			}
		}

		/*
		 * check if answer for query found
		 */
		if (ontologyHandler.checkIfSubClass(current_concept, query)) {
			completed_ref_exps.addAll(ref_exp.complete());
			// done after cycle-detection to ensure that completed referring expression
			// really contain cycle-notation
		}

		/*
		 * continue construction of current referring expression
		 */
		// consider each selected existential restriction ∃R.D
		for (OWLObjectSomeValuesFrom nxt_rest : next_rests) {
			// add current restriction to set of used ones
			HashSet<OWLObjectSomeValuesFrom> new_used_rests = new HashSet<>(used_ex_rests);
			new_used_rests.add(nxt_rest);
			// recursive call with D as new current concept, extended referring expression
			// and updated used_ex_rests set
			completed_ref_exps.addAll(constructRefExpAnswers(ontologyHandler, pos_exist_rests, pos_univ_rests_table,
					query, nxt_rest.getFiller(), ref_exp.getExtended(nxt_rest), new_used_rests));
		}

		return completed_ref_exps;
	}

	/**
	 * Get a universal restriction {@code ∀R.D} that can serve as constraint for a
	 * existential restriction with the same role and which was created by combining
	 * all universal restrictions {@code ∀R.D_i} with most specific inner concepts
	 * {@code D_i} from a node hierarchy for which {@code C ⊑ ∀R.D_i} holds (w.r.t.
	 * to the considered ontology) with {@code C} as current concept and {@code D}
	 * as conjunction of every {@code D_i}
	 * 
	 * @param ontologyHandler A {@link OntologyHandler} representing the considered
	 *                        ontology
	 * @param current_concept A {@link OWLClassExpression} instance
	 * @param nodes_for_role  A {@link ConceptNodeSet} for
	 *                        {@link OWLClassExpression} instances
	 * @param role            A {@link OWLObjectProperty} instance representing the
	 *                        role that all the considered universal restrictions
	 *                        have in common
	 * @return One {@link OWLObjectAllValuesFrom} object or {@code null} if no
	 *         appropriate could be found
	 */
	protected OWLObjectAllValuesFrom getRoleConstraint(OntologyHandler ontologyHandler,
			OWLClassExpression current_concept, ConceptNodeSet<OWLClassExpression> nodes_for_role,
			OWLObjectProperty role) {
		// collect inner concepts of universal restrictions for current concept C
		Set<OWLClassExpression> fillers = considerSubUnivRestrictions(ontologyHandler, current_concept, nodes_for_role,
				role);
		if (fillers.isEmpty()) {
			return null;
		} else {
			// return one universal restriction that serves as combined role constraint
			return ontologyHandler.createUnivRestriction(role, fillers);
		}
	}

	/**
	 * Get set of most specific inner concepts {@code D} of universal restrictions
	 * {@code ∀R.D} from given nodes and their sub-elements for which
	 * {@code C ⊑ ∀R.D} holds (w.r.t. to the considered ontology) for a given
	 * concept {@code C}
	 * 
	 * @param ontologyHandler A {@link OntologyHandler} representing the considered
	 *                        ontology
	 * @param current_concept A {@link OWLClassExpression} instance
	 * @param nodes_for_role  A {@link ConceptNodeSet} for
	 *                        {@link OWLClassExpression} instances
	 * @param role            A {@link OWLObjectProperty} instance representing the
	 *                        role that all the considered universal restrictions
	 *                        have in common
	 * @return A {@link Set} of {@link OWLClassExpression} elements
	 */
	private Set<OWLClassExpression> considerSubUnivRestrictions(OntologyHandler ontologyHandler,
			OWLClassExpression current_concept, ConceptNodeSet<OWLClassExpression> nodes_for_role,
			OWLObjectProperty role) {
		// sets to collect suitable most specific inner concepts of universal
		// restrictions
		Set<OWLClassExpression> fillers = new HashSet<>();
		Set<OWLClassExpression> sub_fillers = new HashSet<>();

		for (ConceptNode<OWLClassExpression> node : nodes_for_role) {
			// C ⊑ ∀R.D
			if (ontologyHandler.checkIfSubClass(current_concept,
					ontologyHandler.createUnivRestriction(role, node.getConcept().asConjunctSet()))) {

				if (no_sorting) {
					ontologyHandler.addConceptIfMostSpecific(fillers, node.getConcept());
				} else {
					// look for candidates in subs
					sub_fillers = considerSubUnivRestrictions(ontologyHandler, current_concept, node.subs, role);
					if (sub_fillers.isEmpty()) {
						// (inner) concept of current node is most specific
						fillers.addAll(node.concepts);
					} else {
						fillers.addAll(sub_fillers);
					}
				}

			}
		}

		return fillers;
	}

	/**
	 * For a current concept {@code C}, get every existential restriction
	 * {@code ∃R.D} from an ordered node hierarchy represented by the top node
	 * (root), for which {@code C ⊑ ∃R.D} holds (w.r.t. to the considered ontology)
	 * and if {@code ∃R.D} is minimal w.r.t. role-concept-subsumption which means
	 * that there does not exist another restriction {@code ∃S.F} for which
	 * {@code S = R} and {@code F ⊑ D} hold
	 * <p>
	 * Note: Minimality is necessary to ensure singularity and uniqueness (no
	 * semantic duplicates) of constructed referring expression
	 * </p>
	 * 
	 * @param ontologyHandler A {@link OntologyHandler} instance representing the
	 *                        considered ontology
	 * @param current_concept A {@link OWLClassExpression} instance
	 * @param nodes           A {@link ConceptNodeSet} for
	 *                        {@link OWLObjectSomeValuesFrom} elements
	 * @param candidates      A {@link Set} of {@link OWLObjectSomeValuesFrom}
	 *                        objects representing potential answers (initially
	 *                        empty)
	 * @return A {@link Set} containing the found minimal existential restrictions
	 */
	protected Set<OWLObjectSomeValuesFrom> considerSubExistRestrictions(OntologyHandler ontologyHandler,
			OWLClassExpression current_concept, ConceptNodeSet<OWLObjectSomeValuesFrom> nodes,
			Set<OWLObjectSomeValuesFrom> candidates) {

		// collection for sub-candidates
		ConceptNodeSet<OWLObjectSomeValuesFrom> sub_candidates = new ConceptNodeSet<>();
		// consider given (sub) nodes
		for (ConceptNode<OWLObjectSomeValuesFrom> node : nodes) {

			// get rest. ∃S.F from considered node
			OWLObjectSomeValuesFrom ex_rest = node.getConcept();

			if (!no_sorting) {
				// check if C ⊑ ∃S.F for current_concept C
				if (ontologyHandler.checkIfSubClass(current_concept, ex_rest)) {
					sub_candidates.add(node);

					// check if minimality of previously collected candidates is impeded
					for (OWLObjectSomeValuesFrom old_cand : new HashSet<>(candidates)) {
						// Note: since minimality w.r.t. role-concept-subsumption requires that
						// restrictions share same role, a super-node cannot impede minimality of a
						// sub-node (except for equivalent filler concepts, in which case either the
						// sub-node or the super-node may be considered minimal)
						for (OWLObjectSomeValuesFrom sb : node.concepts) {
							if (ontologyHandler.checkRoleConceptSubsumption(sb, old_cand)) {
								// old candidate is not minimal
								candidates.remove(old_cand);
							}
						}
					}
				}
				// look for restrictions in subs of sub-nodes
				for (ConceptNode<OWLObjectSomeValuesFrom> sub_cand : sub_candidates) {
					candidates.addAll(sub_cand.concepts);
					candidates = considerSubExistRestrictions(ontologyHandler, current_concept, sub_cand.subs,
							candidates);
				}
			}

			// no sorting applied
			else {
				if (ontologyHandler.checkIfSubClass(current_concept, ex_rest)) {
					ontologyHandler.addExRestIfMinimal(candidates, ex_rest);
				}
			}

		}

		return candidates;
	}

	/**
	 * A helper class to store a {@link OWLObjectProperty} {@code R} together with
	 * an {@link OWLIndividual} {@code b} based on a
	 * {@link OWLObjectPropertyAssertionAxiom} {@code R(a,b)}
	 */
	protected class RoleIndPair {
		OWLObjectProperty role;
		OWLIndividual ind;

		public RoleIndPair(OWLObjectPropertyAssertionAxiom ax) {
			this.role = ax.getProperty().asOWLObjectProperty();
			this.ind = ax.getObject();
		}

		@Override
		public int hashCode() {
			String str = role.getIRI().getIRIString() + ind.toStringID();
			return str.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof RoleIndPair) {
				RoleIndPair op = (RoleIndPair) o;
				if (this.role.equals(op.role) && this.ind.equals(op.ind)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}

		}
	}

	/**
	 * Extract pairs {@code R-b)} from a set of role assertions of the form
	 * {@code R(a,b)}
	 * 
	 * @param roleAssertions A {@link Set} of
	 *                       {@link OWLObjectPropertyAssertionAxiom} objects
	 * @return A {@link Set} of {@link RoleIndPair} objects
	 */
	private Set<RoleIndPair> getRoleIndPairs(Set<OWLObjectPropertyAssertionAxiom> roleAssertions) {
		HashSet<RoleIndPair> pairs = new HashSet<>();
		for (OWLObjectPropertyAssertionAxiom ax : roleAssertions) {
			pairs.add(new RoleIndPair(ax));
		}
		return pairs;
	}

}
