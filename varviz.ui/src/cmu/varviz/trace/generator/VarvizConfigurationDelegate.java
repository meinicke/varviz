package cmu.varviz.trace.generator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

import cmu.varviz.slicing.BackwardsSlicer;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.NodeColor;
import cmu.varviz.trace.Shape;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.view.VarvizView;
import cmu.vatrace.ReturnStatement;

/**
 * Runs the Java Application to generate the {@link Trace}.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

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
			
			Trace trace = view.getGenerator().run(runConfig, resource, monitor, classpath);
			trace.finalizeGraph();

			view.setClassPath(classpath);
			
			System.out.println("Start Slicing");
			final List<Statement> elements = getAllElements(trace.getMain(), new ArrayList<>());
			percentReduction = new ArrayList<>();
			
			boolean random = true;
			if (random) {
				// CASE: random
				while (percentReduction.size() < 1) {
					int randID = new Random().nextInt(elements.size());
					if (trace == null) {
						trace = view.getGenerator().run(runConfig, resource, monitor, classpath);
					}
					trace.finalizeGraph();
					createNewSlice(trace,classpath, randID);
					view.setTrace(trace);
					view.refreshVisuals();
					trace = null;
				}
			} else {
				for (int i = 0; i < elements.size(); i++) {
					try {
						trace = view.getGenerator().run(runConfig, resource, monitor, classpath);
						i = createNewSlice(trace, classpath, i);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			System.out.println(percentReduction);
			
			int sum = 0;
			for (Integer reduction : percentReduction) {
				sum += reduction;
			}
			java.util.Collections.sort(percentReduction);
			
			
			int averageReduction = sum / percentReduction.size();
			System.out.println("Avereage reduction: " + averageReduction + "/10000");
			System.out.println("Min reduction: " + percentReduction.get(0) + "/10000");
			
//			int[] randomElementID = new int[] {new Random().nextInt(elements.size())};
//			Statement randomElement = elements.get(randomElementID[0]);
//			while (randomElement.getColor() != NodeColor.darkorange) {
//				randomElementID[0] = new Random().nextInt(elements.size());
//				randomElement = elements.get(randomElementID[0]);
//			}
//			System.out.println("Slice for: " + randomElement + " id:" + randomElementID[0]);
//			
//			
//			
//			int originalSize = trace.getMain().size();
//			trace.setFilter(element -> elements.indexOf(element) <= randomElementID[0]);
//			trace.finalizeGraph();
//			new BackwardsSlicer().slice(classpath, randomElement, trace);
//			int sliceSize = trace.getMain().size();
//			System.out.println(originalSize + "-> " + sliceSize);
//			System.out.println(100 - (sliceSize * 100 / originalSize) + "% reduction");
//			System.out.println("======================================================");
//			
			
//			if (view.isSliceException()) {
//				// TODO there may be multiple exception statements
//				Statement exceptionSatement = (Statement) trace.getEND().getFrom().simplify(trace.getExceptionContext()).getValue();
//				new BackwardsSlicer().slice(classpath, exceptionSatement, trace);
//			}
			
//			view.setTrace(trace);
			
//			if (view.getTRACE().getMain().size() < 20_000) {
//				view.refreshVisuals();
//			}

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}
			VarvizView.checked.clear();
			monitor.done();
			System.setOut(originalOutputStream);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
	
	private List<Integer> percentReduction;
	
	private int createNewSlice(Trace trace, String[] classpath, int i) throws CoreException {
		final List<Statement> elements = getAllElements(trace.getMain(), new ArrayList<>());
		int[] randomElementID = new int[] {i};
		Statement randomElement = elements.get(randomElementID[0]);
//		while (randomElement.getShape() == Shape.Mdiamond) {
		while (randomElement.getColor() != NodeColor.darkorange || randomElement instanceof ReturnStatement || randomElement.toString().contains("branchTokenTypes")) {
			randomElementID[0] = new Random().nextInt(elements.size());
			if (randomElementID[0] >= elements.size()) {
				return randomElementID[0];
			}
			randomElement = elements.get(randomElementID[0]);
		}
		System.out.println("Slice for: " + randomElement + " id:" + randomElementID[0]);
		int originalSize = trace.getMain().size();
		trace.setFilter(element -> elements.indexOf(element) <= randomElementID[0]);
		trace.finalizeGraph();
		new BackwardsSlicer().slice(classpath, randomElement, trace);
		int sliceSize = trace.getMain().size();
		System.out.println(originalSize + "-> " + sliceSize);
		int reduction = 10000 - (sliceSize * 10000 / originalSize);
		percentReduction.add(reduction);
		System.out.println(reduction + "/1000 reduction");
		System.out.println("======================================================");
		return randomElementID[0];
	}

	private List<Statement> getAllElements(Method method, ArrayList<Statement> elements) {
		for (MethodElement methodElement : method.getChildren()) {
			if (methodElement instanceof Method) {
				getAllElements((Method) methodElement, elements);
			} else {
				elements.add((Statement) methodElement);
			}
		}
		return elements;
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
