package cmu.varviz.trace;

import java.io.PrintWriter;

import org.junit.Test;

import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;

public class TraceTest {

	static {
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
	}
	final FeatureExpr TRUE = FeatureExprFactory.True();
	final FeatureExpr a = FeatureExprFactory.createDefinedExternal("a");
	final FeatureExpr b = FeatureExprFactory.createDefinedExternal("b");
	@Test
	public void traceTest() {
		Trace trace = new Trace();
//		trace.setFilter(new ContextFilter(a));
		
		Method<String> main = new Method<>("Main", TRUE);
		trace.setMain(main);
		
		new MyStatement("main(String[] args)", main, TRUE);
		new MyStatement("someConditionalCall", main, a);
		
		Method<?> method = new Method<>("method", main, a);
		new MyStatement("someConditionalCallA", method, b.and(a));
		new MyStatement("someConditionalCallB", method, a);
		new MyStatement("someConditionalCallB", method, a.not());
		
		PrintWriter pw = new PrintWriter(System.out);
		
		trace.print(pw);
	}

}

class MyStatement extends Statement<String> {

	public MyStatement(String op, Method m, FeatureExpr ctx) {
		super(op, m, ctx);
	}
	
}
