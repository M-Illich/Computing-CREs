package com.ai.cre.representation;

/**
 * A completed concept referring expression in form of a {@link String} with
 * possible cycle-notation constructed by means of a
 * {@link ProcessConRefExpression} instance.
 * <p>
 * Example: "F ⊓ ∃R⎺.([^1 C ⊓ ∃S⎺.(]ᐩ^1 D ⊓ ∃R⎺.{a}))" with cycle around subsequence
 * "C ⊓ ∃S⎺.("
 * </p>
 *
 */
public class ConceptReferringExpression {

	/**
	 * A {@link String} representation of the concept referring expression,
	 * including possible cycle-notation '[ ... ]*'
	 */
	private String refExpString;

	/**
	 * Number of cycles included in current referring expression
	 */
	private int cycleNumber;

	public ConceptReferringExpression(String refExpString, int cycleCount) {
		this.refExpString = refExpString;
		this.cycleNumber = cycleCount;
	}

	/**
	 * Get the referring expression represented as {@link String}
	 * 
	 * @return The String given by {@link #refExpString}
	 */
	public String getString() {
		return refExpString;
	}

	/**
	 * Get the number of cycles included in the referring expression
	 * 
	 * @return An {@code int} value representing the number of cycles
	 */
	public int getCycleNumber() {
		return cycleNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ConceptReferringExpression) {
			ConceptReferringExpression oCon = (ConceptReferringExpression) o;
			return this.refExpString.equals(oCon.getString());
		} else {
			return false;
		}
	}

}
