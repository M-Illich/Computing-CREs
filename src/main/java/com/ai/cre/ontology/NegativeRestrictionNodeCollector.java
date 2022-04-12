package com.ai.cre.ontology;

import java.util.Iterator;

import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

/**
 * An implementation of {@link OWLClassExpressionVisitorEx} to collect both
 * existential and universal restrictions from {@link OWLCLassExpression}
 * objects in form of {@link ConceptNode} objects that appear negatively on the
 * left-hand side of axioms. Counterpart to
 * {@link PositiveRestrictionNodeCollector}.
 *
 */
public class NegativeRestrictionNodeCollector implements OWLClassExpressionVisitorEx<RestrictionNodeCollection> {

	OntologyHandler ontologyHandler;

	public NegativeRestrictionNodeCollector(OntologyHandler ontologyHandler) {
		this.ontologyHandler = ontologyHandler;
	}

	@Override
	public RestrictionNodeCollection visit(OWLObjectComplementOf ce) {
		// switch visitor for negated concept
		return ce.getOperand().accept(new PositiveRestrictionNodeCollector(ontologyHandler));

	}

	@Override
	public RestrictionNodeCollection visit(OWLObjectIntersectionOf ce) {
		// collection to combine restrictions from each conjunct
		RestrictionNodeCollection rest_col = new RestrictionNodeCollection(ontologyHandler);

		// get iterator for collections returned from recursive call for each conjunct
		Iterator<RestrictionNodeCollection> iterator = ce.operands().map(op -> op.accept(this)).iterator();
		// store found restrictions in collection
		while (iterator.hasNext()) {
			RestrictionNodeCollection current = iterator.next();
			if (current != null) {
				rest_col.mergeWith(current);
			}
		}

		return rest_col;
	}

	@Override
	public RestrictionNodeCollection visit(OWLObjectSomeValuesFrom ce) {
		RestrictionNodeCollection rest_col = new RestrictionNodeCollection(ontologyHandler);
		// add normalized negated restriction: ∃R.D --> ∀R.(¬D)
		rest_col.addUnivRestriction((OWLObjectAllValuesFrom) ce.getComplementNNF());
		// call related concept with positive collector
		rest_col.mergeWith(ce.getFiller().accept(this));
		return rest_col;

	}

	@Override
	public RestrictionNodeCollection visit(OWLObjectAllValuesFrom ce) {
		RestrictionNodeCollection rest_col = new RestrictionNodeCollection(ontologyHandler);
		// add normalized negated restriction: ∀R.D --> ∃R.(¬D)
		rest_col.addExistRestriction((OWLObjectSomeValuesFrom) ce.getComplementNNF());
		// call related concept with positive collector
		rest_col.mergeWith(ce.getFiller().accept(this));
		return rest_col;

	}

}
