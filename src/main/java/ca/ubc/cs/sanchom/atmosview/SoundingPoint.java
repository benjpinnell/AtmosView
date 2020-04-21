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

  private double millibars; // /< The pressure level in millibars
  private double metres; // /< The sample height in metres
  private double temperature; // /< The temperature in degrees celcius
  private double dewpoint; // /< The dewpoint in degrees celcius
  private double direction; // /< The wind direction in degrees clockwise from North
  private double speed; // /< The wind speed in nautical miles per hour

  /**
   * Constructor.
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
    this.millibars = millibars;
    this.metres = metres;
    this.temperature = temperature;
    this.dewpoint = dewpoint;
    this.direction = direction;
    this.speed = speed;
  }

  /**
   * Constructor.
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
   * Tests for value equality between two SoundingPoint objects.
   *
   * @param obj the other SoundingPoint object
   * @return true if the objects' members are equal; false otherwise
   */
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SoundingPoint)) {
      return false;
    }

    SoundingPoint that = (SoundingPoint) obj;

    return (this.millibars == that.millibars)
        && (this.metres == that.metres)
        && (this.temperature == that.temperature)
        && (this.dewpoint == that.dewpoint)
        && (((this.direction == that.direction) && (this.speed == that.speed))
            || ((Double.isNaN(this.direction) && Double.isNaN(that.direction))
                && (Double.isNaN(this.speed) && Double.isNaN(that.speed))));
  }

  /**
   * Override compareTo.
   *
   * @param obj the object to compare with this SoundingPoint
   * @return 1 if this SoundingPoint has a higher altitude that obj 0 if this SoundingPoint has the
   *     same altitude as obj, or -1 if this SoundingPoint has a lowe altitude than obj
   */
  public int compareTo(Object obj) throws ClassCastException {
    if (!(obj instanceof SoundingPoint)) {
      throw new ClassCastException(
          "Passed object cannot be compared with object of type SoundingPoint");
    }
    if (equals(obj)) {
      return 0;
    }

    SoundingPoint that = (SoundingPoint) obj;

    if (this.metres == that.metres) {
      return 0;
    }

    return (this.metres < that.metres ? -1 : 1);
  }

  /** Returns the pressure level of the sounding in millibars. */
  public double getMillibars() {
    return this.millibars;
  }

  /** Returns the height of the sounding in metres. */
  public double getMetres() {
    return metres;
  }

  /** Returns the temperature of the sounding in degrees Celcius. */
  public double getTemperature() {
    return temperature;
  }

  /** Returns the dewpoint of the sounding in degrees Celcius. */
  public double getDewpoint() {
    return dewpoint;
  }

  /** Returns the direction of the wind in degrees clockwise from North. */
  public double getDirection() {
    return direction;
  }

  /** Returns the speed of the wind in knots. */
  public double getSpeed() {
    return speed;
  }

  @Override
  public String toString() {
    return millibars
        + "mb, "
        + metres
        + "m, "
        + temperature
        + " C, "
        + dewpoint
        + " C, "
        + direction
        + " degrees, "
        + speed;
  }
}
