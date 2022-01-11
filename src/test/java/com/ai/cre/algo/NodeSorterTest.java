package com.ai.cre.algo;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedList;
import java.util.Set;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.ai.cre.algo.NodeSorter;
import com.ai.cre.ontology.OntologyHandler;
import com.ai.cre.ontology.StringConverter;
import com.ai.cre.representation.ConceptNode;
import com.ai.cre.representation.ConceptNodeSet;

public class NodeSorterTest {

	@Test
	public void testSortBySubsumption() {

		// prepare test ontology
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OntologyHandler ontoHandler = new OntologyHandler(new File("resources/test/sort_example.owl"));

		for (int i = 0; i < 2; i++) {
			// create nodes for concepts in ontology
			OWLObjectProperty r = factory.getOWLObjectProperty("R");
			ConceptNode<OWLObjectSomeValuesFrom> a = new ConceptNode<>(
					factory.getOWLObjectSomeValuesFrom(r, factory.getOWLClass("A")));
			ConceptNode<OWLObjectSomeValuesFrom> b = new ConceptNode<>(
					factory.getOWLObjectSomeValuesFrom(r, factory.getOWLClass("B")));
			ConceptNode<OWLObjectSomeValuesFrom> c = new ConceptNode<>(
					factory.getOWLObjectSomeValuesFrom(r, factory.getOWLClass("C")));
			ConceptNode<OWLObjectSomeValuesFrom> d = new ConceptNode<>(
					factory.getOWLObjectSomeValuesFrom(r, factory.getOWLClass("D")));
			ConceptNode<OWLObjectSomeValuesFrom> e = new ConceptNode<>(
					factory.getOWLObjectSomeValuesFrom(r, factory.getOWLClass("E")));
			ConceptNode<OWLObjectSomeValuesFrom> f = new ConceptNode<>(
					factory.getOWLObjectSomeValuesFrom(r, factory.getOWLClass("F")));
			ConceptNode<OWLObjectSomeValuesFrom> g = new ConceptNode<>(
					factory.getOWLObjectSomeValuesFrom(r, factory.getOWLClass("G")));
			LinkedList<ConceptNode<OWLObjectSomeValuesFrom>> nodes = new LinkedList<>(Set.of(a, b, c, d, e, f, g));

			if (i == 0) {
				// sort nodes with Enhanced Traversal Method
				NodeSorter.sortBySubsumptionUsingEnhancedTraversal(ontoHandler,
						new ConceptNodeSet<OWLObjectSomeValuesFrom>(nodes));
			} else {
				// sort nodes with Classification approach
				NodeSorter.sortBySubsumptionUsingClassification(ontoHandler,
						new ConceptNodeSet<OWLObjectSomeValuesFrom>(nodes));
			}

			// print nodes
//			for(ConceptNode<OWLObjectSomeValuesFrom> n : nodes) {
//				for(OWLClassExpression nc : n.concepts) {
//					System.out.print(nc.accept(new StringConverter()) + "|");
//				}				
//				System.out.print(" supers = { ");
//				for(ConceptNode p : n.supers) {
//					System.out.print(p.getConcept().accept(new StringConverter()) + " ");
//				}
//				System.out.print("},  sub = { ");
//				for(ConceptNode p : n.subs) {
//					System.out.print(p.getConcept().accept(new StringConverter()) + " ");
//				}
//				System.out.println("}");
//			}
//			System.out.println();

			// check results
			if (a.subs.isEmpty()) {
				assertTrue(b.concepts.size() == 2);
				assertTrue(b.subs.contains(c));
				assertTrue(b.subs.contains(d));
				assertTrue(b.subs.contains(e));
				assertTrue(b.subs.size() == 3);
			} else {
				assertTrue(a.concepts.size() == 2);
				assertTrue(a.subs.contains(c));
				assertTrue(a.subs.contains(d));
				assertTrue(a.subs.contains(e));
				assertTrue(a.subs.size() == 3);
			}
			assertTrue(c.subs.contains(g));
			assertTrue(c.subs.size() == 1);
			assertTrue(d.subs.contains(f));
			assertTrue(d.subs.size() == 1);
			assertTrue(e.subs.size() == 0);
			assertTrue(f.subs.size() == 0);
			assertTrue(g.subs.size() == 0);
		}
	}

}
