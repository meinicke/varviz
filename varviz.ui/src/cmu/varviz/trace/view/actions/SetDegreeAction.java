package cmu.varviz.trace.view.actions;

import org.eclipse.jface.action.Action;

import cmu.varviz.trace.Method;
import cmu.varviz.trace.view.VarvizView;

/**
 * Action to hide the selected element from the trace.
 * 
 * @author Jens Meinicke
 *
 */
public class SetDegreeAction extends Action {

	private VarvizView varvizViewView;
	private final int degree;

	public SetDegreeAction(VarvizView varvizViewView, Integer degree) {
		super(degree.toString());
		this.varvizViewView = varvizViewView;
		this.degree = degree;
	}
	
	@Override
	public void run() {
		varvizViewView.minDegree = degree;
		varvizViewView.projectID--;
		varvizViewView.refresh();
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
