package cmu.varviz.trace.uitrace;

import org.eclipse.draw2d.geometry.Point;

import cmu.varviz.trace.NodeColor;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.uitrace.VarvizEvent.EventType;

public class GraphicalStatement {

	private final Statement<?> statement;
	private VarvizEventListener uiObject;
	
	
	
	protected Point location = new Point(0, 0);
	private final NodeColor originalColor;
	
	public GraphicalStatement(Statement<?> statement) {
		this.statement = statement;
		originalColor = statement.getColor();
	}
	
	public void registerUIObject(VarvizEventListener uiObject) {
		this.uiObject = uiObject;
	}
	
	public Statement<?> getStatement() {
		return statement;
	}
	
	public Point getLocation() {
		return location;
	}

	public void setLocation(Point newLocation) {
		if (!location.equals(newLocation)) {
			location = newLocation;
			update(EventType.LOCATION_CHANGED);
		}
	}
	
	public NodeColor getColor() {
		return statement.getColor();
	}
	
	public void setColor(NodeColor color) {
		if (statement.getColor() != color) {
			statement.setColor(color);
			update(EventType.COLOR_CHANGED);
		}
	}
	
	public void resetColor() {
		setColor(originalColor);
	}
	
	private void update(EventType type) {
		if (uiObject != null) {
			uiObject.propertyChange(new VarvizEvent(type));
		}
	}
	
}
