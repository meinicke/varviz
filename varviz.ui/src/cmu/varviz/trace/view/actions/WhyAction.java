package cmu.varviz.trace.view.actions;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.view.editparts.MethodEditPart;

/**
 * Action to hide the selected element from the trace.
 * 
 * @author Jens Meinicke
 *
 */
public class WhyAction extends Action {

	private GraphicalViewerImpl viewer;

	public WhyAction(String text, GraphicalViewerImpl viewer) {
		super(text);
		this.viewer = viewer;
	}

	public static Set<String> whys = new HashSet<>();

	@Override
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object selectedItem = selection.getFirstElement();
		if (selectedItem != null) {
			final MethodElement<?> element;
			if (selectedItem instanceof MethodEditPart) {
				element = ((MethodEditPart) selectedItem).getMethodModel();
			} else {
				return;
			}
			String entry = element.toString();
			if (whys.contains(entry)) {
				whys.remove(entry);
			} else {
				whys.add(entry);
			}
		}
	}
}
