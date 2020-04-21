package ca.ubc.cs.sanchom.atmosview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.GregorianCalendar;

public class SoundingCSVParser {

  public static SoundingData getSounding(File file) {
    SoundingData data = new SoundingData(new GregorianCalendar());

    try {
      BufferedReader in = new BufferedReader(new FileReader(file));

      String inputLine = null;
      while ((inputLine = in.readLine()) != null) {
        String[] tokens = inputLine.split(",");

        if (tokens.length == 4) {
          try {
            double pressure = Double.parseDouble(tokens[0]);
            double height = Double.parseDouble(tokens[1]);
            double temperature = Double.parseDouble(tokens[2]);
            double dewpoint = Double.parseDouble(tokens[3]);

            data.add(new SoundingPoint(pressure, height, temperature, dewpoint));
          } catch (NumberFormatException e) {

          }
        }
      }
    } catch (Exception e) {

    }

    return data;
  }
}
