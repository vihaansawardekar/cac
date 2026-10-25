package com.example.soilmetrics;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// gets weather and NDVI
public class WeatherHelper {

    // NASA MODIS website
    private static final String NASA = "https://modis.ornl.gov/rst/api/v1/MOD13Q1";

    // download text from website
    public static String readUrl(String address) throws Exception {
        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("Accept", "application/json");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder text = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            text.append(line);
            line = reader.readLine();
        }
        reader.close();
        conn.disconnect();
        return text.toString();
    }

    // get weather from Open-Meteo
    public static String getWeatherJson(double lat, double lon) throws Exception {
        String address = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code,soil_moisture_3_to_9cm"
                + "&daily=precipitation_sum,temperature_2m_max,temperature_2m_min"
                + "&past_days=3&forecast_days=4&timezone=auto"
                + "&temperature_unit=fahrenheit&precipitation_unit=inch&wind_speed_unit=mph";
        return readUrl(address);
    }

    // weather code to words
    public static String weatherWords(int code) {
        if (code == 0) {
            return "Clear sky";
        } else if (code <= 3) {
            return "Partly cloudy";
        } else if (code == 45 || code == 48) {
            return "Foggy";
        } else if (code >= 51 && code <= 57) {
            return "Drizzle";
        } else if (code >= 61 && code <= 67) {
            return "Rain";
        } else if (code >= 71 && code <= 77) {
            return "Snow";
        } else if (code >= 80 && code <= 82) {
            return "Rain showers";
        } else if (code >= 85 && code <= 86) {
            return "Snow showers";
        } else if (code >= 95) {
            return "Thunderstorms";
        }
        return "Cloudy";
    }

    // weather code number
    public static int weatherCode(String json) throws Exception {
        JSONObject now = new JSONObject(json).getJSONObject("current");
        return now.optInt("weather_code", 3);
    }

    // picture for the weather
    public static int weatherIcon(int code) {
        if (code <= 1) {
            return R.drawable.ic_sun;
        } else if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) {
            return R.drawable.ic_rain;
        } else if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
            return R.drawable.ic_snow;
        } else if (code >= 95) {
            return R.drawable.ic_storm;
        }
        return R.drawable.ic_cloud;
    }

    // temperature like 51°F
    public static String tempText(String json) throws Exception {
        JSONObject now = new JSONObject(json).getJSONObject("current");
        return Math.round(now.optDouble("temperature_2m", 0)) + "°F";
    }

    // wind like 3 mph
    public static String windText(String json) throws Exception {
        JSONObject now = new JSONObject(json).getJSONObject("current");
        return Math.round(now.optDouble("wind_speed_10m", 0)) + " mph";
    }

    // rain in next days
    public static double rainNext3Days(String json) throws Exception {
        JSONArray rain = new JSONObject(json).getJSONObject("daily").getJSONArray("precipitation_sum");
        return rain.optDouble(4, 0) + rain.optDouble(5, 0) + rain.optDouble(6, 0);
    }

    // extra weather details
    public static String weatherText(String json) throws Exception {
        JSONObject all = new JSONObject(json);
        JSONObject now = all.getJSONObject("current");
        JSONArray highs = all.getJSONObject("daily").getJSONArray("temperature_2m_max");
        int humidity = now.optInt("relative_humidity_2m", 0);

        String text = "Humidity " + humidity + "%   •   Rain last 3 days " + String.format("%.2f", rainLast3Days(json)) + " in\n";
        text = text + "Highs next 3 days: " + Math.round(highs.optDouble(4, 0)) + "°, "
                + Math.round(highs.optDouble(5, 0)) + "°, " + Math.round(highs.optDouble(6, 0)) + "°";
        return text;
    }

    // soil water right now
    public static double soilMoisture(String json) throws Exception {
        JSONObject now = new JSONObject(json).getJSONObject("current");
        return now.optDouble("soil_moisture_3_to_9cm", 0.25);
    }

    // rain in past days
    public static double rainLast3Days(String json) throws Exception {
        JSONArray rain = new JSONObject(json).getJSONObject("daily").getJSONArray("precipitation_sum");
        return rain.optDouble(0, 0) + rain.optDouble(1, 0) + rain.optDouble(2, 0);
    }

    // moisture to words
    public static String moistureWords(double moisture) {
        if (moisture < 0.15) {
            return "Dry";
        } else if (moisture < 0.25) {
            return "Moist";
        } else if (moisture < 0.32) {
            return "Damp";
        } else if (moisture < 0.35) {
            return "Wet";
        }
        return "Very wet";
    }

    // get NDVI from NASA
    public static String getNdviJson(double lat, double lon) throws Exception {
        String place = "latitude=" + lat + "&longitude=" + lon;

        // find newest picture dates
        String datesJson = readUrl(NASA + "/dates?" + place);
        JSONArray dates = new JSONObject(datesJson).getJSONArray("dates");
        String newest = dates.getJSONObject(dates.length() - 1).getString("modis_date");
        String before = dates.getJSONObject(dates.length() - 2).getString("modis_date");

        String address = NASA + "/subset?" + place + "&band=250m_16_days_NDVI"
                + "&startDate=" + before + "&endDate=" + newest
                + "&kmAboveBelow=0&kmLeftRight=0";
        return readUrl(address);
    }

    // newest NDVI number
    public static double ndviValue(String json) throws Exception {
        JSONArray subset = new JSONObject(json).getJSONArray("subset");
        int raw = subset.getJSONObject(subset.length() - 1).getJSONArray("data").getInt(0);
        // -3000 means no data
        if (raw < -2000) {
            return -1;
        }
        return raw * 0.0001;
    }

    // extra NDVI details
    public static String ndviText(String json) throws Exception {
        JSONArray subset = new JSONObject(json).getJSONArray("subset");
        JSONObject newest = subset.getJSONObject(subset.length() - 1);
        String date = newest.getString("calendar_date");
        double ndvi = ndviValue(json);

        if (ndvi < 0) {
            return "No clear satellite picture for " + date + " (clouds).";
        }
        String text = "Picture from " + date;

        // compare with last one
        if (subset.length() > 1) {
            JSONObject older = subset.getJSONObject(0);
            int oldRaw = older.getJSONArray("data").getInt(0);
            if (oldRaw > -2000) {
                double oldNdvi = oldRaw * 0.0001;
                text = text + "\nBefore: " + String.format("%.2f", oldNdvi) + " on " + older.getString("calendar_date");
                text = text + "\nTrend:";
                if (ndvi > oldNdvi + 0.03) {
                    text = text + " plants getting greener";
                } else if (ndvi < oldNdvi - 0.03) {
                    text = text + " plants getting less green";
                } else {
                    text = text + " about the same";
                }
            }
        }
        return text;
    }

    // newest picture date
    public static String ndviDate(String json) throws Exception {
        JSONArray subset = new JSONObject(json).getJSONArray("subset");
        return subset.getJSONObject(subset.length() - 1).getString("calendar_date");
    }

    // download satellite photo
    public static byte[] getSatelliteImage(double lat, double lon) throws Exception {
        // box about 1.5 km wide
        double latSize = 0.007;
        double lonSize = 0.007 / Math.cos(Math.toRadians(lat));
        String box = (lon - lonSize) + "," + (lat - latSize) + "," + (lon + lonSize) + "," + (lat + latSize);
        String address = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/export"
                + "?bbox=" + box + "&bboxSR=4326&imageSR=3857&size=640,640&format=jpg&f=image";

        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(20000);
        InputStream input = conn.getInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int count = input.read(chunk);
        while (count != -1) {
            output.write(chunk, 0, count);
            count = input.read(chunk);
        }
        input.close();
        conn.disconnect();
        return output.toByteArray();
    }

    // NDVI to words
    public static String ndviWords(double ndvi) {
        if (ndvi < 0.2) {
            return "Mostly bare soil or harvested.";
        } else if (ndvi < 0.4) {
            return "Few plants or stressed plants.";
        } else if (ndvi < 0.6) {
            return "Plants are doing okay.";
        }
        return "Thick healthy plants.";
    }
}
