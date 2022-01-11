package com.ai.cre.algo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;

import com.ai.cre.algo.NodeSorter;
import com.ai.cre.algo.RefExpRetrieval;
import com.ai.cre.ontology.OntologyHandler;
import com.ai.cre.ontology.RestrictionNodeCollection;
import com.ai.cre.representation.ConceptNode;
import com.ai.cre.representation.ConceptNodeSet;
import com.ai.cre.representation.ConceptReferringExpression;
import com.ai.cre.representation.ProcessConRefExpression;

public class RefExpRetrievalTest {

	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLDataFactory factory = manager.getOWLDataFactory();

	OWLClass a = factory.getOWLClass("A");
	OWLClass b = factory.getOWLClass("B");
	OWLClass c = factory.getOWLClass("C");
	OWLClass d = factory.getOWLClass("D");
	OWLClass e = factory.getOWLClass("E");
	OWLClass f = factory.getOWLClass("F");
	OWLObjectProperty r = factory.getOWLObjectProperty("R");
	OWLObjectProperty s = factory.getOWLObjectProperty("S");

	@Test
	public void testConsiderSubExistRestrictions() {
		OntologyHandler ontologyHandler = new OntologyHandler(new File("resources/test/exist_retrieval_example.owl"));
		// get ordered existential restrictions from ontology
		ConceptNodeSet<OWLObjectSomeValuesFrom> rhsExistRests = ontologyHandler
				.getPotentialRightSideRestrictionNodes().exist_restrictions;

		ConceptNodeSet<OWLObjectSomeValuesFrom> nodes = new ConceptNodeSet<>(
				NodeSorter.sortBySubsumption(ontologyHandler, rhsExistRests));

		// get restrictions for current concept C
		Set<OWLObjectSomeValuesFrom> rests = new RefExpRetrieval().considerSubExistRestrictions(ontologyHandler,
				factory.getOWLClass("C"), nodes, new HashSet<>());

		// wanted restrictions:
		// ObjectSomeValuesFrom(<R> <A>)
		// ObjectSomeValuesFrom(<S> <D>)
		// ObjectSomeValuesFrom(<S> <B>)
		// NOT wanted:
		// ObjectSomeValuesFrom(<R> <E>)
		// ObjectSomeValuesFrom(<R> <F>)
		// check collected restrictions
		assertEquals(3, rests.size());
		assertTrue(rests.contains(factory.getOWLObjectSomeValuesFrom(r, a)));
		assertTrue(rests.contains(factory.getOWLObjectSomeValuesFrom(s, d)));
		assertTrue(rests.contains(factory.getOWLObjectSomeValuesFrom(s, b)));
		assertFalse(rests.contains(factory.getOWLObjectSomeValuesFrom(r, e)));
		assertFalse(rests.contains(factory.getOWLObjectSomeValuesFrom(r, f)));
	}

	@Test
	public void testGetRoleConstraint() {
		OntologyHandler ontologyHandler = new OntologyHandler(new File("resources/test/univ_retrieval_example.owl"));
		// get ordered universal restrictions from ontology with role R
		RestrictionNodeCollection restNodeCol = ontologyHandler.getPotentialRightSideRestrictionNodes();
		restNodeCol.univ_restrictions.forEach((role, nodes) -> restNodeCol.univ_restrictions.put(role,
				new ConceptNodeSet<>(NodeSorter.sortBySubsumption(ontologyHandler, new ConceptNodeSet<>(nodes)))));

		ConceptNodeSet<OWLClassExpression> nodes = restNodeCol.univ_restrictions.get(r);
		// get role constraint with role R for current concept C
		OWLObjectAllValuesFrom role_constraint = new RefExpRetrieval().getRoleConstraint(ontologyHandler, c, nodes, r);

		// wanted restriction: ObjectAllValuesFrom(<R> ObjectIntersectionOf(<A> <B>))
		assertEquals(factory.getOWLObjectAllValuesFrom(r, factory.getOWLObjectIntersectionOf(a, b)), role_constraint);
	}

	@Test
	public void testConstructRefExpAnswers() {
		OntologyHandler ontologyHandler = new OntologyHandler(new File("resources/test/construct_example.owl"));

		ConceptNodeSet<OWLObjectSomeValuesFrom> pos_exist_rests = new ConceptNodeSet<>();
		pos_exist_rests.add(new ConceptNode<>(factory.getOWLObjectSomeValuesFrom(r, b)));
		pos_exist_rests.add(new ConceptNode<>(factory.getOWLObjectSomeValuesFrom(r, d)));
		pos_exist_rests.add(new ConceptNode<>(factory.getOWLObjectSomeValuesFrom(s, c)));
		pos_exist_rests.add(new ConceptNode<>(factory.getOWLObjectSomeValuesFrom(s, e)));

		HashMap<OWLProperty, ConceptNodeSet<OWLClassExpression>> pos_univ_rests_table = new HashMap<>();
		pos_univ_rests_table.put(r,
				new ConceptNodeSet<OWLClassExpression>(Set.of(new ConceptNode<OWLClassExpression>(c))));

		OWLClassExpression query = c;
		OWLClassExpression current_concept = a;
		ProcessConRefExpression ref_exp = new ProcessConRefExpression(Set.of(factory.getOWLNamedIndividual("a")));

		Set<ConceptReferringExpression> computed = new RefExpRetrieval().constructRefExpAnswers(ontologyHandler,
				pos_exist_rests, pos_univ_rests_table, query, current_concept, ref_exp,
				new HashSet<OWLObjectSomeValuesFrom>());

		Collection<String> expected = new HashSet<>();
		expected.add("C ⊓ D ⊓ ∃R⎺.(B ⊓ ∃R⎺.({a}))");
		expected.add("[^0 C ⊓ ∃S⎺.(]ᐩ^0 {a})");
		expected.add("[^0 C ⊓ ∃S⎺.(]ᐩ^0 C ⊓ D ⊓ ∃R⎺.(B ⊓ ∃R⎺.({a})))");
		assertEquals(expected.size(), computed.size());
		for (ConceptReferringExpression cmptCRE : computed) {
			assertTrue(expected.contains(cmptCRE.getString()));
		}

	}

}
