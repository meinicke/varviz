package cmu.varviz.trace.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import cmu.conditional.Conditional;
import cmu.samplej.Collector;
import cmu.samplej.SampleJMonitor;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.filters.And;
import cmu.varviz.trace.filters.InteractionFilter;
import cmu.varviz.trace.filters.Or;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.actions.IgnoreContext;
import cmu.vatrace.ExceptionFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExprFactory;
import gov.nasa.jpf.JPF;

/**
 * Runs the Java Application to generate the {@link Trace}.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {// TODO JavaLaunchDelegate?

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		final PrintStream originalOutputStream = System.out;
		try {
			final String mainTypeName = verifyMainTypeName(configuration);
			File workingDir = verifyWorkingDirectory(configuration);
			String workingDirName = null;
			if (workingDir != null) {
				workingDirName = workingDir.getAbsolutePath();
			}

			// Environment variables
			String[] envp = getEnvironment(configuration);

			// Program & VM arguments
			String pgmArgs = getProgramArguments(configuration);
			String vmArgs = getVMArguments(configuration);
			ExecutionArguments execArgs = new ExecutionArguments(vmArgs, pgmArgs);

			// VM-specific attributes
			Map<String, Object> vmAttributesMap = getVMSpecificAttributesMap(configuration);

			// Classpath
			String[] classpath = getClasspath(configuration);

			// Create VM config
			VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
			runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
			runConfig.setEnvironment(envp);
			runConfig.setVMArguments(execArgs.getVMArgumentsArray());
			runConfig.setWorkingDirectory(workingDirName);
			runConfig.setVMSpecificAttributesMap(vmAttributesMap);

			// Bootpath
			runConfig.setBootClassPath(getBootpath(configuration));

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}

			// stop in main
			prepareStopInMain(configuration);

			// set the default source locator if required
			// setDefaultSourceLocator(launch, configuration); TODO

			// Launch the configuration - 1 unit of work
			monitor.subTask("Run application with VarexJ");
			StringBuilder cp = new StringBuilder();
			for (String c : classpath) {
				cp.append(c);
				cp.append(',');
			}
			cp.append("${jpf-core}");

			final IResource resource = configuration.getWorkingCopy().getMappedResources()[0];

			IProject project = resource.getProject();
			MessageConsole myConsole = findAndCreateConsole("VarexJ: " + project.getName() + ":" + runConfig.getClassToLaunch());
			myConsole.clearConsole();
			PrintStream myPrintStream = createOutputStream(originalOutputStream, myConsole.newMessageStream());
			System.setOut(myPrintStream);

			VarvizView.PROJECT_NAME = project.getName();

			String featureModelPath = null;
			for (IResource child : project.members()) {
				if (child instanceof IFile) {
					if ("dimacs".equals(child.getFileExtension())) {
						featureModelPath = child.getRawLocation().toOSString();
					}
				}
			}

			project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			if (VarvizView.useVarexJ) {
				// TODO move this to VarexJ Generator Class
				FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
				final String[] args = { "+classpath=" + cp, "+choice=MapChoice", "+stack=HybridStackHandler", "+nhandler.delegateUnhandledNative", "+search.class=.search.RandomSearch",
						featureModelPath != null ? "+ featuremodel=" + featureModelPath : "", runConfig.getClassToLaunch() };
				JPF.vatrace = new Trace();
				JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
				FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
				JPF.main(args);
				Conditional.additionalConstraint = BDDFeatureExprFactory.True();

				if (VarvizView.reExecuteForExceptionFeatures) {
					FeatureExpr exceptionContext = JPF.vatrace.getExceptionContext();
					IgnoreContext.removeContext(exceptionContext);
					if (!JPF.ignoredFeatures.isEmpty()) {
						// second run for important features
						myConsole = findAndCreateConsole("VarexJ: " + project.getName() + ":" + runConfig.getClassToLaunch() + " (exception features only)");
						myConsole.clearConsole();
						PrintStream consoleStream = createOutputStream(originalOutputStream, myConsole.newMessageStream());
						System.setOut(consoleStream);
						JPF.vatrace = new Trace();
						JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
						JPF.main(args);
						Conditional.additionalConstraint = BDDFeatureExprFactory.True();
						JPF.ignoredFeatures.clear();
					}
				}
				JPF.vatrace.finalizeGraph();
				VarvizView.TRACE = JPF.vatrace;
			} else {
				// TODO move to SampleJ builder
				// run SampleJ
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
				Collector collector = new Collector(getOptions(resource));
				String projectPath = project.getLocation().toOSString();
				try {
					Collector.FILTER = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)),
							new ExceptionFilter());
					VarvizView.TRACE = collector.createTrace(runConfig.getClassToLaunch(), projectPath, runConfig.getClassPath(),
							samplejMonitor);
				} finally {
//					project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
				}
			}

			VarvizView.refreshVisuals();

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}
		} finally {
			monitor.done();
			System.setOut(originalOutputStream);
		}
	}

	private String getFeatureModel(IResource resource) {
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

	private PrintStream createOutputStream(PrintStream originalOut, final MessageConsoleStream consoleStream) {
		return new PrintStream(originalOut) {

			@Override
			public void write(int b) {
				try {
					consoleStream.write(b);
					originalOut.write(b);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void write(byte[] b) throws IOException {
				consoleStream.write(b);
				originalOut.write(b);
			}

			@Override
			public void write(byte[] buf, int off, int len) {
				try {
					consoleStream.write(buf, off, len);
					originalOut.write(buf, off, len);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void flush() {
				try {
					consoleStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
	}

	private static MessageConsole findAndCreateConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

}
