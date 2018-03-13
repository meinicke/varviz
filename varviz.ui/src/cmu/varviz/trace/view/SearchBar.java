package cmu.varviz.trace.view;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import cmu.samplej.statement.IFStatement2;
import cmu.varviz.trace.IFStatement;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.NodeColor;
import cmu.vatrace.ReturnStatement;

/**
 * Handles the search in the {@link VarvizView}.
 * 
 * @author Jens Meinicke
 *
 */
public class SearchBar extends ControlContribution {

	private static final String DEFAULT_SEARCH_ENTRY = "Search                         ";

	protected SearchBar() {
		super("searchBar");
	}

	@Override
	protected Control createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.SINGLE);
		GridLayout compositeLayout = new GridLayout(2, false);
		compositeLayout.marginTop = -1;
		compositeLayout.marginBottom = 0;
		compositeLayout.marginLeft = 5;
		compositeLayout.marginWidth = 0;
		composite.setLayout(compositeLayout);

		final Text text = new Text(composite, SWT.BORDER | SWT.SINGLE);
		text.setText(DEFAULT_SEARCH_ENTRY);
		text.setTextLimit(100);
		
		text.addKeyListener(new KeyListener() {
			
			@Override
			public void keyReleased(KeyEvent e) {
				// TODO implement scrolling to next entry
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				// nothing here
			}
		});
		
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				final String value = text.getText();
				final Method<?> main = VarvizView.getTRACE().getMain();
				if (value.trim().isEmpty() || value.equals(DEFAULT_SEARCH_ENTRY)) {
					resetAllStatements(main);
				} else {
					highLightStatements(main, value.trim());
				}
			}

			private void resetAllStatements(Method<?> method) {
				for (final MethodElement<?> child : method.getChildren()) {
					if (child instanceof Method) {
						resetAllStatements((Method<?>) child);
						continue;
					}
					
					resetElement(child, true);
				}
			}

			private void highLightStatements(final Method<?> method, final String value) {
				for (final MethodElement<?> child : method.getChildren()) {
					if (child instanceof Method) {
						highLightStatements((Method<?>) child, value);
						continue;
					}
					
					final String name = child.toString();
					if (!unmodifiableStatement(child) && matchesValue(value, name)) {
						highlightElement(child);
					} else {
						resetElement(child, false);
					}
				}
			}

			private boolean matchesValue(final String value, final String name) {
				final String varName = name.substring(name.indexOf(' ') + 1);
				return varName.toLowerCase().contains(value.toLowerCase());
			}

			private void resetElement(MethodElement<?> element, boolean original) {
				if (original) {
					VarvizView.getGraphicalStatement(element).resetColor();
				} else {
					setColor(element, NodeColor.white);
				}
			}

			private boolean unmodifiableStatement(MethodElement<?> element) {
				return element instanceof ReturnStatement || 
					   element instanceof cmu.samplej.statement.ReturnStatement ||
					   element instanceof IFStatement || 
					   element instanceof IFStatement2;
			}

			private void highlightElement(MethodElement<?> element) {
				if (element instanceof ReturnStatement || element instanceof cmu.samplej.statement.ReturnStatement) {
					return;
				}
				setColor(element, NodeColor.red);
			}

			private void setColor(MethodElement<?> element, NodeColor red) {
				VarvizView.getGraphicalStatement(element).setColor(red);
			}
			
		});
		text.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				if (text.getText().trim().isEmpty()) {
					text.setText(DEFAULT_SEARCH_ENTRY);
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				if (DEFAULT_SEARCH_ENTRY.equals(text.getText())) {
					text.setText("");
				}
				text.setText(text.getText().trim());
			}
		});
		text.addListener(SWT.Paint, e -> {
			if (DEFAULT_SEARCH_ENTRY.equals(text.getText())) {
				GC gc = e.gc;
				Display display = Display.getCurrent();
		        gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
				gc.fillRectangle(text.getBounds());
				gc.drawText(text.getText(), 3, 2);
			}
		});

		return composite;
	}

}