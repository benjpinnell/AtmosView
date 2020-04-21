package ca.ubc.cs.sanchom.atmosview;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Computes and holds a set of derived data from a set of sounding data. It both interpolates the
 * sounding data and computes a set of derived variables.
 *
 * @author Sancho McCann
 */
public class DerivedData {

  // /< The interpolation resolution
  private static final int SAMPLE_STEP = 10;

  // Physical constants
  /// < Coefficient in mixing ratio computations.
  private static final double PRESSURE_COEFFICIENT = 6.1078;
  // /< Specific heat of water in J/kg/degreeC
  private static final double SPECIFIC_HEAT = 1463;
  // /< Latent heat of condensation in J/g
  private static final double LATENT_HEAT = 1800;
  private static final double KELVIN_CONVERSION = 273.15;

  private SoundingData soundingData = null;
  private ArrayList<DerivedPoint> derivedData = null;

  /*
   * Singularly derived values These variables are derived values that indicate a single point in
   * the vertical profile.
   */
  // /< The convective condensation level in metres
  private double ccl = Double.NaN;

  // /< The pressure level of the convective condensation level in millibars
  private double cclPressure = Double.NaN;
  // /< The lifted condensation level in metres
  private double lcl = Double.NaN;
  // /< The pressure level of the lifted condensation level in millibars
  private double lclPressure = Double.NaN;
  // /< The temperature at the lifted condensation level in degrees celcius
  private double lclTemperature = Double.NaN;
  // /< The level of free convection in metres
  private double lfc = Double.NaN;
  // /< The pressure at the level of free convection in millibars
  private double lfcPressure = Double.NaN;
  // /< The equilibriam level in metres
  private double el = Double.NaN;
  /// < The temperature in degrees celcius to which a surface parcel must be raised to
  private double convectiveTemperature = Double.NaN;
  // in order for it to rise convectively to the convective condensation level
  // / < The difference between the sampled surface temperature and the convective
  private double convectiveTemperatureRise = Double.NaN;
  // temperature
  private double cape = 0;
  private double liftedIndex = Double.NaN;
  private double kinx = Double.NaN;
  private double crossTotalsIndex = Double.NaN;
  private double verticalTotalsIndex = Double.NaN;
  private double totalTotalsIndex = Double.NaN;
  private double sweat = Double.NaN;
  private double brch = Double.NaN;

  // @}

  /**
   * Gets the saturation vapour pressure for a given temperature. The formula is an approximation to
   * Herman Wobus's polynomial.
   *
   * @param temp the temperature in degrees celcius
   * @return the vapour pressure
   */
  public static double getVapourPressure(double temp) {
    return PRESSURE_COEFFICIENT * Math.pow(10, (7.5 * (temp) / (237.3 + temp)));
  }

  /**
   * Gets the mixing ratio given a vapour pressure and the pressure level
   *
   * @param pressureLevel the pressure level of the query in millibars
   * @param vapourPressure the saturation vapour pressure at the query
   * @return the mixing ratio in grams of water vapour per kg of air
   */
  public static double getMixingRatio(double pressureLevel, double vapourPressure) {
    return ((0.62197 * vapourPressure) / (pressureLevel - vapourPressure)) * 1000;
  }

  /**
   * Gets the temperature of a parcel that is coolded/heated dry adiabatically as it moves from an
   * initial pressure level to a query pressure level.
   *
   * @param initialPressure starting pressure altitude in millibars
   * @param initialTemp starting temperature in celcius
   * @param queryPressure the pressure level of interest in millibars
   */
  public static double getDryAdiabaticCooledTemperature(
      double initialPressure, double initialTemp, double queryPressure) {
    return toCelcius(toKelvin(initialTemp) * Math.pow(queryPressure / initialPressure, 0.28571));
  }

  public static double toKelvin(double celcius) {
    return celcius + KELVIN_CONVERSION;
  }

  public static double toCelcius(double kelvin) {
    return kelvin - KELVIN_CONVERSION;
  }

  /**
   * Returns a forward difference approximation of the derivative of mixing ratio with respect to
   * temperature.
   *
   * @param p the pressure level in millibars of the estimate
   * @param t the temperature of the estimate in degrees celcius
   */
  private double getDMixingDTemp(double p, double t) {
    double differential = Math.pow(1, -10);

    double satA = getVapourPressure(t);
    double satB = getVapourPressure(t + differential);
    double mixA = getMixingRatio(p, satA);
    double mixB = getMixingRatio(p, satB);

    return (mixB - mixA) / differential;
  }

  // TODO: handle null and empty error cases
  // @SuppressWarnings("unchecked")

