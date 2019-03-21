package cmu.varviz.slicing;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import cmu.varviz.utils.ByteCodeUtils;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.FrameNode;
import jdk.internal.org.objectweb.asm.tree.InsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.LineNumberNode;
import jdk.internal.org.objectweb.asm.tree.LocalVariableNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicValue;
import jdk.internal.org.objectweb.asm.tree.analysis.Frame;
import jdk.internal.org.objectweb.asm.util.Printer;
import jdk.internal.org.objectweb.asm.util.Textifier;
import jdk.internal.org.objectweb.asm.util.TraceMethodVisitor;

/**
 * Represents information on data dependencies in this method.
 * 
 * @author Jens Meinicke
 *
 */
public class MethodDataFlow {

	private final MethodNode methodNode;
	private final Frame<BasicValue>[] frames;

	public MethodDataFlow(MethodNode methodNode, Frame<BasicValue>[] frames) {
		this.methodNode = methodNode;
		this.frames = frames;
	}

	public DataFlowDependencies getDependencies(int index) {
		final DataFlowDependencies dependencies = new DataFlowDependencies();
		
		int actualIndex = getActualIndex(index);
		final AbstractInsnNode instruction = methodNode.instructions.get(actualIndex);
		final Frame<BasicValue> frame = frames[actualIndex];
		switch (instruction.getOpcode()) {
		case IF_ICMPEQ:
		case IF_ICMPNE:
		case IF_ICMPLT:
		case IF_ICMPGE:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ACMPEQ:
		case IF_ACMPNE:
			BasicValue value = frame.pop();
			if (value instanceof DataDependencyValue) {
				dependencies.addDependencies(((DataDependencyValue) value).dependencies);
			}
			value = frame.pop();
			if (value instanceof DataDependencyValue) {
				dependencies.addDependencies(((DataDependencyValue) value).dependencies);
			}
			break;
		case IFEQ:
		case IFNE:
		case IFLT:
		case IFGE:
		case IFGT:
		case IFLE:
		case IFNULL:
		case IFNONNULL:
		case LRETURN:
		case FRETURN:
		case ARETURN:
		case IRETURN:
			value = frame.pop();
			if (value instanceof DataDependencyValue) {
				dependencies.addDependencies(((DataDependencyValue) value).dependencies);
			}
			break;
		case ALOAD:
		case ILOAD:
		case FLOAD:
		case DLOAD:
		case LLOAD:
			value = frame.getLocal(((VarInsnNode) instruction).var);
			if (value instanceof DataDependencyValue) {
				dependencies.addDependencies(((DataDependencyValue) value).dependencies);
			}
			break;
		case PUTFIELD:
		case PUTSTATIC:
			value = frame.pop();
			IDependency dataDependencyValue = new DependencyFactory(methodNode).createDependency(instruction);
			dependencies.addDependency(dataDependencyValue);
			if (value instanceof DataDependencyValue) {
				dependencies.addDependencies(((DataDependencyValue) value).dependencies);
			}
			break;
		case ISTORE:
		case ASTORE:
		case FSTORE:
		case LSTORE:
			value = frame.pop();
			dataDependencyValue = new DependencyFactory(methodNode).createDependency(instruction);
			dependencies.addDependency(dataDependencyValue);
			if (value instanceof DataDependencyValue) {
				dependencies.addDependencies(((DataDependencyValue) value).dependencies);
			}
			break;
		case GETSTATIC:
		case GETFIELD:
		case IINC: 
			dataDependencyValue = new DependencyFactory(methodNode).createDependency(instruction);
			dependencies.addDependency(dataDependencyValue);
			break;
		case ATHROW:
			break;
		default:
			throw new RuntimeException(ByteCodeUtils.getMnemonic(instruction) + " not handled: " + instruction);
		}
		return dependencies;
	}

	/**
	 * VarexJ does not include Label and Line numbers in the index so we need to compute an adjusted index to get the correct frame. 
	 */
	private int getActualIndex(final int index) {
		int j = 0;
		for (int i = 0; i < methodNode.instructions.size(); i++) {
			final AbstractInsnNode instruction = methodNode.instructions.get(i);
			if (instruction instanceof FrameNode || instruction instanceof LabelNode
					|| instruction instanceof LineNumberNode) {
				j++;
			} else {
				if (i - j == index) {
					return i;
				}
			}
		}
		throw new RuntimeException("Failed to compute actual index:" + index + " " + methodNode.name);
	}

}
