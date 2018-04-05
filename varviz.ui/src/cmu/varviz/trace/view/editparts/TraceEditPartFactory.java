package cmu.varviz.trace.view.editparts;

import javax.annotation.Nullable;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

import cmu.varviz.trace.Edge;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.view.VarvizView;

/**
 * The {@link EditPartFactory} for the {@link Trace} elements. 
 * 
 * @author Jens Meinicke
 *
 */
public class TraceEditPartFactory implements EditPartFactory {

	private final VarvizView varvizView;

	public TraceEditPartFactory(VarvizView varvizView) {
		this.varvizView = varvizView;
	}

	@Nullable
	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		if (model instanceof Trace) {
			return new TraceEditPart((Trace) model);
		}
		if (model instanceof Method) {
			return new MethodEditPart((Method) model, varvizView.getTRACE());
		}
		if (model instanceof Statement) {
			return new StatementEditPart((Statement) model, varvizView.getTRACE(), varvizView.getGraphicalTrace());
		}
		if (model instanceof Edge) {
			return new EdgeEditPart((Edge) model, varvizView.getGraphicalTrace());
		}
		if (model == null) {
			System.err.println("null argument");
			return null;
		}
		System.err.println(model.getClass() + " has no corresponding edit part");
		return null;
	}

}
