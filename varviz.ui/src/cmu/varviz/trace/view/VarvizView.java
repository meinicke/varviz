package cmu.varviz.trace.view;

import java.io.File;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import cmu.varviz.VarvizActivator;
import cmu.varviz.io.graphviz.GrapVizExport;
import cmu.varviz.io.xml.XMLReader;
import cmu.varviz.io.xml.XMLWriter;
import cmu.varviz.trace.IFStatement;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.filters.InteractionFilter;
import cmu.varviz.trace.filters.Or;
import cmu.varviz.trace.filters.StatementFilter;
import cmu.varviz.trace.view.editparts.TraceEditPartFactory;
import cmu.vatrace.ExceptionFilter;
import gov.nasa.jpf.JPF;

/**
 * TODO description
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizView extends ViewPart {

	private ScrollingGraphicalViewer viewer;
	private ScalableFreeformRootEditPart rootEditPart;

	@Override
	public void createPartControl(Composite parent) {
//		IWorkbenchWindow editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
//		IEditorPart part = null;
//
//		if (editor != null) {
//			IWorkbenchPage page = editor.getActivePage();
//			if (page != null) {
//				part = page.getActiveEditor();
//			}
//		}

		viewer = new ScrollingGraphicalViewer();
		viewer.createControl(parent);
		// viewer.getControl().setBackground(DIAGRAM_BACKGROUND);
		viewer.setEditDomain(new EditDomain());
		viewer.setEditPartFactory(new TraceEditPartFactory());

		rootEditPart = new ScalableFreeformRootEditPart();
		((ConnectionLayer) rootEditPart.getLayer(LayerConstants.CONNECTION_LAYER)).setAntialias(SWT.ON);
		viewer.setRootEditPart(rootEditPart);
		refresh();

		refreshButton = new Action() {
			public void run() {
				refresh();
			}
		};

		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager toolbarManager = bars.getToolBarManager();
		toolbarManager.add(refreshButton);
		refreshButton.setImageDescriptor(VarvizActivator.REFESH_TAB_IMAGE_DESCRIPTOR);

		viewer.getControl().addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent ev) {
				if ((ev.stateMask & SWT.CTRL) == 0) {
					return;
				}
				if (ev.count > 0) {
					((ScalableFreeformRootEditPart) viewer.getRootEditPart()).getZoomManager().zoomIn();
				} else if (ev.count < 0) {
					((ScalableFreeformRootEditPart) viewer.getRootEditPart()).getZoomManager().zoomOut();
				}
			}
		});
	}

	@Override
	public void setFocus() {

	}

	LayoutManager lm = new LayoutManager();
	private Action refreshButton;

	public void refresh() {
		final Trace model = createTrace();
		if (model.getMain() == null) {
			return;
		}
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				viewer.getEditPartRegistry().clear();
				viewer.setRootEditPart(rootEditPart);
				viewer.setContents(model);
				viewer.getContents().refresh();
				lm.layout(viewer.getContents());
			}
		});
	}

	public static Trace trace = null;

//	public static final String PROJECT_NAME = "MathBug";
	public static final String PROJECT_NAME = "SmallInteractionExamples";
//	public static final String PROJECT_Sources = "MathSources";
//	public static final String PROJECT_Sources_Folder = "Bug6/src/main/java";
//	public static final String PROJECT_Sources_Test_Folder = "Bug6/src/test/java";
	public static final String PROJECT_Sources = "mathIssue280";
	public static final String PROJECT_Sources_Folder = "src/java";
	public static final String PROJECT_Sources_Test_Folder = "src/test";
	
	public static Trace createTrace() {
		if (trace == null) {
//			final String path = "C:/Users/Jens Meinicke/workspaceVarexJ/Elevator/";
//			final String path = "C:/Users/Jens Meinicke/workspaceVarexJ/" + PROJECT_NAME;
			final String path = "C:/Users/Jens Meinicke/git/VarexJ/" + PROJECT_NAME;
			final String[] args = { 
//					"+classpath=" + path + "/bin,${jpf-core}/lib/junit-4.11.jar,${jpf-core}/lib/math6.jar,${jpf-core}/lib/bcel-5.2.jar",
//					"+classpath=" + path + "/bin,${jpf-core}/lib/junit-4.11.jar,C:/Users/Jens Meinicke/workspaceVarexJ/MathBug/commons-math-2.0-SNAPSHOT.jar,${jpf-core}/lib/bcel-5.2.jar",
					"+classpath=" + path + "/bin,${jpf-core}",
					"+nhandler.delegateUnhandledNative",
					"+search.class=.search.RandomSearch",
					"+invocation",
//					"linux.Example"
//					"Main"
					"linux.Linux1"
//					"SmoothingPolynomialBicubicSplineInterpolatorTest"
//					"Test"
//					"SimplexOptimizerNelderMeadTestStarter"
					};
			JPF.vatrace.filter = new Or(
//					new NameFilter("interpolatedDerivatives" , "previousState"),
//					new ReferenceFilter(888),
//					new NameFilter("tMin", "tb"),
//					new And(
//							new NameFilter("r"),
//							new NameFilter("ret"),
//							new StatementFilter() {
//								
//								@Override
//								public boolean filter(Statement s) {
//									return s.getMethod().getMethodInfo().getName().equals("logGamma");
//								}
//							}),
					new InteractionFilter(2),
					new ExceptionFilter(), 
					new StatementFilter() {
				
				@Override
				public boolean filter(Statement<?> s) {
					return s instanceof IFStatement;
				}
			});
			
			JPF.main(args);
			
			final File xmlFile = new File("graph.xml");
			XMLWriter writer = new XMLWriter(JPF.vatrace);
			try {
				writer.writeToFile(xmlFile);

				XMLReader reader = new XMLReader();
				Trace trace = reader.readFromFile(xmlFile);
				GrapVizExport exporter = new GrapVizExport("graph", trace);
				exporter.write();
				VarvizView.trace= trace;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// highlight path ¬patch60&¬patch42&patch53&¬patch48
//			SingleFeatureExpr patch60 = Conditional.features.get("patch60");
//			SingleFeatureExpr patch42 = Conditional.features.get( "patch42");
//			SingleFeatureExpr patch53 = Conditional.features.get("patch53");
//			SingleFeatureExpr patch48 = Conditional.features.get("patch48");
//			FeatureExpr ctx = patch60.not().andNot(patch42).and(patch53).andNot(patch48);
//			JPF.vatrace.highlightContext(ctx, NodeColor.limegreen, 1);
			
//			return JPF.vatrace;
		}
		return trace;
	}

}