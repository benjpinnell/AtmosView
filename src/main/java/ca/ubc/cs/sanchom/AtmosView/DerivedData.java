package ca.ubc.cs.sanchom.AtmosView;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


/**
 * Computes and holds a set of derived data from a set of sounding data.
 * It both interpolates the sounding data and computes a set of derived variables.
 * @author Sancho McCann
 *
 */

public class DerivedData {

	private final static int SAMPLE_STEP = 10; ///< The interpolation resolution

	/**
	 * @name Physical constants
	 */
	//@{
	private final static double PRESSURE_COEFFICIENT = 6.1078; ///< Coefficient in mixing ratio computations.
	//private final static double GAS_CONSTANT = 461;
	//private final static double SPECIFIC_HEAT = 1004; ///< Specific heat of water in J/kg/degreeC
	private final static double SPECIFIC_HEAT = 1463; ///< Specific heat of water in J/kg/degreeC
	private final static double LATENT_HEAT = 1800; ///< Latent heat of condensation in J/g
	private final static double KELVIN_CONVERSION = 273.15;
	//@}

	private SoundingData m_soundingData = null;
	private ArrayList<DerivedPoint> m_derivedData = null;

	/**
	 * @name Singularly derived values
	 * These variables are derived values that indicate a single point in the vertical profile.
	 */
	//@{
	private double CCL = Double.NaN; ///< The convective condensation level in metres
	private double CCL_pressure = Double.NaN; ///< The pressure level of the convective condensation level in millibars
	private double LCL = Double.NaN; ///< The lifted condensation level in metres
	private double LCL_pressure = Double.NaN; ///< The pressure level of the lifted condensation level in millibars
	private double LCL_temperature = Double.NaN; ///< The temperature at the lifted condensation level in degrees celcius
	private double LFC = Double.NaN; ///< The level of free convection in metres
	private double LFC_pressure = Double.NaN; ///< The pressure at the level of free convection in millibars
	private double EL = Double.NaN; ///< The equilibriam level in metres
	private double convectiveTemperature = Double.NaN; ///< The temperature in degrees celcius to which a surface parcel must be raised to in order for it to rise convectively to the convective condensation level
	private double convectiveTemperatureRise = Double.NaN; ///< The difference between the sampled surface temperature and the convective temperature
	private double CAPE = 0;
	private double LIFTED_INDEX = Double.NaN;
	private double KINX = Double.NaN;
	private double CROSS_TOTALS_INDEX = Double.NaN;
	private double VERTICAL_TOTALS_INDEX = Double.NaN;
	private double TOTAL_TOTALS_INDEX = Double.NaN;
	private double SWEAT = Double.NaN;
	private double BRCH = Double.NaN;

	//@}

	/**
	 * Gets the saturation vapour pressure for a given temperature. The formula
	 * is an approximation to Herman Wobus's polynomial.
	 * @param temp the temperature in degrees celcius
	 * @return the vapour pressure
	 */
	public static double getVapourPressure(double temp)
	{
		return PRESSURE_COEFFICIENT * Math.pow(10,( 7.5*(temp) / (237.3+temp) ) );
	}

	/**
	 * Gets the mixing ratio given a vapour pressure and the pressure level
	 * @param pressureLevel the pressure level of the query in millibars
	 * @param vapourPressure the saturation vapour pressure at the query
	 * @return the mixing ratio in grams of water vapour per kg of air
	 */
	public static double getMixingRatio(double pressureLevel, double vapourPressure)
	{
		return ((0.62197*vapourPressure)/(pressureLevel-vapourPressure))*1000;
	}

	/**
	 * Gets the temperature of a parcel that is coolded/heated dry adiabatically as it moves from an initial pressure level to a query pressure level.
	 * @param initialPressure starting pressure altitude in millibars
	 * @param initialTemp starting temperature in celcius
	 * @param queryPressure the pressure level of interest in millibars
	 */
	public static double getDryAdiabaticCooledTemperature(double initialPressure, double initialTemp, double queryPressure)
	{
		return toCelcius(toKelvin(initialTemp) * Math.pow(queryPressure / initialPressure, 0.28571));
	}
//
//	public static double getDryAdiabaticCooledTemperature_KM(double initialHeight, double initialTemp, double queryHeight)
//	{
//		return initialTemp - 9.8 * (queryHeight - initialHeight) / 1000f;
//	}
//
	public static double toKelvin(double celcius)
	{
		return celcius + KELVIN_CONVERSION;
	}

	public static double toCelcius(double kelvin)
	{
		return kelvin - KELVIN_CONVERSION;
	}

