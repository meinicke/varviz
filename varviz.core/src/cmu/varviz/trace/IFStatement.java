package cmu.varviz.trace;

import cmu.conditional.Conditional;
import de.fosd.typechef.featureexpr.FeatureExpr;

public class IFStatement<T> extends Statement<T> {

	private FeatureExpr targetContext;

	private IFStatement(T op, Method<?> m, FeatureExpr ctx) {
		super(op, m, ctx);
	}
	
	public IFStatement(T op, Method<?> m, FeatureExpr targetContext, FeatureExpr ctx) {
		this(op, m, ctx);
		this.targetContext = targetContext;
		setShape(Shape.Mdiamond);
		setColor(NodeColor.white);
	}
		
	@Override
	public String toString() {
		return "if (" + Conditional.getCTXString(targetContext) + ')';
	}

	public FeatureExpr getTargetContext() {
		return targetContext;
	}

	@Override
	public boolean affectsIdentifier(String identifier) {
		return true;
	}
	
	@Override
	public boolean affectsref(int ref) {
		return true;
	}
	
	@Override
	public boolean isInteraction(int degree) {
		return true;
	}
		
}
