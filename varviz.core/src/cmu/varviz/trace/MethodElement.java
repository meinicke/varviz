package cmu.varviz.trace;

import java.io.PrintWriter;

import cmu.conditional.Conditional;
import cmu.conditional.One;
import cmu.varviz.trace.filters.StatementFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;

public abstract class MethodElement<T> {

	private static int ID = 0;
	protected final int id = ID++;
	protected boolean filtered = false;
	
	public Conditional<MethodElement<T>> from = (Conditional<MethodElement<T>>) One.NULL;
	public Conditional<MethodElement<T>> to = (Conditional<MethodElement<T>>) One.NULL;

	public Conditional<MethodElement<T>> getFrom() {
		return from;
	}

	public Conditional<MethodElement<T>> getTo() {
		return to;
	}

	protected FeatureExpr ctx;

	protected final T content;
	
	public final T getContent() {
		return content;
	}

	protected int lineNumber = -1;
	
	public final int getLineNumber() {
		return lineNumber;
	}
	
	public final void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	protected Method<?> parent;
	
	public final void setParent(Method<?> parent) {
		if (parent != null) {
			parent.addMethodElement(this);
		}
		this.parent = parent;
	}
	
	public final Method<?> getParent() {
		return parent;
	}
	
	
	public MethodElement(T content, Method<?> parent, int line, FeatureExpr ctx) {
		this.content = content;
		this.parent = parent;
		this.lineNumber = line;
		this.ctx = ctx;
		if (parent != null) {
			parent.addMethodElement(this);
		}
	}
	
	public FeatureExpr getCTX() {
		return ctx;
	}
	
	public void setCtx(FeatureExpr ctx) {
		this.ctx = ctx;
	}
	
	public abstract void printLabel(PrintWriter pw);

	/**
	 * Keeps only elements that fulfill the filter and<br>
	 * removes all sub-elements that should not be shown in the trace.
	 * 
	 * @return false if the element should be removed. 
	 */
	public abstract boolean filterExecution(StatementFilter statementFilter);
	
	public abstract void addStatements(Trace trace);
	
	public abstract int size();
	
	@Override
	public String toString() {
		return getContent().toString();
	}
	
	protected NodeColor color = null;
	protected int width = 1;
	
	public void setColor(NodeColor color) {
		this.color = color;
	}
	
	public NodeColor getColor() {
		return color;
	}

	public void setWidth(int width) {
		this.width = Math.max(0, width);
	}
	
	public int getWidth() {
		return width;
	}

	public String getID() {
		return TraceUtils.toShortID(id);
	}

	public void setFiltered(boolean filtered) {
		this.filtered = filtered;
	}
	
	public boolean isFiltered() {
		return filtered;
	}
	
	public boolean canBeRemoved(int line) {// TODO revise this, seems unnecessary
		return false;
	}
}
