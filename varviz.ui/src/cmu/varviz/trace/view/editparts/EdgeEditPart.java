package cmu.varviz.trace.view.editparts;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MidpointLocator;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import cmu.conditional.Conditional;
import cmu.varviz.VarvizConstants;
import cmu.varviz.trace.Edge;
import cmu.varviz.trace.NodeColor;
import cmu.varviz.trace.view.VarvizView;

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
		PolylineConnection line = new PolylineConnection();
		
		if (edge.getColor() == NodeColor.gray || edge.getColor() == NodeColor.yellow) {
			line.setLineStyle(SWT.LINE_CUSTOM);
			line.setLineDash(new float[]{5, 5});
		}
		
		line.setForegroundColor(VarvizConstants.getColor(edge.getColor()));
		line.setLineWidth(edge.getWidth());

		PolygonDecoration arrow = new PolygonDecoration();
		PointList arrowPointList = new PointList();
		arrowPointList.addPoint(0, 0);
		arrowPointList.addPoint(-2, 1);
		arrowPointList.addPoint(-2, -1);
		arrow.setTemplate(arrowPointList);
		arrow.setForegroundColor(VarvizConstants.getColor(edge.getColor()));
		line.setTargetDecoration(arrow);

		if (!Conditional.isTautology(edge.getCtx()) && VarvizView.showLables) {
			createLabel(edge, line);
		}
		
		return line;
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
		figure.setToolTip(new Label(EditPartUtils.getContext(edge.getCtx())));
		
		figure.setForegroundColor(VarvizConstants.getColor(edge.getColor()));
		((PolylineConnection)figure).setLineWidth(edge.getWidth());
	}



	private void createLabel(Edge edge, PolylineConnection figure) {
		MidpointLocator sourceEndpointLocator = new MidpointLocator(figure, 0);
		Label label = new Label();
		// Fonts that support logical symbols:
		// Cambria, Lucida Sans Unicode, Malgun Gothic, Segoe UI Symbol
		label.setFont(new Font(null, "Segoe UI Symbol",10, SWT.NORMAL));
		label.setText(EditPartUtils.getContext(edge.getCtx()));
		label.setForegroundColor(VarvizConstants.BLACK);
		label.setBackgroundColor(new Color(null, 239, 242, 185));
		figure.add(label, sourceEndpointLocator);
		label.setOpaque(true);
		label.setBorder(new LineBorder(VarvizConstants.BLACK));
	}

	@Override
	protected void createEditPolicies() {
		// nothing here
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
