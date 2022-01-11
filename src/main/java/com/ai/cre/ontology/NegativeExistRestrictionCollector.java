package com.ai.cre.ontology;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

/**
 * An implementation of {@link OWLClassExpressionVisitorEx} to collect
 * existential restrictions from {@link OWLCLassExpression} objects that appear
 * negatively on the left-hand side of axioms. Counterpart to
 * {@link PositiveExistRestrictionCollector}.
 *
 */
public class NegativeExistRestrictionCollector implements OWLClassExpressionVisitorEx<Set<OWLObjectSomeValuesFrom>> {

	@Override
	public Set<OWLObjectSomeValuesFrom> visit(OWLObjectComplementOf ce) {
		// switch visitor for negated concept
		return ce.getOperand().accept(new PositiveExistRestrictionCollector());
	}

	@Override
	public Set<OWLObjectSomeValuesFrom> visit(OWLObjectIntersectionOf ce) {
		// set to collect restrictions from each conjunct
		Set<OWLObjectSomeValuesFrom> rest_set = new HashSet<>();
		// get iterator for sets returned by recursive call for each conjunct
		Iterator<Set<OWLObjectSomeValuesFrom>> iterator = ce.operands().map(op -> op.accept(this)).iterator();
		// store found restrictions in collection
		while (iterator.hasNext()) {
			Set<OWLObjectSomeValuesFrom> current = iterator.next();
			if (current != null) {
				rest_set.addAll(current);
			}
		}
		return rest_set;
	}

	@Override
	public Set<OWLObjectSomeValuesFrom> visit(OWLObjectAllValuesFrom ce) {
		Set<OWLObjectSomeValuesFrom> rest_set = new HashSet<>();
		// transform universal restriction to existential one based on negated normal
		// form
		rest_set.add((OWLObjectSomeValuesFrom) ce.getComplementNNF());
		// look for nested existential restrictions
		Set<OWLObjectSomeValuesFrom> nested = ce.getFiller().accept(this);
		if (nested != null) {
			rest_set.addAll(nested);
		}
		return rest_set;
	}

}
