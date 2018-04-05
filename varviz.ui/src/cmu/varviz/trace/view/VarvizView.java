package cmu.varviz.trace.view;

import java.util.IdentityHashMap;
import java.util.Map;

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
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.filters.Or;
import cmu.varviz.trace.filters.StatementFilter;
import cmu.varviz.trace.generator.SampleJGenerator;
import cmu.varviz.trace.generator.TraceGenerator;
import cmu.varviz.trace.generator.VarexJGenerator;
import cmu.varviz.trace.uitrace.GraphicalStatement;
import cmu.varviz.trace.uitrace.GraphicalTrace;
import cmu.varviz.trace.view.actions.HideAction;
import cmu.varviz.trace.view.actions.RemovePathAction;
import cmu.varviz.trace.view.actions.Slicer;
import cmu.varviz.trace.view.editparts.TraceEditPartFactory;

/**
 * The varviz view.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizView extends ViewPart {
	
	public static final int MIN_INTERACTION_DEGREE = 2;
	
	private static final String SAMPLEJ = "SampleJ";
	private static final String VAREXJ = "VarexJ";
	private static final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	private static final QualifiedName SHOW_LABELS_QN = new QualifiedName(VarvizView.class.getName() + "#showLables","showLables");
	private static final QualifiedName USE_VAREXJ_QN = new QualifiedName(VarvizView.class.getName() + "#useVarexJ", "useVarexJ");
	private static final QualifiedName REEXECUTE_QN = new QualifiedName(VarvizView.class.getName() + "#REEXECUTE", "REEXECUTE");
	
	private static final LayoutManager LAYOUT_MANAGER = new LayoutManager();

	
	public static final Map<Method, Boolean> checked = new IdentityHashMap<>();
	public static final StatementFilter basefilter = new Or(new StatementFilter() {

		@Override
		public boolean filter(Statement s) {
			return !(hasParent(s.getParent(), "java."));
		}

		private boolean hasParent(Method parent, String filter) {
			Boolean isChecked = checked.get(parent);
			if (isChecked != null) {
				return isChecked;
			}
			if (parent.toString().contains(filter)) {
				checked.put(parent, true);
				return true;
			}
			parent = parent.getParent();
			if (parent != null) {
				boolean result = hasParent(parent, filter);
				checked.put(parent, result);
				return result;
			}
			return false;
		}
	});
	
	private String projectName = "";

	private boolean showForExceptionFeatures = Boolean.parseBoolean(getProperty(REEXECUTE_QN));
	private boolean showLables = Boolean.parseBoolean(getProperty(SHOW_LABELS_QN));

	private ScrollingGraphicalViewer viewer;

	private boolean useVarexJ = Boolean.parseBoolean(getProperty(USE_VAREXJ_QN));
	private TraceGenerator generator = useVarexJ ? VarexJGenerator.geGenerator() : SampleJGenerator.geGenerator();

	private Trace trace = new Trace();
	private GraphicalTrace graphicalTrace = null;
	
	// TODO dirty solutions for VarvizConfigurationDelegate (remove if possible)
	private static VarvizView INSTANCE = null;
	
	public static VarvizView getInstance() {
		return INSTANCE;
	}
	
	public VarvizView() {
		INSTANCE = this;
	}
	
	public GraphicalTrace getGraphicalTrace() {
		return graphicalTrace;
	}
	
	public GraphicalStatement getGraphicalStatement(MethodElement element) {
		return graphicalTrace.getGraphicalStatement((Statement) element);
	}
	
	public TraceGenerator getGenerator() {
		return generator;
	}
	
	public boolean isShowForExceptionFeatures() {
		return showForExceptionFeatures;
	}
	
	public boolean isShowLables() {
		return showLables;
	}
	
	public boolean isUseVarexJ() {
		return useVarexJ;
	}
	
	public Trace getTRACE() {
		return trace;
	}
	
	public ScrollingGraphicalViewer getViewer() {
		return viewer;
	}
	
	public void setTrace(Trace trace) {
		this.trace = trace;
		graphicalTrace = new GraphicalTrace(trace);
	}
	
	public String getProjectName() {
		return projectName;
	}
	
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	

	private static final double[] ZOOM_LEVELS;
	static {
		ZOOM_LEVELS = new double[] { .1, .15, .2, .3, .4, .5, .6, .7, .8, .9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2 };
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new ScrollingGraphicalViewer();
		viewer.createControl(parent);
		viewer.setEditDomain(new EditDomain());
		viewer.setEditPartFactory(new TraceEditPartFactory(this));
		

		final ScalableFreeformRootEditPart rootEditPart = new ScalableFreeformRootEditPart();
		((ConnectionLayer) rootEditPart.getLayer(LayerConstants.CONNECTION_LAYER)).setAntialias(SWT.ON);
		viewer.setRootEditPart(rootEditPart);
		refreshVisuals();

		Action printAction = new PrintAction(this);

		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.PRINT.getId(), printAction);

		IToolBarManager toolbarManager = bars.getToolBarManager();

		toolbarManager.add(new SearchBar(this));
		createShowLabelsButton(toolbarManager);
		createExceptionButton(toolbarManager);
		createGeneratorButton(toolbarManager);

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

	private void createGeneratorButton(IToolBarManager toolbarManager) {
		Action generatorAction = new Action(useVarexJ ? VAREXJ : SAMPLEJ, Action.AS_DROP_DOWN_MENU) {
			@Override
			public void run() {
				useVarexJ = !useVarexJ;
				setProperty(USE_VAREXJ_QN, Boolean.toString(useVarexJ));
				setText(useVarexJ ? VAREXJ : SAMPLEJ);
				if (useVarexJ) {
					generator = VarexJGenerator.geGenerator();
				} else  {
					generator = SampleJGenerator.geGenerator();
				}
			}
		};
		generatorAction.setMenuCreator(new IMenuCreator() {
			Menu fMenu = null;

			@Override
			public Menu getMenu(Menu parent) {
				return null;
			}

			@Override
			public Menu getMenu(Control parent) {
				fMenu = new Menu(parent);
				ActionContributionItem exportImageContributionItem = new ActionContributionItem(new Action(VAREXJ) {
					@Override
					public void run() {
						generatorAction.setText(this.getText());
						useVarexJ = true;
						setProperty(USE_VAREXJ_QN, Boolean.toString(useVarexJ));
					}
				});
				exportImageContributionItem.fill(fMenu, -1);
				ActionContributionItem exportXMLContributionItem = new ActionContributionItem(new Action(SAMPLEJ) {
					@Override
					public void run() {
						generatorAction.setText(this.getText());
						useVarexJ = false;
						setProperty(USE_VAREXJ_QN, Boolean.toString(useVarexJ));
					}
				});
				exportXMLContributionItem.fill(fMenu, -1);
				return fMenu;
			}

			@Override
			public void dispose() {
				// nothing here
			}

		});
		toolbarManager.add(generatorAction);
	}

	@SuppressWarnings("unused")
	private void createExportButton(Composite parent, IToolBarManager toolbarManager) {
		Action exportGraphVizButton = new Action() {
			@Override
			public void run() {
				FileDialog fileDialog = new FileDialog(parent.getShell(), SWT.SAVE);
				fileDialog.setFileName(projectName);
				final String[] extensions = new String[Format.values().length];
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
		exportGraphVizButton.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID,
				"icons/full/etool16/export_wiz.png"));
	}

	private void createExceptionButton(IToolBarManager toolbarManager) {
		Action exceptionButton = new Action() {
			@Override
			public void run() {
				showForExceptionFeatures = !showForExceptionFeatures;
				setProperty(REEXECUTE_QN, Boolean.toString(showForExceptionFeatures));
				
				if (showForExceptionFeatures) {
					int originalSize = trace.getMain().size();
					Slicer.sliceForExceptiuon(trace, generator);
					trace.finalizeGraph();
					int slicedSize = trace.getMain().size();
					if (originalSize > slicedSize && slicedSize < 10_000) {
						refreshVisuals();
					}

				}
			}
		};
		exceptionButton.setToolTipText("Show Trace for Exception Features Only");
		toolbarManager.add(exceptionButton);
		exceptionButton.setChecked(showForExceptionFeatures);
		exceptionButton.setImageDescriptor(VarvizActivator.REFESH_EXCEPTION_IMAGE_DESCRIPTOR);
	}

	private void createShowLabelsButton(IToolBarManager toolbarManager) {
		Action showLablesButton = new Action() {
			@Override
			public void run() {
				showLables = !showLables;
				setProperty(SHOW_LABELS_QN, Boolean.toString(showLables));
				graphicalTrace.refreshGraphicalEdges();
			}
		};
		showLablesButton.setChecked(showLables);
		showLablesButton.setToolTipText("Show edge context");
		toolbarManager.add(showLablesButton);
		showLablesButton.setImageDescriptor(VarvizActivator.LABEL_IMAGE_DESCRIPTOR);
	}

	private static String getProperty(QualifiedName qn) {
		try {
			return workspaceRoot.getPersistentProperty(qn);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return "";
	}

	private static void setProperty(QualifiedName qn, String value) {
		try {
			workspaceRoot.setPersistentProperty(qn, value);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void createContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(this::fillContextMenu);
		Control control = viewer.getControl();
		Menu menu = menuMgr.createContextMenu(control);
		control.setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void fillContextMenu(IMenuManager menuMgr) {
		menuMgr.add(new HideAction("Hide Element", this));
		menuMgr.add(new RemovePathAction("Remove Path", this));
	}

	@Override
	public void setFocus() {
		// nothing here
	}

	public void refreshVisuals() {
		Display.getDefault().syncExec(() -> {
			viewer.setContents(trace);
			viewer.getContents().refresh();
			LAYOUT_MANAGER.layout(viewer.getContents());

			VarvizViewerUtils.refocusView(viewer, trace);
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object getAdapter(Class adapter) {
		if (GraphicalViewer.class.equals(adapter) || ViewPart.class.equals(adapter))
			return viewer;
		return super.getAdapter(adapter);
	}

}