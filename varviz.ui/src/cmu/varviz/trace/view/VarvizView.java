package cmu.varviz.trace.view;

import java.io.File;

import org.eclipse.draw2d.ConnectionLayer;
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
import cmu.varviz.trace.view.editparts.TraceEditPartFactory;
import cmu.varviz.utils.FileUtils;
import cmu.vatrace.ExceptionFilter;
import de.fosd.typechef.featureexpr.FeatureExpr;
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
	
	static {
		// create the site.properties in the .jpf folder 
		File userHome = new File(System.getProperty("user.home"));
		File jpfPath = new File(userHome.getPath() + "/.jpf");
		if (!jpfPath.exists()) {
			jpfPath.mkdir();
		}
		FileUtils.CopyFileFromVarvizJar("/res", "site.properties", jpfPath);
	}
	
	private static final String PROGRAMS_PATH = System.getProperty("user.home") + "/git/EvaluationPrograms/";

	private PrintAction printAction;

	private ScrollingGraphicalViewer viewer;
	private ScalableFreeformRootEditPart rootEditPart;

	private Action refreshButton;
	private Action showLablesButton;
	private Action exportGraphVizButton;

	public static boolean showLables = false;

	public static int projectID = 0;

	public static int minDegree = 2;
	
	private LayoutManager lm = new LayoutManager();

	public static Trace trace = null;

	private enum projects {
		FOOBAR, GAME_SCREEN, ELEVATOR, NANOXML
	}
	
	private static projects SELECTED_PROJECT = projects.FOOBAR;
	
	private static String[] PROJECT_PRAMETERS;
	
	public static String PROJECT_NAME = "";
	
	public static final String PROJECT_Sources = "NanoXML";
	public static final String PROJECT_Sources_Folder = "Sources/Java";
	public static final String PROJECT_Sources_Test_Folder = "Test/Java";
	
	private String getPath() {
		return PROGRAMS_PATH + PROJECT_NAME;
	}
	
	private void setProject() {
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
		case FOOBAR:
			PROJECT_PRAMETERS = new String[]{"FooBar", "Main"};
			break;
		default:
			throw new RuntimeException(SELECTED_PROJECT + " not covered");
		}
		PROJECT_NAME = PROJECT_PRAMETERS[0];
	}
	
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
				fileDialog.setFilterPath(getPath());
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

		viewer.getControl().addMouseWheelListener(ev -> {
			if ((ev.stateMask & SWT.CTRL) == 0) {
				return;
			}
			if (ev.count > 0) {
				((ScalableFreeformRootEditPart) viewer.getRootEditPart()).getZoomManager().zoomIn();
			} else if (ev.count < 0) {
				((ScalableFreeformRootEditPart) viewer.getRootEditPart()).getZoomManager().zoomOut();
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
		menuMgr.add(new HideAction("Hide Element", viewer, this));
	}

	@Override
	public void setFocus() {

	}

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

	public Trace createTrace() {
		setProject();
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
		final String[] args = {
				"+classpath=" + getPath() + "/bin,${jpf-core}",
				"+choice=MapChoice",
				"+stack=StackHandler",
				 "+nhandler.delegateUnhandledNative", "+search.class=.search.RandomSearch",
				 PROJECT_PRAMETERS.length == 3 ? "+featuremodel=" + getPath() + "/" + PROJECT_PRAMETERS[2] : "",
				 PROJECT_PRAMETERS[1]
		};
		JPF.vatrace = new Trace();
		JPF.vatrace.filter = new Or(new And(basefilter, new InteractionFilter(minDegree)), new ExceptionFilter());
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
		JPF.main(args);
		Conditional.additionalConstraint = BDDFeatureExprFactory.True();
		
		FeatureExpr exceptionContext = JPF.vatrace.getExceptionContext();
		IgnoreContext.removeContext(exceptionContext);
		if (!JPF.ignoredFeatures.isEmpty()) {
			JPF.vatrace = new Trace();
			JPF.vatrace.filter = new Or(new And(basefilter, new InteractionFilter(minDegree)), new ExceptionFilter());
			JPF.main(args);
			Conditional.additionalConstraint = BDDFeatureExprFactory.True();
		}
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