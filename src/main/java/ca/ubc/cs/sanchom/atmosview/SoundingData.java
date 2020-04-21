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
  private static final long serialVersionUID = 1L;

  private GregorianCalendar m_date_time; // /< The time of this SoundingData

  private String m_stationName; // /< The station name from which this SoundingData's data came

  /**
   * Constructor
   *
   * @param date_time the time of the sounding
   */
  public SoundingData(GregorianCalendar date_time) {
    m_date_time = (GregorianCalendar) date_time.clone();
    m_stationName = "Unknown location";
  }

  /** returns the time of the sounding */
  public GregorianCalendar getTime() {
    return (GregorianCalendar) m_date_time.clone();
  }

  /**
   * Sets the station name for this sounding
   *
   * @param stationName the name of the station from which this SoundingData's data came
   */
  public void setStationName(String stationName) {
    m_stationName = stationName;
  }

  /** Returns the station name */
  public String getStationName() {
    return m_stationName;
  }

  /**
   * Returns a human readable string of the time of the sounding. The format is: "YYYY-M(M)-D(D)
   * H(H) UTC"
   */
  public String timeString() {
    return ""
        + m_date_time.get(GregorianCalendar.YEAR)
        + "-"
        + (m_date_time.get(GregorianCalendar.MONTH) + 1)
        + "-"
        + m_date_time.get(GregorianCalendar.DATE)
        + " "
        + m_date_time.get(GregorianCalendar.HOUR_OF_DAY)
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
    copy.m_date_time = (GregorianCalendar) m_date_time.clone();

    return copy;
  }
}
