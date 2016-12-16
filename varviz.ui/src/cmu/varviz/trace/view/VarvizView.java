package cmu.varviz.trace.view;

import java.io.File;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import cmu.varviz.VarvizActivator;
import cmu.varviz.io.graphviz.GrapVizExport;
import cmu.varviz.io.xml.XMLReader;
import cmu.varviz.io.xml.XMLWriter;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.view.actions.HideAction;
import cmu.varviz.trace.view.actions.HighlightPathAction;
import cmu.varviz.trace.view.editparts.TraceEditPartFactory;
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
		viewer = new ScrollingGraphicalViewer();
		viewer.createControl(parent);
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
		
		createContextMenu();
	}
	
	public void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				fillContextMenu(m);
			}
		});
		Control control = viewer.getControl();
		Menu menu = menuMgr.createContextMenu(control);
		control.setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);

	}
	
	private void fillContextMenu(IMenuManager menuMgr) {
		menuMgr.add(new HideAction("Hide", viewer, this));
		menuMgr.add(new HighlightPathAction("Highlight Path", viewer, this));
	}

	@Override
	public void setFocus() {

	}

	LayoutManager lm = new LayoutManager();
	private Action refreshButton;

	public static Trace trace = null;
	public void refresh() {
		trace = createTrace();
		refreshVisuals();
	}

	public void refreshVisuals() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				viewer.setContents(trace);
				viewer.getContents().refresh();
				lm.layout(viewer.getContents());
			}
		});
	}


//	public static final String PROJECT_NAME = "MathBug";
	public static final String PROJECT_NAME = "SmallInteractionExamples";
//	public static final String PROJECT_Sources = "MathSources";
//	public static final String PROJECT_Sources_Folder = "Bug6/src/main/java";
//	public static final String PROJECT_Sources_Test_Folder = "Bug6/src/test/java";
	public static final String PROJECT_Sources = "mathIssue280";
	public static final String PROJECT_Sources_Folder = "src/java";
	public static final String PROJECT_Sources_Test_Folder = "src/test";
	
	static int projectID = 1;
	
	public Trace createTrace() {
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
//					"linux.Linux1"
					"linux.Linux" + ((projectID++)%5 +1)
//					"SmoothingPolynomialBicubicSplineInterpolatorTest"
//					"Test"
//					"SimplexOptimizerNelderMeadTestStarter"
					};
			JPF.vatrace = new Trace();
//			JPF.vatrace.filter = new Or(
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
//					new InteractionFilter(2),
//					new ExceptionFilter(), 
//					new StatementFilter() {
//				
//				@Override
//				public boolean filter(Statement<?> s) {
//					return s instanceof IFStatement;
//				}
//			});
			
			JPF.main(args);
			
			final File xmlFile = new File("graph.xml");
			XMLWriter writer = new XMLWriter(JPF.vatrace);
			try {
				writer.writeToFile(xmlFile);
				XMLReader reader = new XMLReader();
				Trace trace = reader.readFromFile(xmlFile);
				GrapVizExport exporter = new GrapVizExport("graph", trace);
				exporter.write();
				return trace;
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
//		}
		return null;
	}

}