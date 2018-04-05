package cmu.varviz.trace.view;

import org.eclipse.gef.EditPartViewer;

import cmu.varviz.trace.Trace;
import cmu.varviz.trace.view.editparts.MethodEditPart;

/**
 * Utility class for the {@link VarvizView}.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizViewerUtils {
	
	private VarvizViewerUtils() {
		// nothing here
	}

	/**
	 * Sets the focus to the trace again if the trace is out of sight.
	 * 
	 */
	public static void refocusView(EditPartViewer viewer, Trace trace) {
		MethodEditPart mainEditPart = (MethodEditPart) viewer.getEditPartRegistry().get(trace.getMain());
		if (mainEditPart != null) {
			viewer.reveal(mainEditPart);
		}
	}
}
