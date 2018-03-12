package cmu.varviz.trace.view.figures;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;

import cmu.varviz.trace.MethodElement;

public class TargetAnchor  extends AbstractConnectionAnchor {


	public TargetAnchor(IFigure owner, MethodElement<?> statement) {
		super(owner);
	}

	@Override
	public Point getLocation(Point ref) {
		ref = getOwner().getBounds().getTop();
		getOwner().translateToAbsolute(ref);
		return ref;
	}


}
