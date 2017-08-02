package cmu.varviz.trace.generator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import cmu.varviz.trace.Edge;
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
import interaction.Excel;
import interaction.InteractionFinder;
import scala.collection.Iterator;
import scala.collection.immutable.Set;


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
			JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
			FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
			JPF.main(args);
//			Conditional.additionalConstraint = BDDFeatureExprFactory.True();

//			if (VarvizView.reExecuteForExceptionFeatures) {
//				FeatureExpr exceptionContext = JPF.vatrace.getExceptionContext();
//				IgnoreContext.removeContext(exceptionContext);
//				if (!JPF.ignoredFeatures.isEmpty()) {
//					// second run for important features
//					myConsole = findAndCreateConsole("VarexJ: " + resource.getProject().getName() + ":" + runConfig.getClassToLaunch() + " (exception features only)");
//					myConsole.clearConsole();
//					PrintStream consoleStream = createOutputStream(originalOutputStream, myConsole.newMessageStream());
//					System.setOut(consoleStream);
//					JPF.vatrace = new Trace();
//					JPF.vatrace.filter = new Or(new And(VarvizView.basefilter, new InteractionFilter(VarvizView.minDegree)), new ExceptionFilter());
//					JPF.main(args);
//					Conditional.additionalConstraint = BDDFeatureExprFactory.True();
//					JPF.ignoredFeatures.clear();
//				}
//			}
			JPF.vatrace.finalizeGraph();
			VarvizView.TRACE = JPF.vatrace;
			
			//XXX Lari
			List<Edge> ed = VarvizView.TRACE.getEdges();
			getImplications(ed, workingDir);
			
			
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

	//Lari
	private void getImplications(List<Edge> edges, File workingDir) {
		
		List<FeatureExpr> expressions = new ArrayList<>();		
		for (Edge edge : edges) {
			FeatureExpr ctx = edge.getCtx();
			String ctxString = Conditional.getCTXString(ctx);				
			
			if (!expressions.contains(ctx) && ctxString != "True") {
				expressions.add(ctx);
				System.out.println(ctx);
			} 
		}
		
		InteractionFinder finder = new InteractionFinder();
		finder.getInteractionsTable(expressions);
		
//		Collection<SingleFeatureExpr> features = Conditional.features.values();//the whole set of features
//		List<PairExp> exprPairs = new ArrayList<>();//the pairs present in the expressions
//		List<PairExp> contain = new ArrayList<>();//only to not repeat the same pair "do not interact"
//		List<SingleFeatureExpr> noEffectlist = new ArrayList<>();
//		Map<PairExp, List<String>> hashMap = new HashMap<>();
//		
//		exprPairs = getExpressionsPairs(expressions);//get all the pairs in the expressions
//		noEffectlist = getNoEffectlist(features, expressions);//list of features that do not appear in the expressions
//		
//		for (SingleFeatureExpr feature1 : features) {
//			
//			if (Conditional.isTautology(feature1)) {
//				continue;
//			}
//			final FeatureExpr unique = createUnique2(feature1, expressions);
//			
//			if (Conditional.isContradiction(unique)) {//when a feature doesn't appear in the expressions
//				continue;
//			}		
//			
//			for (SingleFeatureExpr feature2 : features) {
//				if (feature1 == feature2 || Conditional.isTautology(feature2)) {
//					continue;//Conditional.isTautology(feature2) when the feature is the feature model root feature
//				}			
//				FeatureExpr first = feature2.implies(unique.not());
//				FeatureExpr second = feature2.not().implies(unique.not());
//				String phrase = new String("a");
//									
//				if (first.isTautology()) {
//					//System.out.println(Conditional.getCTXString(feature1) + " suppresses " + Conditional.getCTXString(feature2));
//					phrase = Conditional.getCTXString(feature1) + " suppresses " + Conditional.getCTXString(feature2);
//				}
//				if (second.isTautology()) {
//					//System.out.println(Conditional.getCTXString(feature1) + " enables " + Conditional.getCTXString(feature2));
//					phrase = Conditional.getCTXString(feature1) + " enables " + Conditional.getCTXString(feature2);
//				}			
//					
//				PairExp pairAB = new PairExp(feature1, feature2);
//				PairExp pairBA = new PairExp(feature2, feature1);	
//				
//				//if the pair is no present in the expressions
//				if (!exprPairs.contains(pairAB) && !exprPairs.contains(pairBA) && !contain.contains(pairAB)){
//					
//					if (!noEffectlist.contains(feature1) && !noEffectlist.contains(feature2)) {				
//						phrase = "do not interact";
//					}
//					else if(noEffectlist.contains(feature1)){
//						phrase = Conditional.getCTXString(feature1) + " has no effect";
//					}
//					
//					else if(noEffectlist.contains(feature2)){
//						phrase = Conditional.getCTXString(feature2) + " has no effect";
//					}
//					contain.add(pairAB);//to avoid repeat the same pair in a different order
//					contain.add(pairBA);
//				}
//				
//				if((!hashMap.containsKey(pairAB)) && (!hashMap.containsKey(pairBA)) && (!phrase.equals("a"))){
//					hashMap.put(pairAB, new ArrayList<>());
//					hashMap.get(pairAB).add(phrase);
//				}
//				else{
//					if(!phrase.equals("a")){
//					hashMap.get(pairAB).add(phrase);
//					}
//				}
//			}	
//		}
//		
//		//when both features of a pair have no effect
//		addDoubleNoEffect(noEffectlist, hashMap);
//		
//		//when both features of a pair interact but they are not suppressing or enabling each other
//		String phrase = "a";
//		for(PairExp pair: exprPairs){
//			if(!hashMap.containsKey(pair)){
//				phrase = "do interact";
//				hashMap.put(pair, new ArrayList<>());
//				hashMap.get(pair).add(phrase);			
//			}
//		}
//		
//		//creates excel table
//		createExcelTable(hashMap, features, workingDir);		
//		
	}

	private void createExcelTable(Map<PairExp, List<String>> hashMap, Collection<SingleFeatureExpr> features, File workingDir) {
		//print hash
		for (Entry<PairExp, List<String>> pair : hashMap.entrySet()) {
			System.out.println("Pair = [" + pair.getKey() + " , " + pair.getValue() + "]");
		}
		
		int c = features.size();
		for (SingleFeatureExpr feature1 : features) {
			 if (Conditional.isTautology(feature1)) {
					c--;
				}
		 }
		
		 Map < String, Object[] > excelTable = new TreeMap < String, Object[] >();
		 int count = 0;
		 Object[] line1 = new Object[c+1];
		 line1[count++] = "Features";//array of 1 line
		 for (SingleFeatureExpr feature1 : features) {
			 if (Conditional.isTautology(feature1)) {
					continue;
				}
			 line1[count++] = Conditional.getCTXString(feature1);
		 }
		 excelTable.put( Integer.toString(1), line1);//first line with the name of all features
		 
		 
		 int excelline = 2;
		 for (int i = 1; i< line1.length; i++) {
			 count = 1;
			 Object[] line = new Object[c+1];
			 line[0] = line1[i];
			 for (int j = 1; j< line1.length; j++) {
				 
				 if(line1[j].equals(line[0])){
					 line[count] = " X ";
					 System.out.println("line[" + count + "] = " + line[count]);
					 count++;
					 continue;
				 }
				 
				 for (Entry<PairExp, List<String>> pair : hashMap.entrySet()) {
					 String A = Conditional.getCTXString(pair.getKey().A);
					 String B = Conditional.getCTXString(pair.getKey().B);
					 //System.out.println("A: " + A + " = " + line[0] + " && " + "B: " + B + " = " + line1[j]);
					// System.out.println("A: " + A + " = " + line1[j] + " && " + "B: " + B + " = " + line[0]);
					 
					 
					 if(A.equals(line[0]) && B.equals(line1[j])
						|| A.equals(line1[j]) && B.equals(line[0])) {
						 
						 line[count] = pair.getValue().get(0);
						 if(pair.getValue().size() >1){
							 line[count] = "both have no effect";
						 }
						 
						 System.out.println("line[" + count + "] = " + line[count]);
						 count++;
					 }
				 }
			 }
			 
			 excelTable.put( Integer.toString(excelline++), line);
		 }
		
		 
		Excel ex = new Excel();
		try {
			ex.writesheet(excelTable, workingDir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	
	//add in the table when both features of a pair have no effect
	private void addDoubleNoEffect(List<SingleFeatureExpr> noEffectlist, Map<PairExp, List<String>> hashMap) {
		
		if(noEffectlist.size()>1){
			String phrase = "a";
			for(int i = 0; i<noEffectlist.size(); i++){
				for(int j = i+1; j<noEffectlist.size(); j++){	
					PairExp pairNoEffect = new PairExp(noEffectlist.get(i), noEffectlist.get(j));
					PairExp pairNoEffect2 = new PairExp(noEffectlist.get(j), noEffectlist.get(i));
					if(!hashMap.containsKey(pairNoEffect) && !hashMap.containsKey(pairNoEffect2)){
						hashMap.put(pairNoEffect, new ArrayList<>());
						phrase = Conditional.getCTXString(noEffectlist.get(i)) + " has no effect";
						hashMap.get(pairNoEffect).add(phrase);
						phrase = Conditional.getCTXString(noEffectlist.get(j)) + " has no effect";
						hashMap.get(pairNoEffect).add(phrase);
					}
					else{
						hashMap.get(pairNoEffect).add(phrase);
					}
				}
			}
		}		
	}

	private FeatureExpr createUnique2(SingleFeatureExpr feature, List<FeatureExpr> contexts) {
		FeatureExpr unique = FeatureExprFactory.False();
		for (FeatureExpr ctx : contexts) {
			//System.out.println("ctx.unique(feature):" + ctx.unique(feature));
			unique = unique.or(ctx.unique(feature));
		}
		return unique;
	}
	
	//list of features that do not appear in the expressions
	private List<SingleFeatureExpr> getNoEffectlist(Collection<SingleFeatureExpr> features,
			List<FeatureExpr> expressions) {
		
		List<SingleFeatureExpr> noEffectlist = new ArrayList<>();
		for (SingleFeatureExpr feature : features) {
			final FeatureExpr unique = createUnique2(feature, expressions);
			if (Conditional.isContradiction(unique)) {//when a feature doesn't appear in the expressions
				noEffectlist.add(feature);
			}
		}
		return noEffectlist;
	}

	//get all the pairs in the expressions
	private List<PairExp> getExpressionsPairs(List<FeatureExpr> expressions) {
		
		List<PairExp> exprPairs = new ArrayList<>();
		int cutNumber = 7;
		
		for(FeatureExpr featureexpr : expressions){
			
			Set<String> dist = featureexpr.collectDistinctFeatures();
			if(dist.size() == 2){
				scala.collection.Iterator<String> it = dist.iterator();
				String s = it.next().substring(cutNumber);
				String s2 = it.next().substring(cutNumber);
				
				// Step 2: get features
				SingleFeatureExpr f1  = Conditional.createFeature(s);
				SingleFeatureExpr f2 = Conditional.createFeature(s2);
				
				PairExp pairAB = new PairExp(f1, f2);			
				if (!exprPairs.contains(pairAB)){
					exprPairs.add(pairAB);
				}
			}
//			else if (dist.size() == 3){
//				scala.collection.Iterator<String> it = dist.iterator();
//				String s = it.next().substring(cutNumber);
//				String s2 = it.next().substring(cutNumber);
//				String s3 = it.next().substring(cutNumber);
//				
//				// Step 2: get features
//				SingleFeatureExpr f1  = Conditional.createFeature(s);
//				SingleFeatureExpr f2 = Conditional.createFeature(s2);
//				
//				PairExp pairAB = new PairExp(f1, f2);			
//				if (!exprPairs.contains(pairAB)){
//					exprPairs.add(pairAB);
//				}
//			}
		}
		return exprPairs;
	}

	class PairExp {
		FeatureExpr A, B;

		public PairExp(SingleFeatureExpr a, SingleFeatureExpr b) {
			A = a;
			B = b;
		}
		public FeatureExpr getA() {
			return A;
		}
		public FeatureExpr getB() {
			return B;
		}
		
		@Override
		public boolean equals(Object obj) {
			PairExp other =  (PairExp) obj;
			return (A.equals(other.A) && B.equals(other.B)) ||
					(A.equals(other.B) && B.equals(other.A));
		}
		
		@Override
		public int hashCode() {
			return A.hashCode() * B.hashCode() * 31;
		}
		
		@Override
		public String toString() {
			return Conditional.getCTXString(A) + ", " + Conditional.getCTXString(B);
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
