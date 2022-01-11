package com.ai.cre.ontology;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

/**
 * A class to modify ontologies in order to adhere to the features required by
 * the algorithm (see {@link #prepareOntology(File, String, OWLOntologyManager)}
 *
 */
public class Preparer {

	/**
	 * Prepare an ontology for its processing by the algorithm, which includes
	 * merging of every imported ontology into one, representing it in functional
	 * syntax, as well as removing axioms that are not supported by Horn-ALC.
	 * 
	 * @param file A {@link File} object containing the original ontology
	 * 
	 * @return An {@link OWLOntology} representing the modified ontology
	 */
	public static OWLOntology prepareOntology(File file) {
		try {
			// create new file with new name
			String path_string = file.getParent() + "/prepared_"
					+ file.getName().substring(0, file.getName().lastIndexOf(".")) + ".owl";
			File new_file = new File(path_string);
			// create new IRI for resulting ontology
			IRI iri = IRI.create(new_file.getAbsoluteFile().toURI());
			// merge ontology with its imports
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			manager.loadOntologyFromOntologyDocument(file);
			OWLOntologyMerger merger = new OWLOntologyMerger(manager);
			OWLOntology ontology = merger.createMergedOntology(OWLManager.createOWLOntologyManager(), iri);

			// only keep axioms adhering to Horn-ALC
			ontology.remove(ontology.axioms().filter(ax -> !ax.accept(new HornALCAxiomVisitor())));
			// save merged ontology in functional syntax
			manager.saveOntology(ontology, new FunctionalSyntaxDocumentFormat(), iri);

			Path path = Paths.get(new_file.getAbsoluteFile().toURI());
			List<String> old_lines = Files.readAllLines(path);
			List<String> new_lines = new LinkedList<>();
			for (String line : old_lines) {
				// remove axioms not supported by Horn-ALC
//				if ((line.startsWith("Prefix(") || line.startsWith("Ontology(") || line.startsWith("Declaration(")
//						|| line.startsWith("SubClassOf(") || line.startsWith("EquivalentClasses(")
//						|| line.startsWith("ClassAssertion(") || line.startsWith("ObjectPropertyAssertion("))
//						&& !line.contains("DataSome") && !line.contains("DataAll") && !line.contains("HasValue")
//						&& !line.contains("HasSelf") && !line.contains("ObjectUnionOf")
//						&& !line.contains("Cardinality(") && !line.contains("OneOf(")) {
//					new_lines.add(line);
//				}
				// remove comments and empty lines
				if (!(line.isEmpty() || line.startsWith("#"))) {
					new_lines.add(line);
				}

			}
//			new_lines.add(")");
			Files.write(path, new_lines);

			return manager.loadOntologyFromOntologyDocument(path.toFile());

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

}
