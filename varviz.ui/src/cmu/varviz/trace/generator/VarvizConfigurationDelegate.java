package cmu.varviz.trace.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
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
			
			try {
				STEP_SIZE = Integer.parseInt(pgmArgs);
			} catch (NumberFormatException e) {
				STEP_SIZE = Integer.MAX_VALUE;
			}
			System.out.println("Set step size to " + STEP_SIZE);
			
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

			MessageConsole myConsole = findAndCreateConsole(
					"VarexJ: " + resource.getProject().getName() + ":" + runConfig.getClassToLaunch());
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
			final String[] args = { "+classpath=" + cp, "+choice=MapChoice", "+stack=HybridStackHandler",
					"+nhandler.delegateUnhandledNative", "+search.class=.search.RandomSearch",
					featureModelPath != null ? "+ featuremodel=" + featureModelPath : "",
					runConfig.getClassToLaunch() };

			JPF.vatrace = new Trace();
			JPF.vatrace.filter = new Or(new And(new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
			FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
			
			JPF.ignoredFeatures.clear();
			
			JPF.main(args);
			JPF.vatrace.finalizeGraph();
			
			final Collection<IFStatement<?>> collectIFStatements = Collections.unmodifiableCollection(JPF.vatrace.getMain().collectIFStatements());
			final FeatureExpr exceptionContext = JPF.vatrace.getExceptionContext();
			
//			createAllTraces(originalOutputStream, args);
			
			if (false && VarvizView.reExecuteForExceptionFeatures) {
				if (Conditional.isSatisfiable(exceptionContext)) {
					IgnoreContext.removeContext(exceptionContext, collectIFStatements);
					if (!JPF.ignoredFeatures.isEmpty()) {
						// slice for exception features
						myConsole = findAndCreateConsole("VarexJ: " + resource.getProject().getName() + ":"
								+ runConfig.getClassToLaunch() + " (exception features only)");
						myConsole.clearConsole();
						PrintStream consoleStream = createOutputStream(originalOutputStream,
								myConsole.newMessageStream());
						System.setOut(consoleStream);
						JPF.vatrace = new Trace();
						JPF.vatrace.filter = new Or(new And(new InteractionFilter(VarvizView.minDegree)),
								new ExceptionFilter());
						JPF.main(args);
						Conditional.additionalConstraint = BDDFeatureExprFactory.True();
						JPF.ignoredFeatures.clear();
					}
				}
			}
			JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)),
					new And(VarvizView.basefilter, new ExceptionFilter()));
			JPF.vatrace.finalizeGraph();

			VarvizView.TRACE = JPF.vatrace;
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
	
	private File folder;
	private File mccabeMax;
	private File nodesMax;

	private int STEP_SIZE = Integer.MAX_VALUE;
	private static final String SEPARATOR = ",";
	private static final int maxDegree = 1;
	
	List<Integer> nodesMaxValues = new ArrayList<>();
	List<Integer> MCCabeMaxValues = new ArrayList<>();
	List<Integer> descissionsMaxValues = new ArrayList<>();
	List<Integer> statementsMaxValues = new ArrayList<>();
	
	private Map<Integer, List<Integer>> resultsMCCabe = new HashMap<>();
	private Map<Integer, List<Integer>> resultsNodes = new HashMap<>();
	private Map<Integer, List<Integer>> resultsDecissions = new HashMap<>();
	private Map<Integer, List<Integer>> resultsStatements = new HashMap<>();

	private void createAllTraces(final PrintStream originalOutputStream, final String[] args) {
		folder = new File(VarvizView.PROJECT_NAME);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		mccabeMax = new File(folder.getName() + File.separator + VarvizView.PROJECT_NAME + "mccabe.csv");
		nodesMax = new File(folder.getName() + File.separator + VarvizView.PROJECT_NAME + "nodes.csv");

		final int edges = JPF.vatrace.getEdges().size();
		final int nodes = JPF.vatrace.getMain().size();
		final int numberOfDecsissions = JPF.vatrace.getMain().accumulate((s, v) -> s instanceof IFStatement ? v + 1 : v, 0);
		final int numberOfStatements = nodes - numberOfDecsissions;

		try (PrintWriter pwMccabeMax = new PrintWriter(mccabeMax, StandardCharsets.UTF_8.name());
			PrintWriter pwNodesMax = new PrintWriter(nodesMax, StandardCharsets.UTF_8.name())) {
  			PrintWriter[] pws = new PrintWriter[] {pwMccabeMax, pwNodesMax};
  			for (PrintWriter pw : pws) {
  				pw.println("Degree");
  				pw.print("All");
  				pw.print(SEPARATOR);
			}
  			
			
			final int MCCabe = edges - nodes + 2;
			pwMccabeMax.println(MCCabe);
			pwNodesMax.println(nodes);
			
			for (int degree = 1; degree <= maxDegree; degree++) {
				for (PrintWriter pw : pws) {
					pw.print(degree);
					pw.print(SEPARATOR);
				}
				
				createAllTraces(originalOutputStream, args, degree);
				
				Collections.sort(nodesMaxValues, (a, b) -> Integer.compare(b, a));
				Collections.sort(MCCabeMaxValues, (a, b) -> Integer.compare(b, a));
				Collections.sort(statementsMaxValues, (a, b) -> Integer.compare(b, a));
				Collections.sort(descissionsMaxValues, (a, b) -> Integer.compare(b, a));
				
				for (Integer v : MCCabeMaxValues) {
					pwMccabeMax.print(v);
					pwMccabeMax.print(SEPARATOR);
				}
				
				for (Integer v : nodesMaxValues) {
					pwNodesMax.print(v);
					pwNodesMax.print(SEPARATOR);
				}
				
				resultsMCCabe.put(degree, MCCabeMaxValues);
				resultsNodes.put(degree, nodesMaxValues);
				resultsDecissions.put(degree, descissionsMaxValues);
				resultsStatements.put(degree, statementsMaxValues);
				
				nodesMaxValues = new ArrayList<>();
				MCCabeMaxValues = new ArrayList<>();
				statementsMaxValues = new ArrayList<>();
				descissionsMaxValues = new ArrayList<>();
				
				for (PrintWriter pw : pws) {
					pw.println();
				}
			}
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
//		writeRFile(resultsMCCabe, "MCCabe");
		writeRFile(resultsDecissions, "Decissions", numberOfDecsissions);
		writeRFile(resultsStatements, "Statements", numberOfStatements);
//		writeRFile(resultsNodes, "Nodes");
	}

	private void writeRFile(Map<Integer, List<Integer>> results, String name, int maxValue) {
		if (maxDegree == 0) {
			System.out.println(name + ": " + maxValue);
			return;
		}
		int[][] res = new int[results.get(maxDegree).size()][maxDegree];
		for (int d = 1; d <= maxDegree; d++) {
			int i = 0;
			for (int value : results.get(d)) {
				res[i++][d - 1] = value; 
			}
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("all: " + maxValue);
		sb.append("\r\n");
		
		for (int degree = 1; degree <= maxDegree;degree++) {
			sb.append(degree).append(',');
		}
		sb.setCharAt(sb.length() - 1, '\r');
		sb.append('\n');
		
		for (int[] row : res) {
			for (int i : row) {
				if (i > 0) {
					sb.append(i);
				}
				sb.append(',');
			}
			sb.setCharAt(sb.length() - 1, '\r');
			sb.append('\n');
		}
		File mccabe = new File(folder.getName() + File.separator + VarvizView.PROJECT_NAME + "_" + name+ "_R.csv");
		try (PrintWriter pwMccabe = new PrintWriter(mccabe, StandardCharsets.UTF_8.name())) {
			pwMccabe.print(sb.toString());
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
		
	private void createAllTraces(PrintStream originalOutputStream, String[] args, int degree) {
		File mccabe = new File(folder.getName() + File.separator + VarvizView.PROJECT_NAME + "_" + degree + "_mccabe.csv");
		File nodes = new File(folder.getName() + File.separator + VarvizView.PROJECT_NAME + "_" + degree + "_nodes.csv");
		Collection<IFStatement<?>> ifStatement = JPF.vatrace.getMain().collectIFStatements();
		MessageConsole myConsole = findAndCreateConsole("create all traces");
		PrintStream consoleStream = createOutputStream(originalOutputStream, myConsole.newMessageStream());
		
		consoleStream.println("create file: " + mccabe.getAbsolutePath());
		consoleStream.println("create file: " + nodes.getAbsolutePath());
		try (PrintWriter pwMccabe = new PrintWriter(mccabe, StandardCharsets.UTF_8.name()); 
				PrintWriter pwNodes = new PrintWriter(nodes, StandardCharsets.UTF_8.name())) {

			pwMccabe.print("Feature");
			pwNodes.print("Feature");
			for (int i = 0; i <= Statistics.insns; i += STEP_SIZE) {
				pwMccabe.print(SEPARATOR);
				pwMccabe.print(i);
				pwNodes.print(SEPARATOR);
				pwNodes.print(i);
			}

			runs = 0;
			int numberOfOptionalFeatures = 0;
			for (SingleFeatureExpr f : Conditional.features.values()) {
				if (!Conditional.isTautology(f)) {
					numberOfOptionalFeatures++;
				}
			}
			
			maxRuns = numberOfOptionalFeatures;
			for (int i = 2; i <= degree; i++) {
				maxRuns = (maxRuns * (numberOfOptionalFeatures - i + 1)) / i;
			}
			
			SingleFeatureExpr[] features = new SingleFeatureExpr[numberOfOptionalFeatures];
			int index = 0;
			for (SingleFeatureExpr f : Conditional.features.values()) {
				if (!Conditional.isTautology(f)) {
					features[index++] = f;
				}
			}
			runSlicedProgram(features, 0, consoleStream, args, ifStatement, degree, new ArrayList<>(degree), pwMccabe, pwNodes);
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	private float runs = 0;
	private float maxRuns = 0;

	private void runSlicedProgram(SingleFeatureExpr[] features, int pointer, PrintStream consoleStream, String[] args,
			Collection<IFStatement<?>> ifStatement, int numberOfFeatures,
			Collection<SingleFeatureExpr> sliceFeatures, PrintWriter... printWriters) {
		if (numberOfFeatures == 0) {
			runSlicedProgram(consoleStream, args, ifStatement, sliceFeatures, printWriters);
			return;
		}
		for (;pointer < features.length; pointer++) {
			final SingleFeatureExpr singleFeatureExpr = features[pointer];
			sliceFeatures.add(singleFeatureExpr);
			runSlicedProgram(features, pointer + 1, consoleStream, args, ifStatement, numberOfFeatures - 1, sliceFeatures, printWriters);
			sliceFeatures.remove(singleFeatureExpr);
		}
	}

	private void runSlicedProgram(PrintStream consoleStream, String[] args,
			Collection<IFStatement<?>> ifStatements, Collection<SingleFeatureExpr> sliceFeatures, PrintWriter... printWriters) {
		runs++;
		consoleStream.println((runs / maxRuns) * 100 + "% finished of " + maxRuns);
		for (PrintWriter pw : printWriters) {
			pw.println();
			for (SingleFeatureExpr singleFeatureExpr : sliceFeatures) {
				pw.print(Conditional.getCTXString(singleFeatureExpr));
				pw.print(" ");
			}
		}
//		ThreadInfo.maxInstruction = Integer.MAX_VALUE;
		Conditional.additionalConstraint = BDDFeatureExprFactory.True();
		JPF.ignoredFeatures.clear();
		
		IgnoreContext.removeContext(sliceFeatures, ifStatements);
		
		consoleStream.print(VarvizView.generator.getIgnoredFeatures().size() + " ignored for ");
		sliceFeatures.forEach(f -> consoleStream.print(Conditional.getCTXString(f) + " - "));
		consoleStream.println();
		
		
		
		for (Entry<FeatureExpr, Boolean> f : VarvizView.generator.getIgnoredFeatures().entrySet()) {
			System.out.println(f);
		}
		
		while (true) {
//			ThreadInfo.maxInstruction += STEP_SIZE;
			int round = 0; 
			while (round++ < 10) {
				JPF.vatrace = new Trace();
				JPF.vatrace.filter = new Or(new And(new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
				try {
					JPF.main(args);
					break;
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("restart: " + round);
				}
			}

			JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)),
					new And(VarvizView.basefilter, new ExceptionFilter()));
			JPF.vatrace.finalizeGraph();
			int edges = JPF.vatrace.getEdges().size();
			
			int numberOfDecsissions = JPF.vatrace.getMain().accumulate((s, v) -> s instanceof IFStatement ? v + 1 : v, 0);
			int nodes = JPF.vatrace.getMain().size();

			final int MCCabe = edges - nodes + 2;
			for (PrintWriter pw : printWriters) {
				pw.print(SEPARATOR);
			}
			printWriters[0].print(MCCabe);
			printWriters[1].print(nodes);

			consoleStream.print(".");
			consoleStream.flush();

			if (Statistics.insns < ThreadInfo.maxInstruction) {
				nodesMaxValues.add(nodes);
				MCCabeMaxValues.add(MCCabe);
				statementsMaxValues.add(nodes - numberOfDecsissions);
				descissionsMaxValues.add(numberOfDecsissions);
				break;
			}
		}
		consoleStream.println();
		
		JPF.ignoredFeatures.clear();
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
