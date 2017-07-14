package cmu.varviz.trace.generator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import cmu.varviz.trace.IFStatement;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.filters.And;
import cmu.varviz.trace.filters.InteractionFilter;
import cmu.varviz.trace.filters.Or;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.actions.IgnoreContext;
import cmu.vatrace.ExceptionFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExprFactory;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.report.Statistics;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * Runs the Java Application to generate the {@link Trace}.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

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
			setDefaultSourceLocator(launch, configuration);

			// Launch the configuration - 1 unit of work
			monitor.subTask("Run application with VarexJ");
			StringBuilder cp = new StringBuilder();
			for (String c : classpath) {
				cp.append(c);
				cp.append(',');
			}
			cp.append("${jpf-core}");

			final IResource resource = configuration.getWorkingCopy().getMappedResources()[0];

			MessageConsole myConsole = findAndCreateConsole("VarexJ: " + resource.getProject().getName() + ":" + runConfig.getClassToLaunch());
			myConsole.clearConsole();
			PrintStream myPrintStream = createOutputStream(originalOutputStream, myConsole.newMessageStream());
			System.setOut(myPrintStream);

			VarvizView.PROJECT_NAME = resource.getProject().getName();

			String featureModelPath = null;
			for (IResource child : resource.getProject().members()) {
				if (child instanceof IFile) {
					if ("dimacs".equals(child.getFileExtension())) {
						featureModelPath = child.getRawLocation().toOSString();
					}
				}
			}
			
			// TODO move this to VarexJ Generator Class
			FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
			final String[] args = { "+classpath=" + cp, "+choice=MapChoice", "+stack=HybridStackHandler", "+nhandler.delegateUnhandledNative", "+search.class=.search.RandomSearch",
					featureModelPath != null ? "+ featuremodel=" + featureModelPath : "", runConfig.getClassToLaunch() };
			JPF.vatrace = new Trace();
			JPF.vatrace.filter = new Or(new And(new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
			FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
			JPF.main(args);
			
			final Collection<IFStatement<?>> collectIFStatements = JPF.vatrace.getMain().collectIFStatements();
			createAllTraces(originalOutputStream, args);
			
			Conditional.additionalConstraint = BDDFeatureExprFactory.True();

			if (VarvizView.reExecuteForExceptionFeatures) {
				FeatureExpr exceptionContext = JPF.vatrace.getExceptionContext();
				if (Conditional.isSatisfiable(exceptionContext)) {
					IgnoreContext.removeContext(exceptionContext, collectIFStatements);
					if (!JPF.ignoredFeatures.isEmpty()) {
						// second run for important features
						myConsole = findAndCreateConsole("VarexJ: " + resource.getProject().getName() + ":" + runConfig.getClassToLaunch() + " (exception features only)");
						myConsole.clearConsole();
						PrintStream consoleStream = createOutputStream(originalOutputStream, myConsole.newMessageStream());
						System.setOut(consoleStream);
						JPF.vatrace = new Trace();
						JPF.vatrace.filter = new Or(new And(new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
						JPF.main(args);
						Conditional.additionalConstraint = BDDFeatureExprFactory.True();
						JPF.ignoredFeatures.clear();
					}
				}
			}
			JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)), new And(VarvizView.basefilter, new ExceptionFilter()));
			JPF.vatrace.finalizeGraph();
			
			VarvizView.TRACE = JPF.vatrace;
			
//			int edges = VarvizView.TRACE.getEdges().size();
//			int nodes = VarvizView.TRACE.getMain().size() + 2;
			
//			System.out.println("Edges: " + edges);
//			System.out.println("Nodes: " + nodes);
			
//			int MCCabe = edges - nodes + 2;
//			System.out.println("Cyclomatic Complexity: " + MCCabe);
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

	private void createAllTraces(PrintStream originalOutputStream, String[] args) {
		Collection<IFStatement<?>> ifStatement = JPF.vatrace.getMain().collectIFStatements();
		
		ArrayList<SingleFeatureExpr> features = new ArrayList<>(Conditional.features.values());
		for (SingleFeatureExpr singleFeatureExpr : features) {
			if (Conditional.isTautology(singleFeatureExpr)) {
				continue;
			}

//			ThreadInfo.maxInstruction = 10_000;
			while (true) {
//				ThreadInfo.maxInstruction += 10_000;
				Conditional.additionalConstraint = BDDFeatureExprFactory.True();
				IgnoreContext.removeContext(singleFeatureExpr, ifStatement);
				
				System.setOut(originalOutputStream);
				JPF.vatrace = new Trace();
				JPF.vatrace.filter = new Or(new And(new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
				JPF.main(args);
				Conditional.additionalConstraint = BDDFeatureExprFactory.True();
				JPF.ignoredFeatures.clear();
				
				MessageConsole myConsole = findAndCreateConsole("create all traces");
				PrintStream consoleStream = createOutputStream(originalOutputStream, myConsole.newMessageStream());
				System.setOut(consoleStream);
	
				JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)), new And(VarvizView.basefilter, new ExceptionFilter()));
				JPF.vatrace.finalizeGraph();
				System.out.println("Instructions: " + Statistics.insns);
				int edges = JPF.vatrace.getEdges().size();
				int nodes = JPF.vatrace.getMain().size() + 2;
				
				
				System.out.println("Edges: " + edges);
				System.out.println("Nodes: " + nodes);
				
				int MCCabe = edges - nodes + 2;
				System.out.println("Cyclomatic Complexity: " + MCCabe);
				
				System.out.println("-------------------------------");
				
				if (nodes > 10) {
					return;
				}
					
					
				if (Statistics.insns < ThreadInfo.maxInstruction) {
					break;
				}
			}
		}
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
