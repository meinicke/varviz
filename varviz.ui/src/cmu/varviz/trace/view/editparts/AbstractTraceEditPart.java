package cmu.varviz.trace.view.editparts;

import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import cmu.varviz.trace.uitrace.VarvizEventListener;

public abstract class AbstractTraceEditPart extends AbstractGraphicalEditPart implements VarvizEventListener {

	@Override
	protected void createEditPolicies() {
		// nothing by default
	}
	
	public abstract void layout();
}
