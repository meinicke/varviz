package cmu.varviz.trace.view.figures;

import org.eclipse.draw2d.AbstractConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;

import cmu.varviz.trace.Statement;

public class TargetAnchor  extends AbstractConnectionAnchor {

	private Statement<?> statement;

	public TargetAnchor(IFigure owner, Statement<?> statement) {
		super(owner);
		this.statement = statement;
	}

	@Override
	public Point getLocation(Point ref) {
		ref = getOwner().getBounds().getTop();
		getOwner().translateToAbsolute(ref);
		return ref;
	}


}
