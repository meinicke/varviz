package cmu.varviz.trace.generator;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import cmu.conditional.Conditional;
import cmu.varviz.trace.Trace;
import cmu.varviz.utils.FileUtils;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import gov.nasa.jpf.JPF;

/**
 * Calls VarexJ to generates the {@link Trace}.
 * 
 * @author Jens Meinicke
 *
 */
public class VarexJGenerator implements TraceGenerator {

	private static final VarexJGenerator INSTANCE = new VarexJGenerator();
	
	public static TraceGenerator geGenerator() {
		return INSTANCE;
	}
	
	private VarexJGenerator() {
		// nothing here
	}
	
	static {
		// create the site.properties in the .jpf folder 
		File userHome = new File(System.getProperty("user.home"));
		File jpfPath = new File(userHome.getPath() + "/.jpf");
		if (!jpfPath.exists()) {
			jpfPath.mkdir();
		}
		FileUtils.copyFileFromVarvizJar("/res", "site.properties", jpfPath);
	}
	

	@Override
	public void clearIgnoredFeatures() {
//		JPF.ignoredFeatures.clear();
	}

	@Override
	public Map<String, SingleFeatureExpr> getFeatures() {
		return Conditional.features;
	}

	@Override
	public Map<FeatureExpr, Boolean> getIgnoredFeatures() {
		return null;//JPF.ignoredFeatures;
	}

	@Override
	public Trace run(VMRunnerConfiguration runConfig, IResource resource, IProgressMonitor monitor, String[] classpath) throws CoreException {
//		Conditional.additionalConstraint = BDDFeatureExprFactory.True();
		clearIgnoredFeatures();
		StringBuilder cp = new StringBuilder();
		for (String c : classpath) {
			cp.append(c);
			cp.append(',');
		}
		String featureModelPath = getFeatureModel(resource);
		//+interaction= [context, composedContext, local, stack, interaction]
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
		final String[] args = { "+classpath=" + cp, "+choice=MapChoice", "+stack=HybridStackHandler", "+nhandler.delegateUnhandledNative", "+search.class=.search.RandomSearch",
				featureModelPath != null ? "+ featuremodel=" + featureModelPath : "","+interaction=context", "+project=" + resource.getProject().getRawLocation().toOSString(), runConfig.getClassToLaunch() };//		JPF.vatrace = new Trace();
//		JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.MIN_INTERACTION_DEGREE)), new ExceptionFilter());/// remove code clone
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
		JPF.main(args);
		
//		return JPF.vatrace;
		return null;
	}


}