  /** Create derived data. */
  public DerivedData(SoundingData soundingData) {
    this.soundingData = (SoundingData) soundingData.clone();
    this.derivedData = new ArrayList<DerivedPoint>();

    DerivedPoint surfaceData = null;
    if (this.soundingData.size() > 0) {
      SoundingPoint p = this.soundingData.get(0);
      surfaceData =
          new DerivedPoint(
              p.getMetres(),
              p.getMillibars(),
              p.getTemperature(),
              p.getDewpoint(),
              p.getDirection(),
              p.getSpeed());
    } else {
      // TODO: throw exception
      return;
    }

    DerivedPoint pblAverage = getPblAverage(surfaceData, 500);

    double liftedParcelTemp = Double.NaN;
    DerivedPoint previousSample = null;

    for (int sampleHeight = (int) Math.ceil(surfaceData.getSampleHeight());
        sampleHeight < this.soundingData.get(this.soundingData.size() - 1).getMetres();
        sampleHeight += SAMPLE_STEP) {
      DerivedPoint currentSample = getInterpolation(sampleHeight);

      // If we're already tracking the lifted parcel above the lcl, update it.
      if (!Double.isNaN(lcl)) {
        double mixingDTemp = getDMixingDTemp(currentSample.getPressure(), liftedParcelTemp);

        // This is how much the parcel would have cooled if it were dry
        double cooled =
            getDryAdiabaticCooledTemperature(
                previousSample.getPressure(), liftedParcelTemp, currentSample.getPressure());
        double dalr = liftedParcelTemp - cooled;
        // This adjusts for the latent heat released during condensation since the parcel is
        // saturated
        double malr = dalr / (1 + (LATENT_HEAT / SPECIFIC_HEAT) * mixingDTemp);
        liftedParcelTemp = liftedParcelTemp - malr;
      }

      if (!Double.isNaN(lfc)
          && Double.isNaN(el)
          && liftedParcelTemp <= currentSample.getTemperature()) {
        el = currentSample.getSampleHeight();
      }

      // If we've found the LFC, but not the EL, accumulate the CAPE
      if (!Double.isNaN(lfc)
          && Double.isNaN(el)
          && liftedParcelTemp > currentSample.getTemperature()) {
        cape +=
            9.8
                * SAMPLE_STEP
                * (toKelvin(liftedParcelTemp) - toKelvin(currentSample.getTemperature()))
                / toKelvin(currentSample.getTemperature());
      }

      // Will only pass once to set the CCL
      if (Double.isNaN(ccl)
          && pblAverage.getVapourPressure() >= getVapourPressure(currentSample.getTemperature())) {
        ccl = currentSample.getSampleHeight();
        cclPressure = currentSample.getPressure();

        convectiveTemperature =
            getDryAdiabaticCooledTemperature(
                currentSample.getPressure(), pblAverage.getTemperature(), pblAverage.getPressure());
        convectiveTemperatureRise =
            Math.max(convectiveTemperature - pblAverage.getTemperature(), 0);
      }

      // Will only get to inner loop once, to set the LCL
      if (Double.isNaN(lcl)) {
        liftedParcelTemp =
            getDryAdiabaticCooledTemperature(
                pblAverage.getPressure(), pblAverage.getTemperature(), currentSample.getPressure());
        double forcedAdiabaticSaturationPressure = getVapourPressure(liftedParcelTemp);

        if (pblAverage.getVapourPressure() >= forcedAdiabaticSaturationPressure) {
          lcl = sampleHeight;
          lclPressure = currentSample.getPressure();
          lclTemperature = currentSample.getTemperature();
        }
      }

      currentSample.setLiftedParcelTemp(liftedParcelTemp);

      // Now, if we haven't found the LFC, but we're tracking the lifted temperature (above the
      // LCL),
      // check to see if this is the LFC
      if (Double.isNaN(lfc)
          && !Double.isNaN(lcl)
          && currentSample.getLiftedParcelTemp() > currentSample.getTemperature()) {
        lfc = currentSample.getSampleHeight();
        lfcPressure = currentSample.getPressure();
      }

      previousSample = currentSample;
      this.derivedData.add(currentSample);
    }

    // get some indices
    DerivedPoint data500 = getDataFromPressureLevel(500);
    DerivedPoint data700 = getDataFromPressureLevel(700);
    DerivedPoint data850 = getDataFromPressureLevel(850);

    liftedIndex = data500.getTemperature() - data500.getLiftedParcelTemp();
    kinx =
        (data850.getTemperature() - data500.getTemperature())
            + data850.getDewpoint()
            - (data700.getTemperature() - data700.getDewpoint());
    crossTotalsIndex = data850.getDewpoint() - data500.getTemperature();
    verticalTotalsIndex = data850.getTemperature() - data500.getTemperature();
    totalTotalsIndex = crossTotalsIndex + verticalTotalsIndex;
    sweat =
        12 * data850.getTemperature()
            + 20 * Math.max(totalTotalsIndex - 49, 0)
            + 2 * data850.getSpeed()
            + data500.getSpeed()
            + 125
                * (Math.sin(
                        Math.toRadians(data500.getDirection())
                            - Math.toRadians(data850.getDirection()))
                    + 0.2);

    double u1Accumulator = 0;
    double v1Accumulator = 0;
    double numAccumulated = 0;
    for (int i = 0; i <= 500; i += 100) {
      DerivedPoint d = getDataFromHeight(surfaceData.getSampleHeight() + i);
      u1Accumulator += d.getSpeed() * Math.cos(Math.toRadians(d.getDirection()));
      v1Accumulator += d.getSpeed() * Math.sin(Math.toRadians(d.getDirection()));

      numAccumulated++;
    }
    double u1 = u1Accumulator / (float) numAccumulated;
    double v1 = v1Accumulator / (float) numAccumulated;

    double u2Accumulator = 0;
    double v2Accumulator = 0;
    numAccumulated = 0;
    for (int i = 0; i <= 500; i += 6000) {
      DerivedPoint d = getDataFromHeight(surfaceData.getSampleHeight() + i);
      u2Accumulator += d.getSpeed() * Math.cos(Math.toRadians(d.getDirection()));
      v2Accumulator += d.getSpeed() * Math.sin(Math.toRadians(d.getDirection()));

      numAccumulated++;
    }
    double u2 = u2Accumulator / (float) numAccumulated;
    double v2 = v2Accumulator / (float) numAccumulated;

    brch = cape / (0.5 * ((u2 - u1) * (u2 - u1) + (v2 - v1) * (v2 - v1)));
  }

