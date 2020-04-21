package ca.ubc.cs.sanchom.atmosview;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JPanel;

/**
 * The new annotated bar display for atmospheric sounding data.
 *
 * @author Sancho McCann
 */
public class BarPanel extends JPanel {

  private static final long serialVersionUID = 1L;

  public static final Color AXIS_COLOUR = Color.BLACK;
  public static final Color GENERIC_LABEL_COLOUR = new Color(0, 0, 0, 0.6f);
  public static final Color CCL_COLOUR =
      new Color(
          Color.RED.getRed() / 255f, Color.RED.getGreen() / 255f, Color.RED.getBlue() / 255f, 0.6f);
  public static final Color CT_COLOUR =
      new Color(
          Color.RED.getRed() / 255f, Color.RED.getGreen() / 255f, Color.RED.getBlue() / 255f, 0.6f);
  public static final Color STRATUS_COLOUR =
      new Color(
          Color.BLUE.getRed() / 255f,
          Color.BLUE.getGreen() / 255f,
          Color.BLUE.getBlue() / 255f,
          0.6f);
  public static final Color CUMULUS_LIFTED_COLOUR =
      new Color(
          Color.BLUE.getRed() / 255f,
          Color.BLUE.getGreen() / 255f,
          Color.BLUE.getBlue() / 255f,
          0.8f);
  public static final Color FREE_CONVECTION_COLOUR =
      new Color(
          Color.RED.getRed() / 255f, Color.RED.getGreen() / 255f, Color.RED.getBlue() / 255f, 0.8f);

  public static final Color CONVECTIVE_MIDDLE_COLOUR = new Color(0.549f, 0.549f, 0.549f);
  public static final Color CONVECTIVE_LOW_COLOUR = new Color(0.161f, 0.027f, 0.408f);
  public static final Color CONVECTIVE_HIGH_COLOUR = new Color(1f, 0.792f, 0.392f);

  public static final Color INDEX_LOW_COLOUR = new Color(1, 1, 1, 0.1f);
  public static final Color INDEX_HIGH_COLOUR = new Color(0.765f, 0, 0, 0.6f);

  public static final Color WIND_LINE_COLOUR = Color.BLACK;

  public static final Color WIND_ZERO_COLOUR = new Color(1f, 1f, 1f, 0f);
  public static final Color WIND_FULL_COLOUR = new Color(0f, 0f, 0f, 1f);

  // Axes bounds
  private static final int MIN_X = 0;
  private static final int MAX_X = 40;
  private static final int MAX_HEIGHT = 15000; // in metres
  private static final int X_TICK_STEP = 5;
  private static final int Y_TICK_STEP = 1000;

  // Layout details
  private static final int TOP_MARGIN = 50;
  private static final int BOTTOM_MARGIN = 50;
  private static final int LEFT_MARGIN = 150;
  private static final int RIGHT_MARGIN = 100;
  private static final double TICK_SIZE = 5;
  private static final int Y_AXIS_ROOM = 60;
  public static final int TRIANGLE_WIDTH = 6;
  private static final int TRIANGLE_OFFSET = 20;
  public static final int TRIANGLE_HEIGHT = 300; // in metres

  private static final int WIND_OFFSET = 10;
  public static final int PIVOT_SIZE = 5;
  private static final int WIND_DOWNSCALE =
      5; /// < The down-scale factor from windspeed to pixel length for the wind markers
  private static final int WIND_STEP = 1000; // /< The interval in metres between wind drawing

  private int actualLeftMargin = 0;
  private int actualRightMargin = 0;
  private int actualYAxisRoom = 0;

  private Line2D axisX = null;
  private Line2D axisY = null;
  private Vector<Line2D> ticksX = null;
  private Vector<Line2D> ticksY = null;

  private class InterestingAltitude {
    public InterestingAltitude(String label, int altitude, Point2D loc) {
      this.label = label;
      this.altitude = altitude;
      this.loc = loc;
      this.labelLoc = (Point2D) (loc.clone());
    }

    public String label;
    public int altitude;
    public Point2D loc;
    public Point2D labelLoc;
  }

  private Vector<InterestingAltitude>
      interestingAltitudes; /// < Add altitudes to this list to have them marked

  private Point2D labelLocX = null; // /< Where the axis label is centred
  private Point2D labelLocY = null; // /< Where the axis label is centred
  private String title = null;
  private Point2D titleLoc = null; // /< Where the title goes

