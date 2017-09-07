package cmu.varviz.trace.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaApplicationLaunchShortcut;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Implements the "run as" shortcut.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizLaunchShortcut extends JavaApplicationLaunchShortcut {

	@Override
	protected void launch(IType type, String mode) {
		List<ILaunchConfiguration> configs = getConfigurationCandidates(type, getConfigurationType());
		if (configs != null) {
			ILaunchConfiguration config = null;
			int count = configs.size();
			if (count == 1) {
				config = configs.get(0);
			} else if (count > 1) {
				config = chooseConfiguration(configs);
				if (config == null) {
					return;
				}
			}
			if (config == null) {
				config = createConfiguration(type);
			}
			if (config != null) {
				runVarexJ(mode, config);
			}
		}
	}

	private void runVarexJ(final String mode, final ILaunchConfiguration config) {
		Job job = new Job("Run with VarexJ") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					new VarvizConfigurationDelegate().launch(config, mode, null, new NullProgressMonitor());
				} catch (CoreException e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.LONG);
		job.schedule();
	}

	@Override
	protected ILaunchConfigurationType getConfigurationType() {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType("cmu.varviz");
	}

	/**
	 * @see JavaLaunchShortcut
	 */
	private List<ILaunchConfiguration> getConfigurationCandidates(IType type, ILaunchConfigurationType ctype) {
		List<ILaunchConfiguration> candidateConfigs = Collections.emptyList();
		try {
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(ctype);
			candidateConfigs = new ArrayList<>(configs.length);
			for (int i = 0; i < configs.length; i++) {
				ILaunchConfiguration config = configs[i];
				if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "").equals(type.getFullyQualifiedName())) { //$NON-NLS-1$
					if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(type.getJavaProject().getElementName())) { //$NON-NLS-1$
						candidateConfigs.add(config);
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return candidateConfigs;
	}
}
