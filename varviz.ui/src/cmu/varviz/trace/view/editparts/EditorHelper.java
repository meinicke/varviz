package cmu.varviz.trace.view.editparts;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import cmu.varviz.VarvizActivator;
import cmu.varviz.VarvizException;
import cmu.varviz.trace.view.VarvizView;
import gov.nasa.jpf.vm.MethodInfo;

/**
 * Helper class to handle the editor.
 * 
 * @author Jens Meinicke
 *
 */
public class EditorHelper {
	
	private EditorHelper() {
		// nothing here
	}
	
	public static void open(MethodInfo mi, int lineNumber) {
		IFile file = getFile(mi);
		IWorkbenchWindow dw = VarvizActivator.getDefault().getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = dw.getActivePage();
		IEditorPart editor = openEditor(file, page);
		scrollToLine(editor, lineNumber);
	}
	
	public static void open(String fileName, int lineNumber) {
		IFile file = getFile(fileName);
		IWorkbenchWindow dw = VarvizActivator.getDefault().getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = dw.getActivePage();
		IEditorPart editor = openEditor(file, page);
		scrollToLine(editor, lineNumber);
	}
	
	 
	
	private static IFile getFile(MethodInfo mi) {
		return getFile(mi.getSourceFileName());
	}

	private static IFile getFile(String fileName) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(VarvizView.getInstance().getProjectName());
		final IJavaProject javaProject = JavaCore.create(project);
		try {
			IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
			for (int i = 0; i < oldEntries.length; i++) {
				if (oldEntries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IFile file = project.getFile(oldEntries[i].getPath().removeFirstSegments(1).append(fileName));
					if (file.exists()) {
						return file;
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		throw new VarvizException("file " + fileName + " does not exist");
		
		
	}
	
	
	private static IEditorPart openEditor(IFile file, IWorkbenchPage page) {
		IEditorPart editor = null;
		if (page != null) {
			IContentType contentType = null;
			try {
				file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
				
				IContentDescription description = file.getContentDescription();
				if (description != null) {
					contentType = description.getContentType();
				}
				IEditorDescriptor desc = null;
				if (contentType != null) {
					desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName(), contentType);
				} else {
					desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
				}

				if (desc != null) {
					editor = page.openEditor(new FileEditorInput(file), desc.getId());
				} else {
					// case: there is no default editor for the file
					editor = page.openEditor(new FileEditorInput(file), "org.eclipse.ui.DefaultTextEditor");
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return editor;
	}

	public static void scrollToLine(IEditorPart editorPart, int lineNumber) {
		if (!(editorPart instanceof ITextEditor) || lineNumber <= 0) {
			return;
		}
		ITextEditor editor = (ITextEditor) editorPart;
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		if (document != null) {
			IRegion lineInfo = null;
			try {
				lineInfo = document.getLineInformation(lineNumber - 1);
			} catch (BadLocationException e) {
				// ignore
			}
			if (lineInfo != null) {
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			}
		}
	}

}
