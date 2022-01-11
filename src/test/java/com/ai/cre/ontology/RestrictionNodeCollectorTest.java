package com.ai.cre.ontology;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.ai.cre.ontology.NegativeRestrictionNodeCollector;
import com.ai.cre.ontology.OntologyHandler;
import com.ai.cre.ontology.PositiveRestrictionNodeCollector;
import com.ai.cre.ontology.RestrictionNodeCollection;

/**
 * Test cases for both {@link PositiveRestrictionNodeCollector} and
 * {@link NegativeRestrictionNodeCollector}
 *
 */
public class RestrictionNodeCollectorTest {

	@Test
	public void test() {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		// classes
		OWLClass a = factory.getOWLClass("A");
		OWLClass b = factory.getOWLClass("B");
		// roles
		OWLObjectProperty r = factory.getOWLObjectProperty("R");
		OWLObjectProperty s = factory.getOWLObjectProperty("S");
		// restrictions
		OWLObjectSomeValuesFrom exRA = factory.getOWLObjectSomeValuesFrom(r, a);
		OWLObjectAllValuesFrom allRnA = factory.getOWLObjectAllValuesFrom(r, a.getComplementNNF());
		OWLObjectSomeValuesFrom exSallRnA = factory.getOWLObjectSomeValuesFrom(s, allRnA);
		OWLObjectAllValuesFrom allSnallRnA = factory.getOWLObjectAllValuesFrom(s, allRnA.getComplementNNF());
		OWLObjectAllValuesFrom allSB = factory.getOWLObjectAllValuesFrom(s, b);
		OWLObjectSomeValuesFrom exSnB = factory.getOWLObjectSomeValuesFrom(s, b.getComplementNNF());
		// intersection
		OWLObjectIntersectionOf exRA_allSB = factory.getOWLObjectIntersectionOf(exRA, allSB);

		// check collecting of positive (right-sided) restrictions
		PositiveRestrictionNodeCollector pc = new PositiveRestrictionNodeCollector(new OntologyHandler());
		RestrictionNodeCollection rnc = exRA.accept(pc);
		assertTrue(rnc.univ_restrictions.isEmpty());
		assertTrue(rnc.containsExistRest(exRA));

		rnc = factory.getOWLObjectComplementOf(exRA).accept(pc);
		assertTrue(rnc.exist_restrictions.isEmpty());
		assertTrue(rnc.containsUnivRest(r, allRnA));

		rnc = exSallRnA.accept(pc);
		assertTrue(rnc.containsExistRest(exSallRnA));
		assertTrue(rnc.containsUnivRest(r, allRnA));

		rnc = exRA_allSB.accept(pc);
		assertTrue(rnc.containsExistRest(exRA));
		assertTrue(rnc.containsUnivRest(s, allSB));

		// check collecting of positive (right-sided) restrictions
		NegativeRestrictionNodeCollector nc = new NegativeRestrictionNodeCollector(new OntologyHandler());
		rnc = exRA.accept(nc);
		assertTrue(rnc.exist_restrictions.isEmpty());
		assertTrue(rnc.containsUnivRest(r, allRnA));

		rnc = factory.getOWLObjectComplementOf(exRA).accept(nc);
		assertTrue(rnc.univ_restrictions.isEmpty());
		assertTrue(rnc.containsExistRest(exRA));

		rnc = exSallRnA.accept(nc);
		assertTrue(rnc.containsExistRest(exRA));
		assertTrue(rnc.containsUnivRest(s, allSnallRnA));

		rnc = exRA_allSB.accept(nc);
		assertTrue(rnc.containsUnivRest(r, allRnA));
		assertTrue(rnc.containsExistRest(exSnB));

	}
}
