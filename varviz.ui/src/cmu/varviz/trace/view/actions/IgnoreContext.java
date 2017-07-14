package cmu.varviz.trace.view.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TreeItem;

import cmu.conditional.Conditional;
import cmu.varviz.trace.IFStatement;
import cmu.varviz.trace.Slicer;
import cmu.varviz.trace.Slicer.State;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.generator.TraceGenerator;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.editparts.EdgeEditPart;
import cmu.varviz.trace.view.editparts.MethodEditPart;
import cmu.varviz.trace.view.editparts.StatementEditPart;
import de.fosd.typechef.featureexpr.FeatureExpr;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.SingleFeatureExpr;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExprFactory;
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
	private static VarvizView varvizViewView;

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
		Set<String> includedFeatures = new HashSet<>();
		Collection<SingleFeatureExpr> includedFeatureExpressions = new ArrayList<>();
		
		SingleFeatureExpr slicefeature = null;
		
		scala.collection.immutable.Set<SingleFeatureExpr> distinctfeatureObjects = ctx.collectDistinctFeatureObjects();
		Iterator<SingleFeatureExpr> iterator = distinctfeatureObjects.iterator();
		while (iterator.hasNext()) {
			SingleFeatureExpr next = iterator.next();
			includedFeatures.add(Conditional.getCTXString(next));
			includedFeatureExpressions.add(next);
		}
		
		

		// select the features of the exception
		// check whether the other features can be (de)selected

		final TraceGenerator generator = VarvizView.generator;
		generator.clearIgnoredFeatures();

		System.out.println("check " + Conditional.getCTXString(ctx));
		FeatureExpr ctxcheck = FeatureExprFactory.True();
		for (Entry<String, SingleFeatureExpr> feature : generator.getFeatures().entrySet()) {
			if (!includedFeatures.contains(Conditional.getCTXString(feature.getValue()))) {
				final State selection = Slicer.slice(ifStatements, includedFeatureExpressions, feature.getValue());
				switch (selection) {
				case UNKNOWN:
				case DESELECTED:
					if (checkSetisfiable(ctxcheck, ctx, feature.getValue().not())) {
						ctxcheck = ctxcheck.andNot(feature.getValue());
						generator.getIgnoredFeatures().put(feature.getValue(), false);
						
					} else if (checkSetisfiable(ctxcheck, ctx, feature.getValue())) { 
						ctxcheck = ctxcheck.and(feature.getValue());
						generator.getIgnoredFeatures().put(feature.getValue(), true);
					}
					break;
				case SELECTED:
					if (checkSetisfiable(ctxcheck, ctx, feature.getValue())) {
						ctxcheck = ctxcheck.and(feature.getValue());
						generator.getIgnoredFeatures().put(feature.getValue(), true);
					} else if (checkSetisfiable(ctxcheck, ctx, feature.getValue().not())) {
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
		
//		for (Entry<String, SingleFeatureExpr> singleFeatureExpr : generator.getFeatures().entrySet()) {
//			Boolean featureSelection = generator.getIgnoredFeatures().get(singleFeatureExpr.getValue());
//			if (featureSelection != null) {
//				System.out.println(singleFeatureExpr.getKey() + " -> " + featureSelection);	
//			} else {
//				System.out.println(singleFeatureExpr.getKey() + " -> Conditional");
//			}
//		}
		
//		System.out.println(ctxcheck);

//		createAdditioanlConstraint();

	}
	
	private static boolean checkSetisfiable(FeatureExpr ctxcheck, FeatureExpr ctx, FeatureExpr B) {
		return Conditional.isSatisfiable(ctxcheck.and(B).and(ctx)) && Conditional.isSatisfiable(ctxcheck.and(B).andNot(ctx));
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
