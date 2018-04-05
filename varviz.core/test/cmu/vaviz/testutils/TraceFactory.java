package cmu.vaviz.testutils;

import cmu.conditional.ChoiceFactory;
import cmu.conditional.One;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExprFactory;

public class TraceFactory {

	static {
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
	}
	static final FeatureExpr TRUE = BDDFeatureExprFactory.True();
	static final FeatureExpr a = BDDFeatureExprFactory.createDefinedExternal("a");
	static final FeatureExpr b = BDDFeatureExprFactory.createDefinedExternal("b");

	public static Trace createTrace() {
		Trace trace = new Trace();
		
		Method main = new Method("Main", 10, TRUE);
		main.setFile("main.java");
		
		trace.setMain(main);
		
		MyStatement s = new MyStatement("some instruction", main, 21, TRUE);
		new MyStatement("PUTFIELD", main, 32, a);
		
		Method method = new Method("method", main, 43, a);
		method.setFile("method.java");
		
		s = new MyStatement("iadd", method, 54, b.and(a));
		s = new MyStatement("PUTSTATIC", method, 56, a);
		
		s = new MyStatement("GETFIELD", method, 75, a.not());
		s.setValue(ChoiceFactory.create(a, new One<>("X"), new One<>("Y")));
		
		s = new MyStatement("iinc", method, 101, a.or(b));
		s.setOldValue(new One<>(1));
		s.setValue(new One<>(12));
		return trace;
	}

}

class MyStatement extends Statement {

	public MyStatement(String op, Method m, int line, FeatureExpr ctx) {
		super(op, m, line, ctx);
	}
	
}
