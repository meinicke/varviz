package cmu.varviz.trace.view.actions;

import static cmu.conditional.Conditional.and;
import static cmu.conditional.Conditional.getCTXString;
import static cmu.conditional.Conditional.isSatisfiable;
import static cmu.conditional.Conditional.not;
import static cmu.conditional.Conditional.simplifyCondition;

import java.util.HashSet;
import java.util.Map;
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
public class Projector {
	
	private Projector() {
		// nothing here
	}

	/**
	 * tries to set all features that do not matter for the given expression to fixed values.
	 * @return returns the new constraint with the fixed features.
	 */
	public static FeatureExpr projectOnContext(final FeatureExpr ctx, TraceGenerator generator) {
		if (Conditional.isContradiction(ctx)) {
			return FeatureExprFactory.True();
		}
		scala.collection.immutable.Set<SingleFeatureExpr> distinctfeatureObjects = ctx.collectDistinctFeatureObjects();
		Set<SingleFeatureExpr> projectionFeatures = new HashSet<>(distinctfeatureObjects.size());
		Iterator<SingleFeatureExpr> iterator = distinctfeatureObjects.iterator();
		while (iterator.hasNext()) {
			projectionFeatures.add(iterator.next());
		}
		return projectFeatures(projectionFeatures, generator);
	}
	
	/**
	 * tries to set all features that do not matter for the given expression to fixed values.
	 * @return returns the new constraint with the fixed features.
	 */
	public static FeatureExpr projectFeatures(final Set<SingleFeatureExpr> projectionFeatures, TraceGenerator generator) {
		System.out.print("project Trace for :");
		for (SingleFeatureExpr singleFeatureExpr : projectionFeatures) {
			System.out.print(getCTXString(singleFeatureExpr, false)+ ", ");
		}
		System.out.println();

		// select the features of the exception
		// check whether the other features can be (de)selected
		generator.clearIgnoredFeatures();

		FeatureExpr constraint = FeatureExprFactory.True();
		for (Entry<String, SingleFeatureExpr> feature : generator.getFeatures().entrySet()) {
			if (!projectionFeatures.contains(feature.getValue())) {
				if (checkSat(constraint, projectionFeatures, not(feature.getValue()))) {
					generator.getIgnoredFeatures().put(feature.getValue(), Boolean.FALSE);
					constraint = constraint.andNot(feature.getValue());
					} else if (checkSat(constraint, projectionFeatures, feature.getValue())) {
					generator.getIgnoredFeatures().put(feature.getValue(), Boolean.TRUE);
					constraint = constraint.and(feature.getValue());
				}
			}
		}
		constraint= simplifyCondition(constraint);
		Conditional.additionalConstraint = constraint;
		return constraint;	
	}

	/**
	 * Checks if the projection features can all be selected and unselected for the given constraint.
	 * @param constraint The constraint 
	 * @param projectionFeatures the features we project for
	 * @param featue A feature (can be selected or unselected); 
	 * @return
	 */
	private static boolean checkSat(FeatureExpr constraint, Set<SingleFeatureExpr> projectionFeatures, FeatureExpr featue) {
		if (!isSatisfiable(and(constraint, featue))) {
			return false;
		}
		// check if context features are still conditional (i.e., they can be true and false)
		for (SingleFeatureExpr projectionfeature : projectionFeatures) {
			if (!isSatisfiable(and(and(constraint, featue), projectionfeature))) {
				return false;
			}
			if (!isSatisfiable(and(and(constraint, featue), not(projectionfeature)))) {
				return false;
			}
		}
		return true;
	}
	
	public static void projectForFeatures(Trace trace, Map<SingleFeatureExpr, Boolean> featureSelection, TraceGenerator generator) {
		long start = System.currentTimeMillis();
		FeatureExpr constraint = FeatureExprFactory.True();
		
		for (Entry<SingleFeatureExpr, Boolean> entry : featureSelection.entrySet()) {
			if (entry.getValue()) {
				constraint = and(constraint, entry.getKey());
			} else {
				constraint = and(constraint, not(entry.getKey()));
			}
		}
		if (Conditional.isTautology(constraint)) {
			return;
		}
		if (!Conditional.isSatisfiable(constraint)) {
			return;
		}
		trace.getMain().simplify(constraint, new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.MIN_INTERACTION_DEGREE)), new ExceptionFilter()));
		long end= System.currentTimeMillis();
		System.out.println((end - start) + "ms");
		System.out.println("constraint:" + Conditional.getCTXString(constraint));
		
		for (Entry<FeatureExpr, Boolean> entry : generator.getIgnoredFeatures().entrySet()) {
			System.out.println("set feature " + getCTXString(entry.getKey(), false) + " to " + entry.getValue());
		}
	}
	
	/**
	 * Sets the given constrint.
	 * 
	 */
	public static void projectionForConstraint(Trace trace, FeatureExpr constraint, TraceGenerator generator) {
		Conditional.additionalConstraint = and(Conditional.additionalConstraint, constraint);
		System.out.println("set Constraint: "+ Conditional.additionalConstraint);
		
		long start = System.currentTimeMillis();
		trace.getMain().simplify(constraint, new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.MIN_INTERACTION_DEGREE)), new ExceptionFilter()));
		long end= System.currentTimeMillis();
		System.out.println((end - start) + "ms");
		System.out.println("constraint:" + Conditional.getCTXString(constraint));
		
		for (Entry<FeatureExpr, Boolean> entry : generator.getIgnoredFeatures().entrySet()) {
			System.out.println("set feature " + getCTXString(entry.getKey(), false) + " to " + entry.getValue());
		}
	}
	
	/**
	 * Projects the given  {@link Trace} for the exception.
	 * @param traceGenerator 
	 * 
	 */
	public static void projectionForExceptiuon(Trace trace, TraceGenerator generator) {
		long start = System.currentTimeMillis();
		FeatureExpr exceptionContext = trace.getExceptionContext();
		exceptionContext = Conditional.simplifyCondition(exceptionContext);
		
		FeatureExpr constraint = Projector.projectOnContext(exceptionContext, generator);
		if (Conditional.isTautology(constraint)) {
			return;
		}
		trace.getMain().simplify(constraint, new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.MIN_INTERACTION_DEGREE)), new ExceptionFilter()));
		long end= System.currentTimeMillis();
		System.out.println((end - start) + "ms");
		System.out.println("constraint:" + Conditional.getCTXString(constraint));
		
		for (Entry<FeatureExpr, Boolean> entry : generator.getIgnoredFeatures().entrySet()) {
			System.out.println("set feature " + getCTXString(entry.getKey(), false) + " to " + entry.getValue());
		}
	}

}
