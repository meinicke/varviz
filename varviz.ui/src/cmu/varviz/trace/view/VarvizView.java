package cmu.varviz.trace.view;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import cmu.conditional.Conditional;
import cmu.varviz.VarvizActivator;
import cmu.varviz.io.graphviz.Format;
import cmu.varviz.io.graphviz.GrapVizExport;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.filters.And;
import cmu.varviz.trace.filters.InteractionFilter;
import cmu.varviz.trace.filters.Or;
import cmu.varviz.trace.filters.StatementFilter;
import cmu.varviz.trace.view.actions.HideAction;
import cmu.varviz.trace.view.actions.IgnoreContext;
import cmu.varviz.trace.view.actions.RemovePathAction;
import cmu.varviz.trace.view.actions.SetDegreeAction;
import cmu.varviz.trace.view.editparts.TraceEditPartFactory;
import cmu.vatrace.ExceptionFilter;
import de.fosd.typechef.featureexpr.FeatureExprFactory;
import de.fosd.typechef.featureexpr.bdd.BDDFeatureExprFactory;
import gov.nasa.jpf.JPF;

/**
 * The varviz view.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizView extends ViewPart {

	private PrintAction printAction;

	private ScrollingGraphicalViewer viewer;
	private ScalableFreeformRootEditPart rootEditPart;

	private Action refreshButton;
	private Action showLablesButton;
	private Action exportGraphVizButton;

	public static boolean showLables = false;

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

		printAction = new PrintAction(this);

		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.PRINT.getId(), printAction);

		IToolBarManager toolbarManager = bars.getToolBarManager();

		createRefreshButton(toolbarManager);
		
		showLablesButton = new Action() {
			public void run() {
				showLables = !showLables;
				trace.createEdges();
				trace.highlightException();
				refreshVisuals();
			}
		};
		showLablesButton.setToolTipText("Show edge context");
		toolbarManager.add(showLablesButton);
		showLablesButton.setChecked(showLables);
		showLablesButton.setImageDescriptor(VarvizActivator.LABEL_IMAGE_DESCRIPTOR);

		exportGraphVizButton = new Action() {
			public void run() {
				FileDialog fileDialog = new FileDialog(parent.getShell(), SWT.SAVE);
				fileDialog.setFilterPath(path);
				fileDialog.setFileName(PROJECT_NAME);
				final String[] extensions = new String[Format.values().length] ;
				for (int i = 0; i < extensions.length; i++) {
					extensions[i] = "*." + Format.values()[i];
				}
				
				fileDialog.setFilterExtensions(extensions);
				String location = fileDialog.open();
				if (location != null) {
					GrapVizExport exporter = new GrapVizExport(location, trace);
					exporter.write();
				}
			}
		};

		exportGraphVizButton.setToolTipText("Export with GraphViz");
		toolbarManager.add(exportGraphVizButton);

		exportGraphVizButton.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID, "icons/full/etool16/export_wiz.png"));

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

	private void createRefreshButton(IToolBarManager toolbarManager) {
		refreshButton = new Action("Build Variational Graph", Action.AS_DROP_DOWN_MENU) {
			public void run() {
				runRefreshButton();
			}
		};
		toolbarManager.add(refreshButton);
		refreshButton.setImageDescriptor(VarvizActivator.REFESH_TAB_IMAGE_DESCRIPTOR);
		
		refreshButton.setMenuCreator(new IMenuCreator() {

			Menu fMenu = null;

			@Override
			public Menu getMenu(Menu parent) {
				return fMenu;
			}

			@Override
			public Menu getMenu(Control parent) {
				fMenu = new Menu(parent);
				
				for (projects p : projects.values()) {
					Action activateProjectAction = new Action(p.toString(), Action.AS_CHECK_BOX) {
						public void run() {
							SELECTED_PROJECT = p;
							setProject();
							runRefreshButton();
						}

						
					};
					ActionContributionItem contributionItem = new ActionContributionItem(activateProjectAction);
					contributionItem.fill(fMenu, -1);
				}
				
				return fMenu;
			}

			@Override
			public void dispose() {
				fMenu = null;
			}
		});
	}
	
	private void runRefreshButton() {
		// resizes the view
		rootEditPart = new ScalableFreeformRootEditPart();
		((ConnectionLayer) rootEditPart.getLayer(LayerConstants.CONNECTION_LAYER)).setAntialias(SWT.ON);
		viewer.setRootEditPart(rootEditPart);
		
		JPF.ignoredFeatures.clear();
		refresh();
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
		menuMgr.add(new HideAction("Hide Statement", viewer, this));
//		menuMgr.add(new RemoveAllMethodsAction("Remove All Method Calls", viewer, this));
//		menuMgr.add(new RemoveClassAction("Remove Class", viewer, this));
		menuMgr.add(new RemovePathAction("Hide Path", viewer, this));
//		menuMgr.add(new HighlightPathAction("Highlight Path", viewer, this));
		menuMgr.add(new IgnoreContext("Remove unnecessary options", viewer, this));

//		MenuManager exportMenu = new MenuManager("Set Min Interaction Degree");
//		for (int degree = 1; degree <= 6; degree++) {
//			exportMenu.add(new SetDegreeAction(this, degree));
//		}
//		menuMgr.add(exportMenu);
	}

	@Override
	public void setFocus() {

	}

	LayoutManager lm = new LayoutManager();

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

	private static StatementFilter basefilter = new Or(new StatementFilter() {

		@Override
		public boolean filter(Statement<?> s) {
			return !(hasParent(s.getParent(), "java.", "<init>") || hasParent(s.getParent(), "java.", "<clinit>"));
		}

		private boolean hasParent(Method<?> parent, String filter, String filter2) {
			if (parent.toString().contains(filter)) {
				return true;
			}
			parent = parent.getParent();
			if (parent != null) {
				return hasParent(parent, filter, filter2);
			}
			return false;
		}
	});
	
	enum projects { NETPOLL, GAME_SCREEN, ELEVATOR, NANOXML}
	
	private static projects SELECTED_PROJECT = projects.NETPOLL;
	public static String[] PROJECT_PRAMETERS;
	static {
		setProject();
	}

	private static void setProject() {
		switch (SELECTED_PROJECT) {
		case ELEVATOR:
			PROJECT_PRAMETERS = new String[]{"Elevator - Spec3", "Main", "elevator.dimacs"};
			break;
		case GAME_SCREEN:
			PROJECT_PRAMETERS = new String[]{"GameScreen", "GameScreen"};
			break;
		case NANOXML:
			PROJECT_PRAMETERS = new String[]{"nanoxml", "net.n3.nanoxml.Parser1_vw_v1"};
			break;
		case NETPOLL:
			PROJECT_PRAMETERS = new String[]{"NetPoll", "Setup"};
			break;
		default:
			throw new RuntimeException(SELECTED_PROJECT + " not covered");
		}
		PROJECT_NAME = PROJECT_PRAMETERS[0];
		path = "C:/Users/Jens Meinicke/workspaceVarexJ/" + PROJECT_NAME;
	}

	public static String PROJECT_NAME;
	static String path;
	
	// public static final String PROJECT_NAME = "MathBug";
//	public static final String PROJECT_NAME = "SmallInteractionExamples";
//	public static final String PROJECT_NAME = "Email";
//	 public static final String PROJECT_NAME = "Mine";
	// public static final String PROJECT_Sources = "MathSources";
	// public static final String PROJECT_Sources_Folder = "Bug6/src/main/java";
	// public static final String PROJECT_Sources_Test_Folder =
	// "Bug6/src/test/java";
//	public static final String PROJECT_Sources = "mathIssue280";

	public static final String PROJECT_Sources = "NanoXML";
	public static final String PROJECT_Sources_Folder = "Sources/Java";
	public static final String PROJECT_Sources_Test_Folder = "Test/Java";
//	public static final String PROJECT_Sources_Folder = "src/java";
//	public static final String PROJECT_Sources_Test_Folder = "src/test";

	public static int projectID = 0;

	public static int minDegree = 2;

//	final String path = "C:/Users/Jens Meinicke/git/VarexJ/" + PROJECT_NAME;

	public Trace createTrace() {
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
		final String[] args = {
				"+classpath=" + path + "/bin,${jpf-core}",
				"+stack=StackHandler",
				 "+nhandler.delegateUnhandledNative", "+search.class=.search.RandomSearch",
				 PROJECT_PRAMETERS.length == 3 ? "+featuremodel=" + path + "/" + PROJECT_PRAMETERS[2] : "",
				 "+invocation",
				 PROJECT_PRAMETERS[1]

//				"+featuremodel=" + path + "/mine.dimacs",
//				 "+featuremodel=" + path + "/email.dimacs",
//				"+featuremodel=C:\\Users\\Jens Meinicke\\git\\VarexJ\\SmallInteractionExamples\\model.dimacs",
//				 "linux.Example"
//				 "Main"
//				 "linux.Linux1"
//				 "linux.Linux" + ((projectID++)%5 +1)
				// "debugging.Tarantula"
//				"jean.GameScreen"
				// "jean.Http"
				// "jean.Netpoll"

				// "SmoothingPolynomialBicubicSplineInterpolatorTest"
				// "Test"
				// "SimplexOptimizerNelderMeadTestStarter"
//				"EmailSystem.Scenario"
				
		};
		JPF.vatrace = new Trace();
		JPF.vatrace.filter = new Or(new And(basefilter, new InteractionFilter(minDegree)), new ExceptionFilter());

		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
		JPF.main(args);
		Conditional.additionalConstraint = BDDFeatureExprFactory.True(); 
		JPF.vatrace.finalizeGraph();
		return JPF.vatrace;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		if (GraphicalViewer.class.equals(adapter) || ViewPart.class.equals(adapter))
			return viewer;
		return super.getAdapter(adapter);
	}

}