package ca.ubc.cs.sanchom.atmosview;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ListIterator;
import java.util.Vector;
import javax.swing.JPanel;

/**
 * A display widget for SoundingData plots.
 *
 * @author Sancho McCann
 */
public class SoundingPanel extends JPanel {

  // The data that is plotted on this panel.
  private SoundingData data = null;
  private DerivedData derivedData = null;

  private static final Color AXIS_COLOUR = Color.BLACK;
  private static final Color DEW_COLOUR = new Color(0.018f, 0.4072f, 0f);
  private static final Color TEMP_COLOUR = new Color(0f, 0.4077f, 0.8385f);

  private static final int MIN_TEMP = -100;
  private static final int MAX_TEMP = 45;
  private static final int TOTAL_TEMP_RANGE = MAX_TEMP - MIN_TEMP;

  private static final int MAX_ALTITUDE_IN_METERS = 15000; // metres metres

  private static final int X_TICK_STEP = 20;
  private static final int Y_TICK_STEP = 1000;

  private static final int TOP_MARGIN = 50;
  private static final int BOTTOM_MARGIN = 50;
  private static final int TOTAL_VERTICAL_MARGIN = TOP_MARGIN + BOTTOM_MARGIN;

  private static final int LEFT_MARGIN = 100;
  private static final int RIGHT_MARGIN = 50;
  private static final int TOTAL_HORIZONTAL_MARGIN = LEFT_MARGIN + RIGHT_MARGIN;

  private static final int TICK_SIZE = 5;

  private Point2D labelLocX = null;
  private Point2D labelLocY = null;

  private Point2D titleLoc = null;

  // private Line2D axisX = null;
  // private Line2D axisY = null;
  private Line2D.Double axisX = null;
  private Line2D.Double axisY = null;
  private Vector<Line2D> ticksX = null;
  private Vector<Line2D> ticksY = null;

  private GeneralPath tempPath = null;
  private GeneralPath dewPath = null;
  private GeneralPath parcelPath = null;

  private String title = null;

  private static final long serialVersionUID = 1L;

  /**
   * Provides a reference to the original sounding data. The reference will be copied for use by
   * this widget. It is a reference so that modifications due to drawing can be used to update other
   * widgets displaying the data in other forms.
   *
   * @param data the SoundingData
   */
  public void linkSoundingData(SoundingData data) {
    this.data = data;
    this.derivedData = new DerivedData(data);
    updateShapes();
  }

