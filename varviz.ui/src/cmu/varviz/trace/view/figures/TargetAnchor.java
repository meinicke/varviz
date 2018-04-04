package cmu.varviz.trace.view.figures;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;

public class TargetAnchor  extends AbstractConnectionAnchor {


	public TargetAnchor(IFigure owner) {
		super(owner);
	}

	@Override
	public Point getLocation(Point ref) {
		Point locations = getOwner().getBounds().getTop();
		getOwner().translateToAbsolute(locations);
		return locations;
	}


}
