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
	private VarvizView varvizViewView;

	public HideAction(String text, GraphicalViewerImpl viewer, VarvizView varvizViewView) {
		super(text);
		this.viewer = viewer;
		this.varvizViewView = varvizViewView;
	}
	
	@Override
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object selectedItem = selection.getFirstElement();
		if (selectedItem != null) {
			final MethodElement<?> element;
			if (selectedItem instanceof MethodEditPart) {
				element = ((MethodEditPart) selectedItem).getMethodModel();
			} else if (selectedItem instanceof StatementEditPart) {
				element = ((StatementEditPart) selectedItem).getStatementModel();
			} else {
				return;
			}
			
			element.filterExecution(e -> false);
			Method<?> parent = element.getParent();
			if (parent != null) {
				parent.filterExecution(e -> e != element);
			}
			filterParents(element.getParent());
			varvizViewView.trace.createEdges();
			varvizViewView.trace.highlightNotTautology();
			varvizViewView.refreshVisuals();
		}
	}
	
	private void filterParents(Method<?> element) {
		if (element != null) {
			if (element.getChildren().isEmpty()) {
				Method<?> parent = element.getParent();
				if (parent != null) {
					parent.remove(element);
					filterParents(parent);
				}
			}
		}
	}
}
