package cmu.varviz.trace.view.actions;

import static cmu.conditional.Conditional.and;
import static cmu.conditional.Conditional.getCTXString;
import static cmu.conditional.Conditional.isSatisfiable;
import static cmu.conditional.Conditional.not;
import static cmu.conditional.Conditional.simplifyCondition;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import cmu.conditional.Conditional;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.filters.And;
import cmu.varviz.trace.filters.InteractionFilter;
import cmu.varviz.trace.filters.Or;
import cmu.varviz.trace.generator.TraceGenerator;
import cmu.varviz.trace.view.VarvizView;
import cmu.vatrace.ExceptionFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import scala.collection.Iterator;

/**
 * Set all features that do not matter for the given expression to fixed values.
 *  
 * @author Jens Meinicke
 *
 */
public class Slicer {

	/**
	 * tries to set all features that do not matter for the given expression to fixed values.
	 * @return returns the new constraint with the fixed features.
	 */
	public static FeatureExpr sliceContext(final FeatureExpr ctx) {
		if (Conditional.isContradiction(ctx)) {
			return FeatureExprFactory.True();
		}
		Set<String> includedFeatures = new HashSet<>();
		scala.collection.immutable.Set<SingleFeatureExpr> distinctfeatureObjects = ctx.collectDistinctFeatureObjects();
		Iterator<SingleFeatureExpr> iterator = distinctfeatureObjects.iterator();
		while (iterator.hasNext()) {
			includedFeatures.add(getCTXString(iterator.next()));
		}

		// select the features of the exception
		// check whether the other features can be (de)selected
		final TraceGenerator generator = VarvizView.generator;
		generator.clearIgnoredFeatures();

		FeatureExpr ctxcheck = simplifyCondition(ctx);
		FeatureExpr constraint = FeatureExprFactory.True();
		for (Entry<String, SingleFeatureExpr> feature : generator.getFeatures().entrySet()) {
			if (!includedFeatures.contains(getCTXString(feature.getValue()))) {
				if (checkSat(constraint, ctx, not(feature.getValue()))) {
					generator.getIgnoredFeatures().put(feature.getValue(), false);
					ctxcheck = ctxcheck.andNot(feature.getValue());
					constraint = constraint.andNot(feature.getValue());
					} else if (checkSat(constraint, ctx, feature.getValue())) {
					generator.getIgnoredFeatures().put(feature.getValue(), true);
					ctxcheck = ctxcheck.and(feature.getValue());
					constraint = constraint.and(feature.getValue());
				}
			}
		}
		constraint= simplifyCondition(constraint);
		Conditional.additionalConstraint = constraint;
		return constraint;	
	}

	private static boolean checkSat(FeatureExpr constraint, FeatureExpr ctx, FeatureExpr value) {
		FeatureExpr constraintAndException = and(constraint, ctx); 
		FeatureExpr constraintAndNotException = and(constraint, not(ctx));
		boolean result = isSatisfiable(and(constraintAndException,value)) && isSatisfiable(and(constraintAndNotException, value));
		

		// check if context features are still conditional (i.e., they can be true and false)
		scala.collection.immutable.Set<SingleFeatureExpr> distinctfeatureObjects = ctx.collectDistinctFeatureObjects();
		Iterator<SingleFeatureExpr> iterator = distinctfeatureObjects.iterator();
		while (iterator.hasNext()) {
			FeatureExpr feature = iterator.next();
			if (!isSatisfiable(constraint.and(value).and(feature))) {
				return false;
			}
			if (!isSatisfiable(constraint.and(value).andNot(feature))) {
				return false;
			}
		}
		return result;
	}
	
	/**
	 * Slices the given  {@link Trace} for the exception.
	 * 
	 */
	public static void sliceForExceptiuon(Trace trace) {
		FeatureExpr exceptionContext = trace.getExceptionContext();
		exceptionContext = Conditional.simplifyCondition(exceptionContext);
		
		FeatureExpr constraint = Slicer.sliceContext(exceptionContext);
		trace.getMain().simplify(constraint, new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter()));
		System.out.println("slice trace : "+ Conditional.getCTXString(constraint));
		
		final TraceGenerator generator = VarvizView.generator;
		for (Entry<FeatureExpr, Boolean> entry : generator.getIgnoredFeatures().entrySet()) {
			System.out.println("set feature " + getCTXString(entry.getKey(), false) + " to " + entry.getValue());
		}
	}

}
