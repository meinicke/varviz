package cmu.varviz.trace.view.actions;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import cmu.conditional.Conditional;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
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

	private VarvizView view;

	public RemovePathAction(String text, VarvizView varvizView) {
		super(text);
		this.view = varvizView;
	}

	@Override
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) view.getViewer().getSelection();
		Object selectedItem = selection.getFirstElement();
		if (selectedItem != null) {
			final Deque<MethodElement> stack;
			final FeatureExpr ctx;
			{
				final MethodElement s;
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
				final MethodElement currentStatement = stack.pop();
				if (currentStatement.getCTX().equals(Conditional.and(currentStatement.getCTX(),ctx))) {
					final Method parent = currentStatement.getParent();
					if (parent != null) {
						parent.filterExecution(e -> e != currentStatement);
					}
					if (currentStatement.to == null) {
						continue;
					}
					for (MethodElement next : currentStatement.to.toList()) {
						if (next != null) {
							stack.push(next);
						}
					}
				}
			}

			view.getTRACE().finalizeGraph();
			view.refreshVisuals();
		}
	}
}
