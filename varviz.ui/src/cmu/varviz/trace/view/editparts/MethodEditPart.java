/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package cmu.varviz.trace.view.editparts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import cmu.conditional.Conditional;
import cmu.varviz.trace.Edge;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.uitrace.VarvizEvent;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.figures.MethodFigure;
import de.fosd.typechef.featureexpr.FeatureExpr;

/**
 * TODO description
 * 
 * @author Jens Meinicke
 */
public class MethodEditPart extends AbstractTraceEditPart implements NodeEditPart {

	private final static int BORDER_MARGIN = 10;

	private ConnectionAnchor sourceAnchor = null;
	private ConnectionAnchor targetAnchor = null;

	public MethodEditPart(Method<?> method) {
		super();
		setModel(method);
	}

	@Override
	protected IFigure createFigure() {
		MethodFigure methodFigure = new MethodFigure((Method<?>) getModel());
		sourceAnchor = methodFigure.getSourceAnchor();
		targetAnchor = methodFigure.getTargetAnchor();

		return methodFigure;
	}

	@Override
	protected List<MethodElement<?>> getModelChildren() {
		List<MethodElement<?>> children = new ArrayList<>();
		for (MethodElement<?> child : ((Method<?>) getModel()).getChildren()) {
			children.add(child);
		}
		return children;
	}

	List<Edge> connections;

	@Override
	protected List<Edge> getModelTargetConnections() {
		if (connections == null) {
			connections = new ArrayList<>();
			for (Edge edge : VarvizView.getTRACE().getEdges()) {
				if (edge.getTo() == getModel()) {
					connections.add(edge);
				}
			}
		}
		return connections;
	}

	enum Direction {
		LEFT, RIGHT, CENTER
	}

	@Override
	public void layout() {
		// TODO revise this method
		final IFigure methodFigure = getFigure();
		Rectangle bounds = methodFigure.getBounds();
		final Point referencePoint = bounds.getTopLeft();
		int h = 40;

		Direction direction = Direction.CENTER;

		Set<AbstractTraceEditPart> previousFigures = new HashSet<>();
		int maxWidth = 0;
		AbstractTraceEditPart previousMax = null;

		MethodElement<?> previous = null;
		AbstractTraceEditPart previousFigure = null;
		for (Object object : getChildren()) {
			if (object instanceof AbstractTraceEditPart) {
				AbstractTraceEditPart childEditPart = (AbstractTraceEditPart) object;

				MethodElement<?> model = (MethodElement<?>) childEditPart.getModel();
				FeatureExpr ctx = model.getCTX();
				if (previous != null) {
					FeatureExpr prevctx = previous.getCTX();
					if (!prevctx.equivalentTo(ctx)) {
						previousFigures.clear();
						childEditPart.layout();
						childEditPart.getFigure().translateToRelative(referencePoint);
						maxWidth = childEditPart.getFigure().getBounds().width;
						previousMax = childEditPart;

						if (Conditional.equivalentTo(((Method<?>) getModel()).getCTX(), ctx)) {
							// a -> True
							direction = Direction.CENTER;
							childEditPart.getFigure().setLocation(new Point(-childEditPart.getFigure().getBounds().width / 2, h));
						} else if (Conditional.isSatisfiable(prevctx.and(ctx)) || direction == Direction.RIGHT/* TODO */) {
							// True -> a
							direction = Direction.LEFT;
							childEditPart.getFigure().setLocation(new Point(previousFigure.getFigure().getBounds().getBottom().x -(childEditPart.getFigure().getBounds().width + BORDER_MARGIN), h));
						} else {
							// a -> -a
							direction = Direction.RIGHT;
							h = previousFigure.getFigure().getBounds().y;// TODO this should be the highet of the other branch 
							childEditPart.getFigure().setLocation(new Point(previousFigure.getFigure().getBounds().right() + BORDER_MARGIN, h));
						}
						h = childEditPart.getFigure().getBounds().bottom() + BORDER_MARGIN * 4;
						previous = model;
						previousFigure = childEditPart;
						continue;
					} else {
						// prevctx.equivalentTo(ctx)
						previousFigures.add(previousFigure);
						if (previousMax == null) {
							previousMax = childEditPart;
						}

						IFigure currentFigure = childEditPart.getFigure();
						childEditPart.layout();
						currentFigure.translateToRelative(referencePoint);
						boolean move = false;
						if (maxWidth < currentFigure.getBounds().width) {
							move = true;
							maxWidth = currentFigure.getBounds().width;
						}

						if (move) {
							// move current figure to edge of previous (max)
							Point point;
							if (direction == Direction.LEFT) {
								point = new Point(previousMax.getFigure().getBounds().right() - currentFigure.getBounds().width,
										h);
							} else {
								point = new Point(previousMax.getFigure().getBounds().x, h);
							}
							currentFigure.setLocation(point);

							// center all previous figures under current element
							for (AbstractTraceEditPart editPart : previousFigures) {
								editPart.getFigure().setLocation(new Point(currentFigure.getBounds().getTop().x
										- editPart.getFigure().getBounds().width / 2, editPart.getFigure().getBounds().y));
							}
							previousMax = childEditPart;
						} else {
							// center under previous element
							Point point = new Point(previousFigure.getFigure().getBounds().getTop().x - currentFigure
									.getBounds().width / 2, h);
							childEditPart.getFigure().setLocation(point);
						}

						h = childEditPart.getFigure().getBounds().bottom() + BORDER_MARGIN * 4;
						previous = model;
						previousFigure = childEditPart;
						continue;
					}
				}

				childEditPart.layout();
				childEditPart.getFigure().translateToRelative(referencePoint);
				childEditPart.getFigure().setLocation(new Point(-childEditPart.getFigure().getBounds().width / 2, h));
				h += childEditPart.getFigure().getSize().height;
				h += BORDER_MARGIN * 4;

				previous = model;
				previousFigure = childEditPart;
			}
		}

		// move all to scope
		int minX = 0;
		for (Object object : getChildren()) {
			if (object instanceof AbstractGraphicalEditPart) {
				bounds = ((AbstractGraphicalEditPart) object).getFigure().getBounds();
				minX = Math.min(bounds.x, minX);
			}
		}

		for (Object object : getChildren()) {
			if (object instanceof AbstractGraphicalEditPart) {
				final AbstractGraphicalEditPart abstractGraphicalEditPart = (AbstractGraphicalEditPart) object;
				Point location = abstractGraphicalEditPart.getFigure().getBounds().getTopLeft();
				abstractGraphicalEditPart.getFigure().setLocation(new Point(BORDER_MARGIN + location.x - minX, location.y));
			}
		}

		int maxW = ((MethodFigure)methodFigure).getMinWidth();
		int maxH = bounds.height;
		for (Object object : getChildren()) {
			if (object instanceof AbstractGraphicalEditPart) {
				bounds = ((AbstractGraphicalEditPart) object).getFigure().getBounds();
				maxW = Math.max(maxW, bounds.right());
				maxH = Math.max(bounds.bottom(), maxH);
			}
		}
		Rectangle oldbounds = methodFigure.getBounds();
		int newHeight = maxH - oldbounds.getTop().y;

		bounds = new Rectangle(oldbounds.x, oldbounds.y, maxW + BORDER_MARGIN, newHeight + BORDER_MARGIN);

		methodFigure.setBounds(bounds);
	}

