package cmu.varviz.trace.generator;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

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

	
	void clearIgnoredFeatures();
	
	Map<String, SingleFeatureExpr> getFeatures();
	
	Map<FeatureExpr, Boolean> getIgnoredFeatures();

	Trace run(VMRunnerConfiguration runConfig, IResource resource, IProgressMonitor monitor, String[] classpath) throws CoreException;
	
	default String getFeatureModel(IResource resource) { 
		try {
			for (IResource child : resource.getProject().members()) {
				if ("dimacs".equals(child.getFileExtension())) {
					return child.getLocation().toOSString();
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return "";
	}
	
}
