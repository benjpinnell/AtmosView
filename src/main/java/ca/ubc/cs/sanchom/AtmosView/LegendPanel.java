package ca.ubc.cs.sanchom.AtmosView;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.LineMetrics;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

public class LegendPanel extends JPanel {

	private static final long serialVersionUID = 6197076998479979532L;

	// Layout
	private final int SYMBOL_X = 15;
	private final int DESCRIPTOR_X = 30;
	private final int DOWN_TRIANGLE_Y = 45;
	private final int UP_TRIANGLE_Y = 65;
	private final int CCL_Y = 85;
	private final int CT_Y = 105;
	private final int STRATUS_Y = 130;
	private final int WIND_100_Y = 165;
	
	private final int RIGHT_SYMBOL_X = 135;
	private final int RIGHT_DESCRIPTOR_X = 150;
	private final int CONVECTIVE_LOW_BOX_Y = 55;
	private final int CONVECTIVE_MID_BOX_Y = 75;
	private final int CONVECTIVE_HIGH_BOX_Y = 95;
	
	private final int INDEX_X = 185;
	private final int INDEX_DESCRIPTOR_X = 200;
	private final int INDEX_LOW_Y = 55;
	private final int INDEX_MID_Y = 75;
	private final int INDEX_HIGH_Y = 95;
	
	private final int TRIANGLE_WIDTH = 6;
	private final int TRIANGLE_HEIGHT = 6;
	private final int BOX_SIZE = 10;
	
	private String title = "Legend";
	private Point2D titleLoc = null;
	
	// Shapes
	private GeneralPath upTriangle = null;
	private GeneralPath downTriangle = null;
	private Line2D underLine = null;
	private Line2D CCL_Line = null;
	private Line2D CT_Line = null;
	private Line2D Wind_Line = null;
	private Ellipse2D Pivot = null;
	private Line2D Stratus_Line = null;
	private Rectangle2D c_low_box = null;
	private Rectangle2D c_mid_box = null;
	private Rectangle2D c_high_box = null;
	
	//TODO: Add the low, mid, and high index markers to the legend
	private Rectangle2D i_low_box = null;
	private Rectangle2D i_mid_box = null;
	private Rectangle2D i_high_box = null;
	
	public void updateShapes()
	{
		titleLoc = new Point2D.Double(getWidth() / 2f, 15);
		underLine = new Line2D.Double(20, 25, getWidth() - 20, 25);
		
		upTriangle = new GeneralPath(GeneralPath.WIND_NON_ZERO);
		
		
		// Mind the int to float conversions
		upTriangle.moveTo(SYMBOL_X, UP_TRIANGLE_Y + TRIANGLE_HEIGHT/2f);
		upTriangle.lineTo(SYMBOL_X - TRIANGLE_WIDTH/2f, UP_TRIANGLE_Y-TRIANGLE_HEIGHT/2f);
		upTriangle.lineTo(SYMBOL_X + TRIANGLE_WIDTH/2f, UP_TRIANGLE_Y-TRIANGLE_HEIGHT/2f);
		upTriangle.lineTo(SYMBOL_X, UP_TRIANGLE_Y + TRIANGLE_WIDTH/2f);
		
		downTriangle = new GeneralPath(GeneralPath.WIND_NON_ZERO);
		
		downTriangle.moveTo(SYMBOL_X, DOWN_TRIANGLE_Y - TRIANGLE_HEIGHT/2f);
		downTriangle.lineTo(SYMBOL_X - TRIANGLE_WIDTH/2f, DOWN_TRIANGLE_Y+TRIANGLE_HEIGHT/2f);
		downTriangle.lineTo(SYMBOL_X + TRIANGLE_WIDTH/2f, DOWN_TRIANGLE_Y+TRIANGLE_HEIGHT/2f);
		downTriangle.lineTo(SYMBOL_X, DOWN_TRIANGLE_Y - TRIANGLE_WIDTH/2f);

		CCL_Line = new Line2D.Double(SYMBOL_X - 5, CCL_Y, SYMBOL_X + 5, CCL_Y);
		CT_Line = new Line2D.Double(SYMBOL_X, CT_Y - 5, SYMBOL_X, CT_Y + 5);
		
		double speed = 100;
		double radians = Math.PI / 4;
		double xDiff = (speed / 5) * Math.cos(radians);
		double yDiff = (speed / 5) * -Math.sin(radians);
		
		Wind_Line = new Line2D.Double(SYMBOL_X, WIND_100_Y, SYMBOL_X + xDiff, WIND_100_Y + yDiff);
		Pivot = new Ellipse2D.Double(SYMBOL_X - BarPanel.PIVOT_SIZE/2f, WIND_100_Y - BarPanel.PIVOT_SIZE/2f, BarPanel.PIVOT_SIZE, BarPanel.PIVOT_SIZE);
		
		Stratus_Line = new Line2D.Double(SYMBOL_X, STRATUS_Y - 10, SYMBOL_X, STRATUS_Y + 10);
		
		c_low_box = new Rectangle2D.Double(RIGHT_SYMBOL_X, CONVECTIVE_LOW_BOX_Y, BOX_SIZE, BOX_SIZE);
		c_mid_box = new Rectangle2D.Double(RIGHT_SYMBOL_X, CONVECTIVE_MID_BOX_Y, BOX_SIZE, BOX_SIZE);
		c_high_box = new Rectangle2D.Double(RIGHT_SYMBOL_X, CONVECTIVE_HIGH_BOX_Y, BOX_SIZE, BOX_SIZE);
	}
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		if (titleLoc != null)
		{
			g2.setFont(new Font("Verdana", Font.BOLD, 10));
			g2.setColor(Color.BLACK);
			Rectangle2D bounds = g2.getFont().getStringBounds(title, g2.getFontRenderContext());
			g2.drawString(title, (float)(titleLoc.getX() - bounds.getWidth() / 2f), (float)(titleLoc.getY() + bounds.getHeight() / 2f));
			g2.draw(underLine);
		}
		
