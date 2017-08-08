package cmu.varviz.trace.view.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import cmu.conditional.Conditional;
import cmu.varviz.trace.IFStatement;
import cmu.varviz.trace.Slicer;
import cmu.varviz.trace.Slicer.State;
import cmu.varviz.trace.generator.TraceGenerator;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.editparts.EdgeEditPart;
import cmu.varviz.trace.view.editparts.MethodEditPart;
import cmu.varviz.trace.view.editparts.StatementEditPart;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureModel;
import gov.nasa.jpf.JPF;
import scala.collection.Iterator;

/**
 * Action to remove all features that are not contained in the context of the
 * selected element.
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
			} else if (selectedItem instanceof EdgeEditPart) {
				ctx = ((EdgeEditPart) selectedItem).getEdgeModel().getCtx();
			} else {
				return;
			}

			removeContext(ctx, JPF.vatrace.getMain().collectIFStatements());

			VarvizView.projectID--;
			varvizViewView.refresh();
		}
	}

	public static void removeContext(final FeatureExpr ctx, Collection<IFStatement<?>> ifStatements) {
		scala.collection.immutable.Set<SingleFeatureExpr> featuresObjects = ctx.collectDistinctFeatureObjects();
		Iterator<SingleFeatureExpr> iterator = featuresObjects.iterator();
		
		SingleFeatureExpr[] features = new SingleFeatureExpr[featuresObjects.size()];
		int i = 0;
		while (iterator.hasNext()) {
			features[i++] = iterator.next();
		}
		removeContext(features, ifStatements);
	}
	
	public static void removeContext(final Collection<SingleFeatureExpr> featureCollection, Collection<IFStatement<?>> ifStatements) {
		SingleFeatureExpr[] features = new SingleFeatureExpr[featureCollection.size()];
		int i = 0;
		for (SingleFeatureExpr singleFeatureExpr : featureCollection) {
			features[i++] = singleFeatureExpr;
		}
		removeContext(features, ifStatements);
	}
		
	public static void removeContext(final SingleFeatureExpr[] features, Collection<IFStatement<?>> ifStatements) {
		Set<String> includedFeatures = new HashSet<>();
		Collection<SingleFeatureExpr> slicingCriterion = new ArrayList<>();
		for (SingleFeatureExpr singleFeatureExpr : features) {
			slicingCriterion.add(singleFeatureExpr);
			includedFeatures.add(Conditional.getCTXString(singleFeatureExpr));
		}
		
		final TraceGenerator generator = VarvizView.generator;
		generator.clearIgnoredFeatures();
		FeatureExpr ctxcheck = FeatureExprFactory.True();
		
		for (Entry<String, SingleFeatureExpr> feature : generator.getFeatures().entrySet()) {
			if (!includedFeatures.contains(Conditional.getCTXString(feature.getValue()))) {
				final State selection = Slicer.slice(ifStatements, slicingCriterion, feature.getValue());
				switch (selection) {
				case UNKNOWN:
				case DESELECTED:
					if (checkSetisfiable(ctxcheck, feature.getValue().not(), features)) {
						ctxcheck = ctxcheck.andNot(feature.getValue());
						generator.getIgnoredFeatures().put(feature.getValue(), false);
					} else if (checkSetisfiable(ctxcheck, feature.getValue(), features)) { 
						ctxcheck = ctxcheck.and(feature.getValue());
						generator.getIgnoredFeatures().put(feature.getValue(), true);
					}
					break;
				case SELECTED:
					if (checkSetisfiable(ctxcheck, feature.getValue(), features)) {
						ctxcheck = ctxcheck.and(feature.getValue());
						generator.getIgnoredFeatures().put(feature.getValue(), true);
					} else if (checkSetisfiable(ctxcheck, feature.getValue().not(), features)) {
						ctxcheck = ctxcheck.andNot(feature.getValue());
						generator.getIgnoredFeatures().put(feature.getValue(), false);
					}
					break;
				case CONDITIONAL:
					break;
				default:
					throw new RuntimeException("implement case " + selection);
				}
			}
		}
	}
	
	private static boolean checkSetisfiable(FeatureExpr ctxcheck, FeatureExpr B, FeatureExpr... features) {
		if (features.length == 0) {
			if (Conditional.isSatisfiable(ctxcheck)) {
				return Conditional.isSatisfiable(ctxcheck.and(B));
			} else {
				return true;
			}
		}
		FeatureExpr andF = ctxcheck.and(features[0]);
		FeatureExpr andNotF = ctxcheck.andNot(features[0]);
		
		FeatureExpr[] subFeatures = new FeatureExpr[features.length - 1];
		System.arraycopy(features, 1, subFeatures, 0, subFeatures.length);
		return checkSetisfiable(andF, B, subFeatures) && checkSetisfiable(andNotF, B, subFeatures); 
	}
	
}
