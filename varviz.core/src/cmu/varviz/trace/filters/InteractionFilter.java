package cmu.varviz.trace.filters;

import cmu.varviz.trace.Statement;

public class InteractionFilter implements StatementFilter {

	private final int degree;

	public InteractionFilter(int degree) {
		this.degree = degree;
	}
	
	@Override
	public boolean filter(Statement<?> s) {
		return s.isInteraction(degree);
	}
}
