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
 * objects in form of {@link ConceptNode} objects that appear positively on the
 * right-hand side of axioms. Counterpart to
 * {@link NegativeRestrictionNodeCollector}.
 *
 */
public class PositiveRestrictionNodeCollector implements OWLClassExpressionVisitorEx<RestrictionNodeCollection> {
	
	OntologyHandler ontologyHandler;
	
	public PositiveRestrictionNodeCollector(OntologyHandler ontologyHandler) {
		this.ontologyHandler = ontologyHandler;
	}

	@Override
	public RestrictionNodeCollection visit(OWLObjectComplementOf ce) {
		// switch visitor for negated concept
		return ce.getOperand().accept(new NegativeRestrictionNodeCollector(ontologyHandler));

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
		rest_col.addExistRestriction(ce);
		rest_col.mergeWith(ce.getFiller().accept(this));
		return rest_col;

	}

	@Override
	public RestrictionNodeCollection visit(OWLObjectAllValuesFrom ce) {
		RestrictionNodeCollection rest_col = new RestrictionNodeCollection(ontologyHandler);
		rest_col.addUnivRestriction(ce);
		rest_col.mergeWith(ce.getFiller().accept(this));
		return rest_col;

	}

}
