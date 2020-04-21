package ca.ubc.cs.sanchom.atmosview;

/**
 * This class holds the derived/interpolated data points. There is derived data not held as points
 * in this class, however. This only holds data that is meaningful for each sample point. Derived
 * data that is simply an altitude like convective condensation level is not held in this structure.
 *
 * @author Sancho McCann
 */
public class DerivedPoint {

  /// < Threshold for the indication of stratus cloud given humidity.
  private static final double STRATUS_RH_THRESHOLD = 99;

  private double temperature; // /< The temperature of this interpolation in degrees celcius
  private double dewpoint; // /< The dewpoint of the interpolation
  private double pressure; // /< The pressure in millibars of this interpolation
  private double sampleHeight; // /< The height of the interpolation in metres.
  private double direction; // /<The wind direction in degree clockwise from north
  private double speed; // /<The wind speed in nautical miles per hour
  private double temperatureDewpointSpread; // /< The temperature-dewpoint difference
  private double
      relativeHumidity; /// < The relative humidity calculated from the temperature and dewpoint
  private double
      vapourPressure; /// < The vapour pressure actually existant in the air at this point
  private double mixingRatio; // /< The mixing ratio at this point in g/kg
  private boolean
      stratusCloud; /// < Boolean flag indicating if the relative humidity is such to expect cloud.
  private double
      liftedParcelTemp; /// < The temperature of a parcel of air cooled adiabatically from the LCL
  // upward
  private double
      liftedDiff; /// < The difference between the environmental temperature and the lifted parcel
  // temperature.

  /** Create derived point from metrics. */
  public DerivedPoint(
      double height,
      double pressure,
      double temperature,
      double dewpoint,
      double direction,
      double speed) {
    sampleHeight = height;
    this.pressure = pressure;
    this.temperature = temperature;
    this.dewpoint = dewpoint;
    this.direction = direction;
    this.speed = speed;
    redoDerivation();
  }

  /** Create dervied point from another derived point. */
  public DerivedPoint(DerivedPoint that) {
    this.sampleHeight = that.sampleHeight;
    this.pressure = that.pressure;
    this.temperature = that.pressure;
    this.dewpoint = that.dewpoint;
    this.direction = that.direction;
    this.speed = that.speed;
    redoDerivation();
  }

  public double getVapourPressure() {
    return vapourPressure;
  }

  public double getMixingRatio() {
    return mixingRatio;
  }

  public double getLiftedDiff() {
    return liftedDiff;
  }

  public double getSpread() {
    return temperatureDewpointSpread;
  }

  public double getDewpoint() {
    return dewpoint;
  }

  public void setDewpoint(double dewpoint) {
    this.dewpoint = dewpoint;
    redoDerivation();
  }

  public double getPressure() {
    return pressure;
  }

  public void setPressure(double pressure) {
    this.pressure = pressure;
  }

  public double getRelativeHumidity() {
    return relativeHumidity;
  }

  public double getSampleHeight() {
    return sampleHeight;
  }

  public void setSampleHeight(double sampleHeight) {
    this.sampleHeight = sampleHeight;
  }

  public double getTemperature() {
    return temperature;
  }

  public void setTemperature(double temperature) {
    this.temperature = temperature;
    liftedDiff = liftedParcelTemp - temperature;
    redoDerivation();
  }

  public double getLiftedParcelTemp() {
    return liftedParcelTemp;
  }

  public void setLiftedParcelTemp(double liftedParcelTemp) {
    this.liftedParcelTemp = liftedParcelTemp;
    liftedDiff = liftedParcelTemp - temperature;
  }

  public double getDirection() {
    return direction;
  }

  public void setDirection(double direction) {
    this.direction = direction;
  }

  public double getSpeed() {
    return speed;
  }

  public void setSpeed(double speed) {
    this.speed = speed;
  }

  public boolean isStratusCloud() {
    return stratusCloud;
  }

  private void redoDerivation() {
    temperatureDewpointSpread = temperature - dewpoint;

    // Calculate relative humidity
    // Formula from http://www.aprweather.com/pages/calc.htm

    double actual = DerivedData.getVapourPressure(dewpoint);
    double sat = DerivedData.getVapourPressure(temperature);

    relativeHumidity = 100 * actual / sat;
    stratusCloud = relativeHumidity >= STRATUS_RH_THRESHOLD;
    vapourPressure = actual;
    mixingRatio = DerivedData.getMixingRatio(pressure, vapourPressure);
  }

  /**
   * Compares two DerivedPoints for sorting purposes.
   *
   * @param obj The object to compare this DerivedPoint with.
   * @return 1 if this DerivedPoint is at a higher altitude than obj
   */
  public int compareTo(Object obj) {
    if (!(obj instanceof DerivedPoint)) {
      throw new ClassCastException(
          "Passed object cannot be compared with object of type DerivedPoint");
    }

    DerivedPoint that = (DerivedPoint) obj;
    if (this.sampleHeight == that.sampleHeight) {
      return 0;
    }

    return (this.sampleHeight < that.sampleHeight ? -1 : 1);
  }

  @Override
  public String toString() {
    return "pressure: "
        + pressure
        + "\theight: "
        + sampleHeight
        + "\tT: "
        + temperature
        + "\tD: "
        + dewpoint
        + "\tLT: "
        + liftedParcelTemp
        + "\tDiff: "
        + liftedDiff;
  }
}
