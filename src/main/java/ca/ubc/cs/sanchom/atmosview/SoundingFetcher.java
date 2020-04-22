package ca.ubc.cs.sanchom.atmosview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A singleton fetcher for data from http://weather.uwyo.edu/upperair/naconf.html.
 *
 * @author Sancho McCann
 */
public class SoundingFetcher {

  // Example:
  //   <AREA COORDS="457,53,5" SHAPE="CIRCLE" HREF="javascript:g('04270')" title="04270
  // Narsarsuaq (BGBW)">
  private static final Pattern SOUNDING_STATION_REGEX =
      Pattern.compile("<AREA [^>]*\\btitle=\"([^\"]*)\".*>");

  // /< The singleton instance
  private static SoundingFetcher instance = null;

  private static TreeMap<String, String> stationMap; // /< Map of station ids to station names

  /**
   * Constructor. Connects to the data source and populates the stationMap.
   *
   * @throws an IOException if connection to server fails
   */
  private SoundingFetcher() throws IOException {
    stationMap = new TreeMap<String, String>();

    try {
      URL indexUrl = new URL("http://weather.uwyo.edu/upperair/naconf.html");
      URLConnection indexConnection = indexUrl.openConnection();
      HttpURLConnection httpConnection = (HttpURLConnection) indexConnection;
      httpConnection.setRequestMethod("GET");
      httpConnection.setDoOutput(true);
      httpConnection.connect();

      int response = httpConnection.getResponseCode();

      if (response != 200) {
        throw new IOException("Bad http response from server: " + response);
      }

      BufferedReader in =
          new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));

      String inputLine = null;

      while ((inputLine = in.readLine()) != null) {
        Matcher matcher = SOUNDING_STATION_REGEX.matcher(inputLine);
        if (matcher.find()) {
          String stationString = matcher.group(1);
          String stationID = stationString.substring(0, stationString.indexOf(" "));
          String stationName = stationString.substring(stationString.indexOf(" ") + 1);

          stationMap.put(stationName, stationID);
        }
      }

