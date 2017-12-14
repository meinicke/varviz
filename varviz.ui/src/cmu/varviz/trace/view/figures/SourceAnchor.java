package cmu.varviz.trace.view.figures;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;

import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;

public class SourceAnchor extends AbstractConnectionAnchor {

	private MethodElement<?> statement;

	public SourceAnchor(IFigure owner, MethodElement<?> statement) {
		super(owner);
		this.statement = statement;
	}

	@Override
	public Point getLocation(final Point ref) {
		final Point newLocation;
		if (statement instanceof Method && !((Method<?>) statement).getChildren().isEmpty()) {
			newLocation = getOwner().getBounds().getTop();
		} else {
			newLocation = getOwner().getBounds().getBottom();
		}
		getOwner().translateToAbsolute(newLocation);
		return newLocation;
	}

}