	public Method<?> getMethodModel() {
		return (Method<?>) getModel();
	}

	@Override
	public void performRequest(Request request) {
		if ("open".equals(request.getType())) {
			final Method<?> method = getMethodModel();
			final int lineNumber = method.getLineNumber();
			Method<?> parent = method.getParent();
			if (parent != null) {
				EditorHelper.open(parent.getFile(), lineNumber);
			}
		}
		super.performRequest(request);
	}
	
	@SuppressWarnings("unchecked")
	public AbstractGraphicalEditPart getLastTrueStatement() {
		ListIterator<Object> iterator = getChildren().listIterator(getChildren().size());
		while (iterator.hasPrevious()) {
			Object object = iterator.previous();
			if (object instanceof AbstractGraphicalEditPart) {
				final AbstractGraphicalEditPart abstractGraphicalEditPart = (AbstractGraphicalEditPart) object;
				MethodElement<?> element = (MethodElement<?>)abstractGraphicalEditPart.getModel();
				if (element.getCTX().isTautology()) {
					if (element instanceof Statement) {
						return abstractGraphicalEditPart;
					} else if (object instanceof MethodEditPart){
						AbstractGraphicalEditPart nextStatement = ((MethodEditPart) object).getLastTrueStatement();
						if (nextStatement != null) {
							return nextStatement;
						}
					}
				}				
			}
		}
		return null;
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart arg0) {
		return sourceAnchor;
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request arg0) {
		return sourceAnchor;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart arg0) {
		return targetAnchor;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request arg0) {
		return targetAnchor;
	}

	@Override
	public void activate() {
		getFigure().setVisible(true);
		super.activate();
	}

	@Override
	public void deactivate() {
		super.deactivate();
		getFigure().setVisible(false);
		getModelTargetConnections().forEach(edge -> ((EdgeEditPart) getViewer().getEditPartRegistry().get(edge)).deactivate());
	}

	@Override
	public void propertyChange(VarvizEvent event) {
		// TODO Auto-generated method stub
		
	}

}