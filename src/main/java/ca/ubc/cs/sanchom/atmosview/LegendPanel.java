package ca.ubc.cs.sanchom.atmosview;

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
  private static final int SYMBOL_X = 15;
  private static final int DESCRIPTOR_X = 30;
  private static final int DOWN_TRIANGLE_Y = 45;
  private static final int UP_TRIANGLE_Y = 65;
  private static final int CCL_Y = 85;
  private static final int CT_Y = 105;
  private static final int STRATUS_Y = 130;
  private static final int WIND_100_Y = 165;

  private static final int RIGHT_SYMBOL_X = 135;
  private static final int RIGHT_DESCRIPTOR_X = 150;
  private static final int CONVECTIVE_LOW_BOX_Y = 55;
  private static final int CONVECTIVE_MID_BOX_Y = 75;
  private static final int CONVECTIVE_HIGH_BOX_Y = 95;

  private static final int INDEX_X = 185;
  private static final int INDEX_DESCRIPTOR_X = 200;
  private static final int INDEX_LOW_Y = 55;
  private static final int INDEX_MID_Y = 75;
  private static final int INDEX_HIGH_Y = 95;

  private static final int TRIANGLE_WIDTH = 6;
  private static final int TRIANGLE_HEIGHT = 6;
  private static final int BOX_SIZE = 10;

  private String title = "Legend";
  private Point2D titleLoc = null;

  // Shapes
  private GeneralPath upTriangle = null;
  private GeneralPath downTriangle = null;
  private Line2D underLine = null;
  private Line2D cclLine = null;
  private Line2D ctLine = null;
  private Line2D windLine = null;
  private Ellipse2D pivot = null;
  private Line2D statusLine = null;
  private Rectangle2D lowBoxC = null;
  private Rectangle2D midBoxC = null;
  private Rectangle2D highBoxC = null;

  // TODO: Add the low, mid, and high index markers to the legend
  // private Rectangle2D lowBoxI = null;
  // private Rectangle2D midBoxI = null;
  // private Rectangle2D highBoxI = null;

  /** Update the shapes. */
  public void updateShapes() {
    titleLoc = new Point2D.Double(getWidth() / 2f, 15);
    underLine = new Line2D.Double(20, 25, getWidth() - 20, 25);

    upTriangle = new GeneralPath(GeneralPath.WIND_NON_ZERO);

    // Mind the int to float conversions
    upTriangle.moveTo(SYMBOL_X, UP_TRIANGLE_Y + TRIANGLE_HEIGHT / 2f);
    upTriangle.lineTo(SYMBOL_X - TRIANGLE_WIDTH / 2f, UP_TRIANGLE_Y - TRIANGLE_HEIGHT / 2f);
    upTriangle.lineTo(SYMBOL_X + TRIANGLE_WIDTH / 2f, UP_TRIANGLE_Y - TRIANGLE_HEIGHT / 2f);
    upTriangle.lineTo(SYMBOL_X, UP_TRIANGLE_Y + TRIANGLE_WIDTH / 2f);

    downTriangle = new GeneralPath(GeneralPath.WIND_NON_ZERO);

    downTriangle.moveTo(SYMBOL_X, DOWN_TRIANGLE_Y - TRIANGLE_HEIGHT / 2f);
    downTriangle.lineTo(SYMBOL_X - TRIANGLE_WIDTH / 2f, DOWN_TRIANGLE_Y + TRIANGLE_HEIGHT / 2f);
    downTriangle.lineTo(SYMBOL_X + TRIANGLE_WIDTH / 2f, DOWN_TRIANGLE_Y + TRIANGLE_HEIGHT / 2f);
    downTriangle.lineTo(SYMBOL_X, DOWN_TRIANGLE_Y - TRIANGLE_WIDTH / 2f);

    cclLine = new Line2D.Double(SYMBOL_X - 5, CCL_Y, SYMBOL_X + 5, CCL_Y);
    ctLine = new Line2D.Double(SYMBOL_X, CT_Y - 5, SYMBOL_X, CT_Y + 5);

    double speed = 100;
    double radians = Math.PI / 4;
    double diffX = (speed / 5) * Math.cos(radians);
    double diffY = (speed / 5) * -Math.sin(radians);

    windLine = new Line2D.Double(SYMBOL_X, WIND_100_Y, SYMBOL_X + diffX, WIND_100_Y + diffY);
    pivot =
        new Ellipse2D.Double(
            SYMBOL_X - BarPanel.PIVOT_SIZE / 2f,
            WIND_100_Y - BarPanel.PIVOT_SIZE / 2f,
            BarPanel.PIVOT_SIZE,
            BarPanel.PIVOT_SIZE);

    statusLine = new Line2D.Double(SYMBOL_X, STRATUS_Y - 10, SYMBOL_X, STRATUS_Y + 10);

    lowBoxC = new Rectangle2D.Double(RIGHT_SYMBOL_X, CONVECTIVE_LOW_BOX_Y, BOX_SIZE, BOX_SIZE);
    midBoxC = new Rectangle2D.Double(RIGHT_SYMBOL_X, CONVECTIVE_MID_BOX_Y, BOX_SIZE, BOX_SIZE);
    highBoxC = new Rectangle2D.Double(RIGHT_SYMBOL_X, CONVECTIVE_HIGH_BOX_Y, BOX_SIZE, BOX_SIZE);
  }

  /** Paint the components. */
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    if (titleLoc != null) {
      g2.setFont(new Font("Verdana", Font.BOLD, 10));
      g2.setColor(Color.BLACK);
      Rectangle2D bounds = g2.getFont().getStringBounds(title, g2.getFontRenderContext());
      g2.drawString(
          title,
          (float) (titleLoc.getX() - bounds.getWidth() / 2f),
          (float) (titleLoc.getY() + bounds.getHeight() / 2f));
      g2.draw(underLine);
    }

    g2.setFont(new Font("Verdana", Font.PLAIN, 10));

    if (upTriangle != null) {
      g2.setColor(BarPanel.CUMULUS_LIFTED_COLOUR);
      g2.fill(upTriangle);
      g2.setColor(Color.BLACK);
      LineMetrics l = g2.getFont().getLineMetrics("LCL-LFC", g2.getFontRenderContext());
      g2.drawString("LCL-LFC", DESCRIPTOR_X, UP_TRIANGLE_Y + l.getAscent() / 2f);
    }

    if (downTriangle != null) {
      g2.setColor(BarPanel.FREE_CONVECTION_COLOUR);
      g2.fill(downTriangle);
      g2.setColor(Color.BLACK);
      LineMetrics l = g2.getFont().getLineMetrics("LFC-EQL", g2.getFontRenderContext());
      g2.drawString("LFC-EQL", DESCRIPTOR_X, DOWN_TRIANGLE_Y + l.getAscent() / 2f);
    }

    if (cclLine != null) {
      Stroke orig = g2.getStroke();
      g2.setColor(BarPanel.CCL_COLOUR);
      g2.setStroke(new BasicStroke(3));
      g2.draw(cclLine);
      g2.setStroke(orig);
      g2.setColor(Color.BLACK);
      LineMetrics l = g2.getFont().getLineMetrics("CCL", g2.getFontRenderContext());
      g2.drawString("CCL", DESCRIPTOR_X, CCL_Y + l.getAscent() / 2f);
    }

    if (cclLine != null) {
      Stroke orig = g2.getStroke();
      g2.setColor(BarPanel.CCL_COLOUR);
      g2.setStroke(new BasicStroke(3));
      g2.draw(ctLine);
      g2.setStroke(orig);
      g2.setColor(Color.BLACK);
      LineMetrics l = g2.getFont().getLineMetrics("CT", g2.getFontRenderContext());
      g2.drawString("CT Rise", DESCRIPTOR_X, CT_Y + l.getAscent() / 2f);
    }

    if (statusLine != null) {
      Stroke orig = g2.getStroke();
      g2.setColor(BarPanel.STRATUS_COLOUR);
      g2.setStroke(new BasicStroke(5));
      g2.draw(statusLine);
      g2.setStroke(orig);
      g2.setColor(Color.BLACK);
      LineMetrics l = g2.getFont().getLineMetrics("Stratus Cloud", g2.getFontRenderContext());
      g2.drawString("Stratus Cloud", DESCRIPTOR_X, STRATUS_Y + l.getAscent() / 2f);
    }

    if (windLine != null) {
      Stroke orig = g2.getStroke();
      g2.setColor(Color.BLACK);
      g2.draw(windLine);
      g2.fill(pivot);
      g2.setStroke(orig);
      g2.setColor(Color.BLACK);
      LineMetrics l =
          g2.getFont()
              .getLineMetrics("Wind Velocity (eg. from 045° at 100kt)", g2.getFontRenderContext());

      g2.drawString(
          "Wind Velocity Eg. from 045° at 100kt", DESCRIPTOR_X, WIND_100_Y + l.getAscent() / 2f);
    }

    g2.setFont(new Font("Verdana", Font.BOLD, 10));
    g2.setColor(Color.BLACK);
    g2.drawString("Bar Colours", RIGHT_SYMBOL_X, 45);
    g2.setFont(new Font("Verdana", Font.PLAIN, 10));

    if (lowBoxC != null) {
      g2.setColor(BarPanel.CONVECTIVE_LOW_COLOUR);
      g2.fill(lowBoxC);
      g2.setColor(Color.BLACK);
      String descriptor = "Convective Inhibition";
      LineMetrics l = g2.getFont().getLineMetrics(descriptor, g2.getFontRenderContext());
      g2.drawString(descriptor, RIGHT_DESCRIPTOR_X, CONVECTIVE_LOW_BOX_Y + l.getAscent() / 2f + 5);
    }
    if (midBoxC != null) {
      g2.setColor(BarPanel.CONVECTIVE_MIDDLE_COLOUR);
      g2.fill(midBoxC);
      g2.setColor(Color.BLACK);
      String descriptor = "Convectively Neutral";
      LineMetrics l = g2.getFont().getLineMetrics(descriptor, g2.getFontRenderContext());
      g2.drawString(descriptor, RIGHT_DESCRIPTOR_X, CONVECTIVE_MID_BOX_Y + l.getAscent() / 2f + 5);
    }
    if (highBoxC != null) {
      g2.setColor(BarPanel.CONVECTIVE_HIGH_COLOUR);
      g2.fill(highBoxC);
      g2.setColor(Color.BLACK);
      String descriptor = "Convective Potential";
      LineMetrics l = g2.getFont().getLineMetrics(descriptor, g2.getFontRenderContext());
      g2.drawString(descriptor, RIGHT_DESCRIPTOR_X, CONVECTIVE_HIGH_BOX_Y + l.getAscent() / 2f + 5);
    }
  }
}
