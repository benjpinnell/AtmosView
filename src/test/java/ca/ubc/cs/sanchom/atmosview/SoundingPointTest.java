package ca.ubc.cs.sanchom.atmosview;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for SoundingPoint.
 *
 * @author Sancho McCann
 */
public class SoundingPointTest {

  private final float pLevel = 700;
  private final float height = 6000;
  private final float temperature = -4;
  private final float dewpoint = -8;
  private final int windDirection = 220;
  private final int windSpeed = 19;

  private SoundingPoint testPoint =
      new SoundingPoint(pLevel, height, temperature, dewpoint, windDirection, windSpeed);

  @Test
  public void testEqualsObject() {
    SoundingPoint a = new SoundingPoint(700, 5000, 3, -4, 100, 10);
    SoundingPoint b = new SoundingPoint(700, 5000, 3, -4, 100, 10);
    assertTrue(a.equals(b), "Comparison of equal points returned false");
  }

  @Test
  public void testEqualsObjectNoWind() {
    SoundingPoint a = new SoundingPoint(700, 5000, 3, -4);
    SoundingPoint b = new SoundingPoint(700, 5000, 3, -4);
    assertTrue(a.equals(b), "Comparison of equal points returned false");
  }

  @Test
  public void testBadCompare() {
    assertThrows(
        ClassCastException.class,
        () -> {
          testPoint.compareTo(new String("bad"));
        });
  }

  @Test
  public void testConstructor() {
    @SuppressWarnings("unused")
    SoundingPoint a = new SoundingPoint(700, 5000, 3, -4);
  }

  @Test
  public void testCompareTo() {
    SoundingPoint ref = new SoundingPoint(700, 5000, 3, -4, 200, 10);
    SoundingPoint lower = new SoundingPoint(800, 4000, 5, 0, 200, 10);
    SoundingPoint same = new SoundingPoint(700, 5000, 2, -4, 200, 10);
    SoundingPoint higher = new SoundingPoint(500, 10000, -10, -15, 200, 10);

    assertTrue(lower.compareTo(ref) < 0, "didn't report lower");
    assertTrue(higher.compareTo(ref) > 0, "didn't report hight");
    assertTrue(same.compareTo(ref) == 0, "didn't report equal");
  }

  @Test
  public void testCompareToNoWind() {
    SoundingPoint ref = new SoundingPoint(700, 5000, 3, -4);
    SoundingPoint lower = new SoundingPoint(800, 4000, 5, 0);
    SoundingPoint same = new SoundingPoint(700, 5000, 2, -4);
    SoundingPoint higher = new SoundingPoint(500, 10000, -10, -15);

    assertTrue(lower.compareTo(ref) < 0, "didn't report lower");
    assertTrue(higher.compareTo(ref) > 0, "didn't report hight");
    assertTrue(same.compareTo(ref) == 0, "didn't report equal");
  }
}
