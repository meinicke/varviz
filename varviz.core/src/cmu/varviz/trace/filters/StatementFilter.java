package cmu.varviz.trace.filters;

import cmu.varviz.trace.Statement;

public interface StatementFilter {
	
	/**
	 * @return true if the element fulfills the predicate.
	 */
	public boolean filter(Statement s);
	
}
