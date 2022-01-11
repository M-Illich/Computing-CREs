package com.ai.cre.representation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

import com.ai.cre.ontology.StringConverter;

/**
 * An (incomplete) concept referring expression still being processed that is
 * represented by means of a {@link LinkedList} of {@link ConRefExpPart} objects
 * with possible cycle-notation and a set of base individuals, thus enabling the
 * construction of completed {@link ConceptReferringExpression} instances.
 *
 */
public class ProcessConRefExpression {
	/**
	 * A {@link LinkedList} of {@link ConRefExpPart} objects representing the parts
	 * to construct a concept referring expression
	 */
	public LinkedList<ConRefExpPart> refExpParts;

	/**
	 * A non-empty {@link Set} of {@link OWLIndividual} elements representing
	 * individuals each serving as potential starting point in form of an nominal
	 * '{a}'
	 */
	public Set<OWLIndividual> baseIndividuals;

	/**
	 * Number of cycles included in current referring expression
	 */
	private int cycleNumber;

	/**
	 * Initialize a {@link ProcessConRefExpression} object with
	 * {@link #baseIndividuals} = {@code inds}
	 * 
	 * @param inds A non-empty {@link Set} of {@link OWLIndividual} elements
	 *             representing individuals
	 */
	public ProcessConRefExpression(Set<OWLIndividual> inds) {
		this.refExpParts = new LinkedList<ConRefExpPart>();
		this.baseIndividuals = inds;
		this.cycleNumber = 0;
	}

	/**
	 * Initialize a {@link ProcessConRefExpression} object with {@link #refExpParts}
	 * = {@code parts}, {@link #baseIndividuals} = {@code inds} and
	 * {@link #cycleNumber} = {@code cycleNum}
	 * 
	 * @param inds     A non-empty {@link Set} of {@link OWLIndividual} elements
	 *                 representing individuals
	 * @param parts    A {@link LinkedList} of {@link ConRefExpPart} objects
	 * @param cycleNum A non-negative {@code int}
	 */
	protected ProcessConRefExpression(Set<OWLIndividual> inds, LinkedList<ConRefExpPart> parts, int cycleNum) {
		this.refExpParts = parts;
		this.baseIndividuals = inds;
		this.cycleNumber = cycleNum;
	}

	/**
	 * Extend the left side of the currently constructed referring expression by a
	 * new {@link ConRefExpPart} based on an existential restriction
	 * 
	 * @param exist_rest A {@link OWLObjectSomeValuesFrom} object representing the
	 *                   existential restriction that was chosen to relate the
	 *                   current concept to another concept
	 * @return A {@link ProcessConRefExpression} as copy of the calling object with
	 *         extended {@link #refExpParts}
	 */
	public ProcessConRefExpression getExtended(OWLObjectSomeValuesFrom exist_rest) {
		LinkedList<ConRefExpPart> parts = new LinkedList<>(this.refExpParts);
		parts.add(0, new ConRefExpPart(exist_rest));

		return new ProcessConRefExpression(this.baseIndividuals, parts, this.cycleNumber);
	}

	/**
	 * Mark a cycle in the referring expression for which the right end is indicated
	 * by a given concept and its applied restriction, while the left end is given
	 * by the current referring expression's left end
	 * 
	 * @param used_part A {@link ConRefExpPart} object
	 */
	public void markCycle(ConRefExpPart used_part) {
		// find used part of referring expression
		int index = this.refExpParts.indexOf(used_part);
		if (index > -1) {
			// mark used part as right end of cycle
			ConRefExpPart rightPart = refExpParts.get(index);
			if (rightPart.RightCycleEnd.isEmpty()) {
				rightPart.RightCycleEnd = "" + cycleNumber;
			}
			// part already used in another cycle
			else {
				rightPart.RightCycleEnd = rightPart.RightCycleEnd + "." + cycleNumber;
			}
			// mark current left end as left end of cycle
			ConRefExpPart leftPart = refExpParts.getFirst();
			if (leftPart.LeftCycleEnd.isEmpty()) {
				leftPart.LeftCycleEnd = "" + cycleNumber;
			}
			// part already used in another cycle
			else {
				leftPart.LeftCycleEnd = leftPart.LeftCycleEnd + "." + cycleNumber;
			}
			// increase number of cycles
			cycleNumber++;
		}

	}

