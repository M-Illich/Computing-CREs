package com.ai.cre.ontology;

import java.util.TreeSet;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

/**
 * An implementation of {@link OWLClassExpressionVisitorEx} that creates a
 * {@link String} representation for {@link OWLClassExpression} objects
 * consisting of {@link OWLClass}, {@link OWLObjectIntersectionOf} with sorted
 * conjuncts, {@link OWLObjectComplementOf}, {@link OWLObjectSomeValuesFrom} or
 * {@link OWLObjectAllValuesFrom} instances
 *
 */
public class StringConverter implements OWLClassExpressionVisitorEx<String> {

	@Override
	public String visit(OWLClass ce) {
		return ce.getIRI().getRemainder().get();
	}

	@Override
	public String visit(OWLObjectIntersectionOf ce) {
		// get sorted set of strings for conjuncts
		TreeSet<String> operands = new TreeSet<>(
				ce.operands().map(conjunct -> conjunct.accept(this)).collect(Collectors.toSet()));
		return operands.stream().collect(Collectors.joining(" ⊓ "));
	}

	@Override
	public String visit(OWLObjectComplementOf ce) {
		return "¬" + ce.getOperand().accept(new StringConverterWithParentheses());
	}

	@Override
	public String visit(OWLObjectSomeValuesFrom ce) {
		return "∃" + ce.getProperty().getNamedProperty().getIRI().getRemainder().get() + "."
				+ ce.getFiller().accept(new StringConverterWithParentheses());
	}

	@Override
	public String visit(OWLObjectAllValuesFrom ce) {
		return "∀" + ce.getProperty().getNamedProperty().getIRI().getRemainder().get() + "."
				+ ce.getFiller().accept(new StringConverterWithParentheses());
	}

	/**
	 * A subclass of {@link StringConverter} that puts parentheses around nested
	 * complex concepts
	 *
	 */
	private class StringConverterWithParentheses extends StringConverter {
		@Override
		public String visit(OWLObjectIntersectionOf ce) {
			// get sorted set of strings for conjuncts
			TreeSet<String> operands = new TreeSet<>(
					ce.operands().map(conjunct -> conjunct.accept(new StringConverter())).collect(Collectors.toSet()));
			if (operands.size() == 1) {
				return operands.first();
			} else {
				return "(" + operands.stream().collect(Collectors.joining(" ⊓ ")) + ")";
			}
		}

		@Override
		public String visit(OWLObjectComplementOf ce) {
			return "(¬" + ce.getOperand().accept(this) + ")";
		}

		@Override
		public String visit(OWLObjectSomeValuesFrom ce) {
			return "(∃" + ce.getProperty().getNamedProperty().getIRI().getRemainder().get() + "."
					+ ce.getFiller().accept(this) + ")";
		}

		@Override
		public String visit(OWLObjectAllValuesFrom ce) {
			return "(∀" + ce.getProperty().getNamedProperty().getIRI().getRemainder().get() + "."
					+ ce.getFiller().accept(this) + ")";
		}
	}

}
