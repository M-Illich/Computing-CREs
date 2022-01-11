package com.ai.cre.evaluation;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.ai.cre.algo.RefExpRetrieval;
import com.ai.cre.representation.ConceptReferringExpression;

/**
 * Performance test of algorithm where several Horn-ALC ontologies are processed
 * and the resulting runtimes measured
 *
 */
public class RuntimeEvaluation {

	public static void main(String[] args) {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();

		// load test ontologies from files
		List<OWLOntology> ontologies = new LinkedList<>();
		File folder = new File("resources/performance");
		File[] files = folder.listFiles();
		for (File file : files) {
			try {
				ontologies.add(manager.loadOntologyFromOntologyDocument(file));
			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
			}
		}

		// use Top as query, i.e., find every (named and anonymous) individual
		OWLClassExpression query = factory.getOWLThing();

		// state if existential and universal restrictions should be sorted by means of
		// subsumption hierarchies
		boolean apply_sorting = true;

		int repetitions = 5;
		System.out.println("Runtime measurements (average of " + repetitions + " runs):");
		System.out.println(
				(apply_sorting ? " with" : " without") + " subsumption hierarchies for exist. + univ. restrictions");

		Collection<ConceptReferringExpression> answers = null;
		for (OWLOntology ontology : ontologies) {
			long start = System.currentTimeMillis();
			for (int i = 0; i < repetitions; i++) {
				answers = new RefExpRetrieval(apply_sorting).getInstanceReferringExpressions(ontology, query);
			}
			long end = System.currentTimeMillis();
			System.out.println(ontology.getOntologyID().getOntologyIRI().get());
			System.out.println("time: " + (end - start) / repetitions + " ms");

			System.out.println("#answers: " + answers.size());
			System.out.println();
		}

	}

}
