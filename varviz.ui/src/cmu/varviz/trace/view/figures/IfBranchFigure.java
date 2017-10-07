package cmu.varviz.trace.view.figures;

import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import cmu.varviz.VarvizConstants;
import cmu.varviz.trace.IFStatement;
import cmu.varviz.trace.NodeColor;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.view.VarvizView;
import cmu.varviz.trace.view.editparts.EditPartUtils;

/**
 * TODO description
 * 
 * @author Jens Meinicke
 *
 */
public class IfBranchFigure extends Shape {

	private static final String FONT_NAME = "Consolas";
	private static final Font TEXT_FONT = new Font(null, FONT_NAME, 12, SWT.NORMAL);
	
	protected PointList diamond = new PointList(4);
	
	private Statement<?> statement;
	private final Label label = new Label();
	private SourceAnchor sourceAnchor;
	private TargetAnchor targetAnchor;
	private static final int BORDER_MARGIN = 10;
	private static final int MIN_WIDTH = 20;
	
	public IfBranchFigure(Statement<?> statement) {
		super();
		this.statement = statement;
		this.setLayoutManager(new FreeformLayout());
		if (VarvizView.useVarexJ) {
			setName(EditPartUtils.getContext(((IFStatement<?>)statement).getTargetContext()));
		} else {
			setName("if");
		}
		NodeColor color = statement.getColor();
		setBackgroundColor(VarvizConstants.getColor(color));
		
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
		bounds.setSize(labelSize.expand(BORDER_MARGIN * 4, BORDER_MARGIN * 4));
		setBounds(bounds);
		label.setLocation(new Point(BORDER_MARGIN * 2, BORDER_MARGIN * 2));
	}

	public SourceAnchor getSourceAnchor() {
		return sourceAnchor;
	}

	public TargetAnchor getTargetAnchor() {
		return targetAnchor;
	}
	
	@Override
	protected void fillShape(Graphics g) {
		g.fillPolygon(diamond);
	}
	
	@Override
	protected void outlineShape(Graphics g) {
		g.setLineWidth(statement.getWidth());
		g.drawPolygon(diamond);
	}

	@Override
	public void validate() {
		Rectangle r = getBounds().getCopy();
		r.crop(getInsets());
		r.resize(-1, -1);
		diamond.removeAllPoints();
		diamond.addPoint(r.getTop());
		diamond.addPoint(r.getRight());
		diamond.addPoint(r.getBottom());
		diamond.addPoint(r.getLeft());
	}

	@Override
	public void primTranslate(int x, int y) {
		super.primTranslate(x, y);
		diamond.translate(x, y);
	}

}
