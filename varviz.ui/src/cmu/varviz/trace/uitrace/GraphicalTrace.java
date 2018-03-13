package cmu.varviz.trace.uitrace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cmu.varviz.trace.Edge;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.uitrace.VarvizEvent.EventType;

public class GraphicalTrace {

	Map<Statement<?>, GraphicalStatement> graphicalStatements = new HashMap<>();
	Map<Edge, GraphicalEdge> graphicalEdges = new HashMap<>();
	
	public GraphicalTrace(Trace trace) {
		createGraphicalStatements(trace.getMain());
		createGraphicalEdges(trace.getEdges());
	}

	private void createGraphicalEdges(List<Edge> edges) {
		for (Edge edge : edges) {
			graphicalEdges.put(edge, new GraphicalEdge(edge));
		}
	}

	private void createGraphicalStatements(Method<?> main) {
		for (MethodElement<?> child : main.getChildren()) {
			if (child instanceof Method) {
				createGraphicalStatements((Method<?>) child);
			} else {
				graphicalStatements.put((Statement<?>) child, new GraphicalStatement((Statement<?>) child));
			}
		}
	}
	
	public GraphicalStatement getGraphicalStatement(Statement<?> statement) {
		return graphicalStatements.get(statement);
	}
	
	public GraphicalEdge getGraphicalEdge(Edge edge) {
		return graphicalEdges.get(edge);
	}
	
	public void refreshGraphicalEdges() {
		for (GraphicalEdge edge : graphicalEdges.values()) {
			edge.update(EventType.LABEL_CHANGED);
		}
	}
}
