package cmu.varviz.trace;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import cmu.varviz.trace.filters.Or;
import cmu.varviz.trace.filters.StatementFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;

public class Method<U> extends MethodElement<U> {

	protected final List<MethodElement<?>> execution = new ArrayList<>();

	private String file = null;

	public void setFile(String file) {
		this.file = file;
	}

	public String getFile() {
		return file;
	}

	public Method(U mi, FeatureExpr ctx) {
		this(mi, null, ctx);
	}

	public Method(U mi, Method<?> parent, FeatureExpr ctx) {
		this(mi, parent, -1, ctx);
	}

	public Method(U mi, int line, FeatureExpr ctx) {
		this(mi, null, line, ctx);
	}

	public Method(U mi, Method<?> parent, int line, FeatureExpr ctx) {
		super(mi, parent, line, ctx);
	}

	public void addMethodElement(MethodElement<?> e) {
		execution.add(e);
	}

	/**
	 * Keeps elements that fulfill any of the filters and<br>
	 * removes all elements that fulfill none.
	 */
	public boolean filterExecution(StatementFilter... filter) {
		return filterExecution(new Or(filter));
	}

	public boolean filterExecution(StatementFilter filter) {
		return filterExecution(filter, false);
	}

	public boolean filterExecution(StatementFilter filter, boolean deep) {
		if (deep) {
			execution.removeIf(e -> {
				if (e instanceof Method) {
					return !((Method<?>) e).filterExecution(filter, deep);
				} else {
					return !e.filterExecution(filter);
				}
			});
		} else {
			execution.removeIf(e -> {
				if (e instanceof Method) {
					return ((Method<?>) e).execution.isEmpty();
				} else {
					return !e.filterExecution(filter);
				}
			});
		}
//		if (parent == null || !Conditional.equivalentTo(parent.getCTX(), getCTX())) {
//			return true;
//		}
		if (Trace.REMOVE_EMPTY_METHODS) {
			return !execution.isEmpty();
		}
		return true;
	}

	public void remove(int index) {
		execution.remove(index);
	}

	public boolean remove(MethodElement<?> element) {
		return remove(element, false);
	}

	public boolean remove(MethodElement<?> element, boolean deep) {
		System.err.println("use remove(int) instead");
		new Exception().printStackTrace();
		if (deep) {
			boolean success = execution.remove(element);
			if (!success) {
				Method<?> lastMethod = null;
				for (MethodElement<?> methodElement : execution) {
					if (methodElement instanceof Method) {
						success = ((Method<?>) methodElement).remove(element, true);
						if (success) {
							lastMethod = (Method<?>) methodElement;
							break;
						}
					}
				}
				if (success) {
					if (lastMethod.getChildren().isEmpty()) {
						remove(lastMethod, true);
					}
				}
			}
			return success;
		}
		return execution.remove(element);
	}

	public void printLabel(PrintWriter pw) {
		pw.println("subgraph \"cluster_" + TraceUtils.toShortID(id) + "\" {");
		pw.println("label = \"" + toString() + "\";");
		execution.forEach(e -> e.printLabel(pw));
		pw.println("}");
	}

	public void addStatements(Trace trace) {
		trace.addStatement(this);
		execution.forEach(e -> e.addStatements(trace));
//		trace.addStatement(new EndStatement(null, this, 0, getCTX()));
	}

	@Override
	public int size() {
		return accumulate(i -> i + 1, 0);
	}

	public <T> T accumulate(Function<T, T> accumulator, T value) {
		return accumulate((__, v) -> accumulator.apply(v), value);
	}

	public <T> T accumulate(BiFunction<Statement<?>, T, T> accumulator, T value) {
		for (final MethodElement<?> methodElement : execution) {
			if (methodElement instanceof Statement) {
				value = accumulator.apply((Statement<?>) methodElement, value);
			} else {
				value = ((Method<?>) methodElement).accumulate(accumulator, value);
			}
		}
		return value;
	}

	public List<MethodElement<?>> getChildren() {
		return Collections.unmodifiableList(execution);
	}

}