	/**
	 * Complete the referring expression by appending a nominal <code>{a}</code> to
	 * the right end of {@link #refExpString} for each individual {@code a} from
	 * {@link #baseIndividuals} resulting in different referring expressions
	 * realized by {@link ConceptReferringExpression} objects, each for one
	 * individual
	 * 
	 * @return A {@link Collection} of {@link ConceptReferringExpression} objects
	 *         each representing a completed version of the calling
	 *         {@link ProcessConRefExpression}
	 */
	public Collection<ConceptReferringExpression> complete() {
		ArrayList<ConceptReferringExpression> completedRefExp = new ArrayList<ConceptReferringExpression>();

		Iterator<ConRefExpPart> partIterator = refExpParts.iterator();
		// String version of referring expression parts
		String refExpString = "";
		// concluding parentheses
		String parentheses = "";
		// String to store existential restriction of previously used part
		String previous_ex_rest = null;
		// go through parts from left to right (most to least recent)
		while (partIterator.hasNext()) {
			ConRefExpPart nextPart = partIterator.next();
			String nextString = nextPart.getExistRestString();
			// remove existential restrictions that appear as conjunct in next part if they
			// are equal to the one from the previous part
			if (previous_ex_rest != null) {
				nextString = nextString.replaceAll(previous_ex_rest + " ⊓ ", "");
			}

			refExpString = refExpString + nextString;
			parentheses = parentheses + ")";
			previous_ex_rest = nextPart.exist_restriction.accept(new StringConverter());
		}

		// create different versions of concept referring expression based on nominal
		// for each individual
		for (OWLIndividual ind : baseIndividuals) {
			completedRefExp.add(new ConceptReferringExpression(
					createCombinedRefExpString(refExpString, ind, parentheses), cycleNumber));
			// NOTE: parentheses around nominal not really necessary, but better for
			// cycle-notation
		}

		return completedRefExp;
	}

	/**
	 * Create the {@link String} representation for the completed referring
	 * expression or if none was constructed, only the given individual
	 * 
	 * @param refExpString A {@link String} representing the constructed referring
	 *                     expression without a nominal for the base individual
	 * @param ind          An {@link OWLIndividual} instance representing the base
	 *                     individual
	 * @param parentheses  A {@link String} consisting of all the closing
	 *                     parentheses that should follow the nominal
	 * @return A {@link String} of the form {@code refExpString} + nominal of
	 *         {@code ind} + {@code parentheses} or if {@code refExpString} is
	 *         empty, only the {@link String} representation of {@code ind}
	 */
	private String createCombinedRefExpString(String refExpString, OWLIndividual ind, String parentheses) {
		if (refExpString.isEmpty()) {
			return ind.asOWLNamedIndividual().getIRI().getRemainder().get();
		} else {
			return refExpString + "{" + ind.asOWLNamedIndividual().getIRI().getRemainder().get() + "}" + parentheses;
		}
	}

	/**
	 * Look for the first appearing {@link ConRefExpPart} element of
	 * {@link #refExpParts} that has {@code ex_rest} as
	 * {@link ConRefExpPart#exist_restriction}
	 * 
	 * @param ex_rest A {@link OWLObjectSomeValuesFrom} object
	 * @return The found {@link ConRefExpPart} element or {@code null} if none could
	 *         be found
	 */
	public ConRefExpPart findPart(OWLObjectSomeValuesFrom ex_rest) {
		Iterator<ConRefExpPart> iter = this.refExpParts.iterator();
		ConRefExpPart part = null;
		while (iter.hasNext()) {
			part = iter.next();
			if (part.exist_restriction.equals(ex_rest)) {
				return part;
			}
		}
		return null;
	}

}
