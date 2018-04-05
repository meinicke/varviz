package cmu.varviz.trace.filters;

import cmu.varviz.trace.Statement;

public class And implements StatementFilter {

	private final StatementFilter[] filter;
	public And(StatementFilter... filter) {
		this.filter = filter;
	}
	
	@Override
	public boolean filter(Statement s) {
		for (StatementFilter f : filter) {
			if (!f.filter(s)) {
				return false;
			}
		}
		return true;
	}
}
