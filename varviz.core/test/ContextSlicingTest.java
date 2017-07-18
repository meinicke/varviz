import static cmu.varviz.trace.Slicer.slice;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cmu.conditional.Conditional;
import cmu.varviz.trace.IFStatement;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.Slicer.State;
import cmu.varviz.trace.Trace;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExpr;

public class ContextSlicingTest {

	static {
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
		Conditional.bddFM = (BDDFeatureExpr) FeatureExprFactory.True();
	}

	private static final FeatureExpr TRUE = FeatureExprFactory.True();
	private static final SingleFeatureExpr FA = FeatureExprFactory.createDefinedExternal("A");
	private static final SingleFeatureExpr FB = FeatureExprFactory.createDefinedExternal("B");

	@Test
	public void test() throws Exception {

		// conditions:

		final FeatureExpr A = FA;
		final FeatureExpr notA = FA.not();
		final FeatureExpr notAB = FA.not().and(FB);
		final FeatureExpr notAnotB = FA.not().andNot(FB);

		System.out.print(A);
		System.out.print(" -> ");
		System.out.println(A.unique(FA));
		// System.out.println(A.unique(FB));

		System.out.print(notA);
		System.out.print(" -> ");
		System.out.println(notA.unique(FA));
		// System.out.println(notA.unique(FB));

		System.out.print(notAB);
		System.out.print(" -> ");
		System.out.println(notAB.unique(FA));
		// System.out.println(notAB.unique(FB));

		System.out.print(notAnotB);
		System.out.print(" -> ");
		System.out.println(notAnotB.unique(FA));
		// System.out.println(notAnotB.unique(FB));

		FeatureExpr[] expressions = new FeatureExpr[] { A, notA, notAB, notAnotB };

		System.out.println("-----------");
		System.out.println(createFullFormula(FA, expressions));
		System.out.println(createFullFormula(FB, expressions));

	}

	@Test
	public void test2() throws Exception {

		// conditions:

		final FeatureExpr B = FB;
		final FeatureExpr notB = FB.not();

		final FeatureExpr BA = B.and(FA);
		final FeatureExpr BAnot = B.and(FA.not());
		final FeatureExpr BnotA = B.not().and(FA);
		final FeatureExpr BnotAnot = B.not().and(FA.not());

		FeatureExpr[] expressions = new FeatureExpr[] { B, notB, BA, BAnot, BnotA, BnotAnot };

		System.out.println("-----------");
		System.out.println(createFullFormula(FA, expressions));
		System.out.println(createFullFormula(FB, expressions));

	}

	@Test
	public void test3() throws Exception {

		// conditions:

		final FeatureExpr B = FB;
		final FeatureExpr notB = FB.not();

		final FeatureExpr BA = B.and(FA);
		final FeatureExpr BAnot = B.and(FA.not());

		FeatureExpr[] expressions = new FeatureExpr[] { B, notB, BA, BAnot };

		System.out.println("-----------");
		System.out.println(createFullFormula(FA, expressions));
		System.out.println(createFullFormula(FB, expressions));

	}

	private FeatureExpr createFullFormula(SingleFeatureExpr feature, FeatureExpr... expressions) {
		FeatureExpr formula = FeatureExprFactory.False();
		for (FeatureExpr ex : expressions) {
			formula = formula.or(ex.unique(feature));
		}
		return formula;
	}

	@Test
	public void testSlicing() throws Exception {
		Trace trace = createTrace(new IFConditions(TRUE, FA), new IFConditions(FA.not(), FA.not().and(FB)));
		assertEquals(State.DESELECTED, slice(trace, FA, FB));
		assertEquals(State.DESELECTED, slice(trace, FB, FA));
	}

	@Test
	public void testSlicing2() throws Exception {
		Trace trace = createTrace(new IFConditions(TRUE, FB), new IFConditions(FB, FB.and(FA)),
				new IFConditions(FB.not(), FB.not().and(FA)));
		assertEquals(State.CONDITIONAL, slice(trace, FA, FB));
		assertEquals(State.DESELECTED, slice(trace, FB, FA));
	}

	@Test
	public void testSlicing3() throws Exception {
		Trace trace = createTrace(new IFConditions(TRUE, FB), new IFConditions(FB, FB.and(FA)));
		assertEquals(State.SELECTED, slice(trace, FA, FB));
		assertEquals(State.DESELECTED, slice(trace, FB, FA));
	}

	@Test
	public void testSlicing4() throws Exception {
		Trace trace = createTrace(new IFConditions(TRUE, FA), new IFConditions(TRUE, FB));
		assertEquals(State.DESELECTED, slice(trace, FA, FB));
		assertEquals(State.DESELECTED, slice(trace, FB, FA));
	}

	@Test
	public void testSlicing5() throws Exception {
		Trace trace = createTrace(new IFConditions(TRUE, FA.and(FB)));
		assertEquals(State.SELECTED, slice(trace, FA, FB));
		assertEquals(State.SELECTED, slice(trace, FB, FA));
	}

	@Test
	public void testSlicing6() throws Exception {
		Trace trace = createTrace(new IFConditions(TRUE, FA.or(FB)));
		assertEquals(State.DESELECTED, slice(trace, FA, FB));
		assertEquals(State.DESELECTED, slice(trace, FB, FA));
	}

	@Test
	public void testSlicing7() throws Exception {
		Trace trace = createTrace(new IFConditions(TRUE, FA.andNot(FB)));
		assertEquals(State.DESELECTED, slice(trace, FA, FB));
		assertEquals(State.SELECTED, slice(trace, FB, FA));
	}

	public static Trace createTrace(IFConditions... conditions) {
		Trace trace = new Trace();

		Method<String> main = new Method<>("Main", 10, TRUE);
		trace.setMain(main);

		for (IFConditions c : conditions) {
			new IFStatement<>(null, main, 0, c.decisionCtx, c.context);
		}

		return trace;
	}

	private static class IFConditions {
		final FeatureExpr context;
		final FeatureExpr decisionCtx;

		public IFConditions(FeatureExpr context, FeatureExpr decisionCtx) {
			this.context = context;
			this.decisionCtx = decisionCtx;
		}
	}
}
