package cmu.varviz.trace.view.actions;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import cmu.conditional.Conditional;
import cmu.varviz.trace.generator.TraceGenerator;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.editparts.EdgeEditPart;
import cmu.varviz.trace.view.editparts.MethodEditPart;
import cmu.varviz.trace.view.editparts.StatementEditPart;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExprFactory;
import scala.collection.Iterator;

/**
 * Action to remove all features that are not contained in the context of the selected element.
 * 
 * @author Jens Meinicke
 *
 */
public class IgnoreContext extends Action {

	private GraphicalViewerImpl viewer;

	public IgnoreContext(String text, GraphicalViewerImpl viewer, VarvizView varvizViewView) {
		super(text);
		this.viewer = viewer;
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
			} else if (selectedItem instanceof EdgeEditPart) {
				ctx = ((EdgeEditPart) selectedItem).getEdgeModel().getCtx();
			} else {
				return;
			}

			removeContext(ctx);

			VarvizView.projectID--;
			VarvizView.refreshVisuals();
		}
	}

	public static void removeContext(final FeatureExpr ctx) {
		Set<String> includedFeatures = new HashSet<>();
		scala.collection.immutable.Set<SingleFeatureExpr> distinctfeatureObjects = ctx.collectDistinctFeatureObjects();
		Iterator<SingleFeatureExpr> iterator = distinctfeatureObjects.iterator();
		while (iterator.hasNext()) {
			includedFeatures.add(Conditional.getCTXString(iterator.next()));
		}

		// select the features of the exception
		// check whether the other features can be (de)selected

		final TraceGenerator generator = VarvizView.generator;
		generator.clearIgnoredFeatures();

		FeatureExpr ctxcheck = Conditional.simplifyCondition(ctx);
		for (Entry<String, SingleFeatureExpr> feature : generator.getFeatures().entrySet()) {
			if (!includedFeatures.contains(Conditional.getCTXString(feature.getValue()))) {
				if (!Conditional.isContradiction(ctxcheck.and(feature.getValue())) && !Conditional.isContradiction(ctxcheck.andNot(feature.getValue()))) {
					generator.getIgnoredFeatures().put(feature.getValue(), false);
					ctxcheck = ctxcheck.andNot(feature.getValue());
				}
			}
		}

		createAdditioanlConstraint();

	}

	/**
	 * Sets the additional constraint for VarexJ
	 */
	private static void createAdditioanlConstraint() {
		// TODO does that actually matter?
		FeatureExpr additionalConstraint = BDDFeatureExprFactory.True();
		for (Entry<FeatureExpr, Boolean> feature : VarvizView.generator.getIgnoredFeatures().entrySet()) {
			if (feature.getValue() != null) {
				final FeatureExpr f = feature.getKey();
				if (feature.getValue()) {
					additionalConstraint = additionalConstraint.and(f);
				} else {
					additionalConstraint = additionalConstraint.and(f.not());
				}
			}
		}
		Conditional.additionalConstraint = additionalConstraint;
	}

}
