package cmu.vaviz.io.testutils;

import cmu.varviz.trace.Method;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;

public class TraceFactory {

	static {
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
	}
	static final FeatureExpr TRUE = FeatureExprFactory.True();
	static final FeatureExpr a = FeatureExprFactory.createDefinedExternal("a");
	static final FeatureExpr b = FeatureExprFactory.createDefinedExternal("b");

	public static Trace createTrace() {
		Trace trace = new Trace();
		
		Method<String> main = new Method<>("Main", 10, TRUE);
		main.setFile("main.java");
		
		trace.setMain(main);
		
		new MyStatement("some instruction", main, 21, TRUE);
		new MyStatement("PUTFIELD", main, 32, a);
		
		Method<?> method = new Method<>("method", main, 43, a);
		method.setFile("method.java");
		
		new MyStatement("iadd", method, 54, b.and(a));
		new MyStatement("PUTSTATIC", method, 56, a);
		new MyStatement("GETFIELD", method, 75, a.not());
		new MyStatement("iinc", method, 101, a.or(b));
		
		return trace;
	}

}

class MyStatement extends Statement<String> {

	public MyStatement(String op, Method<?> m, int line, FeatureExpr ctx) {
		super(op, m, line, ctx);
	}
	
}
