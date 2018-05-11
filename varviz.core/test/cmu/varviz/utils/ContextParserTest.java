package cmu.varviz.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import cmu.conditional.ChoiceFactory;
import cmu.conditional.ChoiceFactory.Factory;
import cmu.conditional.Conditional;
import cmu.conditional.One;
import cmu.varviz.io.xml.XMLvarviz;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExprFactory;

@RunWith(Parameterized.class)
public class ContextParserTest {
	
	static {
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
	}

	private final static FeatureExpr a = Conditional.createFeature("a");
	private final static FeatureExpr b = Conditional.createFeature("b");

	@Parameters(name = "{0}")
	public static List<Object[]> configurations() {
		List<Object[]> params = new LinkedList<>(); 
		for (Object[] choice : ChoiceFactory.asParameter()) {
			params.add(choice);
		}
		return params;
	}
	
	private char featureName = 'a';

	public ContextParserTest(Factory factory) {
		ChoiceFactory.setDefault(factory);
	}
	
	@Test
	public void testContext_True() {
		FeatureExpr expectedCtx = BDDFeatureExprFactory.True();
		String contextString = "True";
		FeatureExpr ctx =  ContextParser.getContext(contextString);
		assertTrue(ctx.equivalentTo(expectedCtx));
	}
	
	@Test
	public void testContext_a_and_b() {
		FeatureExpr expectedCtx = Conditional.and(a,b);
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
	
	@Test
	public void testContext_a_and_b2() {
		FeatureExpr expectedCtx = Conditional.and(a,b);
		String contextString = "a&b";
		FeatureExpr ctx =  ContextParser.getContext(contextString);
		assertTrue(ctx.equivalentTo(expectedCtx));
	}
	
	@Test
	public void conditionalToStringTest1() {
		String content = "A";
		Conditional<String> cvalue = new One<>(content);
		String text = ContextParser.ConditionalToString(cvalue);
		assertEquals(content, text);
	}
	
	@Test
	public void conditionalToStringTest2() {
		String contentA = "A";
		String contentB = "B";
		Conditional<String> cvalue = ChoiceFactory.create(a, new One<>(contentA), new One<>(contentB));
		String text = ContextParser.ConditionalToString(cvalue);
		String[] split = text.split("\\" + XMLvarviz.valueSplitChar);
		assertEquals("a:" + contentA, split[0]);
		assertEquals("¬a:" + contentB, split[1]);
	}
	
	@Test
	public void conditionalToStringToConditionalTest() {
		String contentA = "A";
		String contentB = "B";
		Conditional<String> cvalue = ChoiceFactory.create(a, new One<>(contentA), new One<>(contentB));
		String text = ContextParser.ConditionalToString(cvalue);
		Conditional<String> conditional = ContextParser.StringToConditional(text);
		assertEquals(cvalue, conditional);
	}
	
	@Test
	public void conditionalToStringToConditionalTest2() {
		String content = "A";
		Conditional<String> cvalue = new One<>(content);
		String text = ContextParser.ConditionalToString(cvalue);
		Conditional<String> conditional = ContextParser.StringToConditional(text);
		assertEquals(cvalue, conditional);
	}
	
	@Test
	public void conditionalToStringToConditionalTest3() {
		final Conditional<String> originalValue = createCValue();
		String text = ContextParser.ConditionalToString(originalValue);
		Conditional<String> conditional = ContextParser.StringToConditional(text);
		
		Map<String, FeatureExpr> map1 = originalValue.toMap();
		Map<String, FeatureExpr> map2 = conditional.toMap();
		
		for (Entry<String, FeatureExpr> entry : map1.entrySet()) {
			FeatureExpr value2 = map2.get(entry.getKey());
			assertTrue(entry.getValue().equivalentTo(value2));
		}
	}
	
	private Conditional<String> createCValue() {
		Conditional<String> value = new One<>("ROOT");
		for (int i = 0; i < 10; i++) {
			value = ChoiceFactory.create(createExpr(), new One<>(i + ""), value);
		}
		return value;
	}
	
	private FeatureExpr createExpr() {
		return Conditional.createFeature(featureName++ + ""); 
	}
}
