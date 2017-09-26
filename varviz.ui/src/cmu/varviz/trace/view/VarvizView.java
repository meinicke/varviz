package cmu.varviz.trace.view;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
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

import cmu.varviz.VarvizActivator;
import cmu.varviz.io.graphviz.Format;
import cmu.varviz.io.graphviz.GrapVizExport;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.filters.Or;
import cmu.varviz.trace.filters.StatementFilter;
import cmu.varviz.trace.generator.TraceGenerator;
import cmu.varviz.trace.generator.varexj.VarexJGenerator;
import cmu.varviz.trace.view.actions.HideAction;
import cmu.varviz.trace.view.actions.RemovePathAction;
import cmu.varviz.trace.view.editparts.TraceEditPartFactory;

/**
 * The varviz view.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizView extends ViewPart {
	
	public static final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	public static final QualifiedName SHOW_LABELS_QN = new QualifiedName(VarvizView.class.getName() + "#showLables", "showLables");
	public static final QualifiedName USE_VAREXJ_QN = new QualifiedName(VarvizView.class.getName() + "#useVarexJ", "useVarexJ");
	public static final QualifiedName REEXECUTE_QN = new QualifiedName(VarvizView.class.getName() + "#REEXECUTE", "REEXECUTE");

	public static TraceGenerator generator = new VarexJGenerator();

	private static ScrollingGraphicalViewer viewer;
	private ScalableFreeformRootEditPart rootEditPart;

	private PrintAction printAction;
	private Action showLablesButton;
	private Action exportGraphVizButton;
	private Action exceptionButton;
	private Action exportAsToolbarIcon;

	public static boolean reExecuteForExceptionFeatures = Boolean.parseBoolean(getProperty(REEXECUTE_QN));
	public static boolean showLables = Boolean.parseBoolean(getProperty(SHOW_LABELS_QN));
	public static boolean useVarexJ = Boolean.parseBoolean(getProperty(USE_VAREXJ_QN));;
	
	public static int projectID = 0;
	public static int minDegree = 2;
	
	private static LayoutManager lm = new LayoutManager();

	public static Trace TRACE = new Trace();
	public static String PROJECT_NAME = "";
	
	// TODO remove
	public static final String PROJECT_Sources = "NanoXML";
	public static final String PROJECT_Sources_Folder = "Sources/Java";
	public static final String PROJECT_Sources_Test_Folder = "Test/Java";
	
	private static final double[] ZOOM_LEVELS;
	static {
		ZOOM_LEVELS = new double[] { .1, .15, .2, .3, .4, .5, .6, .7, .8, .9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2};
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
		
		showLablesButton = new Action() {
			public void run() {
				showLables = !showLables;
				setProperty(SHOW_LABELS_QN, Boolean.toString(showLables));
				TRACE.createEdges();
				TRACE.highlightException();
				refreshVisuals();
			}
		};
		showLablesButton.setChecked(showLables);
		showLablesButton.setToolTipText("Show edge context");
		toolbarManager.add(showLablesButton);
		showLablesButton.setImageDescriptor(VarvizActivator.LABEL_IMAGE_DESCRIPTOR);
		
		exceptionButton = new Action() {
			public void run() {
				reExecuteForExceptionFeatures = !reExecuteForExceptionFeatures;
				setProperty(REEXECUTE_QN, Boolean.toString(reExecuteForExceptionFeatures));
			}
		};
		exceptionButton.setToolTipText("Show Trace for Exception Features Only");
		toolbarManager.add(exceptionButton);
		exceptionButton.setChecked(reExecuteForExceptionFeatures);
		exceptionButton.setImageDescriptor(VarvizActivator.REFESH_EXCEPTION_IMAGE_DESCRIPTOR);

		exportGraphVizButton = new Action() {
			public void run() {
				FileDialog fileDialog = new FileDialog(parent.getShell(), SWT.SAVE);
				fileDialog.setFileName(PROJECT_NAME);
				final String[] extensions = new String[Format.values().length] ;
				for (int i = 0; i < extensions.length; i++) {
					extensions[i] = "*." + Format.values()[i];
				}
				
				fileDialog.setFilterExtensions(extensions);
				String location = fileDialog.open();
				if (location != null) {
					GrapVizExport exporter = new GrapVizExport(location, TRACE);
					exporter.write();
				}
			}
		};
		exportGraphVizButton.setToolTipText("Export with GraphViz");
		toolbarManager.add(exportGraphVizButton);
		exportGraphVizButton.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID, "icons/full/etool16/export_wiz.png"));

		exportAsToolbarIcon = new Action(useVarexJ ? "VarexJ" : "SampleJ", Action.AS_DROP_DOWN_MENU) {
			@Override
			public void run() {
				useVarexJ = !useVarexJ;
				setProperty(USE_VAREXJ_QN, Boolean.toString(useVarexJ));
				setText(useVarexJ ? "VarexJ" : "SampleJ");
			}
		};
		exportAsToolbarIcon.setMenuCreator(new IMenuCreator() {
			Menu fMenu = null;

			@Override
			public Menu getMenu(Menu parent) {
				return null;
			}

			@Override
			public Menu getMenu(Control parent) {
				fMenu = new Menu(parent);
				ActionContributionItem exportImageContributionItem = new ActionContributionItem(new Action("VarexJ") {
					@Override
					public void run() {
						exportAsToolbarIcon.setText(this.getText());
						useVarexJ = true;
						setProperty(USE_VAREXJ_QN, Boolean.toString(useVarexJ));
					}
				});
				exportImageContributionItem.fill(fMenu, -1);
				ActionContributionItem exportXMLContributionItem = new ActionContributionItem(new Action("SampleJ") {
					@Override
					public void run() {
						exportAsToolbarIcon.setText(this.getText());
						useVarexJ = false;
						setProperty(USE_VAREXJ_QN, Boolean.toString(useVarexJ));
					}
				});
				exportXMLContributionItem.fill(fMenu, -1);
				return fMenu;
			}
			
			@Override
			public void dispose() {}

		});
		toolbarManager.add(exportAsToolbarIcon);
		
		
		((ScalableFreeformRootEditPart) viewer.getRootEditPart()).getZoomManager().setZoomLevels(ZOOM_LEVELS);
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
	
	private static String getProperty(QualifiedName qn) {
		try {
			return workspaceRoot.getPersistentProperty(qn);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	private static  void setProperty(QualifiedName qn, String value) {
		try {
			workspaceRoot.setPersistentProperty(qn, value);
		} catch (CoreException e) {
			e.printStackTrace();
		}
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
		menuMgr.add(new RemovePathAction("Remove Path", viewer, this));
	}

	@Override
	public void setFocus() {

	}

	public void refresh() {
		refreshVisuals();
	}

	public static void refreshVisuals() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				viewer.setContents(TRACE);
				viewer.getContents().refresh();
				lm.layout(viewer.getContents());
			}
		});
	}

	public static StatementFilter basefilter = new Or(new StatementFilter() {

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		if (GraphicalViewer.class.equals(adapter) || ViewPart.class.equals(adapter))
			return viewer;
		return super.getAdapter(adapter);
	}

}