	/**
	 * Returns a forward difference approximation of the derivative of mixing ratio with respect to temperature
	 * @param p the pressure level in millibars of the estimate
	 * @param t the temperature of the estimate in degrees celcius
	 */
	private double getDMixingDTemp(double p, double t)
	{
		double differential = Math.pow(1,-10);

		double e_sat_a = getVapourPressure(t);
		double e_sat_b = getVapourPressure(t+differential);
		double mix_a = getMixingRatio(p, e_sat_a);
		double mix_b = getMixingRatio(p, e_sat_b);

		return (mix_b - mix_a) / differential;
	}

	//TODO: handle null and empty error cases
	@SuppressWarnings("unchecked")
	public DerivedData(SoundingData soundingData)
	{
		m_soundingData = (SoundingData)soundingData.clone();
		m_derivedData = new ArrayList<DerivedPoint>();

		DerivedPoint surfaceData = null;
		if (m_soundingData.size() > 0)
		{
			SoundingPoint p = m_soundingData.get(0);
			surfaceData = new DerivedPoint(p.getMetres(), p.getMillibars(), p.getTemperature(), p.getDewpoint(), p.getDirection(), p.getSpeed());
		}
		else
		{
			//TODO: throw exception
			return;
		}

		DerivedPoint pblAverage = getPblAverage(surfaceData, 500);

		double liftedParcelTemp = Double.NaN;
		DerivedPoint previousSample = null;

		for (int sampleHeight = (int)Math.ceil(surfaceData.getSampleHeight());
		sampleHeight < m_soundingData.get(m_soundingData.size()-1).getMetres(); sampleHeight+=SAMPLE_STEP)
		{
			DerivedPoint currentSample = getInterpolation(sampleHeight);

			// If we're already tracking the lifted parcel above the LCL, update it.
			if (!Double.isNaN(LCL))
			{
				double dMixingdTemp = getDMixingDTemp(currentSample.getPressure(), liftedParcelTemp);

				// This is how much the parcel would have cooled if it were dry
				double cooled = getDryAdiabaticCooledTemperature(previousSample.getPressure(), liftedParcelTemp,currentSample.getPressure());
				double DALR = liftedParcelTemp - cooled;
				// This adjusts for the latent heat released during condensation since the parcel is saturated
				double MALR = DALR / (1+(LATENT_HEAT/SPECIFIC_HEAT)*dMixingdTemp);
				liftedParcelTemp = liftedParcelTemp - MALR;
			}

			if (!Double.isNaN(LFC) && Double.isNaN(EL) && liftedParcelTemp <= currentSample.getTemperature())
			{
				EL = currentSample.getSampleHeight();
			}

			// If we've found the LFC, but not the EL, accumulate the CAPE
			if (!Double.isNaN(LFC) && Double.isNaN(EL) && liftedParcelTemp > currentSample.getTemperature())
			{
				CAPE += 9.8 * SAMPLE_STEP * (toKelvin(liftedParcelTemp) - toKelvin(currentSample.getTemperature())) / toKelvin(currentSample.getTemperature());
			}

			// Will only pass once to set the CCL
			if (Double.isNaN(CCL) && pblAverage.getVapourPressure() >= getVapourPressure(currentSample.getTemperature()))
			{
				CCL = currentSample.getSampleHeight();
				CCL_pressure = currentSample.getPressure();

				convectiveTemperature = getDryAdiabaticCooledTemperature(currentSample.getPressure(), pblAverage.getTemperature(), pblAverage.getPressure());
				convectiveTemperatureRise = Math.max(convectiveTemperature - pblAverage.getTemperature(), 0);
			}

			// Will only get to inner loop once, to set the LCL
			if (Double.isNaN(LCL))
			{
				liftedParcelTemp = getDryAdiabaticCooledTemperature(pblAverage.getPressure(), pblAverage.getTemperature(), currentSample.getPressure());
				double forcedAdiabaticSaturationPressure = getVapourPressure(liftedParcelTemp);

				if (pblAverage.getVapourPressure() >= forcedAdiabaticSaturationPressure)
				{
					LCL = sampleHeight;
					LCL_pressure = currentSample.getPressure();
					LCL_temperature = currentSample.getTemperature();
				}
			}

			currentSample.setLiftedParcelTemp(liftedParcelTemp);

			// Now, if we haven't found the LFC, but we're tracking the lifted temperature (above the LCL),
			// check to see if this is the LFC
			if (Double.isNaN(LFC) && !Double.isNaN(LCL) && currentSample.getLiftedParcelTemp() > currentSample.getTemperature())
			{
				LFC = currentSample.getSampleHeight();
				LFC_pressure = currentSample.getPressure();
			}

			previousSample = currentSample;
			m_derivedData.add(currentSample);
		}

		// get some indices
		DerivedPoint data500 = getDataFromPressureLevel(500);
		DerivedPoint data700 = getDataFromPressureLevel(700);
		DerivedPoint data850 = getDataFromPressureLevel(850);

		LIFTED_INDEX = data500.getTemperature() - data500.getLiftedParcelTemp();
		KINX = (data850.getTemperature() - data500.getTemperature()) + data850.getDewpoint() -(data700.getTemperature() - data700.getDewpoint());
		CROSS_TOTALS_INDEX = data850.getDewpoint() - data500.getTemperature();
		VERTICAL_TOTALS_INDEX = data850.getTemperature() - data500.getTemperature();
		TOTAL_TOTALS_INDEX = CROSS_TOTALS_INDEX + VERTICAL_TOTALS_INDEX;
		SWEAT = 12 * data850.getTemperature() + 20 * Math.max(TOTAL_TOTALS_INDEX - 49, 0) + 2 * data850.getSpeed() + data500.getSpeed() + 125 * (Math.sin( Math.toRadians(data500.getDirection()) - Math.toRadians(data850.getDirection()) ) + 0.2);


		double u1Accumulator = 0;
		double v1Accumulator = 0;
		double numAccumulated = 0;
		for (int i = 0; i <= 500; i += 100)
		{
			DerivedPoint d = getDataFromHeight(surfaceData.getSampleHeight() + i);
			u1Accumulator += d.getSpeed() * Math.cos(Math.toRadians(d.getDirection()));
			v1Accumulator += d.getSpeed() * Math.sin(Math.toRadians(d.getDirection()));

			numAccumulated++;
		}
		double u1 = u1Accumulator / (float)numAccumulated;
		double v1 = v1Accumulator / (float)numAccumulated;

		double u2Accumulator = 0;
		double v2Accumulator = 0;
		numAccumulated = 0;
		for (int i = 0; i <= 500; i += 6000)
		{
			DerivedPoint d = getDataFromHeight(surfaceData.getSampleHeight() + i);
			u2Accumulator += d.getSpeed() * Math.cos(Math.toRadians(d.getDirection()));
			v2Accumulator += d.getSpeed() * Math.sin(Math.toRadians(d.getDirection()));

			numAccumulated++;
		}
		double u2 = u2Accumulator / (float)numAccumulated;
		double v2 = v2Accumulator / (float)numAccumulated;

		BRCH = CAPE / (0.5 * ((u2 - u1) * (u2 - u1) + (v2 - v1) * (v2 - v1)));

	}

