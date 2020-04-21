package ca.ubc.cs.sanchom.atmosview;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.ListIterator;

/**
 * A list of SoundingPoints. Extends an ArrayList of SoundingPoints with time information.
 *
 * @author Sancho McCann
 */
public class SoundingData extends ArrayList<SoundingPoint> {
  // /< The time of this SoundingData
  private static final long serialVersionUID = 1L;

  private GregorianCalendar dateTime;
  // /< The station name from which this SoundingData's data came
  private String stationName;

  /**
   * Constructor.
   *
   * @param dateTime the time of the sounding
   */
  public SoundingData(GregorianCalendar dateTime) {
    this.dateTime = (GregorianCalendar) dateTime.clone();
    stationName = "Unknown location";
  }

  /** Returns the time of the sounding. */
  public GregorianCalendar getTime() {
    return (GregorianCalendar) this.dateTime.clone();
  }

  /**
   * Sets the station name for this sounding.
   *
   * @param stationName the name of the station from which this SoundingData's data came
   */
  public void setStationName(String stationName) {
    this.stationName = stationName;
  }

  /** Returns the station name. */
  public String getStationName() {
    return stationName;
  }

  /**
   * Returns a human readable string of the time of the sounding. The format is: "YYYY-M(M)-D(D)
   * H(H) UTC"
   */
  public String timeString() {
    return ""
        + this.dateTime.get(GregorianCalendar.YEAR)
        + "-"
        + (this.dateTime.get(GregorianCalendar.MONTH) + 1)
        + "-"
        + this.dateTime.get(GregorianCalendar.DATE)
        + " "
        + this.dateTime.get(GregorianCalendar.HOUR_OF_DAY)
        + " UTC";
  }

  /** A nice output of the sounding time and list of SoundingPoint data. */
  public String toString() {
    String outString = new String("");

    outString = "Sounding time: " + timeString() + "\n";

    ListIterator i = this.listIterator();
    while (i.hasNext()) {
      SoundingPoint sp = (SoundingPoint) (i.next());
      outString = outString + sp + "\n";
    }

    return outString;
  }

  /** Returns a deep copy of this SoundingData object. */
  public Object clone() {
    SoundingData copy = (SoundingData) super.clone();
    copy.dateTime = (GregorianCalendar) this.dateTime.clone();

    return copy;
  }
}
