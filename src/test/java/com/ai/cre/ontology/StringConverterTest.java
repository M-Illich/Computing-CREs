package com.ai.cre.ontology;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.ai.cre.ontology.StringConverter;

public class StringConverterTest {

	@Test
	public void test() {
		StringConverter strCon = new StringConverter();
		// create OWLClassExpression for concept C = A ⊓ ∃S.B ⊓ ∀R.(¬(C ⊓ D))
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClassExpression concept = factory.getOWLObjectIntersectionOf(factory.getOWLClass("A"),
				factory.getOWLObjectSomeValuesFrom(factory.getOWLObjectProperty("S"), factory.getOWLClass("B")),
				factory.getOWLObjectAllValuesFrom(factory.getOWLObjectProperty("R"), factory.getOWLObjectIntersectionOf(
						factory.getOWLObjectSomeValuesFrom(factory.getOWLObjectProperty("P"), factory.getOWLClass("E")),
						factory.getOWLObjectComplementOf(factory.getOWLObjectIntersectionOf(factory.getOWLClass("D"),
								factory.getOWLClass("C"))))));

		// check String transformation
		assertEquals("A ⊓ ∀R.(¬(C ⊓ D) ⊓ ∃P.E) ⊓ ∃S.B", concept.accept(strCon));
	}
}
