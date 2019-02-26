package cmu.varviz.slicing;

import cmu.samplej.instrumentation.InstrumentationUtils;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.IincInsnNode;
import jdk.internal.org.objectweb.asm.tree.LocalVariableNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;

public class DependencyFactory {

	private final MethodNode methodNode;

	public DependencyFactory(MethodNode methodNode) {
		this.methodNode = methodNode;
	}

	public IDependency createDependency(AbstractInsnNode node) {
		if (node instanceof VarInsnNode) {
			final LocalVariableNode localVariableNode = InstrumentationUtils.getLocalVariableNode(methodNode, ((VarInsnNode) node).var, node);
			if (localVariableNode == null) {
				// FIXME
				return new LocalVarDependency("UNKNOWN");
			}
			return new LocalVarDependency((localVariableNode).name);
		} else if (node instanceof IincInsnNode) {
			final LocalVariableNode localVariableNode = InstrumentationUtils.getLocalVariableNode(methodNode, ((IincInsnNode) node).var, node);
			return new LocalVarDependency((localVariableNode).name);
		} else if (node instanceof FieldInsnNode) {
			return new FieldDependency(((FieldInsnNode) node).owner, ((FieldInsnNode) node).name);
		} else if (node instanceof MethodInsnNode) {
			return new MethodDependency(((MethodInsnNode) node).owner, ((MethodInsnNode) node).name);
		} else {
			throw new RuntimeException("cannot create dependency for " + node + " " + node.getClass());
		}
	}
}
