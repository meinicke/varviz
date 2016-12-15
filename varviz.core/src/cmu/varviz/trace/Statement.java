package cmu.varviz.trace;

import java.io.PrintWriter;

import cmu.conditional.Conditional;
import cmu.varviz.trace.filters.StatementFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;

public class Statement<T> extends MethodElement<T> {

	public Statement(T operation, Method<?> method, int line, FeatureExpr ctx) {
		super(operation, method, line, ctx);
	}

	public Statement(T operation, Method<?> method, FeatureExpr ctx) {
		this(operation, method, -1, ctx);
	}

	@Override
	public final void printLabel(PrintWriter out) {
		out.print(getID());
		out.print("[penwidth=" + width);
		out.print(",label=");
		out.print("\"" + this + "\"");
		if (color != null) {
			if (color == NodeColor.white) {
				out.print(",fillcolor=\"" + color + '\"');
			} else {
				out.print(",color=\"" + color + '\"');
			}
		}
		if (shape != null) {
			out.print(",shape=" + shape);
		}
		out.println(']');
	}

	@Override
	public boolean filterExecution(StatementFilter filter) {
		return filter.filter(this);
	}

	@Override
	public void addStatements(Trace trace) {
		trace.addStatement(this);
	}

	public boolean affectsIdentifier(String identifier) {
		return false;
	}

	public boolean affectsref(int ref) {
		return false;
	}

	public boolean isInteraction(int degree) {
		return false;
	}

	@Override
	public int size() {
		return 1;
	}

	/**
	 * If the operation changes a value, this method returns its old value.
	 */
	public Conditional<String> getOldValue() {
		return null;
	}

	/**
	 * Returns the value of the statement.
	 */
	public Conditional<String> getValue() {
		return null;
	}

	protected Shape shape = null;

	public void setShape(Shape shape) {
		this.shape = shape;
	}

	public Shape getShape() {
		return shape;
	}
}