  /** Triggers an update of the shape objects. */
  public void updateShapes() {
    Dimension panelSize = getSize();
    Point2D translatedOrigin =
        new Point2D.Double(panelSize.width * 0.66f, panelSize.height - BOTTOM_MARGIN);

    // This transform is only for the drawing, not zoom and pan.
    // This allows quick change from temp-height space to canvas space.

    // Helpful visual reference for affine transforms:
    // https://en.wikipedia.org/wiki/Affine_transformation#/media/File:2D_affine_transformation_matrix.svg
    //
    //  Scale about origin
    //       ^                       Affine Transform Matrix
    //       |                       W 0 0
    //       |      (W,H)            0 H 0
    //  (0,H)|-----|                 0 0 1
    //      1|--   |
    //       |_|___|___________>     NOTE: (W,0) => W    (0,H) => 0
    //  (0,0)  1    (W,0)                           0             H
    //
    //  Translate
    //       ^                       Affine Transform Matrix
    //       |                       1 0 X
    //       |  |-----|              0 1 Y
    //      1|--|--|  |              0 0 1
    //       |  |__|__|
    //       |(X,Y)|___________>
    //      0      1
    //
    double affineW = (panelSize.width - TOTAL_HORIZONTAL_MARGIN) / (double) TOTAL_TEMP_RANGE;
    double affineH = -(panelSize.height - TOTAL_VERTICAL_MARGIN) / (double) MAX_ALTITUDE_IN_METERS;
    double affineX = translatedOrigin.getX();
    double affineY = translatedOrigin.getY();
    AffineTransform tx = new AffineTransform(affineW, 0, 0, affineH, affineX, affineY);
    // AffineTransform tx =
    // new AffineTransform(
    // (panelSize.width - TOTAL_HORIZONTAL_MARGIN) / (float) TOTAL_TEMP_RANGE,
    // 0,
    // 0,
    // -(panelSize.height - BOTTOM_MARGIN - TOP_MARGIN) / (float) MAX_ALTITUDE_IN_METERS,
    // translatedOrigin.getX(),
    // translatedOrigin.getY());

    axisY =
        new Line2D.Double(
            tx.transform(new Point2D.Double(MIN_TEMP, 0), null),
            tx.transform(new Point2D.Double(MIN_TEMP, MAX_ALTITUDE_IN_METERS), null));
    axisX =
        new Line2D.Double(
            tx.transform(new Point2D.Double(MIN_TEMP, 0), null),
            tx.transform(new Point2D.Double(MAX_TEMP, 0), null));

    ticksY = new Vector<Line2D>();
    ticksX = new Vector<Line2D>();
    for (int j = Y_TICK_STEP; j <= MAX_ALTITUDE_IN_METERS; j += Y_TICK_STEP) {
      Point2D loc = new Point2D.Double();

      tx.transform(new Point2D.Double(MIN_TEMP, j), loc);

      ticksY.add(
          new Line2D.Double(
              loc.getX() - TICK_SIZE / 2f, loc.getY(), loc.getX() + TICK_SIZE / 2f, loc.getY()));
    }

    for (int j = MIN_TEMP; j <= MAX_TEMP; j += X_TICK_STEP) {
      Point2D loc = new Point2D.Double();

      tx.transform(new Point2D.Double(j, 0), loc);

      ticksX.add(
          new Line2D.Double(
              loc.getX(), loc.getY() + TICK_SIZE / 2f, loc.getX(), loc.getY() - TICK_SIZE / 2f));
    }

    labelLocX =
        new Point2D.Double(
            tx.transform(new Point2D.Double((MAX_TEMP + MIN_TEMP) / 2f, 0), null).getX(),
            panelSize.height - 15 / 2f);

    labelLocY =
        new Point2D.Double(
            20, tx.transform(new Point2D.Double(0, MAX_ALTITUDE_IN_METERS / 2), null).getY());

    if (this.data != null) {
      title = this.data.getStationName();

      parcelPath = new GeneralPath();
      tempPath = new GeneralPath();
      dewPath = new GeneralPath();

      ListIterator i = this.data.listIterator();
      int n = 0;
      while (i.hasNext()) {
        SoundingPoint sp = (SoundingPoint) (i.next());

        if (sp.getMetres() < MAX_ALTITUDE_IN_METERS) {
          double valY = sp.getMetres();
          double temp = sp.getTemperature();
          double dew = sp.getDewpoint();

          Point2D transformedTemp = new Point2D.Double();
          Point2D transformedDewpoint = new Point2D.Double();
          tx.transform(new Point2D.Double(temp, valY), transformedTemp);
          tx.transform(new Point2D.Double(dew, valY), transformedDewpoint);
          if (n == 0) {
            tempPath.moveTo((float) transformedTemp.getX(), (float) transformedTemp.getY());
            dewPath.moveTo((float) transformedDewpoint.getX(), (float) transformedDewpoint.getY());
          } else {
            tempPath.lineTo((float) transformedTemp.getX(), (float) transformedTemp.getY());
            dewPath.lineTo((float) transformedDewpoint.getX(), (float) transformedDewpoint.getY());
          }
          n++;
        }
      }

      ListIterator j = this.derivedData.getList().listIterator();
      boolean first = true;
      while (j.hasNext()) {
        DerivedPoint dp = (DerivedPoint) (j.next());
        double parcelTemp = dp.getLiftedParcelTemp();

        if (dp.getSampleHeight() < MAX_ALTITUDE_IN_METERS && parcelTemp >= MIN_TEMP) {
          double valY = dp.getSampleHeight();

          Point2D transformedTemp = new Point2D.Double();
          tx.transform(new Point2D.Double(parcelTemp, valY), transformedTemp);
          if (first) {
            parcelPath.moveTo((float) transformedTemp.getX(), (float) transformedTemp.getY());
            first = false;
          } else {
            parcelPath.lineTo((float) transformedTemp.getX(), (float) transformedTemp.getY());
          }
        }
      }
    }
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    g2.setFont(new Font("Verdana", Font.PLAIN, Math.min(getWidth() / 30, 12)));
    titleLoc = new Point2D.Double(getWidth() / 2, 25);

    // TODO: Use canvas transformations to allow pan and zoom
    g2.setColor(AXIS_COLOUR);
    if (axisY != null && axisX != null) {
      g2.draw(axisY);
      g2.draw(axisX);
    }

    if (title != null) {
      g2.setColor(AXIS_COLOUR);
      Rectangle2D bounds = g2.getFont().getStringBounds(title, g2.getFontRenderContext());
      g2.drawString(title, (int) (titleLoc.getX() - bounds.getWidth() / 2f), (int) titleLoc.getY());
    }

    if (tempPath != null) {
      g2.setColor(TEMP_COLOUR);
      g2.draw(tempPath);
    }
    if (dewPath != null) {
      g2.setColor(DEW_COLOUR);
      g2.draw(dewPath);
    }
    if (parcelPath != null) {
      g2.setColor(
          new Color(
              Color.RED.getRed() / 255f,
              Color.red.getGreen() / 255f,
              Color.red.getBlue() / 255f,
              0.3f));
      g2.draw(parcelPath);
    }

    if (ticksY != null) {
      g2.setColor(AXIS_COLOUR);
      for (int i = 0; i < ticksY.size(); i++) {
        g2.draw(ticksY.get(i));
        String tickLabel = new String("" + (i + 1) * Y_TICK_STEP);
        Rectangle2D bounds = g2.getFont().getStringBounds(tickLabel, g2.getFontRenderContext());

        if ((i + 1) % 5 == 0) {
          g2.drawString(
              tickLabel,
              (int) (ticksY.get(i).getP1().getX() - (bounds.getWidth() + 5)),
              (int) (ticksY.get(i).getP1().getY() + bounds.getHeight() / 2));
        }
      }

      TextLayout layout = new TextLayout("Altitude (m)", g2.getFont(), g2.getFontRenderContext());

      final AffineTransform orig = g2.getTransform();

      g2.translate(labelLocY.getX(), labelLocY.getY());
      g2.rotate(-Math.PI / 2);
      layout.draw(g2, -layout.getAdvance() / 2, 0);

      g2.setTransform(orig);
    }

    if (ticksX != null) {
      g2.setColor(AXIS_COLOUR);
      for (int i = 0; i < ticksX.size(); i++) {
        g2.draw(ticksX.get(i));
        String tickLabel = new String("" + (MIN_TEMP + i * X_TICK_STEP));
        Rectangle2D bounds = g2.getFont().getStringBounds(tickLabel, g2.getFontRenderContext());

        g2.drawString(
            tickLabel,
            (int) (ticksX.get(i).getP1().getX() - bounds.getWidth() / 2.0),
            (int) (ticksX.get(i).getP1().getY() + bounds.getHeight() + 5));
      }

      String labelX = new String("Temperature and Dewpoint (°C)");
      Rectangle2D bounds = g2.getFont().getStringBounds(labelX, g2.getFontRenderContext());
      g2.drawString(
          labelX, (int) (labelLocX.getX() - bounds.getWidth() / 2f), (int) (labelLocX.getY()));
    }

    final Stroke orig = g2.getStroke();
    g2.setStroke(new BasicStroke(2));
    g2.setColor(this.TEMP_COLOUR);
    g2.draw(new Line2D.Double(getWidth() - 125, 50, getWidth() - 100, 50));

    g2.setColor(this.DEW_COLOUR);
    g2.draw(new Line2D.Double(getWidth() - 125, 75, getWidth() - 100, 75));

    g2.setColor(
        new Color(
            Color.RED.getRed() / 255f,
            Color.red.getGreen() / 255f,
            Color.red.getBlue() / 255f,
            0.3f));
    g2.draw(new Line2D.Double(getWidth() - 125, 100, getWidth() - 100, 100));

    g2.setStroke(orig);
    g2.setColor(Color.BLACK);

    g2.drawString("Temp", getWidth() - 90, 55);
    g2.drawString("Dewpoint", getWidth() - 90, 80);
    g2.drawString("Parcel Temp", getWidth() - 90, 105);
  }
}
