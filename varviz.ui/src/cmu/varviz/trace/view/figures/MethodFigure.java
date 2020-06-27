package cmu.varviz.trace.view.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import cmu.varviz.VarvizColors;
import cmu.varviz.trace.Method;
import cmu.varviz.trace.MethodElement;
import cmu.varviz.trace.view.VarvizView;

/**
 * The {@link Figure} representing methods in the trace. 
 * 
 * @author Jens Meinicke
 *
 */
public class MethodFigure extends Figure {

	private static final String FONT_NAME = "Consolas";
	private static final Font TEXT_FONT = new Font(null, FONT_NAME, 12, SWT.NORMAL);

	private final Label label = new Label();
	private int height = 20;
	
	private SourceAnchor sourceAnchor;
	private TargetAnchor targetAnchor;

	public MethodFigure(Method method) {
		super();
		this.setLayoutManager(new FreeformLayout());
		if (!isEmptyForwardingMethod(method)) {
			Method parent = method.getParent();
			if (parent != null && isEmptyForwardingMethod(parent)) {
				this.setName(method.toString() + " ...");
			} else {
				this.setName(method.toString());
				Label tooltip = new Label();
				tooltip.setText(method.toString());
				setToolTip(tooltip);
			}
		}
		setBackgroundColor(VarvizColors.WHITE.getColor());
		setBorder(new LineBorder(VarvizColors.BLACK.getColor(), 1));
		this.add(label);
		this.setOpaque(false);
		Label tooltip = new Label();
		tooltip.setText(getToolTipText(method));
		setToolTip(tooltip);
		
		sourceAnchor = new SourceAnchor(this, method);
		targetAnchor = new TargetAnchor(this);
	}
	
	private String getToolTipText(Method method) {
		if (!isEmptyForwardingMethod(method)) {
			Method parent = method.getParent();
			if (parent != null && isEmptyForwardingMethod(parent)) {
				StringBuilder sb = new StringBuilder();
				Method currentMethod = method;
				while (currentMethod != null) {
					sb.append(currentMethod.toString());
					sb.append('\n');
					currentMethod = currentMethod.getParent();
				}
				return sb.substring(0, sb.length() - 1);
			} else {
				return method.toString();
			}
		}
		return "";
	}

	private boolean isEmptyForwardingMethod(Method method) {
		if (VarvizView.getInstance().isHideForwardingMethods()) {
			if (method.getChildren().size() == 1) {
				MethodElement child = method.getChildren().get(0);
				if (child instanceof Method) {
					return true;
				}
				
			}
		}
		return false;
	}

	public int getMinWidth() {
		return label.getPreferredSize().width + 10;
	}

	private void setName(String name) {
		label.setText(name);
		label.setFont(TEXT_FONT);
		Dimension labelSize = label.getPreferredSize();
		label.setLocation(new Point(10, 10));
		if (labelSize.width < 100)
			labelSize.width = 100;

		if (labelSize.equals(label.getSize()))
			return;

		label.setSize(labelSize);

		Rectangle bounds = getBounds();
		int w = 20;

		bounds.setSize(labelSize.expand(w, height));

		Dimension oldSize = getSize();
		if (!oldSize.equals(0, 0)) {
			int dx = (oldSize.width - bounds.width) / 2;
			bounds.x += dx;
		}
		setBounds(bounds);
	}
	

	public SourceAnchor getSourceAnchor() {
		return sourceAnchor;
	}
	
	public TargetAnchor getTargetAnchor() {
		return targetAnchor;
	}


}
