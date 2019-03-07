package cmu.varviz.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.util.Printer;
import jdk.internal.org.objectweb.asm.util.Textifier;
import jdk.internal.org.objectweb.asm.util.TraceMethodVisitor;

/**
 * 
 * @author Jens Meinicke
 *
 */
public final class ByteCodeUtils {

	private ByteCodeUtils() {
		// private constructor
	}
	
	private static final Printer printer = new Textifier();
	private static final TraceMethodVisitor mp = new TraceMethodVisitor(printer);

	/**
	 * Get the mnemonic for the given instruction
	 * 
	 * @see https://stackoverflow.com/questions/18028093/java-asm-tree-api-how-to-pretttyprint-abstractinstructionnode
	 */
	public static String getMnemonic(AbstractInsnNode insn) {
		insn.accept(mp);
		StringWriter sw = new StringWriter();
		printer.print(new PrintWriter(sw));
		printer.getText().clear();
		return sw.toString().trim().replace("\"", "''");
	}
}
