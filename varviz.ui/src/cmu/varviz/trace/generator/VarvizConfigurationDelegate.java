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
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import gov.nasa.jpf.JPF;

/**
 * Runs the Java Application to generate the {@link Trace}.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	private static final int numberOfRuns = 1;
	
	private long currentMemory = Integer.MAX_VALUE;
	
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
			long[] times = new long[numberOfRuns];
			long[] memorys = new long[numberOfRuns];
			FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
			for (int i = 0; i < numberOfRuns; i++) {
				System.gc();
				currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())>>20;
				System.out.println("Memory before: " + currentMemory);
			
				long start =System.currentTimeMillis();
				Trace trace = view.getGenerator().run(runConfig, resource, monitor, classpath);
				memorys[i] = currentMemory;
				System.out.println("Memory after: " + currentMemory);
				trace.finalizeGraph();
				long end = System.currentTimeMillis();
				times[i] = end - start;

				JPF.vatrace = null;
				VarvizView.checked.clear();
				System.gc();
			}
			if (monitor.isCanceled()) {
				return;
			}
			System.out.println(Arrays.toString(times));
			Arrays.sort(times);
			System.out.println("MIN: " +  times[0] + "ms");
			long medianTime = times[times.length]/2;
			if (numberOfRuns % 2 == 0) { 
				medianTime = (medianTime + times[times.length/2 - 1]) / 2;
			}
			System.out.println("MED: " + medianTime + "ms");
			
			System.out.println(Arrays.toString(memorys));
			Arrays.sort(memorys);
			System.out.println("MIN: " + memorys[0] + "MB");
			long medianMemory = memorys[memorys.length/2];
			if (numberOfRuns % 2 == 0) {
				medianMemory = (medianMemory + memorys[memorys.length/2 - 1]) / 2;
			}
			System.out.println("MED: " + medianMemory + "MB");
		} finally {
			VarvizView.checked.clear();
			monitor.done();
			System.setOut(originalOutputStream);
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
			public void print(String s) {
				super.print(s);
			}
			
			@Override
			public void println(String s) {
				if (s.startsWith("max memory:")) {
					String memory = s;
					memory = memory.replaceFirst("max memory:", "");
					memory = memory.replaceFirst("MB", "");
					memory = memory.trim();
					currentMemory = Integer.parseInt(memory) - currentMemory;
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
