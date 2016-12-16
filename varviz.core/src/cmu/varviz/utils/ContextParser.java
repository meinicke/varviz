package cmu.varviz.utils;

import java.util.Map.Entry;

import cmu.conditional.ChoiceFactory;
import cmu.conditional.Conditional;
import cmu.conditional.One;
import cmu.varviz.io.xml.XMLvarviz;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;

public class ContextParser implements XMLvarviz {

	public static FeatureExpr getContext(String context) {
		if (context.equals("True")) {
			return FeatureExprFactory.True();
		}
		FeatureExpr ctx = FeatureExprFactory.False();
		String[] orsplit = context.split("\\|");
		for (int i = 0; i < orsplit.length; i++) {
			String orCTX = orsplit[i];
			orCTX = orCTX.replaceAll("\\&amp;", "\\&");
			String[] andSplit = orCTX.split("\\&");
			FeatureExpr andContext = FeatureExprFactory.True();
			for (int j = 0; j < andSplit.length; j++) {
				String string = andSplit[j];
				if (string.startsWith("¬") || string.startsWith("!")) {
					SingleFeatureExpr feature = Conditional.features.get(string);
					if (feature == null) {
						feature = Conditional.createFeature(string.substring(1));
					}
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
		
	public static String ConditionalToString(Conditional<?> conditional) {
		if (conditional.isOne()) {
			return conditional.getValue() + "";
		}
		StringBuilder text = new StringBuilder();
		for (Entry<?, FeatureExpr> entry : conditional.toMap().entrySet()) {
			text.append(Conditional.getCTXString(entry.getValue()));
			text.append(entrySplitChar);
			text.append(entry.getKey());
			text.append(valueSplitChar);
		}
		return text.substring(0, text.length() - valueSplitChar.length());
	}
	
	public static Conditional<String> StringToConditional(String string) {
		if (!string.contains(valueSplitChar)) {
			return new One<>(string);
		}
		try {
			String[] split = string.split(valueSplitChar);
			
			Conditional<String> value = new One<>(null);
			for (String entry : split) {
				String[] entrySplit = entry.split(entrySplitChar);
				String contextString = entrySplit[0];
				String valueString = entrySplit[1];
				value = ChoiceFactory.create(getContext(contextString), new One<>(valueString), value);
			}
			return value.simplify();
		} catch (Exception e) {
			System.out.println(string);
			throw e;
		}
	}

}
