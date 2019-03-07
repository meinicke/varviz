package cmu.varviz.trace.view.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import cmu.varviz.slicing.BackwardsSlicer;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.editparts.StatementEditPart;

public class SliceAction extends Action {

	private VarvizView view;

	public SliceAction(String text, VarvizView view) {
		super(text);
		this.view = view;
	}

	@Override
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) view.getViewer().getSelection();
		Object selectedItem = selection.getFirstElement();
		if (selectedItem != null) {
			final Statement element;
			if (selectedItem instanceof StatementEditPart) {
				element = ((StatementEditPart) selectedItem).getStatementModel();
			} else {
				return;
			}
			
			Trace trace = view.getTRACE();
			trace.setFilter(e -> e.getElementID() <= element.getElementID());
			trace.finalizeGraph();
			
			new BackwardsSlicer().slice(view.getClasspath(), element, trace);
			view.setTrace(view.getTRACE());
			view.refreshVisuals();
		}
	}

}
