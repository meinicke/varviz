package cmu.varviz.trace;

import java.io.PrintWriter;

import cmu.conditional.Conditional;
import cmu.varviz.trace.filters.StatementFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;

@SuppressWarnings("rawtypes")
public class Statement extends MethodElement {

	protected Conditional<?> oldValue;
	protected Conditional<?> value;

	public Statement(Object operation, Method method, int line, FeatureExpr ctx) {
		super(operation, method, line, ctx);
	}

	public Statement(Object operation, Method method, FeatureExpr ctx) {
		this(operation, method, -1, ctx);
	}

	@Override
	public final void printLabel(PrintWriter out) {
		out.print(getID());
		out.print("[penwidth=" + width);
		out.print(",label=");
		out.print("\"");
		out.print(this);
		if (oldValue != null) {
			out.print(' ');
			out.print(oldValue.toString());
			out.print(" \u2192");
		}
		if (value != null) {
			out.print(' ');
			out.print(value.toString());
		}
		out.print("\"");
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
	public Conditional getOldValue() {
		return oldValue;
	}

	public void setOldValue(Conditional<?> oldValue) {
		this.oldValue = oldValue;
	}

	/**
	 * Returns the value of the statement.
	 */
	
	public Conditional getValue() {
		return value;
	}

	public void setValue(Conditional<?> value) {
		this.value = value;
	}

	protected Shape shape = null;

	public void setShape(Shape shape) {
		this.shape = shape;
	}

	public Shape getShape() {
		return shape;
	}

	public boolean isModificationStatement() {
		return false;
	}

	@Override
	public MethodElement simplify(FeatureExpr ctx, StatementFilter filter) {
		setCtx(Conditional.simplifyCondition(this.getCTX(), Conditional.additionalConstraint));
		Conditional<?> value2 = getValue();
		if (value2 != null) {
			value2 = value2.simplify(ctx);
			setValue(value2);
		}
		if (getOldValue() != null) {
			setOldValue(getOldValue().simplify(ctx));
		}
		return this;
	}
}
