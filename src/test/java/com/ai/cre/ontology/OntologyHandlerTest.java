package com.ai.cre.ontology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;

import com.ai.cre.ontology.OntologyHandler;
import com.ai.cre.ontology.RestrictionNodeCollection;
import com.ai.cre.representation.ConceptNode;
import com.ai.cre.representation.ConceptNodeSet;

public class OntologyHandlerTest {
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLDataFactory factory = manager.getOWLDataFactory();
	File of = new File("resources/test/test_ontology.owl");
	OntologyHandler ontoHandler = new OntologyHandler(of);
	// primitive concepts in ontology
	OWLClass a = factory.getOWLClass("A");
	OWLClass b = factory.getOWLClass("B");
	OWLClass c = factory.getOWLClass("C");
	OWLClass d = factory.getOWLClass("D");
	OWLClass e = factory.getOWLClass("E");
	OWLClass f = factory.getOWLClass("F");
	OWLClass g = factory.getOWLClass("G");
	// roles in ontology
	OWLObjectProperty r = factory.getOWLObjectProperty("R");
	OWLObjectProperty s = factory.getOWLObjectProperty("S");
	// individuals in ontology
	OWLIndividual ind_a = factory.getOWLNamedIndividual("a");
	OWLIndividual ind_b = factory.getOWLNamedIndividual("b");
	OWLIndividual ind_c = factory.getOWLNamedIndividual("c");
	OWLIndividual ind_d = factory.getOWLNamedIndividual("d");

	@Test
	public void testIsSubClassOf() {
		assertTrue(ontoHandler.checkIfSubClass(e, f));
		assertTrue(ontoHandler.checkIfSubClass(e, g));
		assertFalse(ontoHandler.checkIfSubClass(g, e));
	}

	@Test
	public void testGetPotentialRightSideRestrictions() {

		// wanted restrictions:
		// [from subclass]
		// ObjectSomeValuesFrom(<S> ObjectComplementOf(<C>))
		// ObjectAllValuesFrom(<R> <A>)
		//
		// [from superclass]
		// ObjectSomeValuesFrom(<R> ObjectSomeValuesFrom(<S> <B>))
		// ObjectSomeValuesFrom(<S> <B>)
		// ObjectAllValuesFrom(<R> ObjectComplementOf(<D>))
		// ObjectAllValuesFrom(<S> <C>)
		RestrictionNodeCollection restNodeCol = ontoHandler.getPotentialRightSideRestrictionNodes();
		assertEquals(3, restNodeCol.exist_restrictions.size());
		assertTrue(restNodeCol
				.containsExistRest(factory.getOWLObjectSomeValuesFrom(s, factory.getOWLObjectComplementOf(c))));
		assertTrue(restNodeCol
				.containsExistRest(factory.getOWLObjectSomeValuesFrom(r, factory.getOWLObjectSomeValuesFrom(s, b))));
		assertTrue(restNodeCol.containsExistRest(factory.getOWLObjectSomeValuesFrom(s, b)));
		assertEquals(2, restNodeCol.univ_restrictions.size());
		assertEquals(2, restNodeCol.univ_restrictions.get(r).size());
		assertEquals(1, restNodeCol.univ_restrictions.get(s).size());
		assertTrue(restNodeCol.containsUnivRest(r, factory.getOWLObjectAllValuesFrom(r, a)));
		assertTrue(restNodeCol.containsUnivRest(r,
				factory.getOWLObjectAllValuesFrom(r, factory.getOWLObjectComplementOf(d))));
		assertTrue(restNodeCol.containsUnivRest(s, factory.getOWLObjectAllValuesFrom(s, c)));

	}

	@Test
	public void testGetIndividuals() {
		Set<OWLIndividual> inds = ontoHandler.getIndividuals();
		assertEquals(4, inds.size());
		assertTrue(inds.contains(ind_a));
		assertTrue(inds.contains(ind_b));
		assertTrue(inds.contains(ind_c));
		assertTrue(inds.contains(ind_d));
	}

