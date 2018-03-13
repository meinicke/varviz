package cmu.varviz.trace.uitrace;

import org.eclipse.draw2d.geometry.Point;

import cmu.varviz.trace.Edge;
import cmu.varviz.trace.NodeColor;
import cmu.varviz.trace.uitrace.VarvizEvent.EventType;

/**
 * Graphical representation of edges in the variational trace.
 * 
 * @author Jens Meinicke
 *
 */
public class GraphicalEdge {

	private final Edge edge;
	private VarvizEventListener uiObject;
	
	protected Point location = new Point(0, 0);
	private final NodeColor originalColor;
	
	public GraphicalEdge(Edge edge) {
		this.edge = edge;
		originalColor = edge.getColor();
	}
	
	public void registerUIObject(VarvizEventListener uiObject) {
		this.uiObject = uiObject;
	}
	
	public Edge getEdge() {
		return edge;
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
		return edge.getColor();
	}
	
	public void setColor(NodeColor color) {
		if (edge.getColor() != color) {
			edge.setColor(color);
			update(EventType.COLOR_CHANGED);
		}
	}
	
	public void resetColor() {
		setColor(originalColor);
	}
	
	public void update(EventType type) {
		if (uiObject != null) {
			uiObject.propertyChange(new VarvizEvent(type));
		}
	}
}
