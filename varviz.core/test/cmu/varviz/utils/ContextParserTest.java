package cmu.varviz.utils;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cmu.conditional.Conditional;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;

public class ContextParserTest {

	private final static FeatureExpr a = Conditional.createFeature("a");
	private final static FeatureExpr b = Conditional.createFeature("b");
	
	@Test
	public void testContext_True() {
		FeatureExpr expectedCtx = FeatureExprFactory.True();
		String contextString = "True";
		FeatureExpr ctx =  ContextParser.getContext(contextString);
		assertTrue(ctx.equivalentTo(expectedCtx));
	}
	
	@Test
	public void testContext_a_and_b() {
		FeatureExpr expectedCtx = a.and(b);
		String contextString = "a&amp;b";
		FeatureExpr ctx =  ContextParser.getContext(contextString);
		assertTrue(ctx.equivalentTo(expectedCtx));
	}
	
	@Test
	public void testContext_not_a() {
		FeatureExpr expectedCtx = a.not();
		String contextString = "¬a";
		FeatureExpr ctx =  ContextParser.getContext(contextString);
		assertTrue(ctx.equivalentTo(expectedCtx));
	}
	
	@Test
	public void testContext_a_or_b() {
		FeatureExpr expectedCtx = a.or(b);
		String contextString = "a|b";
		FeatureExpr ctx =  ContextParser.getContext(contextString);
		assertTrue(ctx.equivalentTo(expectedCtx));
	}
}
