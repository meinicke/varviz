package cmu.varviz.trace.view.editparts;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MidpointLocator;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.swt.graphics.Color;

import cmu.conditional.Conditional;
import cmu.varviz.VarvizConstants;
import cmu.varviz.trace.Edge;

/**
 * TODO description
 * 
 * @author Jens Meinicke
 *
 */
public class EdgeEditPart extends AbstractConnectionEditPart {

	public EdgeEditPart(Edge edge) {
		super();
		setModel(edge);
	}

	@Override
	protected IFigure createFigure() {
		Edge edge = (Edge) getModel();
		PolylineConnection figure = new PolylineConnection();
		
		figure.setForegroundColor(VarvizConstants.getColor(edge.getColor()));
		figure.setLineWidth(edge.getWidth());

		PolygonDecoration decoration = new PolygonDecoration();
		PointList decorationPointList = new PointList();
		decorationPointList.addPoint(0, 0);
		decorationPointList.addPoint(-2, 1);
		decorationPointList.addPoint(-2, -1);
		decoration.setTemplate(decorationPointList);
		decoration.setForegroundColor(VarvizConstants.getColor(edge.getColor()));
		figure.setTargetDecoration(decoration);

		if (!Conditional.isTautology(edge.getCtx())) {
			createLabel(edge, figure);
		}
		
		return figure;
	}
	
	@Override
	public void refresh() {
		super.refresh();
		refreshVisuals();
	}
	
	@Override
	protected void refreshVisuals() {
		Edge edge = (Edge) getModel();
		StatementEditPart source = (StatementEditPart) getViewer().getEditPartRegistry().get(edge.getFrom());
		if (source == null) {
			deactivate();
			return;
		}
		setSource(source);
		StatementEditPart target = (StatementEditPart) getViewer().getEditPartRegistry().get(edge.getTo());
		setTarget(target);
		figure.setToolTip(new Label(Conditional.getCTXString(edge.getCtx())));
		
		figure.setForegroundColor(VarvizConstants.getColor(edge.getColor()));
		((PolylineConnection)figure).setLineWidth(edge.getWidth());
	}

	private void createLabel(Edge edge, PolylineConnection figure) {
		MidpointLocator sourceEndpointLocator = new MidpointLocator(figure, 0);
		Label label = new Label(Conditional.getCTXString(edge.getCtx()));
		label.setForegroundColor(VarvizConstants.BLACK);
		label.setBackgroundColor(new Color(null, 239, 242, 185));
		figure.add(label, sourceEndpointLocator);
		label.setOpaque(true);
		label.setBorder(new LineBorder(VarvizConstants.BLACK));
	}

	@Override
	protected void createEditPolicies() {

	}
	
	@Override
	public void activate() {
		getFigure().setVisible(getTarget() != null);
		super.activate();
	}

	@Override
	public void deactivate() {
		super.deactivate();
		getFigure().setVisible(false);
	}

	public Edge getEdgeModel() {
		return (Edge)getModel();
	}

}
