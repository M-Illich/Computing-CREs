package com.ai.cre.ontology;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.HornAxiomVisitorEx;

/**
 * Returns {@code true} if the visited axioms adhere to Horn-ALC. (Similar to
 * {@link HornAxiomVisitorEx})
 *
 */
public class HornALCAxiomVisitor implements OWLAxiomVisitorEx<Boolean> {

	final private LeftSideVisitor leftSideVisitor = new LeftSideVisitor();
	final private RightSideVisitor rightSideVisitor = new RightSideVisitor();

	protected boolean checkLeft(OWLClassExpression c) {
		return c.accept(leftSideVisitor).booleanValue();
	}

	protected boolean checkRight(OWLClassExpression c) {
		return c.accept(rightSideVisitor).booleanValue();
	}

	@Override
	public Boolean doDefault(Object object) {
		return Boolean.FALSE;
	}

	@Override
	public Boolean visit(OWLDeclarationAxiom axiom) {
		return Boolean.TRUE;
	}

	@Override
	public Boolean visit(OWLClassAssertionAxiom axiom) {
		return Boolean.TRUE;
	}

	@Override
	public Boolean visit(OWLObjectPropertyAssertionAxiom axiom) {
		return Boolean.TRUE;
	}

	@Override
	public Boolean visit(OWLSubClassOfAxiom axiom) {
		return Boolean.valueOf(checkLeftNegation(axiom.getSubClass(), axiom.getSuperClass())
				&& checkLeft(axiom.getSubClass()) && checkRight(axiom.getSuperClass()));
	}

	@Override
	public Boolean visit(OWLEquivalentClassesAxiom axiom) {
		return (axiom.classExpressions()
				.filter(c -> c.getClassExpressionType().equals(ClassExpressionType.OBJECT_COMPLEMENT_OF)).count() != 1
				&& !axiom.classExpressions().anyMatch(c -> !(checkLeft(c) && checkRight(c))));
	}

	/**
	 * If left-hand side of axiom is negative, the right-hand side must be, too.
	 */
	private boolean checkLeftNegation(OWLClassExpression subClass, OWLClassExpression superClass) {
		if (subClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_COMPLEMENT_OF)) {
			return superClass.getClassExpressionType().equals(ClassExpressionType.OBJECT_COMPLEMENT_OF);
		}
		return true;
	}

	private class RightSideVisitor implements OWLClassExpressionVisitorEx<Boolean> {
		@Override
		public Boolean doDefault(Object object) {
			return Boolean.FALSE;
		}

		@Override
		public Boolean visit(OWLClass ce) {
			return Boolean.TRUE;
		}

		@Override
		public Boolean visit(OWLObjectIntersectionOf ce) {
			return Boolean.valueOf(!ce.operands().anyMatch(c -> c.accept(this) == Boolean.FALSE));
		}

		@Override
		public Boolean visit(OWLObjectComplementOf ce) {
			return Boolean.valueOf(checkLeft(ce.getOperand()));
		}

		@Override
		public Boolean visit(OWLObjectSomeValuesFrom ce) {
			return ce.getFiller().accept(this);
		}

		@Override
		public Boolean visit(OWLObjectAllValuesFrom ce) {
			return ce.getFiller().accept(this);
		}

	}

	private class LeftSideVisitor extends RightSideVisitor {

		@Override
		public Boolean visit(OWLObjectUnionOf ce) {
			return Boolean.valueOf(!ce.operands().anyMatch(c -> c.accept(this) == Boolean.FALSE));
		}

		@Override
		public Boolean visit(OWLObjectComplementOf ce) {
			return Boolean.valueOf(checkRight(ce.getOperand()));
		}

	}

}
