package cmu.varviz;

import javax.annotation.Nullable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.fosd.typechef.featureexpr.FeatureExprFactory;

/**
 * The activator class controls the plug-in life cycle
 */
public class VarvizActivator extends AbstractUIPlugin {
	
	static {
		FeatureExprFactory.setDefault(FeatureExprFactory.bdd());
	}
	
	public static final ImageDescriptor REFESH_TAB_IMAGE_DESCRIPTOR = getImageDescriptor("refresh_tab.gif");
	public static final ImageDescriptor REFESH_EXCEPTION_IMAGE_DESCRIPTOR = getImageDescriptor("refresh_exception.gif");
	
	public static final ImageDescriptor LABEL_IMAGE_DESCRIPTOR = getImageDescriptor("label.gif");
	public static final ImageDescriptor GENERATOR_IMAGE_DESCRIPTOR = getImageDescriptor("debug.gif");
	public static final Image REFESH_TAB_IMAGE = REFESH_TAB_IMAGE_DESCRIPTOR.createImage();

	public static final String PLUGIN_ID = "varviz";
	private static VarvizActivator plugin;
	
	public VarvizActivator() {
		// nothing here
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static VarvizActivator getDefault() {
		return plugin;
	}
	
	public static void log(String message) {
		getDefault().getLog().log(new Status(IStatus.INFO, "VARVIZ", message, new Exception()));
	}

	@Nullable
	public static Image getImage(String name) {
		if (getDefault() != null) {
			return getImageDescriptor(name).createImage();
		}
		return null;
	}
	
	@Nullable
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, "images/" + path);
	}

}
