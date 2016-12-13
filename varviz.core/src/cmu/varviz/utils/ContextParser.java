package cmu.varviz.utils;

import cmu.conditional.Conditional;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;

public class ContextParser {

	public static FeatureExpr getContext(String context) {
		if (context.equals("True")) {
			return FeatureExprFactory.True();
		}
		FeatureExpr ctx = FeatureExprFactory.False();
		String[] orsplit = context.split("\\|");
		for (int i = 0; i < orsplit.length; i++) {
			String orCTX = orsplit[i];
			String[] andSplit = orCTX.split("\\&amp;");
			FeatureExpr andContext = FeatureExprFactory.True();
			for (int j = 0; j < andSplit.length; j++) {
				String string = andSplit[j];
				if (string.startsWith("¬") || string.startsWith("!")) {
					SingleFeatureExpr feature = Conditional.createFeature(string.substring(1));
					andContext = andContext.andNot(feature);
				} else {
					SingleFeatureExpr feature = Conditional.createFeature(string);
					andContext = andContext.and(feature);
				}
			}
			ctx = ctx.or(andContext);
		}
		return ctx;
	}

}