	public DerivedPoint getDataFromHeight(double targetHeight)
	{
		DerivedPoint closestPoint = null;

		int bottomIndex = 0;
		int topIndex = m_derivedData.size() - 1;

		while (closestPoint == null && bottomIndex + 1 < topIndex)
		{

			int midIndex = (bottomIndex + topIndex) / 2;
			double midHeight = m_derivedData.get(midIndex).getSampleHeight();

			if (targetHeight == midHeight)
			{
				closestPoint = m_derivedData.get(midIndex);
			}

			// the highest millibars are stored closer to the zero index
			if (targetHeight > midHeight)
			{
				bottomIndex = midIndex;
			}
			else
			{
				topIndex = midIndex;
			}
		}

		if (bottomIndex + 1 >= topIndex)
		{
			closestPoint = m_derivedData.get(bottomIndex);
		}

		return closestPoint;
	}

	public DerivedPoint getDataFromPressureLevel(double targetMillibars)
	{
		DerivedPoint closestPoint = null;

		int bottomIndex = 0;
		int topIndex = m_derivedData.size() - 1;

		while (closestPoint == null && bottomIndex + 1 < topIndex)
		{

			int midIndex = (bottomIndex + topIndex) / 2;
			double midPressure = m_derivedData.get(midIndex).getPressure();

			if (targetMillibars == midPressure)
			{
				closestPoint = m_derivedData.get(midIndex);
			}

			// the highest millibars are stored closer to the zero index
			if (targetMillibars > midPressure)
			{
				topIndex = midIndex;
			}
			else
			{
				bottomIndex = midIndex;
			}
		}

		if (bottomIndex + 1 >= topIndex)
		{
			closestPoint = m_derivedData.get(bottomIndex);
		}

		return closestPoint;
	}

