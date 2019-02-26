package cmu.varviz.slicing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicValue;
import jdk.internal.org.objectweb.asm.tree.analysis.Frame;

public final class DataFlowEngine {

	private Map<String, Map<String, MethodDataFlow>> cache = new HashMap<>();
	
	public MethodDataFlow getDataDependncies(String binPath, String classPath, String methodName) {
		Map<String, MethodDataFlow> classCache = cache.computeIfAbsent(classPath, x -> new HashMap<>());
		MethodDataFlow methodDataFlow = classCache.get(methodName);
		if (methodDataFlow == null) {
			ClassNode classNode = getClassNode(classPath, binPath);
			if (classNode == null) {
				// case search in jars
				return null;
			}
			MethodNode methodNode = getMethodNode(classNode, methodName); 
			methodDataFlow = getDataDependncies(classNode, methodNode);
		}
		return methodDataFlow;
	}
	
	private @Nullable ClassNode getClassNode(String classPath, String binPath) {
		File binFolder = new File(binPath);
		
		File classFile = new File(binFolder, classPath);
		try (InputStream in = new FileInputStream(classFile)) {
			ClassReader classReader = new ClassReader(in);
			ClassNode classNode = new ClassNode();
			classReader.accept(classNode, 0);
			return classNode;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private @Nullable MethodNode getMethodNode(ClassNode classNode, String methodName) {
		for (MethodNode methodNode : classNode.methods) {
			if (methodNode.name.equals(methodName)) {
				return methodNode;
			}
		}
		return null;
	}

	private MethodDataFlow getDataDependncies(ClassNode classNode, MethodNode methodNode) {
		try {
			DataFlowInterpreter interpreter = new DataFlowInterpreter(methodNode);
			Frame<BasicValue>[] frames = new Analyzer<>(interpreter).analyze(classNode.name, methodNode);
			return new MethodDataFlow(methodNode, frames);
		} catch (AnalyzerException e) {
			e.printStackTrace();
		}
		return new MethodDataFlow(methodNode, new Frame[0]);
	}

}
