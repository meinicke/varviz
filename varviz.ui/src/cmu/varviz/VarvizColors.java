package cmu.varviz;

import org.eclipse.swt.graphics.Color;

import cmu.varviz.trace.NodeColor;

/**
 * Colors used by VarViz.
 * 
 * @author Jens Meinicke
 *
 */
public enum VarvizColors {

	BLACK(0, 0, 0), WHITE(255, 255, 255), GRAY2(240, 240, 240), GRAY(150, 150, 150), RED(255, 0, 0), YELLOW(255, 200,
			0), DARKORANGE(255, 160, 0), LIMEGREEN(124, 252, 0), FIREBRICK1(255, 100, 0), TOMATO(255, 100, 0);

	private final Color color;

	private VarvizColors(int red, int green, int blue) {
		color = new Color(null, red, green, blue);
	}

	public Color getColor() {
		return color;
	}

	public static Color getColor(NodeColor c) {
		return getVarvizColor(c).getColor();
	}

	public static VarvizColors getVarvizColor(NodeColor c) {
		if (c == null) {
			return GRAY2;
		}
		switch (c) {
		case black:
			return BLACK;
		case darkorange:
			return DARKORANGE;
		case firebrick1:
			return FIREBRICK1;
		case limegreen:
			return LIMEGREEN;
		case red:
			return RED;
		case tomato:
			return TOMATO;
		case white:
			return WHITE;
		case yellow:
			return YELLOW;
		case gray:
			return GRAY;
		default:
			break;
		}
		return GRAY;
	}
}