  /** Return the data for the specified height. */
  public DerivedPoint getDataFromHeight(double targetHeight) {
    DerivedPoint closestPoint = null;

    int bottomIndex = 0;
    int topIndex = this.derivedData.size() - 1;

    while (closestPoint == null && bottomIndex + 1 < topIndex) {

      int midIndex = (bottomIndex + topIndex) / 2;
      double midHeight = this.derivedData.get(midIndex).getSampleHeight();

      if (targetHeight == midHeight) {
        closestPoint = this.derivedData.get(midIndex);
      }

      // the highest millibars are stored closer to the zero index
      if (targetHeight > midHeight) {
        bottomIndex = midIndex;
      } else {
        topIndex = midIndex;
      }
    }

    if (bottomIndex + 1 >= topIndex) {
      closestPoint = this.derivedData.get(bottomIndex);
    }

    return closestPoint;
  }

  /** Return the data for the specified pressure level. */
  public DerivedPoint getDataFromPressureLevel(double targetMillibars) {
    DerivedPoint closestPoint = null;

    int bottomIndex = 0;
    int topIndex = this.derivedData.size() - 1;

    while (closestPoint == null && bottomIndex + 1 < topIndex) {

      int midIndex = (bottomIndex + topIndex) / 2;
      double midPressure = this.derivedData.get(midIndex).getPressure();

      if (targetMillibars == midPressure) {
        closestPoint = this.derivedData.get(midIndex);
      }

      // the highest millibars are stored closer to the zero index
      if (targetMillibars > midPressure) {
        topIndex = midIndex;
      } else {
        bottomIndex = midIndex;
      }
    }

    if (bottomIndex + 1 >= topIndex) {
      closestPoint = this.derivedData.get(bottomIndex);
    }

    return closestPoint;
  }

  private DerivedPoint getPblAverage(DerivedPoint baseData, int windowHeight) {
    int baseHeight = (int) Math.ceil(baseData.getSampleHeight());

    DerivedPoint accumulator = new DerivedPoint(0, 0, 0, 0, 0, 0);

    int numSamples = 0;
    for (int sampleHeight = baseHeight;
        sampleHeight < baseHeight + windowHeight;
        sampleHeight += SAMPLE_STEP) {
      DerivedPoint currentSample = getInterpolation(sampleHeight);

      accumulator.setSampleHeight(accumulator.getSampleHeight() + currentSample.getSampleHeight());
      accumulator.setPressure(accumulator.getPressure() + currentSample.getPressure());
      accumulator.setTemperature(accumulator.getTemperature() + currentSample.getTemperature());
      accumulator.setDewpoint(accumulator.getDewpoint() + currentSample.getDewpoint());

      numSamples++;
    }

    // Do the average
    accumulator.setSampleHeight(accumulator.getSampleHeight() / numSamples);
    accumulator.setPressure(accumulator.getPressure() / numSamples);
    accumulator.setTemperature(accumulator.getTemperature() / numSamples);
    accumulator.setDewpoint(accumulator.getDewpoint() / numSamples);

    return accumulator;
  }

