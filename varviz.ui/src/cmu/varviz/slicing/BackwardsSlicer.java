package cmu.varviz.slicing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cmu.conditional.Conditional;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.Shape;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.vatrace.FieldPutStatement;
import cmu.vatrace.LocalGetStatement;
import cmu.vatrace.ReturnStatement;
import de.fosd.typechef.featureexpr.FeatureExpr;
import gov.nasa.jpf.jvm.bytecode.FieldInstruction;
import gov.nasa.jpf.jvm.bytecode.LocalVariableInstruction;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.MethodInfo;

public final class BackwardsSlicer {

	private final Map<MethodElement, Statement> ifStatementGraph = new HashMap<>();
	private final Map<String, Set<Statement>> fields = new HashMap<>();
	private final Map<String, Set<Statement>> localVars = new HashMap<>();
	private final Map<String, Set<Method>> methods = new HashMap<>();
	
	private final DataFlowEngine dataFlowEngine = new DataFlowEngine();

	public void slice(String[] classpath, Statement sliceSatement, Trace trace) {
		createMaps(trace.getMain());
		createIfStatementRelation(trace.getMain());
				
		Set<Statement> dependencies = new HashSet<>();
		dependencies.add(sliceSatement);
		Set<Statement> allDependencies = new HashSet<>();
		while (!dependencies.isEmpty()) {
			Set<Statement> statements = dependencies;
			dependencies = new HashSet<>();
			for (Statement statement : statements) {
				if (!allDependencies.contains(statement)) {
					allDependencies.add(statement);
					Set<Statement> nextDependencies = getDependentStatements(statement, classpath);
					dependencies.addAll(nextDependencies);
				}
			}
		}
		
		trace.setFilter(allDependencies::contains);
		trace.finalizeGraph();
	}
	
	private Set<Statement> getDependentStatements(Statement statement, String[] classpath) {
		Object content = statement.getContent();
		int instructionIndex = ((Instruction) content).getInstructionIndex();
		MethodInfo method = ((Instruction) content).getMethodInfo();
		String methodName = method.getName();
		String className = method.getClassInfo().getSimpleName();
		String packageName = method.getClassInfo().getPackageName();
		
		MethodDataFlow methodDataFlow = null;
		for (String cp : classpath) {
			if (!cp.endsWith(".jar")) {
				methodDataFlow = dataFlowEngine.getDataDependncies(cp + "/" +  packageName.replaceAll("\\.", "/"), className + ".class", methodName);
			}
			if (methodDataFlow != null) {
				break;
			}
		}
		if (methodDataFlow == null) {
			System.err.println("no method information: " + method);
			return Collections.emptySet();
		}
		Set<IDependency> dependencies = methodDataFlow.getDependencies(instructionIndex).getDependencies();
		Set<Statement> dependentStatements = new HashSet<>();
		for (IDependency dependency : dependencies) {
			if (dependency instanceof FieldDependency) {
				String fieldName = ((FieldDependency) dependency).fieldName;
				if (fields.containsKey(fieldName)) {
					Set<Statement> fieldStatements = fields.get(fieldName);
					dependentStatements.addAll(fieldStatements);
				}
			} else if (dependency instanceof LocalVarDependency) {
				String varName = ((LocalVarDependency) dependency).varName;
				if (localVars.containsKey(varName)) {
					Set<Statement> localVarStatements = localVars.get(varName);
					dependentStatements.addAll(localVarStatements);
				}
			} else if (dependency instanceof MethodDependency) {
				String mName = ((MethodDependency) dependency).methodName;
				String mOwner = ((MethodDependency) dependency).owner.replaceAll("/", ".");
				Set<Method> dependentMethods = methods.get(mOwner + "#" + mName + "()");
				if (dependentMethods == null) {
					Set<MethodElement> previous = new HashSet<>(statement.getFrom().toMap().keySet());
					while (!previous.isEmpty()) {
						Iterator<MethodElement> it = previous.iterator();
						MethodElement next = it.next();
						it.remove();
						
						if (next instanceof FieldPutStatement || next instanceof LocalGetStatement || next instanceof Method) {
							Set<MethodElement> keySet = next.getFrom().toMap().keySet();
							for (MethodElement methodElement : keySet) {
								if (methodElement != null) {
									previous.add(methodElement);
								}
							}
							if (next instanceof Statement) {
								dependentStatements.add((Statement) next);
							}
						}
					}
					continue;
				}
				Set<Statement> returnStatements = getRetrunStatements(dependentMethods);
				dependentStatements.addAll(returnStatements);
				
			}
		}
		
		Statement ifStatement = ifStatementGraph.get(statement);
		if (ifStatement != null) {
			dependentStatements.add(ifStatement);
		}
		return dependentStatements;
	}

