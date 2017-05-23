package cmu.varviz.trace.generator;

import java.util.Map;

import cmu.varviz.trace.Trace;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;

/**
 * Interface to the tool that generates the {@link Trace}.
 * 
 * @author Jens Meinicke
 *
 */
public interface TraceGenerator {

	Trace createTrace();
	
	void clearIgnoredFeatures();
	
	Map<String, SingleFeatureExpr> getFeatures();
	
	Map<FeatureExpr, Boolean> getIgnoredFeatures();
}
