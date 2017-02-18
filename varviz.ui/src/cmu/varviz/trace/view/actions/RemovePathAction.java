package cmu.varviz.trace.view.actions;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import cmu.varviz.trace.Method;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.editparts.EdgeEditPart;
import cmu.varviz.trace.view.editparts.StatementEditPart;
import de.fosd.typechef.featureexpr.FeatureExpr;

/**
 * Action to hide the the branch after the selected element.
 * 
 * @author Jens Meinicke
 *
 */
public class RemovePathAction extends Action {

	private GraphicalViewerImpl viewer;
	private VarvizView varvizViewView;

	public RemovePathAction(String text, GraphicalViewerImpl viewer, VarvizView varvizViewView) {
		super(text);
		this.viewer = viewer;
		this.varvizViewView = varvizViewView;
	}
	
	@Override
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object selectedItem = selection.getFirstElement();
		if (selectedItem != null) {
			final Deque<Statement<?>> stack;
			final FeatureExpr ctx;
			{
				final Statement<?> s;
				if (selectedItem instanceof EdgeEditPart) {
					s = ((EdgeEditPart) selectedItem).getEdgeModel().getTo();
				} else if (selectedItem instanceof StatementEditPart) {
					s = ((StatementEditPart) selectedItem).getStatementModel();
				} else {
					return;
				}
				stack = new ArrayDeque<>();
				stack.push(s);
				ctx = s.getCTX();
			}
			while (!stack.isEmpty()) {
				final Statement<?> currentStatement = stack.pop();
				if (currentStatement.getCTX().equals(currentStatement.getCTX().and(ctx))) {
					final Method<?> parent = currentStatement.getParent();
					if (parent != null) {
						parent.filterExecution(e -> e != currentStatement);
						filterParents(parent);
					}
					if (currentStatement.to == null) {
						continue;
					}
					for (Statement<?> next : currentStatement.to.toList()) {
						if (next != null) {
							stack.push(next);
						}
					}
				}
			}
			
			VarvizView.trace.createEdges();
			VarvizView.trace.highlightException();
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
