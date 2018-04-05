/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package cmu.varviz.trace.view.editparts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import cmu.varviz.VarvizColors;
import cmu.varviz.VarvizException;
import cmu.varviz.trace.Edge;
import cmu.varviz.trace.Shape;
import cmu.varviz.trace.Statement;
import cmu.varviz.trace.Trace;
import cmu.varviz.trace.uitrace.GraphicalStatement;
import cmu.varviz.trace.uitrace.GraphicalTrace;
import cmu.varviz.trace.uitrace.VarvizEvent;
import cmu.varviz.trace.view.figures.IfBranchFigure;
import cmu.varviz.trace.view.figures.SquareFigure;
import cmu.varviz.trace.view.figures.StatementFigure;

/**
 * The {@link EditPart} representing any kind of statements.
 * @see {@link StatementFigure}, {@link IfBranchFigure}, {@link SquareFigure}
 * 
 * @author Jens Meinicke
 */
public class StatementEditPart extends AbstractTraceEditPart implements NodeEditPart {

	private ConnectionAnchor sourceAnchor = null;
	private ConnectionAnchor targetAnchor = null;
	private final Trace trace;
	private final GraphicalTrace graphicalTrace;
	
	public StatementEditPart(Statement statement, Trace trace, GraphicalTrace graphicalTrace) {
		super();
		this.trace = trace;
		this.graphicalTrace = graphicalTrace;
		setModel(statement);
	}

	@Override
	protected IFigure createFigure() {
		Statement model = getStatementModel();
		final Shape shape = model.getShape();
		if (shape == null) {
			StatementFigure statementFigure = new StatementFigure(model);
			sourceAnchor = statementFigure.getSourceAnchor();
			targetAnchor = statementFigure.getTargetAnchor();
			statementFigure.setAntialias(SWT.ON);
			return statementFigure;
		}
		
		switch (shape) {
		case Mdiamond:
			IfBranchFigure ifBranchFigure = new IfBranchFigure(model);
			sourceAnchor = ifBranchFigure.getSourceAnchor();
			targetAnchor = ifBranchFigure.getTargetAnchor();
			ifBranchFigure.setAntialias(SWT.ON);
			return ifBranchFigure;
		case Msquare:
			SquareFigure boxFigure = new SquareFigure(model);
			boxFigure.setAntialias(SWT.ON);
			sourceAnchor = boxFigure.getSourceAnchor();
			targetAnchor = boxFigure.getTargetAnchor();
			return boxFigure;
		default:
			throw new VarvizException("shape not supported: " + shape);
		}
		
	}

	List<Edge> connections;

	@Override
	protected List<Edge> getModelTargetConnections() {
		if (connections == null) {
			connections = new ArrayList<>();
			for (Edge edge : trace.getEdges()) {
				if (edge.getTo() == getModel()) {
					connections.add(edge);
				}
			}
		}
		return connections;
	}

	@Override
	public void layout() {
		// nothing here
	}
	
	public Statement getStatementModel() {
		return (Statement) getModel();
	}
	
	@Override
	public void activate() {
		Statement statementModel = getStatementModel();
		if (graphicalTrace != null) {
			GraphicalStatement graphicalStatement = graphicalTrace.getGraphicalStatement(statementModel);
			if (graphicalStatement != null) {
				graphicalStatement.registerUIObject(this);
			}
		}
		getFigure().setVisible(true);
		super.activate();
	}

	@Override
	public void deactivate() {
		super.deactivate();
		getFigure().setVisible(false);		
		getModelTargetConnections().forEach(edge ->	((EdgeEditPart) getViewer().getEditPartRegistry().get(edge)).deactivate());
	}
	
	@Override
	public void performRequest(Request request) {
		if ("open".equals(request.getType())) {
			final Statement statement = getStatementModel();
			String file = statement.getParent().getFile();
			EditorHelper.open(file, statement.getLineNumber());
		}
		super.performRequest(request);
	}
	
	@Override
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart arg0) {
		return sourceAnchor;
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request arg0) {
		return sourceAnchor;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart arg0) {
		return targetAnchor;
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request arg0) {
		return targetAnchor;
	}
	
	@Override
	public void propertyChange(VarvizEvent event) {
		switch (event.getType()) {
		case COLOR_CHANGED:
			Statement statement = (Statement)getModel();
			Color color = VarvizColors.getColor(statement.getColor());
			figure.setBackgroundColor(color);
			break;
		case BORDER_CHANGED:
			refreshVisuals();
			break;
		case LABEL_CHANGED:
		case LOCATION_CHANGED:
		default:
			throw new VarvizException("Event " + event.getType() + " not supported for " + getClass());
		}
	}
}
