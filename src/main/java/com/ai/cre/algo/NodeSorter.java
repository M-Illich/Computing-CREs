package com.ai.cre.algo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.ai.cre.ontology.OntologyHandler;
import com.ai.cre.representation.ConceptNode;
import com.ai.cre.representation.ConceptNodeSet;

import uk.ac.manchester.cs.jfact.JFactFactory;

/**
 * Providing methods to create a sorted subsumption hierarchy for concepts
 * represented by {@link ConceptNode} instances
 *
 */
public class NodeSorter {

	static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	static OWLDataFactory factory = manager.getOWLDataFactory();

	/**
	 * Sort a set of concepts given as nodes w.r.t. their subsumption relations in a
	 * given ontology, such that a sub-concept is placed underneath its
	 * super-concept and equivalent concepts are merged into one node
	 * 
	 * @param <C>         A subclass of {@link OWLClassExpression} defining the type
	 *                    of concepts represented by the nodes
	 * @param ontoHandler An {@link OntologyHandler} instance for a (Horn-ALC)
	 *                    ontology that contains the concepts represented by the
	 *                    related nodes
	 * @param nodes       A {@link Set} of {@link ConceptNode} elements representing
	 *                    concepts of type {@code C} that shall be sorted
	 * @return A {@link Set} of {@link ConceptNode} objects that represent the top
	 *         nodes in the created subsumption order, i.e., they do not have any
	 *         super-concepts among the other nodes
	 */
	public static <C extends OWLClassExpression> Set<ConceptNode<C>> sortBySubsumption(OntologyHandler ontoHandler,
			Set<ConceptNode<C>> nodes) {
		if (nodes.size() == 1) {
			return nodes;
		} else if (nodes.size() < 100) {
			return sortBySubsumptionUsingEnhancedTraversal(ontoHandler, nodes);
		} else {
			return sortBySubsumptionUsingClassification(ontoHandler, nodes);
		}
	}

	/**
	 * Apply the Enhanced Traversal Method to sort a set of concepts given as nodes
	 * w.r.t. their subsumption relations in a given ontology
	 * 
	 * @param <C>         A subclass of {@link OWLClassExpression} defining the type
	 *                    of concepts represented by the nodes
	 * @param ontoHandler An {@link OntologyHandler} instance for a (Horn-ALC)
	 *                    ontology that contains the concepts represented by the
	 *                    related nodes
	 * @param nodes       A {@link Set} of {@link ConceptNode} elements representing
	 *                    concepts of type {@code C} that shall be sorted
	 * @return A {@link Set} of {@link ConceptNode} objects that represent the top
	 *         nodes in the created subsumption order, i.e., they do not have any
	 *         super-concepts among the other nodes
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <C extends OWLClassExpression> Set<ConceptNode<C>> sortBySubsumptionUsingEnhancedTraversal(
			OntologyHandler ontoHandler, Set<ConceptNode<C>> nodes) {

		ConceptNodeSet<C> sorted_nodes = new ConceptNodeSet<>();
		// TOP and BOTTOM concepts as entry points for hierarchy
		ConceptNode<C> top = new ConceptNode(factory.getOWLThing());
		ConceptNode<C> bottom = new ConceptNode(factory.getOWLNothing());

		Iterator<ConceptNode<C>> iter = nodes.iterator();
		if (iter.hasNext()) {
			// initialize hierarchy with first node placed between TOP and BOTTOM
			ConceptNode<C> node = iter.next();
			top.addRelationToSub(node);
			node.addRelationToSub(bottom);
			// insert remaining nodes
			while (iter.hasNext()) {
				node = iter.next();
				addFromTop(ontoHandler, top, node, new ConceptNodeSet<C>());
				addFromBottom(ontoHandler, bottom, node, new ConceptNodeSet<C>());
			}
			// remove bottom-concept
			for (ConceptNode<C> low_node : bottom.supers) {
				low_node.subs = new ConceptNodeSet<>();
			}
			// top-concept not needed
			sorted_nodes.addAll(top.subs);
		}
		return sorted_nodes;
	}

	/**
	 * Add a newly introduced concept to a subsumption hierarchy by recursively
	 * looking for its super-concepts traversing from top to bottom
	 * 
	 * @param <C>          concept type occurring in sorted nodes
	 * @param ontoHandler  A {@link OntologyHandler} to check subsumption relations
	 * @param current_node A {@link ConceptNode} that is a super-concept of
	 *                     {@code new_node} and whose sub-nodes are considered as
	 *                     further super-concept candidates
	 * @param new_node     A {@link ConceptNode} being added to the subsumption
	 *                     hierarchy
	 * @param visited      A {@link ConceptNodeSet} containing the already visited
	 *                     nodes to avoid repeated processing
	 */
	private static <C extends OWLClassExpression> void addFromTop(OntologyHandler ontoHandler,
			ConceptNode<C> current_node, ConceptNode<C> new_node, ConceptNodeSet<C> visited) {
		if (!current_node.getConcept().isOWLNothing()) {
			visited.add(current_node);
			boolean no_super_found = true;
			// consider sub-nodes
			for (ConceptNode<C> sub_node : current_node.subs) {
				if (visited.contains(sub_node)) {
					// visited means that sub_node has already been determined as super-concept
					no_super_found = false;
				} else if (ontoHandler.checkIfSubClass(new_node.getConcept(), sub_node.getConcept())) {
					no_super_found = false;
					// go deeper in hierarchy to find direct super-concept
					addFromTop(ontoHandler, sub_node, new_node, visited);
				}
			}
			if (no_super_found) {
				current_node.addRelationToSub(new_node);
			}
		}

	}

