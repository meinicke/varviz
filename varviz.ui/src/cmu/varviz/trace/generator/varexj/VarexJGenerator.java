package cmu.varviz.trace.generator.varexj;

import java.io.File;
import java.util.Map;

import cmu.conditional.Conditional;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.generator.TraceGenerator;
import cmu.varviz.utils.FileUtils;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import gov.nasa.jpf.JPF;

/**
 * Calls VarexJ to generates the {@link Trace}.
 * 
 * @author Jens Meinicke
 *
 */
public class VarexJGenerator implements TraceGenerator {

	static {
		// create the site.properties in the .jpf folder 
		File userHome = new File(System.getProperty("user.home"));
		File jpfPath = new File(userHome.getPath() + "/.jpf");
		if (!jpfPath.exists()) {
			jpfPath.mkdir();
		}
		FileUtils.CopyFileFromVarvizJar("/res", "site.properties", jpfPath);
	}
	
	@Override
	public Trace createTrace() {
		// TODO
		return JPF.vatrace;
	}

	@Override
	public void clearIgnoredFeatures() {
		JPF.ignoredFeatures.clear();
	}

	@Override
	public Map<String, SingleFeatureExpr> getFeatures() {
		return Conditional.features;
	}

	@Override
	public Map<FeatureExpr, Boolean> getIgnoredFeatures() {
		return JPF.ignoredFeatures;
	}

}