  private Vector<Rectangle2D> derivedSpreads = null;
  private Vector<Line2D> stratusLayerLines = null;
  private Vector<GeneralPath> cumulusLiftedTriangles = null;
  private Vector<GeneralPath> freeConvectionTriangles = null;
  private Line2D cclLine = null;
  private Line2D ctLine = null;

  private Vector<Line2D> windLines = null;
  private Vector<Ellipse2D> windPivots = null;
  private Vector<Integer> windSpeeds = null;

  private Vector<Rectangle2D> indexMarkerFrames;
  private Vector<Rectangle2D> indexBars;

  private static final double KINX_MAX = 40;
  private static final double LIFTED_MAX = 10;
  private static final double CTOT_MAX = 30;
  private static final double VTOT_MAX = 35;
  private static final double SWEAT_MAX = 600;
  private static final double BRCH_MAX = 100;

  private SoundingData data = null;
  private DerivedData derived = null;

  /**
   * Gives this widget a reference to the original sounding data. This will allow updating of this
   * display if the original data is modified. Also gets a set of derived data for use by this
   * BarPanel widget.
   *
   * @param data the original sounding data
   */
  public void linkSoundingData(SoundingData data) {
    this.data = data;
    this.derived = new DerivedData(this.data);

    updateShapes();
  }

  public SoundingData getSoundingData() {
    return this.data;
  }

