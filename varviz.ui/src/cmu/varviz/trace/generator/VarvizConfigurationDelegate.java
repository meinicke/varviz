package cmu.varviz.trace.generator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import cmu.varviz.trace.Trace;
import cmu.varviz.trace.view.VarvizView;

/**
 * Runs the Java Application to generate the {@link Trace}.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	private static final int numberOfRuns = 1;
	
	private int currentMemory = Integer.MAX_VALUE;
	
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
			String[] classpath = getUserClasspath(configuration);
			
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
			//setDefaultSourceLocator(launch, configuration);

			// Launch the configuration - 1 unit of work
			monitor.subTask("Run application with VarexJ");

			final VarvizView view = VarvizView.getInstance();
			final IResource resource = configuration.getWorkingCopy().getMappedResources()[0];
			IProject project = resource.getProject();
			MessageConsole myConsole = findAndCreateConsole((view.isUseVarexJ() ?"VarexJ: ": "SampleJ: ") + project.getName() + ":" + runConfig.getClassToLaunch());
			myConsole.clearConsole();
			
			PrintStream myPrintStream = createOutputStream(originalOutputStream, myConsole.newMessageStream());
			System.setOut(myPrintStream);

			view.setProjectName(project.getName());

			project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
//<<<<<<< HEAD
//			Trace trace = view.getGenerator().run(runConfig, resource, monitor, classpath);
//			
//			
//			if (view.isShowForExceptionFeatures()) {
//				Projector.projectionForExceptiuon(trace, view.getGenerator());
//=======
//			
			long[] times = new long[numberOfRuns];
			int[] memorys = new int[numberOfRuns];
//			for (int i = 0; i < numberOfRuns; i++) {
//				long start = 0;
//				long end = Long.MAX_VALUE;
//				if (VarvizView.useVarexJ) {
//					// TODO move this to VarexJ Generator Class
//					FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
//					final String[] args = { "+classpath=" + cp, "+choice=MapChoice", "+stack=HybridStackHandler", "+nhandler.delegateUnhandledNative", "+search.class=.search.RandomSearch",
//							featureModelPath != null ? "+ featuremodel=" + featureModelPath : "", runConfig.getClassToLaunch() };
//					JPF.vatrace = new Trace();
//					JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
//					FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
//					
//					start = System.currentTimeMillis();
//					currentMemory = Integer.MAX_VALUE;
//					JPF.main(args);
//					memorys[i] = currentMemory;
//					JPF.vatrace.finalizeGraph();
//					end = System.currentTimeMillis();
//					
//					VarvizView.TRACE = JPF.vatrace;
//				} else {
//					// TODO move to SampleJ builder
//					// run SampleJ
//					final SampleJMonitor samplejMonitor = new SampleJMonitor() {
//						@Override
//						public void beginTask(String name, int totalWork) {
//							monitor.beginTask(name, totalWork);
//						}
//	
//						@Override
//						public void worked(int work) {
//							monitor.worked(work);
//						}
//					};
//	
//					Conditional.setFM(getFeatureModel(resource));
//					Collector collector = new Collector(getOptions(resource));
//					String projectPath = project.getLocation().toOSString();
//					try {
//						Collector.FILTER = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)),
//								new ExceptionFilter());
//						
//						start = System.currentTimeMillis();
//						VarvizView.TRACE = collector.createTrace(runConfig.getClassToLaunch(), projectPath, runConfig.getClassPath(),
//								samplejMonitor);
//						end = System.currentTimeMillis();
//						
//						memorys[i] = collector.stats.getStats("Memory (MB)");
//					} finally {
//						project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
//					}
//				}
//				times[i] = end - start;
				
				// clean up
//				VarvizView.TRACE = null;
//				JPF.vatrace = null;
//				VarvizView.checked.clear();
//				System.gc();
//>>>>>>> refs/heads/FSEEval
//			}
//<<<<<<< HEAD
//			trace.finalizeGraph();
//
//			view.setClassPath(classpath);
//			if (view.isSliceException()) {
//				// TODO there may be multiple exception statements
//				Statement exceptionSatement = (Statement) trace.getEND().getFrom().simplify(trace.getExceptionContext()).getValue();
//				new BackwardsSlicer().slice(classpath, exceptionSatement, trace);
//			}
//			
//			view.setTrace(trace);
//			
//			if (view.getTRACE().getMain().size() < 20_000) {
//				view.refreshVisuals();
//			}
//=======
//			if (VarvizView.TRACE.getMain().size() < 10_000) {
//				VarvizView.refreshVisuals();
//			}
//>>>>>>> refs/heads/FSEEval

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}
//<<<<<<< HEAD
//=======
			
			System.out.println(Arrays.toString(times));
			Arrays.sort(times);
			System.out.println(times[0] + "ms");
			System.out.println(Arrays.toString(memorys));
			Arrays.sort(memorys);
			System.out.println("MIN: " + memorys[0] + "MB");
		} finally {
//>>>>>>> refs/heads/FSEEval
			VarvizView.checked.clear();
			monitor.done();
			System.setOut(originalOutputStream);
//		} catch (Exception e) {
//			throw new RuntimeException(e);
		}
		
		
	}
	
	/** 
	 *  returns the user specified classpath entries.
	 *  the eclipse implementations changed with version Oxigen.3 and {@link AbstractJavaLaunchConfigurationDelegate#getClassPath} 
	 *  returns JVM classes that must not be contained for VarexJ.
	 */
	private String[] getUserClasspath(ILaunchConfiguration config) {
		IRuntimeClasspathEntry[] entries;
		try {
			entries = JavaRuntime.computeUnresolvedRuntimeClasspath(config);
			entries = JavaRuntime.resolveRuntimeClasspath(entries, config);
		} catch (CoreException e) {
			e.printStackTrace();
			return new String[0];
		}
		Set<String> classpathEntries = new HashSet<>(entries.length);
		for (IRuntimeClasspathEntry entry : entries) {
			String location = entry.getLocation();
			if (location != null && entry.getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES && !classpathEntries.contains(location)) {
				classpathEntries.add(location);
			}
		}
		
		return classpathEntries.toArray(new String[classpathEntries.size()]);
	}


	private PrintStream createOutputStream(PrintStream originalOut, final MessageConsoleStream consoleStream) {
		return new PrintStream(originalOut) {
			
			@Override
			public void println(String s) {
				if (s.startsWith("max memory:")) {
					String memory = s;
					memory = memory.replaceFirst("max memory:", "");
					memory = memory.replaceFirst("MB", "");
					memory = memory.trim();
					currentMemory = Integer.parseInt(memory);
				}
				
				super.println(s);
			}
			
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
				return ((MessageConsole) existing[i]);//.destroy();
			}
		}
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

}