	private Set<Statement> getRetrunStatements(Set<Method> methods) {
		Set<Statement> returnStatements = new HashSet<>();
		for (Method method : methods) {
			getRetrunStatements(method, returnStatements);
		}
		return returnStatements;
	}

	private void getRetrunStatements(Method method, Set<Statement> returnStatements) {
		for (MethodElement element: method.getChildren()) {
			if (element instanceof ReturnStatement) {
				returnStatements.add((Statement) element);
			}
		}
	}

	/**
	 * Creates a graph structure to find the if statement for each element
	 * @param method
	 */
	private final void createIfStatementRelation(Method method) {
		final List<MethodElement> children = method.getChildren();
		for (int i = 0; i < children.size(); i++) {
			final MethodElement element = children.get(i);
			if (element instanceof Statement && ((Statement) element).getShape() == Shape.Mdiamond) {
				Statement ifStatement = (Statement) element;
				createIfStatementRelation(ifStatement);
			}
			if (element instanceof Method) {
				createIfStatementRelation((Method)element);
			}
		}
	}
	
	private void createIfStatementRelation(Statement ifStatement) {
		FeatureExpr ifCondition = ifStatement.getCTX();
		List<MethodElement> nextList = new ArrayList<>();
		nextList.addAll(ifStatement.getTo().toList());
		// TODO why would there be visited elements / is there a loop in the trace?
		Set<MethodElement> visited = new HashSet<>();
		while (!nextList.isEmpty()) {
			MethodElement next = nextList.remove(0);
			if (next == null) {
				continue;
			}
			if (visited.contains(next)) {
				continue;
			}
			visited.add(next);
			
			FeatureExpr nextCondition = next.getCTX();
			if (Conditional.isSatisfiable(Conditional.andNot(ifCondition, nextCondition))) {
				ifStatementGraph.put(next, ifStatement);
				for (MethodElement methodElement : next.getTo().toList()) {
					nextList.add(methodElement);
				}
			}
		}
	}
	
	private void createMaps(Method method) {
		methods.computeIfAbsent(method.toString(), (k) -> new HashSet<>());
		methods.get(method.toString()).add(method);
		final List<MethodElement> children = method.getChildren();
		for (MethodElement element : children) {
			if (element instanceof Statement) {
				Statement statement = (Statement) element;
				Instruction instruction = (Instruction) statement.getContent();
				if (instruction instanceof FieldInstruction) {
					String fieldName = ((FieldInstruction) instruction).getFieldName();
					fields.computeIfAbsent(fieldName, (k) -> new HashSet<>());
					fields.get(fieldName).add(statement);
				} else if (instruction instanceof LocalVariableInstruction) {
					// TODO improve this: local var dependencies only matter inside of the current method
					String localVarName = ((LocalVariableInstruction) instruction).getLocalVariableName();
					localVars.computeIfAbsent(localVarName, (k) -> new HashSet<>());
					localVars.get(localVarName).add(statement);
				}
				
			}
			if (element instanceof Method) {
				createMaps((Method)element);
			}
		}
	}

}
