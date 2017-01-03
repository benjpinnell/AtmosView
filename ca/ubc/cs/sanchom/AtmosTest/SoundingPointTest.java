package ca.ubc.cs.sanchom.AtmosTest;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ca.ubc.cs.sanchom.AtmosView.SoundingPoint;

/**
 * @author Sancho McCann
 *
 */
public class SoundingPointTest {

	private final float pLevel = 700;
	private final float height = 6000;
	private final float temperature = -4;
	private final float dewpoint = -8;
	private final int windDirection = 220;
	private final int windSpeed = 19;
	
	private SoundingPoint testPoint = null;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		testPoint = new SoundingPoint(pLevel, height, temperature, dewpoint, windDirection, windSpeed);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link SoundingPoint#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		SoundingPoint a = new SoundingPoint(700, 5000, 3, -4, 100, 10);
		SoundingPoint b = new SoundingPoint(700, 5000, 3, -4, 100, 10);
		assertTrue("Comparison of equal points returned false", a.equals(b));
	}
	
	/**
	 * Test method for {@link SoundingPoint#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObjectNoWind() {
		SoundingPoint a = new SoundingPoint(700, 5000, 3, -4);
		SoundingPoint b = new SoundingPoint(700, 5000, 3, -4);
		assertTrue("Comparison of equal points returned false", a.equals(b));
	}
	
	@Test(expected= ClassCastException.class) public void testBadCompare()
	{
		testPoint.compareTo(new String("bad"));
	}
	
	/**
	 * Test method for {@link SoundingPoint#SoundingPoint(double, double, double, double)
	 */
	@Test
	public void testConstructor() {
		@SuppressWarnings("unused")
		SoundingPoint a = new SoundingPoint(700, 5000, 3, -4);
	}

	/**
	 * Test method for {@link SoundingPoint#compareTo(java.lang.Object)}.
	 */
	@Test
	public void testCompareTo() {
		SoundingPoint ref = new SoundingPoint(700, 5000, 3, -4, 200, 10);
		SoundingPoint lower = new SoundingPoint(800, 4000, 5, 0, 200, 10);
		SoundingPoint same = new SoundingPoint(700, 5000, 2, -4, 200, 10);
		SoundingPoint higher = new SoundingPoint(500, 10000, -10, -15, 200, 10);
		
		assertTrue("didn't report lower", lower.compareTo(ref) < 0);
		assertTrue("didn't report hight", higher.compareTo(ref) > 0);
		assertTrue("didn't report equal", same.compareTo(ref) == 0);
		
	}
	
	/**
	 * Test method for {@link SoundingPoint#compareTo(java.lang.Object)}.
	 */
	@Test
	public void testCompareToNoWind() {
		SoundingPoint ref = new SoundingPoint(700, 5000, 3, -4);
		SoundingPoint lower = new SoundingPoint(800, 4000, 5, 0);
		SoundingPoint same = new SoundingPoint(700, 5000, 2, -4);
		SoundingPoint higher = new SoundingPoint(500, 10000, -10, -15);
		
		assertTrue("didn't report lower", lower.compareTo(ref) < 0);
		assertTrue("didn't report hight", higher.compareTo(ref) > 0);
		assertTrue("didn't report equal", same.compareTo(ref) == 0);
		
	}

}