  /** Triggers an update of the shape objects. */
  public void updateShapes() {
    double scaling = Math.sqrt((getHeight() / 600f));
    actualLeftMargin = (int) (LEFT_MARGIN * scaling);
    actualYAxisRoom = (int) (Y_AXIS_ROOM * scaling);
    actualRightMargin = (int) (RIGHT_MARGIN * scaling);

    Point2D translatedOrigin =
        new Point2D.Double(actualLeftMargin, getSize().height - BOTTOM_MARGIN);

    // This transform is only for the drawing, not zoom and pan.
    // This allows quick change from temp-height space to canvas space.
    AffineTransform tx =
        new AffineTransform(
            (getSize().width - actualLeftMargin - actualRightMargin) / (float) MAX_X,
            0,
            0,
            -(getSize().height - BOTTOM_MARGIN - TOP_MARGIN) / (float) MAX_HEIGHT,
            translatedOrigin.getX(),
            translatedOrigin.getY());

    // Make the axes and ticks
    axisY =
        new Line2D.Double(
            tx.transform(new Point2D.Double(MIN_X, 0), null),
            tx.transform(new Point2D.Double(MIN_X, MAX_HEIGHT), null));
    axisX =
        new Line2D.Double(
            tx.transform(new Point2D.Double(MIN_X, 0), null),
            tx.transform(new Point2D.Double(MAX_X, 0), null));

    ticksY = new Vector<Line2D>();
    ticksX = new Vector<Line2D>();
    for (int j = Y_TICK_STEP; j <= MAX_HEIGHT; j += Y_TICK_STEP) {
      Point2D loc = new Point2D.Double();

      tx.transform(new Point2D.Double(MIN_X, j), loc);

      ticksY.add(
          new Line2D.Double(
              loc.getX() - TICK_SIZE / 2, loc.getY(), loc.getX() + TICK_SIZE / 2, loc.getY()));
    }

    for (int j = MIN_X; j <= MAX_X; j += X_TICK_STEP) {
      Point2D loc = new Point2D.Double();

      tx.transform(new Point2D.Double(j, 0), loc);

      ticksX.add(
          new Line2D.Double(
              loc.getX(), loc.getY() + TICK_SIZE / 2, loc.getX(), loc.getY() - TICK_SIZE / 2));
    }

    labelLocX =
        new Point2D.Double(
            tx.transform(new Point2D.Double(MAX_X / 2, 0), null).getX(),
            getSize().height - 15 / 2f);

    labelLocY =
        new Point2D.Double(20, tx.transform(new Point2D.Double(0, MAX_HEIGHT / 2), null).getY());
    titleLoc = new Point2D.Double(getWidth() / 2, 25);

    indexMarkerFrames = new Vector<Rectangle2D>();
    indexBars = new Vector<Rectangle2D>();

    double markerSize = getHeight() / 20;
    double markerFrameHeight = markerSize / 2;

    for (int i = 0; i < 6; i++) {
      indexMarkerFrames.add(
          new Rectangle2D.Double(
              getWidth() - actualRightMargin / 2 + WIND_OFFSET,
              getHeight() / 2 + markerFrameHeight * (-10 + 4 * i),
              markerSize,
              markerFrameHeight));
    }

    if (this.data != null) {
      derivedSpreads = new Vector<Rectangle2D>();
      stratusLayerLines = new Vector<Line2D>();
      freeConvectionTriangles = new Vector<GeneralPath>();
      cumulusLiftedTriangles = new Vector<GeneralPath>();
      interestingAltitudes = new Vector<InterestingAltitude>();
      windLines = new Vector<Line2D>();
      windPivots = new Vector<Ellipse2D>();
      windSpeeds = new Vector<Integer>();

      title = this.data.getStationName();

      double canvasTriangleSize =
          tx.transform(new Point2D.Double(0, 0), null).getY()
              - tx.transform(new Point2D.Double(0, TRIANGLE_HEIGHT * .8), null).getY();

      int previousTriangleHeight = 0;

      for (int i = 0; i < this.derived.size(); i++) {
        DerivedPoint p = this.derived.get(i);

        Point2D z = new Point2D.Double();
        Point2D w = new Point2D.Double();
        tx.transform(new Point2D.Double(0, p.getSampleHeight()), z);
        tx.transform(new Point2D.Double(0, p.getSampleHeight() + this.derived.getSampleStep()), w);
        double barHeight = z.getY() - w.getY();

        if (p.getSampleHeight() < MAX_HEIGHT) {
          // Make the horizontal temp/dewpoint bar
          Point2D transformedEnd = new Point2D.Double();
          Point2D transformedBase = new Point2D.Double();
          tx.transform(new Point2D.Double(0, p.getSampleHeight()), transformedBase);
          tx.transform(
              new Point2D.Double(Math.min(p.getSpread(), MAX_X), p.getSampleHeight()),
              transformedEnd);
          Rectangle2D bar =
              new Rectangle2D.Double(
                  transformedBase.getX(),
                  transformedBase.getY() - barHeight,
                  transformedEnd.getX() - transformedBase.getX(),
                  barHeight);
          derivedSpreads.add(bar);

          tx.transform(new Point2D.Double(0, p.getSampleHeight()), transformedBase);
          tx.transform(
              new Point2D.Double(0, p.getSampleHeight() + this.derived.getSampleStep()),
              transformedEnd);

          // TODO: make these shifts of lines consts
          // Add a stratus indicator if necessary
          if (p.isStratusCloud()) {
            stratusLayerLines.add(
                new Line2D.Double(
                    transformedBase.getX() - 10,
                    transformedBase.getY(),
                    transformedEnd.getX() - 10,
                    transformedEnd.getY()));
          }

          // Make triangles
          if (p.getSampleHeight() >= this.derived.getLCL()
              && (Double.isNaN(this.derived.getLFC())
                  || p.getSampleHeight() < this.derived.getLFC())) {
            if (p.getSampleHeight() >= previousTriangleHeight + TRIANGLE_HEIGHT) {
              GeneralPath triangle = new GeneralPath(GeneralPath.WIND_NON_ZERO);

              triangle.moveTo(
                  (float) transformedBase.getX() - TRIANGLE_OFFSET,
                  (float) transformedBase.getY() + (float) canvasTriangleSize / 2f);
              triangle.lineTo(
                  (float) (transformedBase.getX() - TRIANGLE_OFFSET - TRIANGLE_WIDTH / 2),
                  (float) (transformedBase.getY() - canvasTriangleSize / 2f));
              triangle.lineTo(
                  (float) (transformedBase.getX() - TRIANGLE_OFFSET + TRIANGLE_WIDTH / 2),
                  (float) (transformedBase.getY() - canvasTriangleSize / 2f));
              triangle.lineTo(
                  (float) transformedBase.getX() - TRIANGLE_OFFSET,
                  (float) transformedBase.getY() + (float) canvasTriangleSize / 2f);

              cumulusLiftedTriangles.add(triangle);

              previousTriangleHeight = (int) p.getSampleHeight();
            }

          } else if (p.getSampleHeight() >= this.derived.getLFC()
              && (Double.isNaN(this.derived.getEL())
                  || p.getSampleHeight() < this.derived.getEL())) {
            if (p.getSampleHeight() >= previousTriangleHeight + TRIANGLE_HEIGHT) {
              GeneralPath triangle = new GeneralPath(GeneralPath.WIND_NON_ZERO);

              triangle.moveTo(
                  (float) transformedBase.getX() - TRIANGLE_OFFSET,
                  (float) transformedBase.getY() - (float) canvasTriangleSize / 2f);
              triangle.lineTo(
                  (float) (transformedBase.getX() - TRIANGLE_OFFSET - TRIANGLE_WIDTH / 2),
                  (float) (transformedBase.getY() + canvasTriangleSize / 2f));
              triangle.lineTo(
                  (float) (transformedBase.getX() - TRIANGLE_OFFSET + TRIANGLE_WIDTH / 2),
                  (float) (transformedBase.getY() + canvasTriangleSize / 2f));
              triangle.lineTo(
                  (float) transformedBase.getX() - TRIANGLE_OFFSET,
                  (float) transformedBase.getY() - (float) canvasTriangleSize / 2f);

              freeConvectionTriangles.add(triangle);

              previousTriangleHeight = (int) p.getSampleHeight();
            }
          }
        }
      }

      // Add line for the convective condensation level
      double ccl = this.derived.getCCL();
      Point2D transformed = new Point2D.Double();
      tx.transform(new Point2D.Double(0, ccl), transformed);
      cclLine =
          new Line2D.Double(
              transformed.getX() - 5,
              transformed.getY(),
              transformed.getX() + 5,
              transformed.getY());

      // Add line for the convective temperature
      double ct = this.derived.getConvectiveTemperatureRise();
      tx.transform(new Point2D.Double(ct, 0), transformed);
      ctLine =
          new Line2D.Double(
              transformed.getX(),
              transformed.getY() - 5,
              transformed.getX(),
              transformed.getY() + 5);

      if (!Double.isNaN(this.derived.getLFC())) {
        Point2D loc = new Point2D.Double();
        tx.transform(new Point2D.Double(MIN_X, this.derived.getLFC()), loc);
        interestingAltitudes.add(new InterestingAltitude("LFC", (int) this.derived.getLFC(), loc));
      }

      if (!Double.isNaN(this.derived.getCCL())) {
        Point2D loc = new Point2D.Double();
        tx.transform(new Point2D.Double(MIN_X, this.derived.getCCL()), loc);
        interestingAltitudes.add(new InterestingAltitude("CCL", (int) this.derived.getCCL(), loc));
      }

      if (!Double.isNaN(this.derived.getLCL())) {
        Point2D loc = new Point2D.Double();
        tx.transform(new Point2D.Double(MIN_X, this.derived.getLCL()), loc);
        interestingAltitudes.add(new InterestingAltitude("LCL", (int) this.derived.getLCL(), loc));
      }

      if (!Double.isNaN(this.derived.getEL()) && this.derived.getEL() <= MAX_HEIGHT) {
        Point2D loc = new Point2D.Double();
        tx.transform(new Point2D.Double(MIN_X, this.derived.getEL()), loc);
        interestingAltitudes.add(new InterestingAltitude("EQL", (int) this.derived.getEL(), loc));
      }

      Collections.sort(
          interestingAltitudes,
          new Comparator<InterestingAltitude>() {
            public int compare(InterestingAltitude a, InterestingAltitude b) {
              return a.altitude - b.altitude;
            }
          });

      indexBars.add(
          new Rectangle2D.Double(
              getWidth() - actualRightMargin / 2 + WIND_OFFSET,
              getHeight() / 2 + markerFrameHeight * -10,
              Math.min(markerSize, markerSize * (this.derived.getKINX() / KINX_MAX)),
              markerFrameHeight));
      indexBars.add(
          new Rectangle2D.Double(
              getWidth() - actualRightMargin / 2 + WIND_OFFSET,
              getHeight() / 2 + markerFrameHeight * -6,
              Math.min(markerSize, markerSize * (-this.derived.getLIFTED_INDEX() / LIFTED_MAX)),
              markerFrameHeight));
      indexBars.add(
          new Rectangle2D.Double(
              getWidth() - actualRightMargin / 2 + WIND_OFFSET,
              getHeight() / 2 + markerFrameHeight * -2,
              Math.min(markerSize, markerSize * (this.derived.getCROSS_TOTALS_INDEX() / CTOT_MAX)),
              markerFrameHeight));
      indexBars.add(
          new Rectangle2D.Double(
              getWidth() - actualRightMargin / 2 + WIND_OFFSET,
              getHeight() / 2 + markerFrameHeight * 2,
              Math.min(
                  markerSize, markerSize * (this.derived.getVERTICAL_TOTALS_INDEX() / VTOT_MAX)),
              markerFrameHeight));
      indexBars.add(
          new Rectangle2D.Double(
              getWidth() - actualRightMargin / 2 + WIND_OFFSET,
              getHeight() / 2 + markerFrameHeight * 6,
              Math.min(markerSize, markerSize * (this.derived.getSWEAT() / SWEAT_MAX)),
              markerFrameHeight));
      indexBars.add(
          new Rectangle2D.Double(
              getWidth() - actualRightMargin / 2 + WIND_OFFSET,
              getHeight() / 2 + markerFrameHeight * 10,
              Math.min(markerSize, markerSize * (this.derived.getBRCH() / BRCH_MAX)),
              markerFrameHeight));

      for (int height = 0;
          height < Math.min(MAX_HEIGHT, this.derived.maxHeight());
          height += WIND_STEP) {
        double clampedHeight = Math.max(height, this.derived.minHeight());
        DerivedPoint p = this.derived.getDataFromHeight(clampedHeight);
        double clockwiseFromNorth = p.getDirection();
        double counterclockwiseFromXAxis = 90 - clockwiseFromNorth;
        if (counterclockwiseFromXAxis < 0) {
          counterclockwiseFromXAxis += 360;
        }

        Point2D loc = new Point2D.Double();
        tx.transform(new Point2D.Double(MAX_X, clampedHeight), loc);
        loc.setLocation(loc.getX() + WIND_OFFSET, loc.getY());

        windPivots.add(
            new Ellipse2D.Double(
                loc.getX() - PIVOT_SIZE / 2f,
                loc.getY() - PIVOT_SIZE / 2f,
                PIVOT_SIZE,
                PIVOT_SIZE));

        double speed = p.getSpeed();
        double radians = Math.toRadians(counterclockwiseFromXAxis);
        double diffX = (speed / WIND_DOWNSCALE) * Math.cos(radians);
        double diffY =
            (speed / WIND_DOWNSCALE)
                * -Math.sin(radians); // negative because the Y axis is upside down in drawing

        windLines.add(
            new Line2D.Double(loc.getX(), loc.getY(), loc.getX() + diffX, loc.getY() + diffY));
        windSpeeds.add(new Integer((int) speed));
      }
    }
  }

