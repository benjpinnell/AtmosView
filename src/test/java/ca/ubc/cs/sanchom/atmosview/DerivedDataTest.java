package ca.ubc.cs.sanchom.atmosview;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.GregorianCalendar;
import org.junit.jupiter.api.Test;

public class DerivedDataTest {

  @Test
  public void testConstructor() {
    try {
      // Port Hardy, Jan 1 00:00UTC
      SoundingData s1 =
          SoundingFetcher.getInstance()
              .getSounding("71109", new GregorianCalendar(2006, 0, 1, 0, 0));
      DerivedData d1 = new DerivedData(s1);

      System.out.println(d1);

      // Dallas Fort-Worth, Jan 1 00:00UTC
      SoundingData s2 =
          SoundingFetcher.getInstance()
              .getSounding("72249", new GregorianCalendar(2006, 0, 1, 0, 0));
      DerivedData d2 = new DerivedData(s2);

      System.out.println(d2);

      // Churchill, Dec 1 12:00UTC
      SoundingData s3 =
          SoundingFetcher.getInstance()
              .getSounding("71913", new GregorianCalendar(2006, 11, 1, 12, 0));
      DerivedData d3 = new DerivedData(s3);

      System.out.println(d3);
    } catch (IOException e) {
      System.err.println(e);
      fail();
    }
  }

  @Test
  public void testGetVapourPressure() {
    double errorAllowed = 0.05;
    assertTrue(
        Math.abs(0.06356 - DerivedData.getVapourPressure(-50)) < errorAllowed,
        "Vapour pressure incorrect");
    assertTrue(
        Math.abs(2.8627 - DerivedData.getVapourPressure(-10)) < errorAllowed,
        "Vapour pressure incorrect");
    assertTrue(
        Math.abs(42.430 - DerivedData.getVapourPressure(30)) < errorAllowed,
        "Vapour pressure incorrect");
  }

  @Test
  public void testGetMixingRatio() {
    double mr = 0;

    // Test values from the Emagram	chart
    mr = DerivedData.getMixingRatio(1000, DerivedData.getVapourPressure(-40));
    assertTrue(0.1 < mr && mr < 0.15, "Mixing ratio incorrect");
    mr = DerivedData.getMixingRatio(1000, DerivedData.getVapourPressure(0));
    assertTrue(3.5 < mr && mr < 4.0, "Mixing ratio incorrect");
    mr = DerivedData.getMixingRatio(1000, DerivedData.getVapourPressure(30));
    assertTrue(25.0 < mr && mr < 30.0, "Mixing ratio incorrect");
    mr = DerivedData.getMixingRatio(400, DerivedData.getVapourPressure(-40));
    assertTrue(0.3 < mr && mr < 0.35, "Mixing ratio incorrect");
    mr = DerivedData.getMixingRatio(400, DerivedData.getVapourPressure(0));
    assertTrue(9.0 < mr && mr < 10.0, "Mixing ratio incorrect");
    mr = DerivedData.getMixingRatio(400, DerivedData.getVapourPressure(30));
    assertTrue(70.0 < mr && mr < 80.0, "Mixing ratio incorrect");
  }

  @Test
  public void testDALR() {
    double allowedError = 0.5; // one degree celcius
    double t = 0;

    t = DerivedData.getDryAdiabaticCooledTemperature(1000, 20, 500);
    assertTrue(Math.abs(-32.5 - t) < allowedError, "DALR incorrect");
  }
}
