package cmu.varviz.trace.view.actions;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.editparts.EdgeEditPart;
import cmu.varviz.trace.view.editparts.MethodEditPart;
import cmu.varviz.trace.view.editparts.StatementEditPart;
import de.fosd.typechef.featureexpr.FeatureExpr;

/**
 * Action to hide the selected element from the trace.
 * 
 * @author Jens Meinicke
 *
 */
public class HighlightPathAction extends Action {

	private GraphicalViewerImpl viewer;

	public HighlightPathAction(String text, GraphicalViewerImpl viewer, VarvizView varvizViewView) {
		super(text);
		this.viewer = viewer;
	}

	@Override
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object selectedItem = selection.getFirstElement();
		if (selectedItem != null) {
			final FeatureExpr ctx;
			if (selectedItem instanceof MethodEditPart) {
				ctx = ((MethodEditPart) selectedItem).getMethodModel().getCTX();
			} else if (selectedItem instanceof StatementEditPart) {
				ctx = ((StatementEditPart) selectedItem).getStatementModel().getCTX();
			} else if (selectedItem instanceof EdgeEditPart) {
				ctx = ((EdgeEditPart) selectedItem).getEdgeModel().getCtx();
			} else {
				return;
			}

			// TODO revise update
			VarvizView.getTRACE().createEdges();
//			varvizViewView.trace.highlightNotTautology();
			VarvizView.getTRACE().highlightException(ctx);
//			varvizViewView.trace.highlightContext(ctx, NodeColor.limegreen, 2);
			VarvizView.refreshVisuals();
		}
	}
}