  @SuppressWarnings("unchecked")
  private DerivedPoint getInterpolation(int sampleHeight) {
    SoundingPoint dummy =
        new SoundingPoint(Double.NaN, sampleHeight, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    int index =
        Collections.<SoundingPoint>binarySearch(this.soundingData, dummy, (a, b) -> a.compareTo(b));
    double interpolatedTemp;
    double interpolatedDew;
    double interpolatedPressure;
    double interpolatedDirection;
    double interpolatedSpeed;
    if (index >= 0) {
      // No interpolation needed
      SoundingPoint a = this.soundingData.get(index);
      interpolatedTemp = a.getTemperature();
      interpolatedDew = a.getDewpoint();
      interpolatedPressure = a.getMillibars();
      interpolatedDirection = a.getDirection();
      interpolatedSpeed = a.getSpeed();
    } else {
      // Interpolation needed
      int insertPoint = -index - 1;
      // Guaranteed to be in bounds by construction of the loop
      SoundingPoint a = this.soundingData.get(insertPoint - 1);
      SoundingPoint b = this.soundingData.get(insertPoint);

      double diff = Math.abs(b.getMetres() - a.getMetres());
      double weightA = 1 - Math.abs(sampleHeight - a.getMetres()) / diff;
      double weightB = 1 - weightA;

      interpolatedTemp = weightA * a.getTemperature() + weightB * b.getTemperature();
      interpolatedDew = weightA * a.getDewpoint() + weightB * b.getDewpoint();
      interpolatedPressure = weightA * a.getMillibars() + weightB * b.getMillibars();

      // Interpolate direction: This isn't necessarily valid to do

      double smallestDirection;
      double largestDirection;
      double smallWeight;

      if (a.getDirection() < b.getDirection()) {
        smallestDirection = a.getDirection();
        largestDirection = b.getDirection();
        smallWeight = weightA;
      } else {
        smallestDirection = b.getDirection();
        largestDirection = a.getDirection();
        smallWeight = weightB;
      }

      double cwFromLargest = smallestDirection - largestDirection;
      if (cwFromLargest < 0) {
        cwFromLargest += 360;
      }

      double cwFromSmallest = largestDirection - smallestDirection;

      if (cwFromLargest < cwFromSmallest) {
        interpolatedDirection = smallWeight * cwFromLargest + largestDirection;
        if (interpolatedDirection > 360) {
          interpolatedDirection -= 360;
        }
      } else {
        interpolatedDirection = (1 - smallWeight) * cwFromSmallest + smallestDirection;
        if (interpolatedDirection > 360) {
          interpolatedDirection -= 360;
        }
      }

      interpolatedSpeed = weightA * a.getSpeed() + weightB * b.getSpeed();
    }
    DerivedPoint d =
        new DerivedPoint(
            sampleHeight,
            interpolatedPressure,
            interpolatedTemp,
            interpolatedDew,
            interpolatedDirection,
            interpolatedSpeed);

    return d;
  }

  public DerivedPoint get(int index) {
    return this.derivedData.get(index);
  }

  public double getLcl() {
    return lcl;
  }

  public double getCcl() {
    return ccl;
  }

  public double getLfc() {
    return lfc;
  }

  public double getEl() {
    return el;
  }

  public double getLiftedIndex() {
    return liftedIndex;
  }

  public double getKinx() {
    return kinx;
  }

  public double getCrossTotalsIndex() {
    return crossTotalsIndex;
  }

  public double getTotalTotalsIndex() {
    return totalTotalsIndex;
  }

  public double getVerticalTotalsIndex() {
    return verticalTotalsIndex;
  }

  public double getSweat() {
    return sweat;
  }

  public double getBrch() {
    return brch;
  }

  public double getConvectiveTemperatureRise() {
    return convectiveTemperatureRise;
  }

  public double getCape() {
    return cape;
  }

  public int size() {
    return this.derivedData.size();
  }

  /** Gets the altitude of the lowest sample in metres. */
  public double minHeight() {
    return this.derivedData.get(0).getSampleHeight();
  }

  /** Gets the altitude of the highest sample in metres. */
  public double maxHeight() {
    return this.derivedData.get(this.derivedData.size() - 1).getSampleHeight();
  }

  public double getSampleStep() {
    return SAMPLE_STEP;
  }

  public ArrayList<DerivedPoint> getList() {
    return this.derivedData;
  }

  @Override
  public String toString() {
    String outString = "Derived Data\n";

    outString += "CCL: " + ccl + "\n";
    outString += "CCL_pressure: " + cclPressure + "\n";
    outString += "CT: " + convectiveTemperature + "\n";
    outString += "LCL: " + lcl + "\n";
    outString += "LCL_pressure: " + lclPressure + "\n";
    outString += "LCL_temperature: " + (toKelvin(lclTemperature)) + "k\n";
    outString += "LFC: " + lfc + "\n";
    outString += "LFC_pressure: " + lfcPressure + "\n";

    return outString;
  }
}
