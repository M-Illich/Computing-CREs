package com.ai.cre.ontology;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLProperty;

import com.ai.cre.ontology.OntologyHandler;
import com.ai.cre.ontology.RestrictionNodeCollection;

public class RestrictionNodeCollectionTest {

	@Test
	public void testAddUnivRestriction() {
		RestrictionNodeCollection rnCol = new RestrictionNodeCollection(new OntologyHandler());
		// create some universal restriction
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLObjectProperty rest_role = factory.getOWLObjectProperty("role");
		OWLObjectAllValuesFrom univ_rest = factory.getOWLObjectAllValuesFrom(rest_role, factory.getOWLClass("class"));
		// add univ. rest. to collection
		rnCol.addUnivRestriction(univ_rest);
		// check if rest. in collection
		assertTrue((rnCol.containsUnivRest(rest_role, univ_rest)));

		// add another rest. with different role
		OWLObjectProperty rest_role_2 = factory.getOWLObjectProperty("role2");
		OWLObjectAllValuesFrom univ_rest_2 = factory.getOWLObjectAllValuesFrom(rest_role_2,
				factory.getOWLClass("class2"));
		// add univ. rest. to collection
		rnCol.addUnivRestriction(univ_rest_2);
		// check if new rest. in collection
		assertTrue(rnCol.containsUnivRest(rest_role_2, univ_rest_2));
		// check if old rest. still in collection
		assertTrue(rnCol.containsUnivRest(rest_role, univ_rest));

		// add another rest. with same role
		OWLObjectAllValuesFrom univ_rest_3 = factory.getOWLObjectAllValuesFrom(rest_role,
				factory.getOWLClass("class3"));
		// add univ. rest. to collection
		rnCol.addUnivRestriction(univ_rest_3);
		// check if new rest. in collection
		assertTrue(rnCol.containsUnivRest(rest_role, univ_rest_3));
		// check if old rest. still in collection
		assertTrue(rnCol.containsUnivRest(rest_role, univ_rest));
		assertTrue(rnCol.containsUnivRest(rest_role_2, univ_rest_2));

		// add nested restriction
		OWLObjectAllValuesFrom univ_rest_nested = factory.getOWLObjectAllValuesFrom(rest_role_2,
				factory.getOWLObjectAllValuesFrom(rest_role, factory.getOWLClass("class")));
		rnCol.addUnivRestriction(univ_rest_nested);
		assertTrue(rnCol.containsUnivRest(rest_role_2, univ_rest_nested));

		// check number of role-specific entries
		assertTrue(rnCol.univ_restrictions.size() == 2);
		// no existential restriction should be present
		assertTrue(rnCol.exist_restrictions.isEmpty());

	}

	@Test
	public void testMergeWith() {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();

		// create a RestrictionNodeCollection with one existential and universal
		// restriction
		RestrictionNodeCollection rnCol1 = new RestrictionNodeCollection(new OntologyHandler());
		// add existential rest.
		OWLObjectSomeValuesFrom exist_rest = factory.getOWLObjectSomeValuesFrom(factory.getOWLObjectProperty("roleX"),
				factory.getOWLClass("classx"));
		rnCol1.addExistRestriction(exist_rest);
		OWLProperty rest_role = factory.getOWLObjectProperty("role");
		OWLObjectAllValuesFrom univ_rest = factory.getOWLObjectAllValuesFrom((OWLObjectPropertyExpression) rest_role,
				factory.getOWLClass("class"));
		// add univ. rest. to collection
		rnCol1.addUnivRestriction(univ_rest);

		// create another RestrictionNodeCollection with some different restrictions
		RestrictionNodeCollection rnCol2 = new RestrictionNodeCollection(new OntologyHandler());
		// same role like univ. rest. from above
		OWLObjectAllValuesFrom univ_rest_2 = factory.getOWLObjectAllValuesFrom((OWLObjectPropertyExpression) rest_role,
				factory.getOWLClass("class2"));
		rnCol2.addUnivRestriction(univ_rest_2);
		// univ. rest. with different role
		OWLProperty rest_role_3 = factory.getOWLObjectProperty("role3");
		OWLObjectAllValuesFrom univ_rest_3 = factory
				.getOWLObjectAllValuesFrom((OWLObjectPropertyExpression) rest_role_3, factory.getOWLClass("class3"));
		rnCol2.addUnivRestriction(univ_rest_3);
		// add existential rest.
		OWLObjectSomeValuesFrom exist_rest_2 = factory
				.getOWLObjectSomeValuesFrom(factory.getOWLObjectProperty("roleX2"), factory.getOWLClass("classx2"));
		rnCol2.addExistRestriction(exist_rest_2);

		rnCol1.mergeWith(rnCol2);
		assertTrue(rnCol1.containsExistRest(exist_rest));
		assertTrue(rnCol1.containsExistRest(exist_rest_2));
		assertTrue(rnCol1.containsUnivRest(rest_role, univ_rest));
		assertTrue(rnCol1.containsUnivRest(rest_role, univ_rest_2));
		assertTrue(rnCol1.containsUnivRest(rest_role_3, univ_rest_3));

	}

}
