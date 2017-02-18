package cmu.varviz.trace;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import cmu.conditional.ChoiceFactory;
import cmu.conditional.Conditional;
import cmu.conditional.One;
import cmu.varviz.trace.filters.StatementFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExprFactory;

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
		lastStatement = null;
	}
	
	public void finalizeGraph() {
		System.out.print("Number of nodes: " + main.size());
		System.out.flush();
		filterExecution();
		removeUnnecessaryIfs(main);
		createEdges();
		highlightException();
		System.out.println(" -> " + main.size());
	}

	public void printToGraphViz(PrintWriter pw) {// TODO move to graphviz
		pw.println("digraph G {");
		pw.println("graph [ordering=\"out\"];");
		pw.println("node [style=\"rounded,filled\", width=0, height=0, shape=box, concentrate=true]");
		
		pw.println("// Edges");
		Edge previous = null;
		for (Edge e : edges) {
			e.print(pw, previous);
			previous = e;
		}
		if (previous != null && !Conditional.isTautology(previous.ctx)) {
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
		}, BDDFeatureExprFactory.False());
		highlightException(exceptionContext);
	}
	
	public void highlightException(FeatureExpr ctx) {
		for (Edge e : edges) {// TODO shouldn't we go through the nodes?
			if (!Conditional.isContradiction(e.ctx.and(ctx))) {
				if (e.ctx.equivalentTo(ctx)) {// TODO only works if there is only one exception
					e.setWidth(2);
					e.setColor(NodeColor.red);
					e.from.setWidth(2);
					e.to.setWidth(2);
				} else if (Conditional.isTautology(e.ctx)) {
					e.setWidth(1);
					e.setColor(NodeColor.black);
				} else if (!Conditional.isContradiction(e.ctx.not().and(ctx))) {
					e.setWidth(1);
					e.setColor(NodeColor.yellow);
				} else {
					e.setWidth(2);
					e.from.setWidth(2);
					e.to.setWidth(2);
					e.setColor(NodeColor.darkorange);
				}
			} else {
				e.setWidth(1);
				e.setColor(NodeColor.gray);
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
	
	private static final Statement<?> removeUnnecessaryIfs(Method<?> method) {
		List<Statement<?>> remove = new ArrayList<>();
		Statement<?> lastif = null;
		for (MethodElement<?> element : method.getChildren()) {
			if (lastif != null && element.getCTX().equals(lastif.getCTX())) {
				remove.add(lastif);
			}
			if (element instanceof Statement && ((Statement<?>)element).getShape() == Shape.Mdiamond) {
				lastif = (Statement<?>) element;
			} else {
				lastif = null;
			}
			
			if (element instanceof Method) {
				lastif = removeUnnecessaryIfs((Method<?>)element);
			}
		}
		
		for (Statement<?> statement : remove) {
			method.remove(statement);
		}
		return lastif;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addStatement(final Statement<?> statement) {
		if (lastStatement == null) {
			lastStatement = new One<>(statement);
		} else {
			lastStatement.mapf(statement.getCTX(), (FeatureExpr ctx, Statement<?> from) -> {
				if (!Conditional.isContradiction(ctx)) {
					edges.add(new Edge(ctx, from, statement));
					from.to = ChoiceFactory.create(ctx, new One(statement), from.to).simplify();
					statement.from = ChoiceFactory.create(ctx, new One(from), statement.from).simplify();
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
