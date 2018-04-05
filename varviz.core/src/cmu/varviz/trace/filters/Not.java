package cmu.varviz.trace.filters;

import cmu.varviz.trace.Statement;

public class Not implements StatementFilter {

	private final StatementFilter filter;
	public Not(StatementFilter filter) {
		this.filter = filter;
	}
	
	@Override
	public boolean filter(Statement s) {
		return !filter.filter(s);
	}
}
