package cmu.varviz.trace.view.editparts;

import cmu.conditional.Conditional;
import de.fosd.typechef.featureexpr.FeatureExpr;

/**
 * Utility class for edit parts.
 * 
 * @author Jens Meinicke
 *
 */
public class EditPartUtils {

	private static final char LOGICAL_OR = (char) 0x2228;
	private static final char LOGICAL_AND = (char) 0x2227;

	private EditPartUtils() {
	}

	public static String getContext(FeatureExpr ctx) {
		final String originalContext = Conditional.getCTXString(ctx);
		final String[] split = originalContext.split("&");

		for (int i = 0; i < split.length; i++) {
			String current = split[i];
			if (current.contains("|")) {
				current = current.replaceAll("\\|", Character.toString(LOGICAL_OR));
				if (split.length == 1) {
					split[i] = current;
				} else {
					split[i] = '(' + current + ')';
				}
			}
		}
		final StringBuilder contextWithParenthesis = new StringBuilder();
		for (int i = 0; i < split.length; i++) {
			contextWithParenthesis.append(split[i]).append(LOGICAL_AND);
		}

		contextWithParenthesis.deleteCharAt(contextWithParenthesis.length() - 1);
		return contextWithParenthesis.toString();
	}
}
