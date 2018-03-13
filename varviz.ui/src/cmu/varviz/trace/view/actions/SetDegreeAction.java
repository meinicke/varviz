package cmu.varviz.trace.view.actions;

import org.eclipse.jface.action.Action;

import cmu.varviz.trace.view.VarvizView;

/**
 * Action to hide the selected element from the trace.
 * 
 * @author Jens Meinicke
 *
 */
public class SetDegreeAction extends Action {

	private final int degree;

	public SetDegreeAction(VarvizView varvizViewView, Integer degree) {
		super(degree.toString());
		this.degree = degree;
	}
	
	@Override
	public void run() {
		VarvizView.minDegree = degree;
		VarvizView.projectID--;
		VarvizView.refreshVisuals();
	}
}
