package cmu.varviz.trace;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import cmu.conditional.ChoiceFactory;
import cmu.conditional.Conditional;
import cmu.conditional.One;
import cmu.varviz.trace.filters.StatementFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;

public class Trace {
	
	private Statement<?> START, END;
	
	private Conditional<Statement<?>> lastStatement;

	private List<Edge> edges = new ArrayList<>();
	
	Method<?> main;

	public Trace() {
		START = new NoStatement<String>("Start");
		START.setShape(Shape.Msquare);
		
		END = new NoStatement<String>("End");
		END.setShape(Shape.Msquare);
	}

	public void setMain(Method<?> main) {
		this.main = main;
	}
	
	public StatementFilter filter = s -> true;
	
	public void setFilter(StatementFilter filter) {
		this.filter = filter;
	}
	
	public void filterExecution() {
		filterExecution(main);
	}
	
	public void filterExecution(Method<?> m) {
		if (m == null || m.size() == 0) {
			return;
		}
		m.filterExecution(filter);
	}
	
	public void createEdges() {
		edges.clear();
		addStatement(START);
		main.addStatements(this);
		addStatement(END);
	}

	@SuppressWarnings("null")
	public void print(PrintWriter pw) {
		filterExecution();
		System.out.println("Number of nodes: " + main.size());
		
		pw.println("digraph G {");
		pw.println("graph [ordering=\"out\"];");
		pw.println("node [style=\"rounded,filled\", width=0, height=0, shape=box, concentrate=true]");
		
		addStatement(START);
		main.addStatements(this);
		addStatement(END);
		
//		highlightNotTautology();
		
		// highlight exception
		
		highlightException();
		
		pw.println("// Edges");
		Edge previous = null;
		for (Edge e : edges) {
			e.print(pw, previous);
			previous = e;
		}
		if (!Conditional.isTautology(previous.ctx)) {
			previous.printLabel(pw);
		}
		pw.println();
		pw.println("// clusters");
		
		START.printLabel(pw);
		main.printLabel(pw);
		END.printLabel(pw);
		
		pw.println('}');
		pw.flush();
	}

	public void highlightException() {
		FeatureExpr exceptionContext = main.accumulate((Statement<?> s, FeatureExpr u) -> {
			return s.toString().contains("Exception") || s.toString().contains("Error") ? u.or(s.getCTX()) : u;
		}, FeatureExprFactory.False());
		highlightException(exceptionContext);
	}
	
	public void highlightException(FeatureExpr ctx) {
		for (Edge e : edges) {// TODO shouldn't we go through the nodes?
			if (!Conditional.isContradiction(e.ctx.and(ctx))) {
				if (e.ctx.equivalentTo(ctx)) {// TODO only works if there is only one exception
					e.setWidth(3);
					e.setColor(NodeColor.red);
					e.from.setWidth(3);
					e.to.setWidth(3);
				} else if (e.ctx.isTautology()) {
					e.setWidth(1);
					e.setColor(NodeColor.darkorange);
				} else if (!Conditional.isContradiction(e.ctx.not().and(ctx))) {
					e.setWidth(1);
					e.setColor(NodeColor.yellow);
				} else {
					e.setWidth(2);
					e.from.setWidth(2);
					e.to.setWidth(2);
					e.setColor(NodeColor.firebrick1);
//					e.from.setWidth(1);
//					e.to.setWidth(1);
				}
			}
		}
	}

	public void highlightContext(FeatureExpr ctx, NodeColor color, int width) {
		for (Edge e : edges) {
			if (e.ctx.and(ctx).isSatisfiable()) {
					e.setWidth(width);
					e.setColor(color);
					e.from.setWidth(width);
					e.to.setWidth(width);
			}
		}
	}

	public void highlightNotTautology() {
		for (Edge e : edges) {
			if (!e.ctx.isTautology()) {
				e.setColor(NodeColor.darkorange);
			}
		}
	}

	public void addStatement(final Statement<?> statement) {
		if (lastStatement == null) {
			lastStatement = new One<>(statement);
		} else {
			lastStatement.mapf(statement.getCTX(), (FeatureExpr ctx, Statement<?> from) -> {
				if (!Conditional.isContradiction(ctx)) {
					edges.add(new Edge(ctx, from, statement));
				}
			});
			lastStatement = ChoiceFactory.create(statement.getCTX(), new One<>(statement), lastStatement).simplify();
		}
	}

	public boolean hasMain() {
		return main != null;
	}
	
	public Method<?> getMain() {
		return main;
	}
	
	public Statement<?> getSTART() {
		return START;
	}
	
	public Statement<?> getEND() {
		return END;
	}
	
	public List<Edge> getEdges() {
		return edges;
	}
	
}
