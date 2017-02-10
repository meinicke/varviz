package cmu.varviz.trace.view.actions;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import cmu.conditional.Conditional;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.editparts.EdgeEditPart;
import cmu.varviz.trace.view.editparts.MethodEditPart;
import cmu.varviz.trace.view.editparts.StatementEditPart;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import gov.nasa.jpf.JPF;
import scala.collection.Iterator;

/**
 * Action to remove all features that are not contained in the context of the selected element.
 * 
 * @author Jens Meinicke
 *
 */
public class IgnoreContext extends Action {

	private GraphicalViewerImpl viewer;
	private VarvizView varvizViewView;
		
	public IgnoreContext(String text, GraphicalViewerImpl viewer, VarvizView varvizViewView) {
		super(text);
		this.viewer = viewer;
		this.varvizViewView = varvizViewView;
	}
	
	@Override
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object selectedItem = selection.getFirstElement();
		if (selectedItem != null) {
			final FeatureExpr ctx;
			if (selectedItem instanceof MethodEditPart) {
				ctx = ((MethodEditPart) selectedItem).getMethodModel().getCTX();
			} else if (selectedItem instanceof StatementEditPart) {
				ctx = ((StatementEditPart) selectedItem).getStatementModel().getCTX();
			} else if (selectedItem instanceof EdgeEditPart){
				ctx = ((EdgeEditPart) selectedItem).getEdgeModel().getCtx();
			} else {
				return;
			}
			
			Set<String> includedFeatures = new HashSet<>();
			scala.collection.immutable.Set<SingleFeatureExpr> distinctfeatureObjects = ctx.collectDistinctFeatureObjects();
			Iterator<SingleFeatureExpr> iterator = distinctfeatureObjects.iterator();
			while (iterator.hasNext()) {
				includedFeatures.add(Conditional.getCTXString(iterator.next()));
			}
			
			for (Entry<String, SingleFeatureExpr> feature : Conditional.features.entrySet()) {
				if (!includedFeatures.contains(Conditional.getCTXString(feature.getValue()))) {
					JPF.ignoredFeatures.put(Conditional.getCTXString(feature.getValue()), false);
				}
			}
			varvizViewView.refresh();
		}
	}
	
}