	/**
	 * Add a newly introduced concept to a subsumption hierarchy by recursively
	 * looking for its sub-concepts traversing from bottom to top
	 * 
	 * @param <C>          concept type occurring in sorted nodes
	 * @param ontoHandler  A {@link OntologyHandler} to check subsumption relations
	 * @param current_node A {@link ConceptNode} that is a sub-concept of
	 *                     {@code new_node} and whose super-nodes are considered as
	 *                     further sub-concept candidates
	 * @param new_node     A {@link ConceptNode} being added to the subsumption
	 *                     hierarchy
	 * @param visited      A {@link ConceptNodeSet} containing the already visited
	 *                     nodes to avoid repeated processing
	 */
	private static <C extends OWLClassExpression> void addFromBottom(OntologyHandler ontoHandler,
			ConceptNode<C> current_node, ConceptNode<C> new_node, ConceptNodeSet<C> visited) {

		if (!current_node.getConcept().isOWLThing() && !new_node.concepts.isEmpty()) {
			visited.add(current_node);
			boolean no_sub_found = true;
			// get shared super-nodes
			Set<ConceptNode<C>> sharedSuperNodes = current_node.supers.stream().filter(n -> new_node.supers.contains(n))
					.collect(Collectors.toSet());

			for (ConceptNode<C> super_node : current_node.supers) {
				if (new_node.getConcept() == null) {
					break;
				}
				if (visited.contains(super_node)) {
					no_sub_found = false;
				} else if (ontoHandler.checkIfSubClass(super_node.getConcept(), new_node.getConcept())) {
					no_sub_found = false;
					// check if nodes are equivalent,
					// i.e., sub-concept and super-concept at the same time
					if (sharedSuperNodes.contains(super_node)) {
						// for existential restrictions, only keep minimal ones w.r.t.
						// role-concept-subsumption
						if (new_node.getConcept() instanceof OWLObjectSomeValuesFrom) {
							// merge equivalent nodes only keeping minimal concepts
							ontoHandler.addExRestIfMinimal(super_node.concepts, new_node.getConcept());

							// remove connections of new node
							for (ConceptNode<C> sp : new_node.supers) {
								sp.subs.remove(new_node);
							}
							for (ConceptNode<C> sb : new_node.subs) {
								sb.supers.remove(new_node);
							}
							// remove concepts of new node to prevent further processing
							new_node.concepts.clear();
							break;
						}

					} else {
						// check next super-nodes
						addFromBottom(ontoHandler, super_node, new_node, visited);
					}
				}
			}

			if (no_sub_found) {
				// only keep direct connections to super-concepts
				for (ConceptNode<C> n : sharedSuperNodes) {
					current_node.supers.remove(n);
					n.subs.remove(current_node);
				}
				// create subsumption relation
				new_node.addRelationToSub(current_node);
			}
		}
	}

