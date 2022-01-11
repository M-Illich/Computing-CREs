package com.ai.cre.representation;

import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

import com.ai.cre.ontology.StringConverter;

/**
 * Represents a part {@code D ⊓ ∃R⎺.(} of a concept referring expression based
 * on a restriction {@code ∃R.D}.
 */
public class ConRefExpPart {

	/**
	 * Existential restriction relating {@link #base_concept} to another concept;
	 * responsible for {@code D ⊓ ∃R⎺} in referring expression component
	 * {@code D ⊓ ∃R⎺.(C}
	 */
	public OWLObjectSomeValuesFrom exist_restriction;

	/**
	 * String that is either empty or states the numbers (separated by '.') of the
	 * cycles for which the component marks the left end, e.g. in form of
	 * {@code [^1.2.3 D ⊓ ∃R⎺.(...}
	 */
	public String LeftCycleEnd;

	/**
	 * String that is either empty or states the numbers (separated by '.') of the
	 * cycles for which the component marks the right end, e.g. in form of
	 * {@code ... ∃R⎺.(]ᐩ^1.2.3 ...}
	 */
	public String RightCycleEnd;

	public ConRefExpPart(OWLObjectSomeValuesFrom exist_rest) {
		this.exist_restriction = exist_rest;
		this.LeftCycleEnd = "";
		this.RightCycleEnd = "";
	}

	/**
	 * Get String version of {@link #exist_restriction} as part of referring
	 * expression including cycle-notation
	 * <p>
	 * Example: Given {@link #exist_restriction} = {@code ∃R.D},
	 * {@link #LeftCycleEnd} = {@code 1} and {@link #RightCycleEnd} = {@code 2}, the
	 * resulting String would be {@code "[^1 D ⊓ ∃R⎺.(]ᐩ^2 "}
	 * </p>
	 * 
	 * @return A {@link String} of the form "D ⊓ ∃R⎺.(" (plus cycle-notation if
	 *         necessary)
	 */
	public String getExistRestString() {
		// get String versions
		String restString = this.exist_restriction.accept(new StringConverter());
		// change restriction "∃R.D" to form "D ⊓ ∃R⎺."
		int index_dot = restString.indexOf(".");
		// remove surrounding parentheses of related concept D if present
		String rel_concept = restString.substring(index_dot + 1);
		if (rel_concept.startsWith("(")) {
			rel_concept = rel_concept.substring(1, rel_concept.length() - 1);
		}
		String changed_restString = rel_concept + " ⊓ " + restString.substring(0, index_dot) + "⎺.(";
		// add cycle-notation if necessary
		if (!this.LeftCycleEnd.isEmpty()) {
			changed_restString = "[^" + LeftCycleEnd + " " + changed_restString;
		}
		if (!this.RightCycleEnd.isEmpty()) {
			changed_restString = changed_restString + "]ᐩ^" + RightCycleEnd + " ";
		}
		return changed_restString;

	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ConRefExpPart) {
			ConRefExpPart oCon = (ConRefExpPart) o;
			return this.exist_restriction.equals(oCon.exist_restriction);
		} else {
			return false;
		}
	}

}
