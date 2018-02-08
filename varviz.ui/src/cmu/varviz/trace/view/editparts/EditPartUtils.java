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
		if (ctx.isContradiction()) {
			return "False";
		}
		String contextString = Conditional.getCTXString(ctx);
		contextString = contextString.replaceAll("\\|", Character.toString(LOGICAL_OR));
		contextString = contextString.replaceAll("\\&", Character.toString(LOGICAL_AND));
		if (contextString.isEmpty()) {
			return "??";
		}
		if (contextString.charAt(0) == '(' && contextString.charAt(contextString.length() - 1) == ')') {
			contextString = contextString.substring(1, contextString.length() - 1);
		}
		return contextString;
	}
}
