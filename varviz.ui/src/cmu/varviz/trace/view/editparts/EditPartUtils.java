package cmu.varviz.trace.view.editparts;

import cmu.conditional.Conditional;
import de.fosd.typechef.featureexpr.FeatureExpr;

public class EditPartUtils {

	public static String getContext(FeatureExpr ctx) {
		final String originalContext = Conditional.getCTXString(ctx); 
		final String[] split = originalContext.split("&");
		
		for (int i = 0; i < split.length; i++) {
			String current = split[i];
			if (current.contains("|")) {
				current = current.replaceAll("\\|", Character.toString((char)0x2228));
				if (split.length == 1) {
					split[i] = current;
				} else {
					split[i] = '(' + current + ')';
				}
			}
		}
		final StringBuilder contextWithParenthesis = new StringBuilder();
		for (int i = 0; i < split.length; i++) {
			contextWithParenthesis.append(split[i]).append((char)0x2227);
		}
		
		contextWithParenthesis.deleteCharAt(contextWithParenthesis.length() - 1);
		return contextWithParenthesis.toString();
	}
}