		g2.setFont(new Font("Verdana", Font.PLAIN, 10));
		
		if (upTriangle != null)
		{
			g2.setColor(BarPanel.CUMULUS_LIFTED_COLOUR);
			g2.fill(upTriangle);
			g2.setColor(Color.BLACK);
			LineMetrics l = g2.getFont().getLineMetrics("LCL-LFC", g2.getFontRenderContext());
			g2.drawString("LCL-LFC", DESCRIPTOR_X, UP_TRIANGLE_Y+l.getAscent()/2f);
		}
		
		if (downTriangle != null)
		{
			g2.setColor(BarPanel.FREE_CONVECTION_COLOUR);
			g2.fill(downTriangle);
			g2.setColor(Color.BLACK);
			LineMetrics l = g2.getFont().getLineMetrics("LFC-EQL", g2.getFontRenderContext());
			g2.drawString("LFC-EQL", DESCRIPTOR_X, DOWN_TRIANGLE_Y+l.getAscent()/2f);
		}

		if (CCL_Line != null)
		{
			Stroke orig = g2.getStroke();
			g2.setColor(BarPanel.CCL_COLOUR);
			g2.setStroke(new BasicStroke(3));
			g2.draw(CCL_Line);
			g2.setStroke(orig);
			g2.setColor(Color.BLACK);
			LineMetrics l = g2.getFont().getLineMetrics("CCL", g2.getFontRenderContext());
			g2.drawString("CCL", DESCRIPTOR_X, CCL_Y+l.getAscent()/2f);
		}
		
		if (CCL_Line != null)
		{
			Stroke orig = g2.getStroke();
			g2.setColor(BarPanel.CCL_COLOUR);
			g2.setStroke(new BasicStroke(3));
			g2.draw(CT_Line);
			g2.setStroke(orig);
			g2.setColor(Color.BLACK);
			LineMetrics l = g2.getFont().getLineMetrics("CT", g2.getFontRenderContext());
			g2.drawString("CT Rise", DESCRIPTOR_X, CT_Y+l.getAscent()/2f);
		}
		
		if (Stratus_Line != null)
		{
			Stroke orig = g2.getStroke();
			g2.setColor(BarPanel.STRATUS_COLOUR);
			g2.setStroke(new BasicStroke(5));
			g2.draw(Stratus_Line);
			g2.setStroke(orig);
			g2.setColor(Color.BLACK);
			LineMetrics l = g2.getFont().getLineMetrics("Stratus Cloud", g2.getFontRenderContext());
			g2.drawString("Stratus Cloud", DESCRIPTOR_X, STRATUS_Y+l.getAscent()/2f);
		}
		
		if (Wind_Line != null)
		{
			Stroke orig = g2.getStroke();
			g2.setColor(Color.BLACK);
			g2.draw(Wind_Line);
			g2.fill(Pivot);
			g2.setStroke(orig);
			g2.setColor(Color.BLACK);
			LineMetrics l = g2.getFont().getLineMetrics("Wind Velocity (eg. from 045\u00B0 at 100kt)", g2.getFontRenderContext());
			g2.drawString("Wind Velocity Eg. from 045\u00B0 at 100kt", DESCRIPTOR_X, WIND_100_Y+l.getAscent()/2f);
		}
		
		g2.setFont(new Font("Verdana", Font.BOLD, 10));
		g2.setColor(Color.BLACK);
		g2.drawString("Bar Colours", RIGHT_SYMBOL_X, 45);
		g2.setFont(new Font("Verdana", Font.PLAIN, 10));
		
		if (c_low_box != null)
		{
			g2.setColor(BarPanel.CONVECTIVE_LOW_COLOUR);
			g2.fill(c_low_box);
			g2.setColor(Color.BLACK);
			String descriptor = "Convective Inhibition";
			LineMetrics l = g2.getFont().getLineMetrics(descriptor, g2.getFontRenderContext());
			g2.drawString(descriptor, RIGHT_DESCRIPTOR_X, CONVECTIVE_LOW_BOX_Y+l.getAscent()/2f+5);
		}
		if (c_mid_box != null)
		{
			g2.setColor(BarPanel.CONVECTIVE_MIDDLE_COLOUR);
			g2.fill(c_mid_box);
			g2.setColor(Color.BLACK);
			String descriptor = "Convectively Neutral";
			LineMetrics l = g2.getFont().getLineMetrics(descriptor, g2.getFontRenderContext());
			g2.drawString(descriptor, RIGHT_DESCRIPTOR_X, CONVECTIVE_MID_BOX_Y+l.getAscent()/2f+5);
		}
		if (c_high_box != null)
		{
			g2.setColor(BarPanel.CONVECTIVE_HIGH_COLOUR);
			g2.fill(c_high_box);
			g2.setColor(Color.BLACK);
			String descriptor = "Convective Potential";
			LineMetrics l = g2.getFont().getLineMetrics(descriptor, g2.getFontRenderContext());
			g2.drawString(descriptor, RIGHT_DESCRIPTOR_X, CONVECTIVE_HIGH_BOX_Y+l.getAscent()/2f+5);
		}
		
	}

}
