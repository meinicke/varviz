package cmu.varviz.slicing;

import java.util.List;

import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.IincInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;
import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicInterpreter;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicValue;

/**
 * Annotates the control flow graph with conditional nodes.
 * 
 * @author Jens Meinicke
 *
 */
public class DataFlowInterpreter extends BasicInterpreter {

	private final DependencyFactory dependencyFactory;

	public DataFlowInterpreter(MethodNode methodNode) {
		dependencyFactory = new DependencyFactory(methodNode);
	}

	@Override
	public BasicValue copyOperation(AbstractInsnNode instr, BasicValue basicValue) throws AnalyzerException {
		if (instr.getOpcode() == ALOAD) {
			IDependency createDependency = dependencyFactory.createDependency(instr);
			return new DataDependencyValue(basicValue, createDependency);
		}
		if (instr instanceof VarInsnNode) {
			IDependency createDependency = dependencyFactory.createDependency(instr);
			return new DataDependencyValue(basicValue, createDependency);
		}
		
		return basicValue;
	}

	@Override
	public BasicValue unaryOperation(AbstractInsnNode instr, BasicValue basicValue) throws AnalyzerException {
		if (instr instanceof FieldInsnNode) {
			switch (instr.getOpcode()) {
				case PUTSTATIC:
				case GETFIELD:
					return new DataDependencyValue(basicValue, dependencyFactory.createDependency(instr));
			}
		} else if (instr instanceof IincInsnNode || instr instanceof VarInsnNode) {
			return new DataDependencyValue(basicValue, dependencyFactory.createDependency(instr));
		}
	
		
		return basicValue;
	}


	@Override
	public BasicValue binaryOperation(AbstractInsnNode instr, BasicValue basicValue1, BasicValue basicValue2) throws AnalyzerException {
		if (instr.getOpcode() == PUTFIELD) {
			return new DataDependencyValue(basicValue2, dependencyFactory.createDependency(instr));
		}
		return super.binaryOperation(instr, basicValue1, basicValue2);
	}
	
	@Override
	public BasicValue newValue(final Type type) {
		if (type == null) {
			return BasicValue.UNINITIALIZED_VALUE;
		}
		if (type.getSort() == Type.VOID) {
			return null;
		}
		return new BasicValue(type);
	}
	
	@Override
	public BasicValue newOperation(AbstractInsnNode instr) throws AnalyzerException {
		if (instr instanceof FieldInsnNode) {
			return new DataDependencyValue(super.newOperation(instr), dependencyFactory.createDependency(instr));
		}
		return super.newOperation(instr);
	}

	@Override
	public BasicValue ternaryOperation(AbstractInsnNode instr, BasicValue arg1, BasicValue arg2, BasicValue arg3) throws AnalyzerException {
		return super.ternaryOperation(instr, arg1, arg2, arg3);
	}

	@Override
	public BasicValue merge(BasicValue arg0, BasicValue arg1) {
		if (arg0 instanceof DataDependencyValue || arg1 instanceof DataDependencyValue) {
			return new DataDependencyValue(arg0, arg1);
		}
		return super.merge(arg0, arg1);
	}

	@Override
	public BasicValue naryOperation(AbstractInsnNode instr, List<? extends BasicValue> arg1) throws AnalyzerException {
		if (instr instanceof MethodInsnNode) {
			if (((MethodInsnNode)instr).name.equals("equals")) {
				// workaround for native/peer methods -> adds the parameters
				return new DataDependencyValue(super.naryOperation(instr, arg1).getType(), arg1, dependencyFactory.createDependency(instr));
			}
			return new DataDependencyValue(super.naryOperation(instr, arg1), dependencyFactory.createDependency(instr));
		}
		return super.naryOperation(instr, arg1);
	}

}