      in.close();
    } catch (MalformedURLException e) {
      // Should never happen... url is hand coded
      System.err.println(e);
    } catch (IOException e) {
      System.err.println(e);
      throw e;
    }
  }

  /**
   * Gets a reference to the singleton instance.
   *
   * @return the singleton instance
   * @throws an IOException if the constructor fails due to connection problems
   */
  public static SoundingFetcher getInstance() throws IOException {
    if (instance == null) {
      instance = new SoundingFetcher();
    }
    return instance;
  }

  /**
   * Gets the map of station ids to station names.
   *
   * @return the stationMap
   */
  public TreeMap getStationList() {
    return (TreeMap) (stationMap.clone());
  }

  /**
   * A utility function for the building of the url query string.
   *
   * @param property the name of the query string field (eg. id)
   * @param value the value of the query string field (eg. 3554)
   * @return property and value combined in a string (eg. "id=3554")
   */
  private String urlProperty(String property, String value) {
    return "&" + property + "=" + value;
  }

  /**
   * Pads an integer to a string of at least two digits. Ex. 2 -> "02"
   *
   * @param value an integer for padding
   * @return the padded integer string
   */
  private String pad(int value) {
    return (value < 10 ? "0" + value : "" + value);
  }

  /**
   * Rounds a GregorianCalendar time to the nearest 12 hours.
   *
   * @param dateTime the GregorianCalendar time to be rounded
   * @return The rounded time
   */
  private GregorianCalendar round12(GregorianCalendar dateTime) {
    int hour = dateTime.get(Calendar.HOUR_OF_DAY);
    GregorianCalendar rounded = (GregorianCalendar) dateTime.clone();
    if (hour < 6) {
      rounded.set(Calendar.HOUR_OF_DAY, 0);
    } else if (hour >= 6 && hour < 18) {
      rounded.set(Calendar.HOUR_OF_DAY, 12);
    } else {
      rounded.set(Calendar.HOUR_OF_DAY, 0);
      rounded.add(Calendar.DATE, 1);
    }
    return rounded;
  }

  /**
   * Gets a sounding. If no data is available for the requested time or station, an empty
   * SoundingData object is returned.
   *
   * @param id The station identifier
   * @param dateTime The date and time of the requested sounding in UTC. It will be rounded to the
   *     nearest twelve hours before the actual fetch.
   * @return A SoundingData object with the requested data, or an empty SoundingData object if there
   *     was none available.
   */
  public SoundingData getSounding(String id, GregorianCalendar dateTime) throws IOException {
    GregorianCalendar roundedTime = round12(dateTime);

    String urlString = "http://weather.uwyo.edu/cgi-bin/sounding?region=naconf&TYPE=TEXT%3ALIST";
    urlString += urlProperty("YEAR", (new Integer(roundedTime.get(Calendar.YEAR))).toString());
    urlString +=
        urlProperty("MONTH", (new Integer(roundedTime.get(Calendar.MONTH) + 1)).toString());
    urlString +=
        urlProperty(
            "FROM",
            pad(roundedTime.get(Calendar.DAY_OF_MONTH))
                + pad(roundedTime.get(Calendar.HOUR_OF_DAY)));
    urlString +=
        urlProperty(
            "TO",
            pad(roundedTime.get(Calendar.DAY_OF_MONTH))
                + pad(roundedTime.get(Calendar.HOUR_OF_DAY)));
    urlString += urlProperty("STNM", id);

    SoundingData soundingData = new SoundingData(roundedTime);

    try {
      URL indexUrl = new URL(urlString);
      URLConnection indexConnection = indexUrl.openConnection();
      HttpURLConnection httpConnection = (HttpURLConnection) indexConnection;
      httpConnection.setRequestMethod("GET");
      httpConnection.setDoOutput(true);
      httpConnection.connect();

      int response = httpConnection.getResponseCode();

      if (response != 200) {
        throw new IOException("Bad HTTP response from server: " + response);
      }

      // httpConnection.getInputStream();
      BufferedReader in =
          new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));

      String inputLine = null;
      boolean dataSection = false;
      boolean done = false;
      int barCount = 0;

      // wierd parsing needed for input from U of Wyoming site
      while ((inputLine = in.readLine()) != null) {
        if (inputLine.indexOf("<H2>") != -1) {
          String obs = new String("Observations");
          String t = inputLine.substring(10, inputLine.indexOf("</H2>"));
          String first = t.substring(0, t.indexOf(obs));
          String last = t.substring(t.indexOf(obs) + obs.length() + 1);
          soundingData.setStationName(first + last);
        }

        if (inputLine.toLowerCase().indexOf("------------------------") != -1) {
          barCount++;
          if (barCount == 2) {
            dataSection = true;
          }
        } else if (inputLine.toLowerCase().indexOf("</pre>") != -1) {
          dataSection = false;
          done = true;
        } else if (dataSection && !done) {
          StringTokenizer st = new StringTokenizer(inputLine);

          if (st.countTokens() == 11) {
            double pressureLevel = Double.parseDouble(st.nextToken());
            double metres = Double.parseDouble(st.nextToken());
            double temperature = Double.parseDouble(st.nextToken());
            double dewpoint = Double.parseDouble(st.nextToken());
            st.nextToken(); // ignore relative humidity
            st.nextToken(); // ignore mixing ratio
            double windDirection = Double.parseDouble(st.nextToken());
            double windSpeed = Double.parseDouble(st.nextToken());
            soundingData.add(
                new SoundingPoint(
                    pressureLevel, metres, temperature, dewpoint, windDirection, windSpeed));
          }
        }
      }

      Collections.sort(soundingData);
      in.close();

    } catch (MalformedURLException e) {
      // Shouldn't happen... URL is hand crafted
      System.err.println(e);
    } catch (IOException e) {
      System.err.println(e);
      throw e;
    }

    return soundingData;
  }
}
