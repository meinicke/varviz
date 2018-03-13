package cmu.varviz.trace.view;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;

import cmu.varviz.trace.view.editparts.MethodEditPart;

/**
 * Utility class for the {@link VarvizView}.
 * 
 * @author Jens Meinicke
 *
 */
public interface VarvizViewerUtils {

	/**
	 * Sets the focus to the trace again if the trace is out of sight.
	 * 
	 */
	public static void refocusView(GraphicalViewerImpl viewer) {
		MethodEditPart mainEditPart = (MethodEditPart) viewer.getEditPartRegistry().get(VarvizView.getTRACE().getMain());
		if (mainEditPart != null) {
			viewer.reveal(mainEditPart);
		}
	}
}
