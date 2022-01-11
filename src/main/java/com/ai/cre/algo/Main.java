package com.ai.cre.algo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import com.ai.cre.ontology.Preparer;
import com.ai.cre.representation.ConceptReferringExpression;

public class Main {

	public static void main(String[] args) {
		/**
		 * The following provides an example of how the algorithm can be used
		 * considering one of our test ontologies
		 */

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();

		try {
			// file containing ontology
			File file = new File("resources/performance/4 - prepared_ore_ont_3313.owl");
			// if necessary, adapt ontology to fulfill desired conditions, like Horn-ALC
			// (already done for our here tested ontologies)
//			OWLOntology ontology = Preparer.prepareOntology(file);
			OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
			// define reasoner (only necessary if another reasoner than HermiT wanted)
			OWLReasoner reasoner = new Reasoner(new Configuration(), ontology);

			// define query (here Top)
			OWLClassExpression query = factory.getOWLThing();

			// compute CRE answers
			Collection<ConceptReferringExpression> answers = new RefExpRetrieval(true)
					.getInstanceReferringExpressions(ontology, reasoner, query);

			// write answers to a file (in form of sorted strings)
			File ans_file = new File("results.txt");
			List<String> strings = new LinkedList<>();
			for (ConceptReferringExpression ex : answers) {
				strings.add(ex.getString());
			}
			Collections.sort(strings, Comparator.comparing(String::length));
			try {
				Files.write(ans_file.toPath(), strings);
			} catch (IOException e) {
				e.printStackTrace();
			}

			// print out some statistic about created CREs
			System.out.println("number of answers: " + answers.size());
			HashMap<Integer, Integer> depths = new HashMap<>();
			HashMap<Integer, Integer> cycles = new HashMap<>();
			for (ConceptReferringExpression ex : answers) {
				// count number of applied existential restrictions
				int depth = (int) ex.getString().chars().filter(c -> c == '⎺').count();
				int count = (depths.get(depth) != null ? depths.get(depth) : 0) + 1;
				depths.put(depth, count);
				// get number of included cycles
				count = (cycles.get(ex.getCycleNumber()) != null ? cycles.get(ex.getCycleNumber()) : 0) + 1;
				cycles.put(ex.getCycleNumber(), count);
			}
			for (int depth : depths.keySet()) {
				System.out.println("depth of " + depth + " for " + depths.get(depth) + " answers ("
						+ (((float) depths.get(depth)) * 100 / ((float) answers.size())) + "%)");
			}
			for (int cyc : cycles.keySet()) {
				System.out.println("cycle number of " + cyc + " for " + cycles.get(cyc) + " answers");
			}
			System.out.println();

		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}

	}

}
