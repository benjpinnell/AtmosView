package ca.ubc.cs.sanchom.AtmosView;
import javax.swing.JPanel;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ListIterator;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.Vector;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.Stroke;
import java.awt.BasicStroke;


/**
 * A display widget for SoundingData plots.
 * @author Sancho McCann
 *
 */
public class SoundingPanel extends JPanel {

	
	/**
	 * The data that is plotted on this panel
	 */
	private SoundingData m_data = null;
	private DerivedData m_derivedData = null;
	
	private final Color AXIS_COLOUR = Color.BLACK;
	private final Color DEW_COLOUR = new Color(0.018f, 0.4072f, 0f);
	private final Color TEMP_COLOUR = new Color(0f, 0.4077f, 0.8385f);
	
	private final int MIN_TEMP = -100;
	private final int MAX_TEMP = 45;
	private final int MAX_HEIGHT = 15000; // metres metres
	private final int X_TICK_STEP = 20;
	private final int Y_TICK_STEP = 1000;
	
	private Point2D xLabelLoc = null;
	private Point2D yLabelLoc = null;
	
	private Point2D titleLoc = null;
	
	private final int TOP_MARGIN = 50;
	private final int BOTTOM_MARGIN = 50;
	private final int LEFT_MARGIN = 100;
	private final int RIGHT_MARGIN = 50;
	private final int TICK_SIZE = 5;

	private Line2D xAxis = null;
	private Line2D yAxis = null;
	private Vector<Line2D> xTicks = null;
	private Vector<Line2D> yTicks = null;

	private GeneralPath tempPath = null;
	private GeneralPath dewPath = null;
	private GeneralPath parcelPath = null;
	
	private String title = null;

	private static final long serialVersionUID = 1L;


