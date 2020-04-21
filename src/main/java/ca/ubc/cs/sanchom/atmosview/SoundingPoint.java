package ca.ubc.cs.sanchom.atmosview;

import java.io.Serializable;

/**
 * Holds a single sample point from the original sounding data. This represents sufficient
 * information required per sample in a vertical atmospheric sounding required to deliver the
 * visualiztion that this project creates. The wind direction and speed are not necessary.
 *
 * @author Sancho McCann
 */
public class SoundingPoint implements Serializable, Comparable {
  private static final long serialVersionUID = 1L;

  private double m_millibars; // /< The pressure level in millibars
  private double m_metres; // /< The sample height in metres
  private double m_temperature; // /< The temperature in degrees celcius
  private double m_dewpoint; // /< The dewpoint in degrees celcius
  private double m_direction; // /< The wind direction in degrees clockwise from North
  private double m_speed; // /< The wind speed in nautical miles per hour

  /**
   * Constructor
   *
   * @param millibars the pressure level in millibars
   * @param metres the height in metres
   * @param temperature the temperature in degrees celcius
   * @param dewpoint the dewpoint in degrees celcius
   * @param direction the wind direction in degrees clockwise from North
   * @param speed the wind speed in knots
   */
  public SoundingPoint(
      double millibars,
      double metres,
      double temperature,
      double dewpoint,
      double direction,
      double speed) {
    m_millibars = millibars;
    m_metres = metres;
    m_temperature = temperature;
    m_dewpoint = dewpoint;
    m_direction = direction;
    m_speed = speed;
  }

  /**
   * Constructor
   *
   * @param millibars the pressure level in millibars
   * @param metres the height in metres
   * @param temperature the temperature in degrees celcius
   * @param dewpoint the dewpoint in degrees celcius
   */
  public SoundingPoint(double millibars, double metres, double temperature, double dewpoint) {
    this(millibars, metres, temperature, dewpoint, Double.NaN, Double.NaN);
  }

  /**
   * Tests for value equality between two SoundingPoint objects
   *
   * @param obj the other SoundingPoint object
   * @return true if the objects' members are equal; false otherwise
   */
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SoundingPoint)) return false;

    SoundingPoint that = (SoundingPoint) obj;

    return (this.m_millibars == that.m_millibars)
        && (this.m_metres == that.m_metres)
        && (this.m_temperature == that.m_temperature)
        && (this.m_dewpoint == that.m_dewpoint)
        && (((this.m_direction == that.m_direction) && (this.m_speed == that.m_speed))
            || ((Double.isNaN(this.m_direction) && Double.isNaN(that.m_direction))
                && (Double.isNaN(this.m_speed) && Double.isNaN(that.m_speed))));
  }

  /**
   * @param obj the object to compare with this SoundingPoint
   * @return 1 if this SoundingPoint has a higher altitude that obj 0 if this SoundingPoint has the
   *     same altitude as obj, or -1 if this SoundingPoint has a lowe altitude than obj
   */
  public int compareTo(Object obj) throws ClassCastException {
    if (!(obj instanceof SoundingPoint))
      throw new ClassCastException(
          "Passed object cannot be compared with object of type SoundingPoint");
    if (equals(obj)) return 0;

    SoundingPoint that = (SoundingPoint) obj;

    if (this.m_metres == that.m_metres) return 0;

    return (this.m_metres < that.m_metres ? -1 : 1);
  }

  /** returns the pressure level of the sounding in millibars */
  public double getMillibars() {
    return m_millibars;
  }

  /** returns the height of the sounding in metres */
  public double getMetres() {
    return m_metres;
  }

  /** returns the temperature of the sounding in degrees celcius */
  public double getTemperature() {
    return m_temperature;
  }

  /** returns the dewpoint of the sounding in degrees celcuis */
  public double getDewpoint() {
    return m_dewpoint;
  }

  /** returns the direction of the wind in degrees clockwise from North */
  public double getDirection() {
    return m_direction;
  }

  /** returns the speed of the wind in knots */
  public double getSpeed() {
    return m_speed;
  }

  /** returns a string representation of the data in this SoundingPoint */
  public String toString() {
    return m_millibars
        + "mb, "
        + m_metres
        + "m, "
        + m_temperature
        + " C, "
        + m_dewpoint
        + " C, "
        + m_direction
        + " degrees, "
        + m_speed;
  }
}
