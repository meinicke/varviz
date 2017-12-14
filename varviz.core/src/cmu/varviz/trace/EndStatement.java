package cmu.varviz.trace;

import cmu.varviz.trace.filters.StatementFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;

public class EndStatement extends Statement {

	public EndStatement(Object content, Method parent, int line, FeatureExpr ctx) {
		super("", parent, line, ctx);
		setColor(NodeColor.white);
		setShape(Shape.Mcircle);
	}

	@Override
	public boolean filterExecution(StatementFilter statementFilter) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addStatements(Trace trace) {
		// TODO Auto-generated method stub

	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

}