	private DerivedPoint getPblAverage(DerivedPoint baseData, int windowHeight)
	{
		int baseHeight = (int)Math.ceil(baseData.getSampleHeight());

		DerivedPoint accumulator = new DerivedPoint(0,0,0,0,0,0);

		int numSamples = 0;
		for (int sampleHeight = baseHeight;
			sampleHeight < baseHeight + windowHeight; sampleHeight+=SAMPLE_STEP)
		{
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
	private DerivedPoint getInterpolation(int sampleHeight)
	{
		SoundingPoint dummy = new SoundingPoint(Double.NaN, sampleHeight, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
		int index = Collections.<SoundingPoint>binarySearch(m_soundingData, dummy,
        (a,b) -> a.compareTo(b));
		double interpolatedTemp;
		double interpolatedDew;
		double interpolatedPressure;
		double interpolatedDirection;
		double interpolatedSpeed;
		if ( index >= 0 ) {
			// No interpolation needed
			SoundingPoint a = m_soundingData.get(index);
			interpolatedTemp = a.getTemperature();
			interpolatedDew = a.getDewpoint();
			interpolatedPressure = a.getMillibars();
			interpolatedDirection = a.getDirection();
			interpolatedSpeed = a.getSpeed();
		}
		else {
			// Interpolation needed
			int insertPoint = -index - 1;
			// Guaranteed to be in bounds by construction of the loop
			SoundingPoint a = m_soundingData.get(insertPoint - 1);
			SoundingPoint b = m_soundingData.get(insertPoint);

			double diff = Math.abs(b.getMetres() - a.getMetres());
			double aWeight = 1 - Math.abs(sampleHeight - a.getMetres()) / diff;
			double bWeight = 1 - aWeight;

			interpolatedTemp = aWeight * a.getTemperature() + bWeight * b.getTemperature();
			interpolatedDew = aWeight * a.getDewpoint() + bWeight * b.getDewpoint();
			interpolatedPressure = aWeight * a.getMillibars() + bWeight * b.getMillibars();

			// Interpolate direction: This isn't necessarily valid to do

			double smallestDirection;
			double largestDirection;
			double smallWeight;

			if (a.getDirection() < b.getDirection())
			{
				smallestDirection = a.getDirection();
				largestDirection = b.getDirection();
				smallWeight = aWeight;
			}
			else
			{
				smallestDirection = b.getDirection();
				largestDirection = a.getDirection();
				smallWeight = bWeight;
			}

			double cwFromLargest = smallestDirection - largestDirection;
			if (cwFromLargest < 0)
			{
				cwFromLargest += 360;
			}

			double cwFromSmallest = largestDirection - smallestDirection;

			if (cwFromLargest < cwFromSmallest)
			{
				interpolatedDirection = smallWeight * cwFromLargest + largestDirection;
				if (interpolatedDirection > 360)
				{
					interpolatedDirection -= 360;
				}
			}
			else
			{
				interpolatedDirection = (1 - smallWeight) * cwFromSmallest + smallestDirection;
				if (interpolatedDirection > 360)
				{
					interpolatedDirection -= 360;
				}
			}

			interpolatedSpeed = aWeight * a.getSpeed() + bWeight * b.getSpeed();
		}
		DerivedPoint d = new DerivedPoint(sampleHeight, interpolatedPressure, interpolatedTemp, interpolatedDew, interpolatedDirection, interpolatedSpeed);

		return d;
	}

	public DerivedPoint get(int index)
	{
		return m_derivedData.get(index);
	}

	public double getLCL()
	{
		return LCL;
	}

	public double getCCL()
	{
		return CCL;
	}

	public double getLFC()
	{
		return LFC;
	}

	public double getEL() {
		return EL;
	}

	public double getLIFTED_INDEX() {
		return LIFTED_INDEX;
	}

	public double getKINX() {
		return KINX;
	}

	public double getCROSS_TOTALS_INDEX() {
		return CROSS_TOTALS_INDEX;
	}

	public double getTOTAL_TOTALS_INDEX() {
		return TOTAL_TOTALS_INDEX;
	}

	public double getVERTICAL_TOTALS_INDEX() {
		return VERTICAL_TOTALS_INDEX;
	}

	public double getSWEAT() {
		return SWEAT;
	}

	public double getBRCH() {
		return BRCH;
	}

	public double getConvectiveTemperatureRise()
	{
		return convectiveTemperatureRise;
	}

	public double getCAPE() {
		return CAPE;
	}

	public int size()
	{
		return m_derivedData.size();
	}

	/**
	 * Gets the altitude of the lowest sample in metres
	 */
	public double minHeight()
	{
		return m_derivedData.get(0).getSampleHeight();
	}


	/**
	 * Gets the altitude of the highest sample in metres
	 */
	public double maxHeight()
	{
		return m_derivedData.get(m_derivedData.size()-1).getSampleHeight();
	}

	public double getSampleStep()
	{
		return SAMPLE_STEP;
	}

	public ArrayList<DerivedPoint> getList()
	{
		return m_derivedData;
	}

	public String toString()
	{
		String outString = "Derived Data\n";

		outString += "CCL: " + CCL + "\n";
		outString += "CCL_pressure: " + CCL_pressure + "\n";
		outString += "CT: " + convectiveTemperature + "\n";
		outString += "LCL: " + LCL + "\n";
		outString += "LCL_pressure: " + LCL_pressure + "\n";
		outString += "LCL_temperature: " + ( toKelvin(LCL_temperature) ) + "k\n";
		outString += "LFC: " + LFC + "\n";
		outString += "LFC_pressure: " + LFC_pressure + "\n";

		return outString;
	}
}
