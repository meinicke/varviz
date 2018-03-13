package cmu.varviz.trace.view.editparts;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MidpointLocator;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import cmu.conditional.Conditional;
import cmu.varviz.VarvizConstants;
import cmu.varviz.VarvizException;
import cmu.varviz.trace.Edge;
import cmu.varviz.trace.NodeColor;
import cmu.varviz.trace.uitrace.GraphicalEdge;
import cmu.varviz.trace.uitrace.VarvizEvent;
import cmu.varviz.trace.uitrace.VarvizEventListener;
import cmu.varviz.trace.view.VarvizView;

/**
 * Represents the {@link ConnectionEditPart} in the variational trace.
 * 
 * @author Jens Meinicke
 *
 */
public class EdgeEditPart extends AbstractConnectionEditPart implements VarvizEventListener {

	private static final String FONT_NAME = "Segoe UI Symbol";
	private static final Font TEXT_FONT = new Font(null, FONT_NAME, 12, SWT.NORMAL);
	
	private Label label;
	private boolean showLabel = false;
	
	public EdgeEditPart(Edge edge) {
		super();
		setModel(edge);
		if (VarvizView.GRAPHICAL_TRACE != null) {
			GraphicalEdge graphicalEdge = VarvizView.GRAPHICAL_TRACE.getGraphicalEdge(edge);
			if (graphicalEdge != null) {
				graphicalEdge.registerUIObject(this);
			}
		}
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
		
		refreshLabel(edge, line);
		
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
		AbstractTraceEditPart source = (AbstractTraceEditPart) getViewer().getEditPartRegistry().get(edge.getFrom());
		if (source == null) {
			deactivate();
			return;
		}
		setSource(source);
		AbstractTraceEditPart target = (AbstractTraceEditPart) getViewer().getEditPartRegistry().get(edge.getTo());
		setTarget(target);
		final Label contextLabel = new Label(EditPartUtils.getContext(edge.getCtx()));
		contextLabel.setFont(TEXT_FONT);
		figure.setToolTip(contextLabel);
		
		figure.setForegroundColor(VarvizConstants.getColor(edge.getColor()));
		((PolylineConnection)figure).setLineWidth(edge.getWidth());
		
		refreshLabel(edge);
	}
	
	private void refreshLabel(Edge edge) {
		refreshLabel(edge, (PolylineConnection) getFigure());
	}
	
	private void refreshLabel(Edge edge, PolylineConnection figure) {
		if (showLabel || (VarvizView.showLables && !Conditional.isTautology(edge.getCtx()))) {
			createOrSetLabel(edge, figure);
		} else if (label != null) {
			figure.remove(label);
		}
	}

	private void createOrSetLabel(Edge edge, PolylineConnection figure) {
		MidpointLocator sourceEndpointLocator = new MidpointLocator(figure, 0);
		if (label != null) {
			figure.add(label, sourceEndpointLocator);
			return;
		}
		label = new Label();
		// Fonts that support logical symbols:
		// Cambria, Lucida Sans Unicode, Malgun Gothic, Segoe UI Symbol
		label.setFont(TEXT_FONT);
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
		// TODO this is not called????
		Edge edgeModel = getEdgeModel();
		if (VarvizView.GRAPHICAL_TRACE != null) {
			GraphicalEdge graphicalEdge = VarvizView.GRAPHICAL_TRACE.getGraphicalEdge(edgeModel);
			if (graphicalEdge != null) {
				graphicalEdge.registerUIObject(this);
			}
		}
		
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
	
	@Override
	public void performRequest(Request request) {
		if ("open".equals(request.getType())) {
			showLabel = !showLabel;
			refreshVisuals();
		}
	}

	@Override
	public void propertyChange(VarvizEvent event) {
		switch (event.getType()) {
		case LABEL_CHANGED:
			refreshLabel(getEdgeModel());
			break;
		case COLOR_CHANGED:
		case LOCATION_CHANGED:
		default:
			throw new VarvizException("Event " + event.getType() + " not supported for " + getClass());
		}
	}

}
