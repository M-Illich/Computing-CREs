package com.ai.cre.representation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.ai.cre.representation.ConRefExpPart;
import com.ai.cre.representation.ConceptReferringExpression;
import com.ai.cre.representation.ProcessConRefExpression;

public class ProcessConRefExpressionTest {
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLDataFactory factory = manager.getOWLDataFactory();

	OWLClass a = factory.getOWLClass("A");
	OWLClass b = factory.getOWLClass("B");
	OWLClass c = factory.getOWLClass("C");
	OWLClass d = factory.getOWLClass("D");
	OWLObjectProperty r = factory.getOWLObjectProperty("R");
	OWLObjectProperty s = factory.getOWLObjectProperty("S");

	@Test
	public void testMarkCycle() {
		LinkedList<ConRefExpPart> parts = new LinkedList<ConRefExpPart>();
		ConRefExpPart firstPart = new ConRefExpPart(factory.getOWLObjectSomeValuesFrom(r, b));
		ConRefExpPart secondPart = new ConRefExpPart(factory.getOWLObjectSomeValuesFrom(s, c));
		parts.add(firstPart);
		parts.add(0, secondPart);
		ProcessConRefExpression proCRE = new ProcessConRefExpression(Set.of(factory.getOWLNamedIndividual("a")), parts,
				1);

		proCRE.markCycle(firstPart);
		proCRE = proCRE.getExtended(factory.getOWLObjectSomeValuesFrom(factory.getOWLObjectProperty("T"), d));
		proCRE.markCycle(secondPart);
		proCRE.markCycle(firstPart);

		// check if right marks set
		assertEquals("", proCRE.refExpParts.get(2).LeftCycleEnd);
		assertEquals("1.3", proCRE.refExpParts.get(2).RightCycleEnd);
		assertEquals("1", proCRE.refExpParts.get(1).LeftCycleEnd);
		assertEquals("2", proCRE.refExpParts.get(1).RightCycleEnd);
		assertEquals("2.3", proCRE.refExpParts.get(0).LeftCycleEnd);
		assertEquals("", proCRE.refExpParts.get(0).RightCycleEnd);

	}

	@Test
	public void testComplete() {
		LinkedList<ConRefExpPart> parts = new LinkedList<ConRefExpPart>();
		OWLObjectIntersectionOf cb = factory.getOWLObjectIntersectionOf(c, b);

		ConRefExpPart firstPart = new ConRefExpPart(factory.getOWLObjectSomeValuesFrom(r, cb));
		ConRefExpPart secondPart = new ConRefExpPart(factory.getOWLObjectSomeValuesFrom(s, d));
		parts.add(firstPart);
		parts.add(0, secondPart);
		ProcessConRefExpression proCRE = new ProcessConRefExpression(
				Set.of(factory.getOWLNamedIndividual("a"), factory.getOWLNamedIndividual("b")), parts, 0);
		proCRE.markCycle(firstPart);

		Collection<ConceptReferringExpression> computed = proCRE.complete();
		Collection<String> expected = new HashSet<>();
		expected.add("[^0 D ⊓ ∃S⎺.(B ⊓ C ⊓ ∃R⎺.(]ᐩ^0 {a}))");
		expected.add("[^0 D ⊓ ∃S⎺.(B ⊓ C ⊓ ∃R⎺.(]ᐩ^0 {b}))");

		assertEquals(expected.size(), computed.size());
		for (ConceptReferringExpression cmptCRE : computed) {
			assertTrue(expected.contains(cmptCRE.getString()));
		}

		proCRE = new ProcessConRefExpression(Set.of(factory.getOWLNamedIndividual("a")));
		computed = proCRE.complete();
		expected.clear();
		expected.add("a");
		assertEquals(expected.size(), computed.size());
		for (ConceptReferringExpression cmptCRE : computed) {
			assertTrue(expected.contains(cmptCRE.getString()));
		}

		// test removal of repeated ex. rest.
		firstPart = new ConRefExpPart(factory.getOWLObjectSomeValuesFrom(r,
				factory.getOWLObjectIntersectionOf(b, factory.getOWLObjectSomeValuesFrom(s, c))));
		secondPart = new ConRefExpPart(factory.getOWLObjectSomeValuesFrom(s, c));
		parts.clear();
		parts.add(firstPart);
		parts.add(0, secondPart);
		proCRE = new ProcessConRefExpression(Set.of(factory.getOWLNamedIndividual("a")), parts, 0);
		computed = proCRE.complete();
		expected.clear();
		expected.add("C ⊓ ∃S⎺.(B ⊓ ∃R⎺.({a}))");
		assertEquals(expected.size(), computed.size());
		for (ConceptReferringExpression cmptCRE : computed) {
			assertTrue(expected.contains(cmptCRE.getString()));
		}

	}
}