	/**
	 * Provides a reference to the original sounding data. The reference will be copied for use
	 * by this widget. It is a reference so that modifications due to drawing can be used to update
	 * other widgets displaying the data in other forms.
	 * @param data the SoundingData
	 */
	public void linkSoundingData(SoundingData data)
	{
		m_data = data;
		m_derivedData = new DerivedData(data);
		updateShapes();
	}
	
	
	/**
	 * Triggers an update of the shape objects
	 * 
	 */
	public void updateShapes()
	{
		Point2D TRANSLATED_ORIGIN = new Point2D.Double(getSize().width * 0.66f, getSize().height - BOTTOM_MARGIN);

		// This transform is only for the drawing, not zoom and pan.
		// This allows quick change from temp-height space to canvas space.
		AffineTransform tx = new AffineTransform(
				(getSize().width - RIGHT_MARGIN - LEFT_MARGIN)/(float)(MAX_TEMP-MIN_TEMP), 0, 0, -(getSize().height - BOTTOM_MARGIN - TOP_MARGIN)/(float)MAX_HEIGHT, TRANSLATED_ORIGIN.getX(), TRANSLATED_ORIGIN.getY());

		yAxis = new Line2D.Double(
				tx.transform(new Point2D.Double(MIN_TEMP,0), null),
				tx.transform(new Point2D.Double(MIN_TEMP, MAX_HEIGHT), null));
		xAxis = new Line2D.Double(
				tx.transform(new Point2D.Double(MIN_TEMP,0), null),
				tx.transform(new Point2D.Double(MAX_TEMP, 0), null));

		yTicks = new Vector<Line2D>();
		xTicks = new Vector<Line2D>();
		for (int j = Y_TICK_STEP; j <= MAX_HEIGHT; j+= Y_TICK_STEP)
		{
			Point2D loc = new Point2D.Double();

			tx.transform(new Point2D.Double(MIN_TEMP, j), loc);

			yTicks.add(new Line2D.Double(
					loc.getX()-TICK_SIZE/2f, loc.getY(),
					loc.getX()+TICK_SIZE/2f, loc.getY()));
		}

		for (int j = MIN_TEMP; j <= MAX_TEMP; j+= X_TICK_STEP)
		{
			Point2D loc = new Point2D.Double();

			tx.transform(new Point2D.Double(j, 0), loc);

			xTicks.add(new Line2D.Double(
					loc.getX(), loc.getY()+TICK_SIZE/2f,
					loc.getX(), loc.getY()-TICK_SIZE/2f));
		}
		
		xLabelLoc = new Point2D.Double(
				tx.transform(new Point2D.Double((MAX_TEMP+MIN_TEMP)/2f, 0), null).getX(),
				getSize().height - 15 / 2f);

		yLabelLoc = new Point2D.Double(20, tx.transform(new Point2D.Double(0, MAX_HEIGHT/2), null).getY());

		if (m_data != null)
		{
			title = m_data.getStationName();

			parcelPath = new GeneralPath();
			tempPath = new GeneralPath();
			dewPath = new GeneralPath();

			ListIterator i = m_data.listIterator();
			int n = 0;
			while ( i.hasNext() ) {
				SoundingPoint sp = (SoundingPoint)(i.next());

				if ( sp.getMetres() < MAX_HEIGHT )
				{
					double yVal = sp.getMetres();
					double temp = sp.getTemperature();
					double dew = sp.getDewpoint();

					Point2D transformedTemp = new Point2D.Double();
					Point2D transformedDewpoint = new Point2D.Double();
					tx.transform(new Point2D.Double(temp, yVal), transformedTemp);
					tx.transform(new Point2D.Double(dew, yVal), transformedDewpoint);
					if (n == 0)
					{
						tempPath.moveTo((float)transformedTemp.getX(), (float)transformedTemp.getY());
						dewPath.moveTo((float)transformedDewpoint.getX(), (float)transformedDewpoint.getY());
					}
					else
					{
						tempPath.lineTo((float)transformedTemp.getX(), (float)transformedTemp.getY());
						dewPath.lineTo((float)transformedDewpoint.getX(), (float)transformedDewpoint.getY());
					}
					n++;
				}
			}
			
			
			ListIterator j = m_derivedData.getList().listIterator();
			boolean first = true;
			while ( j.hasNext() ) {
				DerivedPoint dp = (DerivedPoint)(j.next());
				double parcelTemp = dp.getLiftedParcelTemp();
				
				if ( dp.getSampleHeight() < MAX_HEIGHT &&  parcelTemp >= MIN_TEMP)
				{
					double yVal = dp.getSampleHeight();

					Point2D transformedTemp = new Point2D.Double();
					tx.transform(new Point2D.Double(parcelTemp, yVal), transformedTemp);
					if (first)
					{
						parcelPath.moveTo((float)transformedTemp.getX(), (float)transformedTemp.getY());
						first = false;
					}
					else
					{
						parcelPath.lineTo((float)transformedTemp.getX(), (float)transformedTemp.getY());
					}
				}
			}
			

		}
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		g2.setFont(new Font("Verdana", Font.PLAIN, Math.min(getWidth() / 30, 12)));
		titleLoc = new Point2D.Double(getWidth()/2, 25);

		// TODO: Use canvas transformations to allow pan and zoom
		g2.setColor(AXIS_COLOUR);
		if (yAxis != null && xAxis != null)
		{
			g2.draw(yAxis);
			g2.draw(xAxis);			
		}
		
		if (title != null)
		{
			g2.setColor(AXIS_COLOUR);
			Rectangle2D bounds = g2.getFont().getStringBounds(title, g2.getFontRenderContext());
			g2.drawString(title, (int)(titleLoc.getX() - bounds.getWidth()/2f), (int)titleLoc.getY());
			
		}
		
//		if (m_data != null ) {
//			g2.setColor(TITLE_COLOUR);
//			g2.drawString(m_data.timeString(), 240, 30);
//		}
		
		if (tempPath != null) {
			g2.setColor(TEMP_COLOUR);
			g2.draw(tempPath);
		}
		if (dewPath != null) {
			g2.setColor(DEW_COLOUR);
			g2.draw(dewPath);
		}
		if (parcelPath != null) {
			g2.setColor(new Color(Color.RED.getRed()/255f, Color.red.getGreen()/255f, Color.red.getBlue()/255f, 0.3f));
			g2.draw(parcelPath);
		}

		if (yTicks != null) {
			g2.setColor(AXIS_COLOUR);
			for (int i = 0; i < yTicks.size(); i++) {
				g2.draw(yTicks.get(i));
				String tickLabel = new String("" + (i+1)*Y_TICK_STEP);
				Rectangle2D bounds = g2.getFont().getStringBounds(tickLabel, g2.getFontRenderContext());

				if ( (i+1) % 5 == 0)
				{
					g2.drawString(tickLabel,
							(int)(yTicks.get(i).getP1().getX()-(bounds.getWidth()+5)),
							(int)(yTicks.get(i).getP1().getY()+bounds.getHeight()/2));
				}
			}


			TextLayout layout = new TextLayout("Altitude (m)", g2.getFont(), g2.getFontRenderContext());
			AffineTransform orig = g2.getTransform();

			g2.translate(yLabelLoc.getX(), yLabelLoc.getY());
			g2.rotate(-Math.PI / 2);
			layout.draw(g2, -layout.getAdvance() / 2, 0);
			g2.setTransform(orig);
		}
		
		if (xTicks != null) {
			g2.setColor(AXIS_COLOUR);
			for (int i = 0; i < xTicks.size(); i++) {
				g2.draw(xTicks.get(i));
				String tickLabel = new String("" + (MIN_TEMP+i*X_TICK_STEP));
				Rectangle2D bounds = g2.getFont().getStringBounds(tickLabel, g2.getFontRenderContext());
			
				g2.drawString(tickLabel,
						(int)(xTicks.get(i).getP1().getX()-bounds.getWidth()/2.0),
						(int)(xTicks.get(i).getP1().getY()+bounds.getHeight()+5));
			}
			
			String xLabel = new String("Temperature and Dewpoint (\u00B0C)");
			Rectangle2D bounds = g2.getFont().getStringBounds(xLabel, g2.getFontRenderContext());
			g2.drawString(xLabel, (int)(xLabelLoc.getX() - bounds.getWidth()/2f), (int)(xLabelLoc.getY()));
		}
		
		
		Stroke orig = g2.getStroke();
		g2.setStroke(new BasicStroke(2));
		g2.setColor(this.TEMP_COLOUR);
		g2.draw(new Line2D.Double(getWidth() - 125, 50, getWidth() - 100, 50));
		
		g2.setColor(this.DEW_COLOUR);
		g2.draw(new Line2D.Double(getWidth() - 125, 75, getWidth() - 100, 75));
		
		g2.setColor(new Color(Color.RED.getRed()/255f, Color.red.getGreen()/255f, Color.red.getBlue()/255f, 0.3f));
		g2.draw(new Line2D.Double(getWidth() - 125, 100, getWidth() - 100, 100));
		
		g2.setStroke(orig);
		g2.setColor(Color.BLACK);
		
		g2.drawString("Temp", getWidth() - 90, 55);
		g2.drawString("Dewpoint", getWidth() - 90, 80);
		g2.drawString("Parcel Temp", getWidth() - 90, 105);
	}
	
}