	@Test
	public void testGetMostSpecificConceptsForIndividuals() {
		OWLIndividual ind_e = factory.getOWLNamedIndividual("e");
		OWLClass b1 = factory.getOWLClass("B1");
		OWLClass b2 = factory.getOWLClass("B2");
		OWLClass b3 = factory.getOWLClass("B3");
		OWLObjectSomeValuesFrom xRb1b2 = factory.getOWLObjectSomeValuesFrom(r,
				factory.getOWLObjectIntersectionOf(b1, b2));
		OWLObjectSomeValuesFrom xSa = factory.getOWLObjectSomeValuesFrom(s, a);
		OWLObjectSomeValuesFrom xR_cxRb1b2xSa = factory.getOWLObjectSomeValuesFrom(r,
				factory.getOWLObjectIntersectionOf(c, xRb1b2, xSa));
		OWLObjectSomeValuesFrom xSe = factory.getOWLObjectSomeValuesFrom(s, e);
		OWLObjectSomeValuesFrom xRT = factory.getOWLObjectSomeValuesFrom(r, factory.getOWLThing());

		OntologyHandler oh = new OntologyHandler(new File("resources/test/test_ontology2.owl"));
		// sorted universal restrictions
		HashMap<OWLProperty, ConceptNodeSet<OWLClassExpression>> univ_rests_nodes = new HashMap<>();
		univ_rests_nodes.put(r,
				new ConceptNodeSet<>(Set.of(new ConceptNode<>(b1))));
		univ_rests_nodes.put(s, new ConceptNodeSet<>(
				Set.of(new ConceptNode<>(b3))));
		// compute results
		HashMap<OWLIndividual, Set<OWLClassExpression>> results = oh
				.getMostSpecificConceptsForIndividuals(Set.of(ind_a, ind_b, ind_c, ind_d, ind_e), univ_rests_nodes);

		// print results
//		for (OWLIndividual ind : results.keySet()) {
//			System.out.print(ind + ": ");
//			Set<OWLClassExpression> concepts = results.get(ind);
//			for (OWLClassExpression c : concepts) {
//				System.out.print(c.accept(new StringConverter()) + " ");
//			}
//			System.out.println();
//		}

		assertEquals(5, results.keySet().size());
		assertEquals(Set.of(a), results.get(ind_a));
		assertEquals(Set.of(b1, b2), results.get(ind_b));
		assertEquals(Set.of(c, xRb1b2, xSa), results.get(ind_c));
		assertEquals(Set.of(xR_cxRb1b2xSa, xRb1b2), results.get(ind_d));
		assertEquals(Set.of(xSe, xRT), results.get(ind_e));

	}

	@Test
	public void testGetMostSpecificABoxConcepts() {
		Set<OWLClassExpression> a_cons = ontoHandler.getMostSpecificABoxConcepts(ind_a);

		// wanted most specific concepts for individual a:
		// <A>
		// ObjectIntersectionOf(<C> <B>)
		// NOT wanted (not most specific):
		// <B>
		// ObjectSomeValuesFrom(<R> ObjectSomeValuesFrom(<S> <B>))
		assertEquals(2, a_cons.size());
		assertTrue(a_cons.contains(a));
		assertTrue(a_cons.contains(factory.getOWLObjectIntersectionOf(c, b)));
		assertFalse(a_cons.contains(b));
		assertFalse(a_cons.contains(c));
		assertFalse(a_cons.contains(factory.getOWLObjectSomeValuesFrom(r, factory.getOWLObjectSomeValuesFrom(s, b))));

	}

	@Test
	public void testCreateUnivRestriction() {		
		OWLObjectIntersectionOf bc = factory.getOWLObjectIntersectionOf(b, c);
		OWLObjectSomeValuesFrom xsd = factory.getOWLObjectSomeValuesFrom(s, d);
		OWLObjectAllValuesFrom combined = ontoHandler.createUnivRestriction(r, Set.of(a, bc, xsd));
		OWLObjectAllValuesFrom expected = factory.getOWLObjectAllValuesFrom(r,
				factory.getOWLObjectIntersectionOf(a, bc, xsd));
		assertEquals(expected, combined);
	}

	@Test
	public void testCheckIfNoEquivalent() {
		OWLObjectSomeValuesFrom rd = factory.getOWLObjectSomeValuesFrom(r, d);
		OWLObjectSomeValuesFrom sd = factory.getOWLObjectSomeValuesFrom(s, d);
		OWLObjectSomeValuesFrom rb = factory.getOWLObjectSomeValuesFrom(r, b);

		assertFalse(ontoHandler.checkIfNoEquivalent(rd, Set.of(sd, rd, rb)));
		assertTrue(ontoHandler.checkIfNoEquivalent(rd, Set.of(sd, rb)));

		try {
			// set B ≡ D
			OWLOntology ontology = manager.loadOntologyFromOntologyDocument(of);
			manager.addAxiom(ontology, factory.getOWLEquivalentClassesAxiom(b, d));
			assertFalse(new OntologyHandler(ontology).checkIfNoEquivalent(rd, Set.of(sd, rb)));
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCheckIfRoleAssertionPresent() {
		assertTrue(ontoHandler.checkIfRoleAssertionPresent(r, ind_a, b));
		assertFalse(ontoHandler.checkIfRoleAssertionPresent(r, ind_a, d));
		assertFalse(ontoHandler.checkIfRoleAssertionPresent(r, ind_c, b));
	}

	@Test
	public void testCombineToExistRestriction() {
		OWLObjectSomeValuesFrom computed = ontoHandler.combineToExistRestriction(
				factory.getOWLObjectSomeValuesFrom(r, factory.getOWLObjectIntersectionOf(a, f, g)),
				factory.getOWLObjectAllValuesFrom(r, factory.getOWLObjectIntersectionOf(b, e)));
		OWLObjectSomeValuesFrom expected = factory.getOWLObjectSomeValuesFrom(r,
				factory.getOWLObjectIntersectionOf(a, b, e));
		assertEquals(expected, computed);

		computed = ontoHandler.combineToExistRestriction(
				factory.getOWLObjectSomeValuesFrom(r, factory.getOWLObjectIntersectionOf(a, f, g)), null);
		assertEquals(factory.getOWLObjectSomeValuesFrom(r, factory.getOWLObjectIntersectionOf(a, f)), computed);
	}

}
