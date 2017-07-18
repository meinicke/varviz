package cmu.varviz.trace;

import java.util.Collection;

import cmu.conditional.Conditional;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;

public class Slicer {

	public enum State {
		UNKNOWN, SELECTED, DESELECTED, CONDITIONAL
	}

	/**
	 * Slice for feature A Checks the selection of feature B
	 */
	public static State slice(Trace trace, SingleFeatureExpr FA, SingleFeatureExpr FB) {
		return slice(trace.getMain().collectIFStatements(), FA, FB);
	}

	/**
	 * Slice for the given features checks the selection of feature B
	 */
	public static State slice(Collection<IFStatement<?>> IFStatements, Collection<SingleFeatureExpr> sliceCondition,
			final SingleFeatureExpr FB) {
		State fbSelection = State.UNKNOWN;
		for (SingleFeatureExpr FA : sliceCondition) {
			fbSelection = slice(IFStatements, FA, FB, fbSelection);
		}
		return fbSelection;
	}

	/**
	 * Slice for feature A Checks the selection of feature B
	 */
	public static State slice(Collection<IFStatement<?>> IFStatements, SingleFeatureExpr FA, SingleFeatureExpr FB) {
		return slice(IFStatements, FA, FB, State.UNKNOWN);
	}

	/**
	 * Slice for feature A Checks the selection of feature B
	 */
	public static State slice(Collection<IFStatement<?>> IFStatements, SingleFeatureExpr FA, SingleFeatureExpr FB,
			State fbSelection) {
		if (fbSelection == State.CONDITIONAL) {
			return State.CONDITIONAL;
		}
		if (FA == FB) {
			return State.CONDITIONAL;
		}

		for (IFStatement<?> ifStatement : IFStatements) {
			FeatureExpr decision = Conditional.simplifyCondition(ifStatement.getTargetContext(), ifStatement.getCTX());
			if (Conditional.isSatisfiable(decision.unique(FA))) {
				// if depends on A
				if (Conditional.isSatisfiable(decision.unique(FB))) {
					// if depends on B
					if (Conditional.isSatisfiable(decision.andNot(FB))) {
						if (fbSelection == State.UNKNOWN) {
							fbSelection = State.DESELECTED;
						} else if (fbSelection == State.SELECTED) {
							fbSelection = State.CONDITIONAL;
							break;
						}
					} else if (Conditional.isSatisfiable(decision.and(FB))) {
						if (fbSelection == State.UNKNOWN) {
							fbSelection = State.SELECTED;
						} else if (fbSelection == State.DESELECTED) {
							fbSelection = State.CONDITIONAL;
							break;
						}
					}
					continue;
				} else {
					// if depends NOT on FB
					FeatureExpr ctx = ifStatement.getCTX();
					if (Conditional.isContradiction(ctx.unique(FA))) {
						// ctx does not depend on A
						if (Conditional.isSatisfiable(ctx.andNot(FB))) {
							// set B to false
							if (fbSelection == State.UNKNOWN) {
								fbSelection = State.DESELECTED;
							} else if (fbSelection == State.SELECTED) {
								fbSelection = State.CONDITIONAL;
								break;
							}
						} else if (Conditional.isSatisfiable(ctx.and(FB))) {
							// set B to true
							if (fbSelection == State.UNKNOWN) {
								fbSelection = State.SELECTED;
							} else if (fbSelection == State.DESELECTED) {
								fbSelection = State.CONDITIONAL;
								break;
							}
						}
					}
				}
			}
		}
		return fbSelection;
	}
}