  /** Paint the component. */
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);
    g2.setComposite(ac);

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(
        RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    g2.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    g2.setFont(new Font("Verdana", Font.PLAIN, Math.min(getHeight() / 30, 12)));

    // TODO: Use canvas transformations to allow pan and zoom
    g2.setColor(AXIS_COLOUR);
    if (axisY != null && axisX != null) {
      g2.draw(axisY);
      g2.draw(axisX);
    }

    if (ticksY != null) {

      for (int i = 0; i < ticksY.size(); i++) {
        g2.setColor(AXIS_COLOUR);
        g2.draw(ticksY.get(i));
        if (i == ticksY.size() - 1) {
          g2.setColor(GENERIC_LABEL_COLOUR);
          String tickLabel = new String("" + (i + 1) * Y_TICK_STEP);
          Rectangle2D bounds = g2.getFont().getStringBounds(tickLabel, g2.getFontRenderContext());

          g2.drawString(
              tickLabel,
              (int) (ticksY.get(i).getP1().getX() - (bounds.getWidth() + TRIANGLE_OFFSET + 3)),
              (int) (ticksY.get(i).getP1().getY() + bounds.getHeight() / 2));
        }
      }
    }

    if (labelLocY != null) {
      g2.setColor(AXIS_COLOUR);
      g2.translate(labelLocY.getX(), labelLocY.getY());
      g2.rotate(-Math.PI / 2);

      TextLayout layout = new TextLayout("Altitude (m)", g2.getFont(), g2.getFontRenderContext());
      layout.draw(g2, -layout.getAdvance() / 2, 0);
      AffineTransform orig = g2.getTransform();
      g2.setTransform(orig);
    }

    if (interestingAltitudes != null) {
      // Prevent overlap between labels
      if (interestingAltitudes.size() > 1) {
        int medianIndex = (interestingAltitudes.size() - 1) / 2;
        int numUpper = (interestingAltitudes.size() % 2 == 0 ? medianIndex + 1 : medianIndex);
        int numLower = medianIndex;
        for (int i = 1; i <= numUpper; i++) {
          InterestingAltitude lowerLabel = interestingAltitudes.get(medianIndex + i - 1);
          InterestingAltitude upperLabel = interestingAltitudes.get(medianIndex + i);

          Rectangle2D lowerBounds =
              g2.getFont().getStringBounds(lowerLabel.label, g2.getFontRenderContext());
          Rectangle2D upperBounds =
              g2.getFont().getStringBounds(upperLabel.label, g2.getFontRenderContext());

          double overlap =
              (upperLabel.labelLoc.getY() + upperBounds.getHeight() / 2)
                  - (lowerLabel.labelLoc.getY() - lowerBounds.getHeight() / 2);

          if (overlap > 0) {
            upperLabel.labelLoc.setLocation(
                upperLabel.labelLoc.getX(), upperLabel.labelLoc.getY() - overlap);
          }
        }
        for (int i = 1; i <= numLower; i++) {
          InterestingAltitude upperLabel = interestingAltitudes.get(medianIndex - i + 1);
          InterestingAltitude lowerLabel = interestingAltitudes.get(medianIndex - i);

          Rectangle2D upperBounds =
              g2.getFont().getStringBounds(upperLabel.label, g2.getFontRenderContext());
          Rectangle2D lowerBounds =
              g2.getFont().getStringBounds(lowerLabel.label, g2.getFontRenderContext());

          double overlap =
              (upperLabel.labelLoc.getY() + upperBounds.getHeight() / 2)
                  - (lowerLabel.labelLoc.getY() - lowerBounds.getHeight() / 2);

          if (overlap > 0) {
            lowerLabel.labelLoc.setLocation(
                lowerLabel.labelLoc.getX(), lowerLabel.labelLoc.getY() + overlap);
          }
        }
      }

      for (int i = 0; i < interestingAltitudes.size(); i++) {
        g2.setColor(GENERIC_LABEL_COLOUR);
        InterestingAltitude a = interestingAltitudes.get(i);
        // g2.draw(new Line2D.Double(new Point2D.Double(a.loc.getX()-5, a.loc.getY()), new
        // Point2D.Double(a.loc.getX() + 5, a.loc.getY())));

        String tickLabel = new String(a.label + " " + Integer.toString(a.altitude));
        Rectangle2D bounds = g2.getFont().getStringBounds(tickLabel, g2.getFontRenderContext());

        g2.drawString(
            tickLabel,
            (int) (a.labelLoc.getX() - (bounds.getWidth() + actualYAxisRoom)),
            (int) (a.labelLoc.getY() + bounds.getHeight() / 2));

        g2.setColor(new Color(0, 0, 0, 0.3f));
        g2.draw(
            new Line2D.Double(
                new Point2D.Double(a.labelLoc.getX() - actualYAxisRoom, a.labelLoc.getY()),
                new Point2D.Double(a.loc.getX() - TRIANGLE_OFFSET, a.loc.getY())));
      }
    }

    if (ticksX != null) {
      g2.setColor(AXIS_COLOUR);
      for (int i = 0; i < ticksX.size(); i++) {
        g2.draw(ticksX.get(i));
        String tickLabel = new String("" + (MIN_X + i * X_TICK_STEP));

        if (i % 2 == 0) {
          Rectangle2D bounds = g2.getFont().getStringBounds(tickLabel, g2.getFontRenderContext());

          g2.drawString(
              tickLabel,
              (int) (ticksX.get(i).getP1().getX() - bounds.getWidth() / 2.0),
              (int) (ticksX.get(i).getP1().getY() + bounds.getHeight() + 5));
        }
      }

      String labelX = new String("Temperature-Dewpoint Spread (°C)");
      Rectangle2D bounds = g2.getFont().getStringBounds(labelX, g2.getFontRenderContext());
      g2.drawString(
          labelX, (int) (labelLocX.getX() - bounds.getWidth() / 2f), (int) labelLocX.getY());
    }

    if (title != null) {
      g2.setColor(AXIS_COLOUR);
      Rectangle2D bounds = g2.getFont().getStringBounds(title, g2.getFontRenderContext());
      g2.drawString(title, (int) (titleLoc.getX() - bounds.getWidth() / 2f), (int) titleLoc.getY());
    }

    if (derivedSpreads != null) {
      for (int i = 0; i < derivedSpreads.size(); i++) {
        double x = this.derived.get(i).getLiftedDiff();

        // Interpolate the colour of the temperature/dewpoint bar based on
        // convective potential.
        // TODO: Make the limits of interpolation consts
        if (x > 0) {
          g2.setColor(
              getInterpolatedColour(CONVECTIVE_MIDDLE_COLOUR, CONVECTIVE_HIGH_COLOUR, 0, 15, x));
        } else if (x < 0) {
          g2.setColor(
              getInterpolatedColour(CONVECTIVE_LOW_COLOUR, CONVECTIVE_MIDDLE_COLOUR, -10, 0, x));
        } else {
          g2.setColor(CONVECTIVE_MIDDLE_COLOUR);
        }

        g2.fill(derivedSpreads.get(i));
      }
    }

    if (stratusLayerLines != null) {
      g2.setStroke(new BasicStroke(5));
      g2.setColor(STRATUS_COLOUR);
      for (int i = 0; i < stratusLayerLines.size(); i++) {
        g2.draw(stratusLayerLines.get(i));
      }
      Stroke orig = g2.getStroke();
      g2.setStroke(orig);
    }

    if (cumulusLiftedTriangles != null) {
      g2.setColor(CUMULUS_LIFTED_COLOUR);
      for (int i = 0; i < cumulusLiftedTriangles.size(); i++) {
        g2.fill(cumulusLiftedTriangles.get(i));
      }
    }

    if (freeConvectionTriangles != null) {
      g2.setColor(FREE_CONVECTION_COLOUR);
      for (int i = 0; i < freeConvectionTriangles.size(); i++) {
        g2.fill(freeConvectionTriangles.get(i));
      }
    }

    if (cclLine != null) {
      Stroke orig = g2.getStroke();
      g2.setStroke(new BasicStroke(3));
      g2.setColor(CCL_COLOUR);
      g2.draw(cclLine);
      g2.setStroke(orig);
    }

    if (ctLine != null) {
      Stroke orig = g2.getStroke();
      g2.setStroke(new BasicStroke(3));
      g2.setColor(CT_COLOUR);
      g2.draw(ctLine);
      g2.setStroke(orig);
    }

    if (windLines != null) {
      g2.setColor(WIND_LINE_COLOUR);
      Iterator i = windLines.iterator();
      while (i.hasNext()) {
        g2.draw((Line2D) i.next());
      }
    }

    if (windPivots != null && windSpeeds != null) {
      Iterator i = windPivots.iterator();
      Iterator s = windSpeeds.iterator();
      while (i.hasNext() && s.hasNext()) {
        Ellipse2D pivot = (Ellipse2D) i.next();
        Integer speed = (Integer) s.next();
        Color c = getInterpolatedColour(WIND_ZERO_COLOUR, WIND_FULL_COLOUR, 0, 80, speed);
        g2.setColor(c);
        g2.fill(pivot);

        Font orig = g2.getFont();
        g2.setFont(new Font("Verdana", Font.PLAIN, (int) (orig.getSize() * 0.8)));
        Rectangle2D bounds =
            g2.getFont().getStringBounds(speed.toString(), g2.getFontRenderContext());
        g2.setColor(GENERIC_LABEL_COLOUR);
        g2.drawString(
            speed.toString(),
            (float) (pivot.getCenterX() + 2 * PIVOT_SIZE),
            (float) (pivot.getCenterY() + bounds.getWidth() / 2f));
        g2.setFont(orig);
      }
    }

    if (this.derived != null) {
      g2.setColor(Color.BLACK);

      String label = null;
      Rectangle2D bounds = null;

      if (indexMarkerFrames != null) {
        Iterator<Rectangle2D> i = indexMarkerFrames.iterator();
        while (i.hasNext()) {
          g2.draw(i.next());
        }
      }

      if (indexBars != null) {
        g2.setColor(GENERIC_LABEL_COLOUR);
        label = new String("KINX");
        bounds = g2.getFont().getStringBounds(label, g2.getFontRenderContext());
        g2.drawString(
            label,
            (float) (indexMarkerFrames.get(0).getCenterX() - bounds.getWidth() / 2f),
            (float) (indexMarkerFrames.get(0).getMinY() - 1));
        g2.setColor(
            getInterpolatedColour(
                INDEX_LOW_COLOUR, INDEX_HIGH_COLOUR, 0, KINX_MAX, this.derived.getKINX()));
        g2.fill(indexBars.get(0));

        g2.setColor(GENERIC_LABEL_COLOUR);
        label = new String("LIFT");
        bounds = g2.getFont().getStringBounds(label, g2.getFontRenderContext());
        g2.drawString(
            label,
            (float) (indexMarkerFrames.get(1).getCenterX() - bounds.getWidth() / 2f),
            (float) (indexMarkerFrames.get(1).getMinY() - 1));
        g2.setColor(
            getInterpolatedColour(
                INDEX_LOW_COLOUR,
                INDEX_HIGH_COLOUR,
                0,
                LIFTED_MAX,
                -this.derived.getLIFTED_INDEX()));
        g2.fill(indexBars.get(1));

        g2.setColor(GENERIC_LABEL_COLOUR);
        label = new String("CTOT");
        bounds = g2.getFont().getStringBounds(label, g2.getFontRenderContext());
        g2.drawString(
            label,
            (float) (indexMarkerFrames.get(2).getCenterX() - bounds.getWidth() / 2f),
            (float) (indexMarkerFrames.get(2).getMinY() - 1));
        g2.setColor(
            getInterpolatedColour(
                INDEX_LOW_COLOUR,
                INDEX_HIGH_COLOUR,
                0,
                CTOT_MAX,
                this.derived.getCROSS_TOTALS_INDEX()));
        g2.fill(indexBars.get(2));

        g2.setColor(GENERIC_LABEL_COLOUR);
        label = new String("VTOT");
        bounds = g2.getFont().getStringBounds(label, g2.getFontRenderContext());
        g2.drawString(
            label,
            (float) (indexMarkerFrames.get(3).getCenterX() - bounds.getWidth() / 2f),
            (float) (indexMarkerFrames.get(3).getMinY() - 1));
        g2.setColor(
            getInterpolatedColour(
                INDEX_LOW_COLOUR,
                INDEX_HIGH_COLOUR,
                0,
                VTOT_MAX,
                this.derived.getVERTICAL_TOTALS_INDEX()));
        g2.fill(indexBars.get(3));

        g2.setColor(GENERIC_LABEL_COLOUR);
        label = new String("SWEAT");
        bounds = g2.getFont().getStringBounds(label, g2.getFontRenderContext());
        g2.drawString(
            label,
            (float) (indexMarkerFrames.get(4).getCenterX() - bounds.getWidth() / 2f),
            (float) (indexMarkerFrames.get(4).getMinY() - 1));
        g2.setColor(
            getInterpolatedColour(
                INDEX_LOW_COLOUR, INDEX_HIGH_COLOUR, 0, SWEAT_MAX, this.derived.getSWEAT()));
        g2.fill(indexBars.get(4));

        g2.setColor(GENERIC_LABEL_COLOUR);
        label = new String("BRCH");
        bounds = g2.getFont().getStringBounds(label, g2.getFontRenderContext());
        g2.drawString(
            label,
            (float) (indexMarkerFrames.get(5).getCenterX() - bounds.getWidth() / 2f),
            (float) (indexMarkerFrames.get(5).getMinY() - 1));
        g2.setColor(
            getInterpolatedColour(
                INDEX_LOW_COLOUR, INDEX_HIGH_COLOUR, 0, BRCH_MAX, this.derived.getBRCH()));
        g2.fill(indexBars.get(5));
      }

      g2.setColor(Color.BLACK);
    }
  }

  private Color getInterpolatedColour(
      Color lowColour, Color highColour, double lowLimit, double highLimit, double value) {
    if (value <= lowLimit) {
      return lowColour;
    } else if (value >= highLimit) {
      return highColour;
    } else {
      double diff = highLimit - lowLimit;
      double valueOffset = value - lowLimit;
      double highWeight = valueOffset / diff;
      double lowWeight = 1 - highWeight;

      float lowAlpha = lowColour.getAlpha() / 255f;
      float highAlpha = highColour.getAlpha() / 255f;

      float interpolatedRed =
          (float)
                  (lowWeight * lowColour.getRed() * lowAlpha
                      + highWeight * highColour.getRed() * highAlpha)
              / 255f;
      float interpolatedBlue =
          (float)
                  (lowWeight * lowColour.getBlue() * lowAlpha
                      + highWeight * highColour.getBlue() * highAlpha)
              / 255f;
      float interpolatedGreen =
          (float)
                  (lowWeight * lowColour.getGreen() * lowAlpha
                      + highWeight * highColour.getGreen() * highAlpha)
              / 255f;
      float interpolatedAlpha =
          (float) (lowWeight * lowColour.getAlpha() + highWeight * highColour.getAlpha()) / 255f;

      return new Color(
          interpolatedRed / interpolatedAlpha,
          interpolatedGreen / interpolatedAlpha,
          interpolatedBlue / interpolatedAlpha,
          interpolatedAlpha);
    }
  }
}
