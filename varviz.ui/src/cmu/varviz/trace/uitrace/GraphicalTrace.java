package cmu.varviz.trace.uitrace;

import java.util.HashMap;
import java.util.Map;

import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;

public class GraphicalTrace {

	Map<Statement<?>, GraphicalStatement> graphicalStatements = new HashMap<>();
	
	public GraphicalTrace(Trace trace) {
		createGraphicalStatements(trace.getMain());
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
}
