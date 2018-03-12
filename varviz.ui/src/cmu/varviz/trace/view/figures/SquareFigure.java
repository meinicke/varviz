package cmu.varviz.trace.view.figures;

import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import cmu.varviz.VarvizConstants;
import cmu.varviz.trace.NodeColor;
import cmu.varviz.trace.Statement;

/**
 * TODO description
 * 
 * @author Jens Meinicke
 *
 */
public class SquareFigure extends RectangleFigure {

	private static final String FONT_NAME = "Consolas";
	private static final Font TEXT_FONT = new Font(null, FONT_NAME, 12, SWT.NORMAL);
	
	private final Label label = new Label();
	private SourceAnchor sourceAnchor;
	private TargetAnchor targetAnchor;
	private static final int BORDER_MARGIN = 10;
	private static final int MIN_WIDTH = 20;
	private static final int BORDER_WIDTH = 2;
	
	public SquareFigure(Statement<?> statement) {
		super();
		this.setLayoutManager(new FreeformLayout());
		setName(statement.toString());
		NodeColor color = statement.getColor();
		setBackgroundColor(VarvizConstants.getColor(color));
		setBorder(new LineBorder(VarvizConstants.BLACK, BORDER_WIDTH));
		
		this.add(label);
		this.setOpaque(true);
		
		sourceAnchor = new SourceAnchor(this, statement);
		targetAnchor = new TargetAnchor(this, statement);
		
	}

	private void setName(String name){
		label.setText(name);
		label.setFont(TEXT_FONT);
		Dimension labelSize = label.getPreferredSize();
		
		if (labelSize.width < MIN_WIDTH)
			labelSize.width = MIN_WIDTH;
				
		label.setSize(labelSize);

		Rectangle bounds = getBounds();
		Dimension expand = labelSize.expand(BORDER_MARGIN, BORDER_MARGIN);
		int sideLength = Math.max(expand.height, expand.width);
		bounds.setSize(sideLength, sideLength);
		setBounds(bounds);
		label.setLocation(new Point(sideLength / 2 - label.getSize().width / 2, sideLength / 2 - label.getSize().height / 2));
	}

	public SourceAnchor getSourceAnchor() {
		return sourceAnchor;
	}

	public TargetAnchor getTargetAnchor() {
		return targetAnchor;
	}
}
