package cmu.varviz.trace.generator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

import cmu.conditional.Conditional;
import cmu.samplej.Collector;
import cmu.samplej.SampleJMonitor;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.filters.And;
import cmu.varviz.trace.filters.InteractionFilter;
import cmu.varviz.trace.filters.Or;
import cmu.varviz.trace.filters.StatementFilter;
import cmu.varviz.trace.view.VarvizView;
import cmu.vatrace.ExceptionFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;

/**
 * Calls SampleJ to generates the {@link Trace}.
 * 
 * @author Jens Meinicke
 *
 */
public class SampleJGenerator implements TraceGenerator {

	private static final SampleJGenerator INSTANCE = new SampleJGenerator();
	
	public static TraceGenerator geGenerator() {
		return INSTANCE;
	}
	
	private SampleJGenerator() {
		// nothing here
	}

	@Override
	public void clearIgnoredFeatures() {
		// nothing here
	}

	@Override
	public Map<String, SingleFeatureExpr> getFeatures() {
		return Conditional.features;
	}

	@Override
	public Map<FeatureExpr, Boolean> getIgnoredFeatures() {
		return new HashMap<>();
	}

	@Override
	public Trace run(VMRunnerConfiguration runConfig, IResource resource,  IProgressMonitor monitor, String[] classpath) throws CoreException {
		IProject project = resource.getProject();
		
		final SampleJMonitor samplejMonitor = new SampleJMonitor() {
			@Override
			public void beginTask(String name, int totalWork) {
				monitor.beginTask(name, totalWork);
			}

			@Override
			public void worked(int work) {
				monitor.worked(work);
			}
		};

		Conditional.setFM(getFeatureModel(resource));
		final StatementFilter filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.MIN_INTERACTION_DEGREE)),
				new ExceptionFilter());
		Collector collector = new Collector(filter, getOptions(resource));
		String projectPath = project.getLocation().toOSString();
		try {
			return(collector.createTrace(runConfig.getClassToLaunch(), projectPath, runConfig.getClassPath(),
					samplejMonitor));
		} finally {
			project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		}
	}
	
	private String[] getOptions(IResource resource) {
		IFile optionsFile = resource.getProject().getFile("options.txt");
		List<String> options = new ArrayList<>();

		try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(optionsFile.getContents(true), optionsFile.getCharset()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				options.add(line.trim());
			}
		} catch (IOException | CoreException e) {
			e.printStackTrace();
		}
		final String[] optionsArr = new String[options.size()];
		options.toArray(optionsArr);
		return optionsArr;
	}
	


}
