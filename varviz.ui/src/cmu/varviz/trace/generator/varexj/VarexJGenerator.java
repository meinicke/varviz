package cmu.varviz.trace.generator.varexj;

import java.io.File;
import java.util.Map;

import cmu.conditional.Conditional;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.filters.And;
import cmu.varviz.trace.filters.InteractionFilter;
import cmu.varviz.trace.filters.Or;
import cmu.varviz.trace.generator.TraceGenerator;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.actions.IgnoreContext;
import cmu.varviz.utils.FileUtils;
import cmu.vatrace.ExceptionFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExprFactory;
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
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
		final String[] args = {
				"+classpath=" + VarvizView.getPath() + "/bin,${jpf-core}",
				"+choice=MapChoice",
				"+stack=StackHandler",
				 "+nhandler.delegateUnhandledNative", "+search.class=.search.RandomSearch",
				 VarvizView.PROJECT_PRAMETERS.length == 3 ? "+featuremodel=" + VarvizView.getPath() + "/" + VarvizView.PROJECT_PRAMETERS[2] : "",
						 VarvizView.PROJECT_PRAMETERS[1]
		};
		JPF.vatrace = new Trace();
		JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
		JPF.main(args);
		Conditional.additionalConstraint = BDDFeatureExprFactory.True();
		
		FeatureExpr exceptionContext = JPF.vatrace.getExceptionContext();
		IgnoreContext.removeContext(exceptionContext);
		if (!JPF.ignoredFeatures.isEmpty()) {
			JPF.vatrace = new Trace();
			JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
			JPF.main(args);
			Conditional.additionalConstraint = BDDFeatureExprFactory.True();
		}
		JPF.vatrace.finalizeGraph();
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
