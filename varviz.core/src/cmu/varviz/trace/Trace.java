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
		filterExecution(main, true);
	}
	
	public void filterExecution(Method<?> m) {
		filterExecution(m, false);
	}
	
	public void filterExecution(Method<?> m, boolean deep) {
		if (m == null || m.size() == 0) {
			return;
		}
		m.filterExecution(filter, deep);
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
		removeUnnecessaryIfs(main, true);
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
		FeatureExpr exceptionContext = getExceptionContext();
		highlightException(exceptionContext);
	}

	public FeatureExpr getExceptionContext() {
		return main.accumulate((Statement<?> s, FeatureExpr u) -> 
			s.toString().contains("Exception") || s.toString().contains("Error") ? u.or(s.getCTX()) : u
		, BDDFeatureExprFactory.False());
	}
	
	public void highlightException(FeatureExpr ctx) {
		for (Edge e : edges) {
			if (!Conditional.isContradiction(e.ctx.and(ctx))) {
				if (e.ctx.equivalentTo(ctx)) {
					e.setWidth(2);
					e.setColor(NodeColor.red);
					if (e.to.ctx.equals(e.ctx)) {
						e.to.setWidth(3);
					}
				} else if (Conditional.isTautology(e.ctx)) {
					e.setWidth(1);
					e.setColor(NodeColor.black);
				} else if (!Conditional.isContradiction(e.ctx.not().and(ctx))) {
					e.setWidth(2);
					e.setColor(NodeColor.yellow);
				} else {
					e.setWidth(2);
					if (e.to.ctx.equals(e.ctx)) {
						e.to.setWidth(2);
					}
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
	
	public static final void removeUnnecessaryIfs(Method<?> method) {
		removeUnnecessaryIfs(method, false);
	}
	
	public static final void removeUnnecessaryIfs(Method<?> method, boolean deep) {
		final List<MethodElement<?>> children = method.getChildren();
		int line = Integer.MIN_VALUE;
		
		for (int i = children.size() - 1; i >= 0; i--) {
			final MethodElement<?> element = children.get(i);
			if (element instanceof Statement && ((Statement<?>) element).getShape() == Shape.Mdiamond) {
				Statement<?> ifStatement = (Statement<?>) element;
				boolean hasDecission = checkForDecision(ifStatement, children, i);
				if (hasDecission) {
					line = ifStatement.lineNumber;
				} else {
					method.remove(i);
					line = Integer.MIN_VALUE;
				}
				continue;
			}

			if (element.canBeRemoved(line)) {
				method.remove(i);
			}

			if (deep && element instanceof Method) {
				removeUnnecessaryIfs((Method<?>) element, deep);
				if (((Method<?>) element).getChildren().isEmpty()) {
					method.remove(i);
				}
			}
		}
	}
	
	private static boolean checkForDecision(Statement<?> ifStatement, List<MethodElement<?>>  children, int index) {
		if (index + 1 >= children.size()) {
			return false;
		}
		final FeatureExpr context = ifStatement.getCTX();
		MethodElement<?> methodElement = children.get(index + 1);
		if (!Conditional.isContradiction(Conditional.and(context, methodElement.getCTX()))) {
			if (Conditional.equivalentTo(context, methodElement.getCTX())) {
				return false;
			}
			if (methodElement.getCTX().andNot(context).isSatisfiable()) {
				return false;
			}
			return true;
		}
		return false;
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
