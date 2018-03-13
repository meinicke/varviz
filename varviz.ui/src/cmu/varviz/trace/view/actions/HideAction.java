package cmu.varviz.trace.view.actions;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.editparts.MethodEditPart;
import cmu.varviz.trace.view.editparts.StatementEditPart;

/**
 * Action to hide the selected element from the trace.
 * 
 * @author Jens Meinicke
 *
 */
public class HideAction extends Action {

	private GraphicalViewerImpl viewer;

	public HideAction(String text, GraphicalViewerImpl viewer) {
		super(text);
		this.viewer = viewer;
	}

	@Override
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object selectedItem = selection.getFirstElement();
		if (selectedItem != null) {
			final MethodElement<?> element;
			if (selectedItem instanceof MethodEditPart) {
				element = ((MethodEditPart) selectedItem).getMethodModel();
				((Method<?>)element).filterExecution(e -> false, true);
			} else if (selectedItem instanceof StatementEditPart) {
				element = ((StatementEditPart) selectedItem).getStatementModel();
			} else {
				return;
			}
			
			Method<?> parent = element.getParent();
			if (parent != null) {
				parent.filterExecution(e -> e != element, true);
			}
			VarvizView.getTRACE().finalizeGraph();
			VarvizView.refreshVisuals();
		}
	}
}