	/**
	 * Use classification performed by a reasoner (HermiT for sets < 1000, else
	 * JFact) in order to sort a set of concepts given as nodes w.r.t. their
	 * subsumption relations in a given ontology
	 * 
	 * @param <C>         A subclass of {@link OWLClassExpression} defining the type
	 *                    of concepts represented by the nodes
	 * @param ontoHandler An {@link OntologyHandler} instance for an (Horn-ALC)
	 *                    ontology that contains the concepts represented by the
	 *                    related nodes
	 * @param nodes       A {@link Set} of {@link ConceptNode} elements representing
	 *                    concepts of type {@code C} that shall be sorted
	 * @return A {@link Set} of {@link ConceptNode} objects that represent the top
	 *         nodes in the created subsumption order, i.e., they do not have any
	 *         super-concepts among the other nodes
	 */
	public static <C extends OWLClassExpression> Set<ConceptNode<C>> sortBySubsumptionUsingClassification(
			OntologyHandler ontoHandler, Set<ConceptNode<C>> nodes) {
		// for smaller ontologies, HermiT reasoner is faster but for some,
		// especially larger ontologies, JFact is quite a lot faster
		boolean useHermit = true;
		if (nodes.size() > 1000) {
			useHermit = false;
		}
		return sortBySubsumptionUsingClassification(ontoHandler, nodes, useHermit);
	}

	/**
	 * Use classification performed by a reasoner in order to sort a set of concepts
	 * given as nodes w.r.t. their subsumption relations in a given ontology
	 * 
	 * @param <C>         A subclass of {@link OWLClassExpression} defining the type
	 *                    of concepts represented by the nodes
	 * @param ontoHandler An {@link OntologyHandler} instance for an (Horn-ALC)
	 *                    ontology that contains the concepts represented by the
	 *                    related nodes
	 * @param nodes       A {@link Set} of {@link ConceptNode} elements representing
	 *                    concepts of type {@code C} that shall be sorted
	 * @param useHermit   A {@code boolean} stating if Hermit or otherwise JFact
	 *                    shall be used as reasoner
	 * @return A {@link Set} of {@link ConceptNode} objects that represent the top
	 *         nodes in the created subsumption order, i.e., they do not have any
	 *         super-concepts among the other nodes
	 */
	public static <C extends OWLClassExpression> Set<ConceptNode<C>> sortBySubsumptionUsingClassification(
			OntologyHandler ontoHandler, Set<ConceptNode<C>> nodes, boolean useHermit) {
		Set<ConceptNode<C>> sorted_nodes = new ConceptNodeSet<>();
		try {
			// create copy of ontology
			OWLOntology new_ontology = manager.createOntology();
			manager.addAxioms(new_ontology, ontoHandler.ontology.axioms());

			// hash map to link new atoms to its associated node
			HashMap<String, ConceptNode<C>> atom_node_map = new HashMap<>();
			int count = 0;
			for (ConceptNode<C> n : nodes) {
				// create new atomic concept
				String atom_string = "ATOMIC" + count;
				OWLClass atom = factory.getOWLClass(atom_string);
				// check if concept name is already taken
				while (new_ontology.containsClassInSignature(atom.getIRI())) {
					atom_string = "ATOMIC-" + new Random().nextInt();
					atom = factory.getOWLClass(atom_string);
				}

				// add equivalent axiom for new atomic concept and node's concept
				manager.addAxiom(new_ontology, factory.getOWLEquivalentClassesAxiom(atom, n.getConcept()));
				// save connection between new atom and concept
				atom_node_map.put(atom_string, n);
				count++;
			}

			/*
			 * general idea: replace old atomic concepts by complex ones such that only new
			 * atoms are considered for classification
			 */
			// save extended ontology to file
			String saved_ontology_path = "extended_tmp.owl";
			IRI iri = IRI.create(new File(saved_ontology_path).getAbsoluteFile().toURI());
			manager.saveOntology(new_ontology, new FunctionalSyntaxDocumentFormat(), iri);

			// replace every old atomic concept A by a new existential restriction ∃A.(TOP)
			// such that A is not considered for classification
			HashMap<String, String> old_atom_string_map = new HashMap<>();
			for (OWLClass old_atom : ontoHandler.ontology.classesInSignature().collect(Collectors.toSet())) {
				String old_string = "<" + old_atom.getIRI().getIRIString() + ">";
				old_atom_string_map.put(old_string, "ObjectSomeValuesFrom(" + old_string + " owl:Thing)");
			}

			Path path = Paths.get(saved_ontology_path);
			List<String> old_lines = Files.readAllLines(path);
			List<String> new_lines = new LinkedList<>();
			for (String line : old_lines) {
				if (line.startsWith("Declaration(Class(<")) {
					String atom_string = line.substring(line.indexOf("<"), line.indexOf(">") + 1);
					String new_string = old_atom_string_map.get(atom_string);
					if (new_string != null) {
						// define old atom as new role
						line = line.replace("Declaration(Class(" + atom_string,
								"Declaration(ObjectProperty(" + atom_string);
					}
					new_lines.add(line);

				} else if (line.startsWith("Sub") || line.startsWith("Equ")) {
					// get all atomic concepts (and roles)
					Set<String> atomic_concepts = new HashSet<>();
					int start = line.indexOf("<");
					int end = line.indexOf(">", start) + 1;
					while (end < line.length() && start != -1) {
						atomic_concepts.add(line.substring(start, end));
						start = line.indexOf("<", end + 1);
						end = line.indexOf(">", start) + 1;
					}

					// replace old atomic concepts if present
					for (String atom : atomic_concepts) {
						String new_string = old_atom_string_map.get(atom);
						if (new_string != null) {
							line = line.replace(atom, new_string);
						}
					}
					new_lines.add(line);

				} else if (line.startsWith("Prefix") || line.startsWith("Ontology(")) {
					new_lines.add(line);
				}
			}
			// add concluding parenthesis
			new_lines.add(")");
			Files.write(path, new_lines);

			// select reasoner
			OWLReasoner reasoner;
			if (useHermit) {
				reasoner = new Reasoner(new Configuration(),
						manager.loadOntologyFromOntologyDocument(new File(saved_ontology_path)));

			} else {
				reasoner = new JFactFactory()
						.createReasoner(manager.loadOntologyFromOntologyDocument(new File(saved_ontology_path)));
			}
			// perform classification
			reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			sorted_nodes = getDirectSubNodes(factory.getOWLThing(), atom_node_map, reasoner, ontoHandler);

		} catch (Exception e) {
			e.printStackTrace();
		}

		if (sorted_nodes.isEmpty()) {
			return nodes;
		} else {
			return sorted_nodes;
		}
	}

