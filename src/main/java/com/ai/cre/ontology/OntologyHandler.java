package com.ai.cre.ontology;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

import com.ai.cre.representation.ConceptNode;
import com.ai.cre.representation.ConceptNodeSet;

/**
 * Simplifies working with (Horn-ALC) ontologies by providing methods for
 * retrieving individuals or certain concepts, as well as performing reasoning
 * tasks using HermiT.
 * 
 */
public class OntologyHandler {

	/**
	 * The considered (Horn-ALC) ontology
	 */
	public OWLOntology ontology;

	/**
	 * A reasoner for the considered ontology
	 */
	public OWLReasoner reasoner;

	private OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	private OWLDataFactory factory = manager.getOWLDataFactory();

	/**
	 * Create {@link OntologyHandler} instance with empty ontology, i.e., every
	 * reasoning query results in {@code false}
	 */
	public OntologyHandler() {
		try {
			this.ontology = manager.createOntology();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		this.reasoner = new Reasoner(new Configuration(), ontology);
	}

	/**
	 * Create {@link OntologyHandler} instance based on a HermiT reasoner for a
	 * (Horn-ALC) ontology loaded from given {@link File}
	 * 
	 * @param ontologyFile A {@link File} representing a (Horn-ALC) OWL-ontology
	 */
	public OntologyHandler(File ontologyFile) {
		try {
			OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
			this.ontology = ontology;
			// create HermiT reasoner instance for ontology
			this.reasoner = new Reasoner(new Configuration(), ontology);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create {@link OntologyHandler} instance based on a HermiT reasoner for given
	 * (Horn-ALC) ontology
	 * 
	 * @param ontology An {@link OWLOntology} instance of a (Horn-ALC) ontology
	 */
	public OntologyHandler(OWLOntology ontology) {
		this.ontology = ontology;
		// create HermiT reasoner instance for ontology
		this.reasoner = new Reasoner(new Configuration(), ontology);
	}

	/**
	 * Create {@link OntologyHandler} instance for given (Horn-ALC) ontology using
	 * the provided reasoner
	 * 
	 * @param ontology An {@link OWLOntology} instance of a (Horn-ALC) ontology
	 * @param reasoner An {@link OWLReasoner} instance
	 */
	public OntologyHandler(OWLOntology ontology, OWLReasoner reasoner) {
		this.ontology = ontology;
		this.reasoner = reasoner;
	}

	/**
	 * Create {@link OntologyHandler} instance based on a HermiT reasoner for an
	 * ontology, which may be the result of merging all the imports of a given
	 * (Horn-ALC) ontology loaded from a given {@link File}
	 * 
	 * @param ontologyFile A {@link File} representing a (Horn-ALC) OWL-ontology
	 * @param merge        A {@code boolean} stating if the considered ontology
	 *                     shall be merged with its imports to create one combined
	 *                     ontology
	 */
	public OntologyHandler(File ontologyFile, boolean merge) {
		try {
			OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
			// create merged ontology if it contains import statements
			if (merge && !ontology.imports().findAny().isEmpty()) {
				OWLOntologyMerger merger = new OWLOntologyMerger(manager);
				IRI iri = IRI.create("merged_" + ontology.getOntologyID().getOntologyIRI().get());
				ontology = merger.createMergedOntology(manager, iri);
			}
			this.ontology = ontology;
			// create HermiT reasoner instance for ontology
			this.reasoner = new Reasoner(new Configuration(), ontology);

		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create {@link OntologyHandler} instance based on a HermiT reasoner for an
	 * ontology, which may be the result of merging all the imports of a given
	 * (Horn-ALC) ontology
	 * 
	 * @param ontology An {@link OWLOntology} instance of a (Horn-ALC) ontology
	 * @param merge    A {@code boolean} stating if the considered ontology shall be
	 *                 merged with its imports to create one combined ontology
	 */
	public OntologyHandler(OWLOntology ontology, boolean merge) {
		// create merged ontology if it contains import statements
		if (merge && !ontology.imports().findAny().isEmpty()) {
			OWLOntologyMerger merger = new OWLOntologyMerger(manager);
			IRI iri = IRI.create("merged_" + ontology.getOntologyID().getOntologyIRI().get());
			try {
				ontology = merger.createMergedOntology(manager, iri);
			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
			}
		}
		this.ontology = ontology;
		// create HermiT reasoner instance for ontology
		this.reasoner = new Reasoner(new Configuration(), ontology);
	}

	/**
	 * Check if a concept {@code c} is a subclass of another concept {@code d}
	 * 
	 * @param c An {@link OWLClassExpression} object
	 * @param d An {@link OWLClassExpression} object
	 * @return {@code true} if {@link #ontology} entails {@code c ⊑ d}, else
	 *         {@code false}
	 */
	public boolean checkIfSubClass(OWLClassExpression c, OWLClassExpression d) {
		// create a subsumption axiom for given concepts and check if it can be entailed
		// by the ontology
		return reasoner.isEntailed(factory.getOWLSubClassOfAxiom(c, d));
	}

	/**
	 * Check if a concept {@code c} is equivalent to another concept {@code d}
	 * 
	 * @param c An {@link OWLClassExpression} object
	 * @param d An {@link OWLClassExpression} object
	 * @return {@code true} if {@link #ontology} entails {@code c ≡ d}, else
	 *         {@code false}
	 */
	public boolean checkIfEquivalentClass(OWLClassExpression c, OWLClassExpression d) {
		return reasoner.isEntailed(factory.getOWLEquivalentClassesAxiom(c, d));
	}

	/**
	 * Find every non-negative restriction (existential and universal) that can
	 * appear (as part of some concept) on the right-hand side of a subsumption
	 * axiom (thus relating some concept to another one) and create a
	 * {@link ConceptNode} for it
	 * 
	 * @return A {@link RestrictionNodeCollection} containing every restriction that
	 *         may occur on the right-hand side of a subsumption axiom
	 */
	public RestrictionNodeCollection getPotentialRightSideRestrictionNodes() {
		RestrictionNodeCollection rest_col = new RestrictionNodeCollection(this);
		// get restrictions from subclass axioms
		Iterator<RestrictionNodeCollection> rest_cols_iterator = ontology.axioms(AxiomType.SUBCLASS_OF)
				.map(ax -> getRestrictionNodesFromSubsumption(ax)).iterator();
		// combine found restrictions to one collection
		while (rest_cols_iterator.hasNext()) {
			rest_col.mergeWith(rest_cols_iterator.next());
		}
		// get restrictions from equivalence axioms (since equivalence also represents
		// subsumption)
		rest_cols_iterator = ontology.axioms(AxiomType.EQUIVALENT_CLASSES)
				.map(ax -> getRestrictionNodesFromEquivalence(ax)).iterator();
		// combine found restrictions to one collection
		while (rest_cols_iterator.hasNext()) {
			rest_col.mergeWith(rest_cols_iterator.next());
		}

		return rest_col;

	}

	/**
	 * Find every non-negative restriction (existential and universal) that appears
	 * in the equivalence axiom {@code ax} and create a {@link ConceptNode} for it
	 * 
	 * @param ax An {@link OWLEquivalentClassesAxiom} object
	 * @return A {@link RestrictionNodeCollection} containing every restriction
	 *         found in the equivalence axiom
	 */
	private RestrictionNodeCollection getRestrictionNodesFromEquivalence(OWLEquivalentClassesAxiom ax) {
		RestrictionNodeCollection rest_col = new RestrictionNodeCollection(this);
		// represent equivalence in form of two subsumption axioms
		Iterator<OWLSubClassOfAxiom> subClassIter = ax.asOWLSubClassOfAxioms().iterator();
		// get restrictions from each subsumption
		while (subClassIter.hasNext()) {
			rest_col.mergeWith(getRestrictionNodesFromSubsumption(subClassIter.next()));
		}
		return rest_col;
	}

	/**
	 * Find every non-negative restriction (existential and universal) that may
	 * appear on the right-hand side of a subsumption based on the subsumption axiom
	 * {@code ax} and create a {@link ConceptNode} for it
	 * 
	 * @param ax An {@link OWLSubClassOfAxiom} object
	 * @return A {@link RestrictionNodeCollection} containing every restriction that
	 *         may occur on the right-hand side of the subsumption axiom
	 */
	private RestrictionNodeCollection getRestrictionNodesFromSubsumption(OWLSubClassOfAxiom ax) {
		// collect restrictions from subclass
		RestrictionNodeCollection rest_col = ax.getSubClass().accept(new NegativeRestrictionNodeCollector(this));
		if (rest_col == null) {
			rest_col = new RestrictionNodeCollection(this);
		}
		// collect restrictions from superclass
		rest_col.mergeWith(ax.getSuperClass().accept(new PositiveRestrictionNodeCollector(this)));
		return rest_col;
	}

	/**
	 * Get all individuals occurring in {@link #ontology}
	 * 
	 * @return A {@link Set} of {@link OWLIndividual} objects
	 */
	public Set<OWLIndividual> getIndividuals() {
		Set<OWLIndividual> inds = new HashSet<>();
		ontology.individualsInSignature().forEach(i -> inds.add(i));
		return inds;
	}

	/**
	 * For each individual {@code i}, get all the most specific concepts {@code C}
	 * from the ontology that satisfy {@code C(i)}, based on available concept and
	 * role assertions
	 * 
	 * @param individuals      A {@link Set} of {@link OWLIndividual} instances
	 * @param univ_rests_nodes A {@link HashMap} relating an {@link OWLProperty} to
	 *                         a {@link ConceptNodeSet} for
	 *                         {@link OWLClassExpression} representing universal
	 *                         restrictions that share the same role and are sorted
	 *                         in a hierarchy based on subsumption
	 * @return A {@link HashMap} relating an {@link OWLIndividual} to a {@link Set}
	 *         of {@link OWLClassExpression} elements representing most specific
	 *         concepts
	 */
	public HashMap<OWLIndividual, Set<OWLClassExpression>> getMostSpecificConceptsForIndividuals(
			Set<OWLIndividual> individuals, HashMap<OWLProperty, ConceptNodeSet<OWLClassExpression>> univ_rests_nodes) {
		// hash map for concepts referring to class assertions in ABox
		HashMap<OWLIndividual, Set<OWLClassExpression>> indToConcepts = new HashMap<>(individuals.size());
		// hash map for role assertions with individual as object (second argument)
		HashMap<OWLIndividual, Set<OWLObjectPropertyAssertionAxiom>> indToRoleAssertObj = new HashMap<>();
		// hash map for role assertions with individual as subject (first argument)
		HashMap<OWLIndividual, Set<OWLObjectPropertyAssertionAxiom>> indToRoleAssertSubj = new HashMap<>();
		// set for individuals for which concepts have already been found
		HashSet<OWLIndividual> finished_inds = new HashSet<>();

		for (OWLIndividual ind : individuals) {
			// find concepts from class assertions in ABox
			Set<OWLClassExpression> concepts = getMostSpecificABoxConcepts(ind);
			if (concepts.isEmpty()) {
				// individual is always part of top-concept
				concepts.add(factory.getOWLThing());
			}
			indToConcepts.put(ind, concepts);
			// note if role assertions found
			boolean no_role_assertions = true;
			// look for role assertions R(j,i) with individual i as object
			Set<OWLObjectPropertyAssertionAxiom> role_assertions = ontology.axioms(AxiomType.OBJECT_PROPERTY_ASSERTION)
					.filter(ax -> ax.getObject().equals(ind)).collect(Collectors.toSet());
			if (role_assertions != null) {
				indToRoleAssertObj.put(ind, role_assertions);
				no_role_assertions = false;
			}
			// look for role assertions R(i,j) with individual i as subject
			role_assertions = ontology.objectPropertyAssertionAxioms(ind).collect(Collectors.toSet());
			if (role_assertions != null) {
				indToRoleAssertSubj.put(ind, role_assertions);
				no_role_assertions = false;
			}
			// if no role assertions given, search for concepts is already done
			if (no_role_assertions) {
				finished_inds.add(ind);
			}
		}

		/*
		 * get concepts based on individual i's occurrence as object of role assertions
		 * R(j,i)
		 */
		for (OWLIndividual i : indToRoleAssertObj.keySet()) {
			// set to store already processed roles of role assertions
			Set<OWLProperty> used_roles = new HashSet<>();
			// go through role assertions R(j,i)
			for (OWLObjectPropertyAssertionAxiom rs : indToRoleAssertObj.get(i)) {
				// get role R
				OWLObjectProperty role = rs.getProperty().asOWLObjectProperty();
				// consider universal restrictions using role
				if (!used_roles.contains(role)) {
					used_roles.add(role);
					Set<ConceptNode<OWLClassExpression>> nodes = univ_rests_nodes.get(role);
					if (nodes != null) {
						// add most specific filler concepts to collected most specific concepts of i
						Set<OWLClassExpression> concepts = indToConcepts.get(i);
						concepts.addAll(getMostSpecificConsforInd(rs.getObject(), nodes));
						indToConcepts.put(i, concepts);
					}
				}
			}
			// if i does not occur as a subject of a role assertions, its processing is done
			if (!indToRoleAssertSubj.containsKey(i)) {
				finished_inds.add(i);
			}
		}

		/*
		 * repeatedly try to get concepts based on individual i's occurrence as subject
		 * of role assertions R(i,j) with updated, finished individuals j
		 */
		Set<OWLIndividual> remaining_inds = new HashSet<>(indToRoleAssertSubj.keySet());
		// state if some adaptations to a set of most specific concepts happened
		boolean adapted = false;
		do {
			adapted = false;
			for (OWLIndividual i : new HashSet<>(remaining_inds)) {
				// get role assertions R(i,j)
				Set<OWLObjectPropertyAssertionAxiom> role_assertions = new HashSet<>(indToRoleAssertSubj.get(i));
				// get current most specific concepts
				Set<OWLClassExpression> most_spec_concepts = new HashSet<>(indToConcepts.get(i));

				for (OWLObjectPropertyAssertionAxiom rs : indToRoleAssertSubj.get(i)) {
					// get related individual j
					OWLIndividual j = rs.getObject();
					// only consider individuals j for which most specific concepts already
					// determined
					if (finished_inds.contains(j)) {
						role_assertions.remove(rs);
						// get role R
						OWLObjectProperty role = rs.getProperty().asOWLObjectProperty();
						// create existential restriction ∃R.G with G as conjunction of most specific
						// concepts of j
						OWLObjectSomeValuesFrom ex_rest = factory.getOWLObjectSomeValuesFrom(role,
								createConjunction(indToConcepts.get(j)));
						// check if ∃R.G is most specific
						boolean is_candidate = true;
						Iterator<OWLClassExpression> con_iter = most_spec_concepts.iterator();
						while (is_candidate && con_iter.hasNext()) {
							if (checkIfSubClass(con_iter.next(), ex_rest)) {
								// restriction not most specific
								is_candidate = false;
							}
						}
						if (is_candidate) {
							// reset iterator
							con_iter = new HashSet<>(indToConcepts.get(i)).iterator();
							// check if ∃R.G is more specific than other concepts
							while (con_iter.hasNext()) {
								OWLClassExpression d = con_iter.next();
								if (checkIfSubClass(ex_rest, d)) {
									most_spec_concepts.remove(d);
								}
							}
							// update changes
							most_spec_concepts.add(ex_rest);
							indToConcepts.put(i, most_spec_concepts);
							adapted = true;
						}
					}
				}
				// check if every most specific concept determined for current individual i
				if (role_assertions.isEmpty()) {
					finished_inds.add(i);
					remaining_inds.remove(i);
					adapted = true;
				} else {
					// update changes
					indToRoleAssertSubj.put(i, role_assertions);
				}
			}

		} while (!remaining_inds.isEmpty() && adapted);

		// check if for some individuals, most specific concepts still not found
		if (!remaining_inds.isEmpty()) {
			/*
			 * remaining individuals appear in at least one loop of the form R1(i_1,i_2),
			 * R2(i_2,i_3), ... Rn(i_n,i_1), which means that we cannot further specify the
			 * restriction ∃R.G like above and have to explicitly search for suitable
			 * existential restrictions in the ontology that may be used as most specific
			 * concepts
			 */
			// look for existential restrictions that may appear on the left-hand side of a
			// subsumption axiom
			Set<OWLObjectSomeValuesFrom> ex_rest_candidates = getPotentialLeftSideExistRestrictions();

			for (OWLIndividual i : remaining_inds) {
				// find most specific ∃R.G with (∃R.G)(i)
				Set<OWLObjectSomeValuesFrom> most_spec_ex_rests = getMostSpecificExRestsForIndividual(i,
						ex_rest_candidates);
				Set<OWLProperty> used_roles = new HashSet<>();
				// add ∃R.T for each remaining R(i,j)
				for (OWLObjectPropertyAssertionAxiom rs : indToRoleAssertSubj.get(i)) {
					// get role R
					OWLObjectProperty role = rs.getProperty().asOWLObjectProperty();
					if (!used_roles.contains(role)) {
						used_roles.add(role);
						addConceptIfMostSpecific(most_spec_ex_rests,
								factory.getOWLObjectSomeValuesFrom(role, factory.getOWLThing()));
					}
				}

				// add to already found most specific concepts if possible
				Set<OWLClassExpression> most_spec_concepts = indToConcepts.get(i);
				for (OWLObjectSomeValuesFrom ex_rest : most_spec_ex_rests) {
					addConceptIfMostSpecific(most_spec_concepts, ex_rest);
				}
				indToConcepts.put(i, most_spec_concepts);
			}
		}

		return indToConcepts;
	}

	/**
	 * Go through a set of concepts and find the most specific ones {@code C} for
	 * which {@code C(ind)} holds
	 * 
	 * @param <C>  extends {@link OWLClassExpression}
	 * @param ind  An {@link OWLIndividual} instance
	 * @param cons A {@link Set} of {@link C} instances
	 * @return A {@link Set} of {@link C} elements
	 * 
	 */
	private <C extends OWLClassExpression> Set<C> getMostSpecificExRestsForIndividual(OWLIndividual ind, Set<C> cons) {
		Set<C> results = new HashSet<>();
		for (C con : cons) {
			// check if C(ind) holds for current C
			if (reasoner.isEntailed(factory.getOWLClassAssertionAxiom(con, ind))) {
				// add if most specific
				addConceptIfMostSpecific(results, con);
			}
		}

		return results;
	}

	/**
	 * Go through sorted nodes representing concepts {@code D} and select most
	 * specific ones for which {@code D(ind)} holds.
	 * 
	 * @param ind   An {@link OWLIndividual} instance
	 * @param nodes A {@link Set} of {@link ConceptNode} elements for
	 *              {@link OWLClassExpression} instances representing the top-nodes
	 *              of a sorted hierarchy
	 * @return A {@link Set} of {@link OWLClassExpression} elements
	 */
	public Set<OWLClassExpression> getMostSpecificConsforInd(OWLIndividual ind,
			Set<ConceptNode<OWLClassExpression>> nodes) {
		// sets to collect most specific related concepts
		Set<OWLClassExpression> concepts = new HashSet<>();

		for (ConceptNode<OWLClassExpression> node : nodes) {
			// get concept of node
			OWLClassExpression con = node.getConcept();
			// check if D(ind) holds for ∀R.D of current node
			if (reasoner.isEntailed(factory.getOWLClassAssertionAxiom(con, ind))) {
				addConceptIfMostSpecific(concepts, con);
			}
			// consider sub-nodes
			concepts.addAll(getMostSpecificConsforInd(ind, node.subs));
		}
		return concepts;
	}

	/**
	 * Get every existential restriction that may appear positively on the left-hand
	 * side of a subsumption axiom
	 * 
	 * @return A {@link Set} of {@link OWLObjectSomeValuesFrom} instances
	 */
	public Set<OWLObjectSomeValuesFrom> getPotentialLeftSideExistRestrictions() {
		Set<OWLObjectSomeValuesFrom> rest_set = new HashSet<>();
		// get restrictions from subclass axioms
		Iterator<Set<OWLObjectSomeValuesFrom>> rest_set_iterator = ontology.axioms(AxiomType.SUBCLASS_OF)
				.map(ax -> getExistRestrictionsFromSubsumption(ax)).iterator();
		// combine found restrictions to one collection
		while (rest_set_iterator.hasNext()) {
			rest_set.addAll(rest_set_iterator.next());
		}
		// get restrictions from equivalence axioms (since equivalence also represents
		// subsumption)
		rest_set_iterator = ontology.axioms(AxiomType.EQUIVALENT_CLASSES)
				.map(ax -> getExistRestrictionsFromEquivalence(ax)).iterator();
		// combine found restrictions to one collection
		while (rest_set_iterator.hasNext()) {
			rest_set.addAll(rest_set_iterator.next());
		}

		return rest_set;
	}

	/**
	 * Get every existential restriction (including nested ones and negated normal
	 * form of universal restrictions) from an equivalence axiom
	 * 
	 * @param ax An {@link OWLEquivalentClassesAxiom}
	 * @return A {@link Set} of {@link OWLObjectSomeValuesFrom} instances
	 */
	private Set<OWLObjectSomeValuesFrom> getExistRestrictionsFromEquivalence(OWLEquivalentClassesAxiom ax) {
		Set<OWLObjectSomeValuesFrom> rest_set = new HashSet<>();
		ax.asOWLSubClassOfAxioms().forEach(sub_ax -> rest_set.addAll(getExistRestrictionsFromSubsumption(sub_ax)));
		return rest_set;
	}

	/**
	 * Get every existential restriction (including nested ones and negated normal
	 * form of universal restrictions) that may appear on the left-hand-side of a
	 * subsumption axiom based on a given subsumption axiom {@code ax}
	 * 
	 * @param ax An {@link OWLSubClassOfAxiom}
	 * @return A {@link Set} of {@link OWLObjectSomeValuesFrom} instances
	 */
	private Set<OWLObjectSomeValuesFrom> getExistRestrictionsFromSubsumption(OWLSubClassOfAxiom ax) {
		// get positive left-hand side existential restrictions
		Set<OWLObjectSomeValuesFrom> rest_set = ax.getSubClass().accept(new PositiveExistRestrictionCollector());
		if (rest_set == null) {
			rest_set = new HashSet<>();
		}
		// get negated normal form of right-hand side universal restrictions
		Set<OWLObjectSomeValuesFrom> super_rests = ax.getSuperClass().accept(new NegativeExistRestrictionCollector());
		if (super_rests != null) {
			rest_set.addAll(super_rests);
		}
		return rest_set;
	}

	/**
	 * For a given individual {@code ind}, get every concept {@code C} for which
	 * {@code C(ind)} appears in the ontology's ABox and there does not occur any
	 * other concept {@code D} with {@code D(ind)} in the ABox for which
	 * {@code D ⊑ C} holds
	 * 
	 * @param ind An {@link OWLIndividual} object
	 * @return A {@link Set} of {@link OWLClassExpression} representing the most
	 *         specific concepts {@code C} with {@code C(ind)} in ABox
	 */
	public Set<OWLClassExpression> getMostSpecificABoxConcepts(OWLIndividual ind) {
		Set<OWLClassExpression> mostSpecConcepts = new HashSet<>();
		ontology.classAssertionAxioms(ind)
				.forEach(as -> addConceptIfMostSpecific(mostSpecConcepts, as.getClassExpression()));
		return mostSpecConcepts;
	}

	/**
	 * Add a new concept {@code C} to a set already consisting of most specific
	 * concepts if there is no concept {@code D} in the set for which {@code D ⊑ C}
	 * holds. Furthermore, if {@code C} is added, remove all elements {@code D} from
	 * the set for which {@code C ⊑ D} holds such that only the most specific
	 * concepts remain.
	 * 
	 * @param <T>    type of considered concepts
	 * @param conSet A {@link Set} of {@link OWLClassExpression} objects
	 *               representing most specific concepts
	 * @param newCon An {@link OWLClassExpression} object representing the concept
	 *               to be added
	 */
	public <T extends OWLClassExpression> void addConceptIfMostSpecific(Set<T> conSet, T newCon) {
		if (newCon != null) {
			boolean no_sub_found = true;
			boolean no_super_found = true;
			for (OWLClassExpression setCon : new HashSet<>(conSet)) {
				// D ⊑ C
				if (no_super_found && checkIfSubClass(setCon, newCon)) {
					// new concept is not most specific -> must not be added
					no_sub_found = false;
					break;
				} // C ⊑ D
				else if (checkIfSubClass(newCon, setCon)) {
					// new concept is most specific because it cannot be a superclass of the other
					// concepts due to being a subclass for a previously most specific concept
					conSet.remove(setCon);
					no_super_found = false;
					// do not stop process in order to remove other subclasses if present
				}
			}
			if (no_sub_found) {
				conSet.add(newCon);
			}
		}
	}

	/**
	 * Create a conjunction out of a set of concepts.
	 * 
	 * @param concept_set A non-empty {@link Set} of {@link OWLClassExpression}
	 *                    elements
	 * @return A {@link OWLObjectIntersectionOf} instance with elements from
	 *         {@code concept_set} as operands
	 */
	public OWLClassExpression createConjunction(Set<? extends OWLClassExpression> concept_set) {
		if (concept_set.size() == 1) {
			return concept_set.iterator().next();
		}
		return factory.getOWLObjectIntersectionOf(concept_set);
	}

	/**
	 * Create an universal restriction with a given role using a conjunction of
	 * provided concepts as filler
	 * 
	 * @param role    A {@link OWLObjectProperty} instance that appears in every
	 *                element from {@code rests}
	 * @param fillers A non-empty {@link Set} of {@link OWLClassExpression} elements
	 * @return One {@link OWLObjectAllValuesFrom} object using {@code role} as role
	 *         and whose filler constitutes the conjunction of the concepts from
	 *         {@code fillers}
	 */
	public OWLObjectAllValuesFrom createUnivRestriction(OWLObjectProperty role, Set<OWLClassExpression> fillers) {
		return factory.getOWLObjectAllValuesFrom(role, createConjunction(fillers));
	}

	/**
	 * Combine an existential restriction {@code ∃R.D} with an universal restriction
	 * {@code ∀R.E} that serves as constraint for the related concept {@code D},
	 * thus resulting in {@code ∃R.G} with {@code G} as conjunction of the most
	 * specific concepts represented by {@code D} and {@code E}
	 * 
	 * @param ex_rest  An {@link OWLObjectSomeValuesFrom} instance
	 * @param role_con An {@link OWLObjectAllValuesFrom} instance with the same role
	 *                 like {@code ex_rest}
	 * @return A {@link OWLObjectSomeValuesFrom} instance
	 */
	public OWLObjectSomeValuesFrom combineToExistRestriction(OWLObjectSomeValuesFrom ex_rest,
			OWLObjectAllValuesFrom role_con) {
		if (role_con == null) {
			return factory.getOWLObjectSomeValuesFrom(ex_rest.getProperty(),
					getMostSpecificConjunction(ex_rest.getFiller(), null));
		} else {
			return factory.getOWLObjectSomeValuesFrom(ex_rest.getProperty(),
					getMostSpecificConjunction(ex_rest.getFiller(), role_con.getFiller()));
		}
	}

	/**
	 * Create a conjunction that only contains the most specific concepts among the
	 * ones provided by two given (complex) concepts {@code C} and {@code D} where
	 * if {@code C} and/or {@code D} are conjunctions, all of their conjuncts are
	 * considered as separate concepts
	 * 
	 * @param c An {@link OWLClassExpression} instance
	 * @param d An {@link OWLClassExpression} instance
	 * @return An {@link OWLObjectIntersectionOf} instance
	 */
	public OWLObjectIntersectionOf getMostSpecificConjunction(OWLClassExpression c, OWLClassExpression d) {
		boolean c_is_conjunction = c instanceof OWLObjectIntersectionOf;
		boolean d_is_conjunction = d instanceof OWLObjectIntersectionOf;
		Set<OWLClassExpression> conjuncts = new HashSet<>();

		if (c_is_conjunction && d_is_conjunction) {
			// get set of most specific conjuncts from C
			for (OWLClassExpression con : c.asConjunctSet()) {
				addConceptIfMostSpecific(conjuncts, con);
			}
			// consider conjuncts from D
			for (OWLClassExpression con : d.asConjunctSet()) {
				addConceptIfMostSpecific(conjuncts, con);
			}
		} else if (c_is_conjunction) {
			for (OWLClassExpression con : c.asConjunctSet()) {
				addConceptIfMostSpecific(conjuncts, con);
			}
			addConceptIfMostSpecific(conjuncts, d);
		} else if (d_is_conjunction) {
			for (OWLClassExpression con : d.asConjunctSet()) {
				addConceptIfMostSpecific(conjuncts, con);
			}
			addConceptIfMostSpecific(conjuncts, c);
		} else {
			conjuncts.add(c);
			if (d != null) {
				conjuncts.add(d);
			}
		}

		return factory.getOWLObjectIntersectionOf(conjuncts);
	}

	/**
	 * For a given existential restriction {@code ∃R.D}, check if a provided set
	 * contains another existential restriction {@code ∃R.F} with the same role
	 * {@code R} and for which {@code D ≡ F} holds (w.r.t. to the considered
	 * ontology)
	 * 
	 * @param ex_rest An {@link OWLObjectSomeValuesFrom} instance
	 * @param restSet A {@link Set} of {@link OWLObjectSomeValuesFrom} elements
	 * @return {@code true} if {@code restSet} does not contain an element
	 *         satisfying the above conditions, else {@code false}
	 */
	public boolean checkIfNoEquivalent(OWLObjectSomeValuesFrom ex_rest, Set<OWLObjectSomeValuesFrom> restSet) {
		OWLObjectProperty role = ex_rest.getProperty().asOWLObjectProperty();
		OWLClassExpression filler = ex_rest.getFiller();

		for (OWLObjectSomeValuesFrom rest : restSet) {
			// check if same role
			if (role.equals(rest.getProperty())) {
				// check if related filler concepts are equivalent w.r.t. ontology
				if (reasoner.isEntailed(factory.getOWLEquivalentClassesAxiom(filler, rest.getFiller()))) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Check if there exists a role assertion {@code R(a,b)} in the ontology's ABox
	 * for given role {@code R} and individual {@code a} such that the related
	 * individual {@code b} is an instance of another given concept {@code C}, i.e.,
	 * for which {@code C(b)} holds (w.r.t. the considered ontology)
	 * 
	 * @param role      An {@link OWLObjectProperty} object
	 * @param a         An {@link OWLIndividual} object
	 * @param b_concept An {@link OWLClassExpression} object
	 * @return {@code true} if suitable role assertion can be found, else
	 *         {@code false}
	 */
	public boolean checkIfRoleAssertionPresent(OWLObjectProperty role, OWLIndividual a, OWLClassExpression b_concept) {
		// get role assertions R(a,b) for given individual a and role R
		for (OWLObjectPropertyAssertionAxiom ax : this.ontology.objectPropertyAssertionAxioms(a)
				.filter(ax -> ax.getProperty().equals(role)).collect(Collectors.toList())) {
			// check if C(b) holds w.r.t. ontology for b_concept C
			if (checkClassAssertion(b_concept, ax.getObject())) {
				return true;
			}
		}
		// no appropriate role assertion found
		return false;
	}

	/**
	 * Check if a given individual {@code a} is an instance of a given concept
	 * {@code C}, i.e., {@code C(a)} holds (w.r.t. the considered ontology)
	 * 
	 * @param concept An {@link OWLClassExpression} object
	 * @param ind     An {@link OWLIndividual} object
	 * @return {@code true} if class assertion entailed by ontology, otherwise
	 *         {@code false}
	 */
	public boolean checkClassAssertion(OWLClassExpression concept, OWLIndividual ind) {
		return reasoner.isEntailed(factory.getOWLClassAssertionAxiom(concept, ind));
	}

	/**
	 * Get every role assertion {@code R(a,b)} for a given individual {@code a}
	 * 
	 * @param ind An {@link OWLIndividual}
	 * @return A {@link Set} of {@link OWLObjectPropertyAssertionAxiom} objects that
	 *         use {@code ind} as subject
	 */
	public Set<OWLObjectPropertyAssertionAxiom> getRoleAssertions(OWLIndividual ind) {
		return new HashSet<OWLObjectPropertyAssertionAxiom>(
				ontology.objectPropertyAssertionAxioms(ind).collect(Collectors.toSet()));
	}

	/**
	 * Add a new existential restriction {@code C} to a set already consisting of
	 * minimal existential restrictions and only keep the members that are minimal
	 * w.r.t. role-concept-subsumption. If the considered concepts are not
	 * existential restrictions, the new concept is just added to the collection.
	 * 
	 * @param <C>     the considered concept type (usually
	 *                {@link OWLObjectSomeValuesFrom}
	 * @param list    A {@link Collection} of concepts usually representing minimal
	 *                existential restrictions
	 * @param new_con A concept of type {@code C} representing the concept (usually
	 *                existential restriction) to be added
	 * @return {@code true} if {@code new_con} was added to list, else {@code false}
	 */
	public <C extends OWLClassExpression> boolean addExRestIfMinimal(Collection<C> list, C new_con) {
		if (new_con != null) {
			boolean no_sub_found = true;
			if (new_con instanceof OWLObjectSomeValuesFrom) {
				boolean no_super_found = true;
				for (C setCon : new HashSet<>(list)) {
					// D is role-concept-subsumed by C
					if (no_super_found && checkRoleConceptSubsumption(setCon, new_con)) {
						// new concept is not minimal -> must not be added
						no_sub_found = false;
						break;
					} // C is role-concept-subsumed by D
					else if (checkRoleConceptSubsumption(new_con, setCon)) {
						// new concept is minimal because it cannot role-concept-subsume another
						// restriction due to being role-concept-subsumed by a previously minimal
						// concept
						list.remove(setCon);
						no_super_found = false;
						// do not stop process in order to remove other non-minimal elements if present
					}
				}
			}
			if (no_sub_found) {
				list.add(new_con);
			}
			return no_sub_found;
		}
		return false;
	}

	/**
	 * Check if an existential restriction {@code a = ∃R.C} is role-concept-subsumed
	 * by another one {@code b = ∃S.D}, i.e., they share the same role
	 * ({@code R = S}) and the related concept {@code C} is subsumed by the other
	 * related concept {@code D} ({@code C ⊑ D})
	 * 
	 * @param a An {@link OWLObjectSomeValuesFrom} instance
	 * @param b An {@link OWLObjectSomeValuesFrom} instance
	 * @return {@code true} if {@code a} is role-concept-subsumed by {@code b} else
	 *         {@code false}
	 */
	public <C extends OWLClassExpression> boolean checkRoleConceptSubsumption(C a, C b) {
		if (a instanceof OWLObjectSomeValuesFrom) {

			return ((OWLObjectSomeValuesFrom) b).getProperty().asOWLObjectProperty()
					.equals(((OWLObjectSomeValuesFrom) a).getProperty().asOWLObjectProperty())
					&& checkIfSubClass(((OWLObjectSomeValuesFrom) a).getFiller(),
							((OWLObjectSomeValuesFrom) b).getFiller());

		}
		return false;

	}

	/**
	 * Add the concept {@code new_con} to a list of already equivalent concepts if
	 * it is equivalent to the latter's elements
	 * 
	 * @param <C>      considered concept type
	 * @param new_con  The concept to be added
	 * @param concepts A {@link List} of equivalent concepts
	 * @return {@code true} if the concept has been added to the list, else
	 *         {@code false}
	 */
	public <C extends OWLClassExpression> boolean addIfEquivalent(C new_con, List<C> concepts) {
		boolean add = false;
		// check if equivalent concept given
		add = checkIfEquivalentClass(concepts.get(0), new_con);
		if (add) {
			if (new_con instanceof OWLObjectSomeValuesFrom) {
				add = addExRestIfMinimal(concepts, new_con);
			} else {
				concepts.add(new_con);
			}
		}
		return add;
	}

}
