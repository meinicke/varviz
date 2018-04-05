package cmu.varviz.trace.uitrace;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;

import cmu.varviz.trace.NodeColor;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.uitrace.VarvizEvent.EventType;

/**
 * Graphical representation of statements in the variational trace.
 * 
 * @author Jens Meinicke
 *
 */
public class GraphicalStatement {

	private final Statement statement;
	private VarvizEventListener uiObject;
	
	protected Point location = new Point(0, 0);
	private final NodeColor originalColor;
	private final int originalBorder;
	
	public GraphicalStatement(Statement statement) {
		this.statement = statement;
		originalColor = statement.getColor();
		originalBorder = statement.getWidth();
	}
	
	public void registerUIObject(VarvizEventListener uiObject) {
		this.uiObject = uiObject;
	}
	
	public Statement getStatement() {
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
	
	public void resetBorder() {
		if (statement.getWidth() != originalBorder) {
			statement.setWidth(originalBorder);
			update(EventType.BORDER_CHANGED);
		}
	}
	
	public void setBorder(int border) {
		if (statement.getWidth() != border) {
			statement.setWidth(border);
			update(EventType.BORDER_CHANGED);
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

	public void reveal(ScrollingGraphicalViewer viewer) {
		if (uiObject != null) {
			viewer.reveal((EditPart) uiObject);
		}
	}

	
	
}
