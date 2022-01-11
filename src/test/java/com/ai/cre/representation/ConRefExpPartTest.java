package com.ai.cre.representation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.ai.cre.representation.ConRefExpPart;

public class ConRefExpPartTest {
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLDataFactory factory = manager.getOWLDataFactory();

	@Test
	public void testGetExistRestString() {
		OWLClass a = factory.getOWLClass("A");
		OWLClass b = factory.getOWLClass("B");
		OWLObjectIntersectionOf ab = factory.getOWLObjectIntersectionOf(b, a);
		OWLObjectSomeValuesFrom exist_rest = factory.getOWLObjectSomeValuesFrom(factory.getOWLObjectProperty("R"), ab);
		ConRefExpPart part = new ConRefExpPart(exist_rest);

		assertEquals("A ⊓ B ⊓ ∃R⎺.(", part.getExistRestString());

		// add cycle-notation
		part.LeftCycleEnd = "1";
		part.RightCycleEnd = "2";

		assertEquals("[^1 A ⊓ B ⊓ ∃R⎺.(]ᐩ^2 ", part.getExistRestString());

	}
}