	/**
	 * Get the {@link ConceptNode} objects linked by a map to the direct
	 * sub-concepts of a given atomic concept based on a subsumption hierarchy
	 * precomputed by a related reasoner
	 * 
	 * @param <C>           the concept type occurring in the {@link ConceptNode}
	 *                      elements of the hash map
	 * @param super_class   A {@link OWLClass} for which the sub-concepts are
	 *                      considered
	 * @param atom_node_map A {@link HashMap} connecting {@link String}
	 *                      representations of atomic concepts to
	 *                      {@link ConceptNode} objects
	 * @param reasoner      A {@link OWLReasoner} with a precomputed hierarchy of
	 *                      the atomic concepts used in {@code atom_node_map}
	 * @param ontoHandler   An {@link OntologyHandler} instance for an (Horn-ALC)
	 *                      ontology that contains the concepts represented by the
	 *                      related nodes
	 * @return A {@link ConceptNodeSet} containing the nodes mapped to the
	 *         sub-concepts of {@code super_class}
	 */
	private static <C extends OWLClassExpression> ConceptNodeSet<C> getDirectSubNodes(OWLClass super_class,
			HashMap<String, ConceptNode<C>> atom_node_map, OWLReasoner reasoner, OntologyHandler ontoHandler) {
		ConceptNodeSet<C> sub_nodes = new ConceptNodeSet<>();
		if (!super_class.isOWLNothing()) {
			// consider each direct subclass of current atom super_class
			for (Node<OWLClass> ordered_node : reasoner.getSubClasses(super_class, true)) {

				// get atomic concept (and equivalent ones if available) from current sub-node
				List<OWLClass> equivalent_atoms = ordered_node.entities().collect(Collectors.toList());
				// get related ConceptNode element for current atomic concept (and equivalent
				// ones if available)
				List<ConceptNode<C>> equivalent_conceptNodes = equivalent_atoms.stream()
						.map(atom -> atom_node_map.get(atom.getIRI().getRemainder().get())).filter(a -> a != null)
						.collect(Collectors.toList());

				if (!equivalent_conceptNodes.isEmpty()) {
					// merge equivalent concepts into one node
					ConceptNode<C> node = equivalent_conceptNodes.get(0);
					for (ConceptNode<C> equiv_node : equivalent_conceptNodes) {
						ontoHandler.addExRestIfMinimal(node.concepts, equiv_node.getConcept());
					}
					// get sub-nodes
					node.subs.addAll(getDirectSubNodes(equivalent_atoms.get(0), atom_node_map, reasoner, ontoHandler));

					// add node to returned sub-nodes
					sub_nodes.add(node);
				}
			}
		}
		return sub_nodes;
	}

}