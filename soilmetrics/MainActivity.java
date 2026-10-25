package com.example.soilmetrics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// the main dashboard screen
public class MainActivity extends AppCompatActivity {

    DatabaseHelper db;
    SharedPreferences prefs;
    ArrayList<Field> fields = new ArrayList<>();
    Field currentField;
    double latestOm = 3.0;

    Spinner fieldSpinner;
    TextView greetingText;
    TextView fieldInfo;
    GaugeView omGauge;
    TextView omLabel;
    TextView trendBadge;
    TextView trendText;
    GraphView omGraph;
    ImageView weatherIcon;
    TextView tempText;
    TextView conditionText;
    TextView windChip;
    TextView rainChip;
    TextView soilChip;
    TextView weatherText;
    ImageView satImage;
    TextView satLoading;
    TextView satCaption;
    TextView ndviBig;
    TextView ndviWords;
    NdviBarView ndviBar;
    TextView ndviText;
    ZoneView zoneView;
    TextView zoneText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // warm up firebase early
        new Thread(new Runnable() {
            @Override
            public void run() {
                AuthHelper.auth();
            }
        }).start();

        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        prefs = getSharedPreferences("soil", MODE_PRIVATE);

        fieldSpinner = findViewById(R.id.fieldSpinner);
        greetingText = findViewById(R.id.greetingText);
        fieldInfo = findViewById(R.id.fieldInfo);
        omGauge = findViewById(R.id.omGauge);
        omLabel = findViewById(R.id.omLabel);
        trendBadge = findViewById(R.id.trendBadge);
        trendText = findViewById(R.id.trendText);
        omGraph = findViewById(R.id.omGraph);
        weatherIcon = findViewById(R.id.weatherIcon);
        tempText = findViewById(R.id.tempText);
        conditionText = findViewById(R.id.conditionText);
        windChip = findViewById(R.id.windChip);
        rainChip = findViewById(R.id.rainChip);
        soilChip = findViewById(R.id.soilChip);
        weatherText = findViewById(R.id.weatherText);
        satImage = findViewById(R.id.satImage);
        satLoading = findViewById(R.id.satLoading);
        satCaption = findViewById(R.id.satCaption);
        ndviBig = findViewById(R.id.ndviBig);
        ndviWords = findViewById(R.id.ndviWords);
        ndviBar = findViewById(R.id.ndviBar);
        ndviText = findViewById(R.id.ndviText);
        zoneView = findViewById(R.id.zoneView);
        zoneText = findViewById(R.id.zoneText);

        // when field is picked
        fieldSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Field picked = fields.get(position);
                if (currentField == null || picked.id != currentField.id) {
                    currentField = picked;
                    prefs.edit().putInt("fieldId", currentField.id).apply();
                    showField();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // must be signed in
        if (!AuthHelper.isSignedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        showGreeting();
        loadFields();
    }

    // hello based on time
    private void showGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            greetingText.setText("Good morning, farmer");
        } else if (hour < 17) {
            greetingText.setText("Good afternoon, farmer");
        } else {
            greetingText.setText("Good evening, farmer");
        }
    }

    // fill the field list
    private void loadFields() {
        fields = db.getAllFields();
        ArrayList<String> names = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            names.add(fields.get(i).name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fieldSpinner.setAdapter(adapter);

        if (fields.size() == 0) {
            currentField = null;
            fieldInfo.setText("Tap the + button to add your first field.");
            omGauge.clear("No tests yet");
            omLabel.setText("No soil tests yet. Take a soil photo!");
            trendBadge.setText("No data");
            trendText.setText("");
            omGraph.setData(new ArrayList<Double>(), new ArrayList<String>());
            tempText.setText("--°F");
            conditionText.setText("Add a field to see weather");
            weatherText.setText("");
            ndviBig.setText("--");
            ndviWords.setText("Add a field to see satellite data");
            satImage.setImageDrawable(null);
            satLoading.setText("Add a field to see its satellite picture.");
            ndviBar.setNdvi(-1);
            ndviText.setText("");
            zoneView.setVisibility(View.INVISIBLE);
            zoneText.setText("");
            return;
        }
        zoneView.setVisibility(View.VISIBLE);

        // pick the saved field
        int savedId = prefs.getInt("fieldId", -1);
        int spot = 0;
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).id == savedId) {
                spot = i;
            }
        }
        fieldSpinner.setSelection(spot);
        currentField = fields.get(spot);
        showField();
    }

    // show everything for field
    private void showField() {
        String info = Math.round(currentField.acres) + " acres  •  " + currentField.tillage;
        if (currentField.coverCrops) {
            info = info + "  •  Cover crops";
        }
        info = info + "\n" + currentField.state + "  (" + String.format("%.3f", currentField.lat)
                + ", " + String.format("%.3f", currentField.lon) + ")";
        fieldInfo.setText(info);

        showSamples();
        // clear old field picture
        satImage.setImageDrawable(null);
        satLoading.setText("Loading satellite picture...");
        showWeather(false);
    }

    // show organic matter info
    private void showSamples() {
        ArrayList<Sample> samples = db.getSamples(currentField.id);
        ArrayList<Double> values = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        SimpleDateFormat shortDate = new SimpleDateFormat("M/d/yy", Locale.US);

        for (int i = 0; i < samples.size(); i++) {
            values.add(samples.get(i).om);
            labels.add(shortDate.format(new Date(samples.get(i).date)));
        }
        omGraph.setData(values, labels);

        if (samples.size() == 0) {
            latestOm = 3.0;
            omGauge.clear("No tests yet");
            omLabel.setText("No soil tests yet. Take a soil photo!");
            trendBadge.setText("No data");
            trendBadge.setTextColor(Color.parseColor("#415B57"));
            trendText.setText("Take soil tests over the seasons to see your trend.");
            return;
        }

        Sample latest = samples.get(samples.size() - 1);
        latestOm = latest.om;
        omGauge.setValue(latest.om, SoilModel.omRating(latest.om));
        SimpleDateFormat longDate = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        omLabel.setText("Last tested " + longDate.format(new Date(latest.date)) + "  •  " + samples.size() + " tests");
        showTrendBadge(samples);
        trendText.setText(seasonText(samples));
    }

    // improving or degrading badge
    private void showTrendBadge(ArrayList<Sample> samples) {
        trendBadge.setBackgroundResource(R.drawable.badge_back);
        if (SoilSimulator.daysCovered(samples) < 30) {
            trendBadge.setText("Need more tests");
            trendBadge.setTextColor(Color.parseColor("#415B57"));
            return;
        }
        double slope = SoilSimulator.trendPerYear(samples);
        if (slope > 0.05) {
            trendBadge.setText("Improving " + String.format("%+.2f", slope) + "%/yr");
            trendBadge.setTextColor(Color.parseColor("#415B57"));
        } else if (slope < -0.05) {
            trendBadge.setText("Degrading " + String.format("%.2f", slope) + "%/yr");
            trendBadge.setTextColor(Color.parseColor("#C9604D"));
            trendBadge.setBackgroundResource(R.drawable.badge_red);
        } else {
            trendBadge.setText("Holding steady");
            trendBadge.setTextColor(Color.parseColor("#6B807B"));
        }
    }

    // season name from date
    private String seasonName(long date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        if (month == 11) {
            return "Winter " + (year + 1);
        } else if (month <= 1) {
            return "Winter " + year;
        } else if (month <= 4) {
            return "Spring " + year;
        } else if (month <= 7) {
            return "Summer " + year;
        }
        return "Fall " + year;
    }

    // season by season text
    private String seasonText(ArrayList<Sample> samples) {
        ArrayList<String> seasons = new ArrayList<>();
        ArrayList<Double> averages = new ArrayList<>();
        String lastSeason = "";
        double sum = 0;
        int count = 0;

        for (int i = 0; i < samples.size(); i++) {
            String season = seasonName(samples.get(i).date);
            if (!season.equals(lastSeason) && count > 0) {
                seasons.add(lastSeason);
                averages.add(sum / count);
                sum = 0;
                count = 0;
            }
            lastSeason = season;
            sum = sum + samples.get(i).om;
            count++;
        }
        seasons.add(lastSeason);
        averages.add(sum / count);

        String text = "";
        for (int i = 0; i < seasons.size(); i++) {
            if (i > 0) {
                text = text + "\n";
            }
            text = text + seasons.get(i) + ":  " + String.format("%.2f%%", averages.get(i));
            if (i > 0) {
                double diff = averages.get(i) - averages.get(i - 1);
                if (diff > 0.05) {
                    text = text + "   up " + String.format("%.2f", diff);
                } else if (diff < -0.05) {
                    text = text + "   down " + String.format("%.2f", -diff);
                } else {
                    text = text + "   steady";
                }
            }
        }
        return text;
    }

    // weather and NDVI
    private void showWeather(boolean force) {
        final Field field = currentField;
        final String weatherKey = "weather_" + field.id;
        final String ndviKey = "ndvi_" + field.id;
        String savedWeather = prefs.getString(weatherKey, "");
        String savedNdvi = prefs.getString(ndviKey, "");
        long weatherTime = prefs.getLong(weatherKey + "_time", 0);
        long ndviTime = prefs.getLong(ndviKey + "_time", 0);
        long now = System.currentTimeMillis();

        // show saved data first
        if (savedWeather.equals("")) {
            conditionText.setText("Loading weather...");
            updateZones(0.2, 0, false);
        } else {
            displayWeather(savedWeather, weatherTime, false);
        }
        if (savedNdvi.equals("")) {
            ndviWords.setText("Loading satellite data...");
        } else {
            displayNdvi(savedNdvi, ndviTime, false);
        }

        // weather old after 30min
        final boolean getWeather = force || savedWeather.equals("") || now - weatherTime > 30 * 60 * 1000;
        // ndvi old after 12hrs
        final boolean getNdvi = force || savedNdvi.equals("") || now - ndviTime > 12 * 60 * 60 * 1000;

        if (!getWeather && !getNdvi) {
            return;
        }

        // internet in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (getWeather) {
                    try {
                        final String json = WeatherHelper.getWeatherJson(field.lat, field.lon);
                        final long time = System.currentTimeMillis();
                        prefs.edit().putString(weatherKey, json).putLong(weatherKey + "_time", time).apply();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentField != null && currentField.id == field.id) {
                                    displayWeather(json, time, false);
                                }
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentField != null && currentField.id == field.id) {
                                    weatherOffline(weatherKey);
                                }
                            }
                        });
                    }
                }

                if (getNdvi) {
                    try {
                        final String json = WeatherHelper.getNdviJson(field.lat, field.lon);
                        final long time = System.currentTimeMillis();
                        prefs.edit().putString(ndviKey, json).putLong(ndviKey + "_time", time).apply();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentField != null && currentField.id == field.id) {
                                    displayNdvi(json, time, false);
                                }
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (currentField != null && currentField.id == field.id) {
                                    ndviOffline(ndviKey);
                                }
                            }
                        });
                    }
                }
            }
        }).start();
    }

    // no internet for weather
    private void weatherOffline(String weatherKey) {
        String saved = prefs.getString(weatherKey, "");
        if (saved.equals("")) {
            conditionText.setText("No internet right now");
            weatherText.setText("Weather will show when you are back online.");
        } else {
            displayWeather(saved, prefs.getLong(weatherKey + "_time", 0), true);
        }
    }

    // no internet for ndvi
    private void ndviOffline(String ndviKey) {
        String saved = prefs.getString(ndviKey, "");
        if (saved.equals("")) {
            ndviWords.setText("No internet right now");
            ndviText.setText("Satellite data will show when you are back online.");
        } else {
            displayNdvi(saved, prefs.getLong(ndviKey + "_time", 0), true);
        }
    }

    // put weather on screen
    private void displayWeather(String json, long time, boolean offline) {
        try {
            int code = WeatherHelper.weatherCode(json);
            double moisture = WeatherHelper.soilMoisture(json);
            weatherIcon.setImageResource(WeatherHelper.weatherIcon(code));
            tempText.setText(WeatherHelper.tempText(json));
            conditionText.setText(WeatherHelper.weatherWords(code));
            windChip.setText(WeatherHelper.windText(json) + "\nwind");
            rainChip.setText(String.format("%.2f in", WeatherHelper.rainNext3Days(json)) + "\nrain 3 days");
            soilChip.setText(Math.round(moisture * 100) + "%\n" + WeatherHelper.moistureWords(moisture).toLowerCase() + " soil");

            String text = WeatherHelper.weatherText(json);
            text = text + "\nUpdated " + timeText(time);
            if (offline) {
                text = text + "  (offline, saved data)";
            }
            weatherText.setText(text);
            updateZones(moisture, WeatherHelper.rainLast3Days(json), true);
        } catch (Exception e) {
            conditionText.setText("Could not read the weather");
        }
    }

    // put ndvi on screen
    private void displayNdvi(String json, long time, boolean offline) {
        try {
            double ndvi = WeatherHelper.ndviValue(json);
            if (ndvi >= 0) {
                ndviBig.setText(String.format("%.2f", ndvi));
                ndviWords.setText(WeatherHelper.ndviWords(ndvi));
            } else {
                ndviBig.setText("--");
                ndviWords.setText("Cloudy picture");
            }
            ndviBar.setNdvi(ndvi);
            showSatellite(WeatherHelper.ndviDate(json));

            String text = WeatherHelper.ndviText(json);
            text = text + "\nUpdated " + timeText(time);
            if (offline) {
                text = text + "  (offline, saved data)";
            }
            ndviText.setText(text);
        } catch (Exception e) {
            ndviWords.setText("Could not read satellite data");
        }
    }

    // show NASA field picture
    private void showSatellite(final String date) {
        final Field field = currentField;
        satCaption.setText("Satellite photo of your field. The circle is the center of your field. "
                + "Plant health below is from NASA MODIS on " + date + ". Photo: Esri, Maxar, Earthstar Geographics.");

        // saved picture on phone
        final File file = new File(getFilesDir(), "photo_" + field.id + "_"
                + Math.round(field.lat * 1000) + "_" + Math.round(field.lon * 1000) + ".jpg");
        if (file.exists()) {
            satImage.setImageBitmap(BitmapFactory.decodeFile(file.getPath()));
            return;
        }

        // download it in background
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] bytes = WeatherHelper.getSatelliteImage(field.lat, field.lon);
                    FileOutputStream out = new FileOutputStream(file);
                    out.write(bytes);
                    out.close();
                    final Bitmap picture = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (currentField != null && currentField.id == field.id) {
                                satImage.setImageBitmap(picture);
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            satLoading.setText("No internet for the satellite picture.");
                        }
                    });
                }
            }
        }).start();
    }

    // color the field map
    private void updateZones(double moisture, double rain, boolean realWeather) {
        double risk = SoilSimulator.compactionToday(latestOm, currentField.tillage, moisture, rain);
        zoneView.setBaseRisk(risk);
        int high = zoneView.countHigh();

        String text = "Field risk today: " + Math.round(risk) + " (" + SoilSimulator.compactionWords(risk) + ")  •  "
                + high + " of " + (ZoneView.ROWS * ZoneView.COLS) + " zones high\n";
        if (moisture >= 0.32 || rain > 1) {
            text = text + "Soil is wet. Wait to drive heavy equipment, most of all near the gate and turn rows.";
        } else if (moisture >= 0.25) {
            text = text + "Soil is damp. Light work is okay";
            if (high > 0) {
                text = text + ", but keep heavy loads out of red zones";
            }
            text = text + ".";
        } else {
            text = text + "Soil is dry enough for field work. Keep to the same wheel tracks.";
        }
        if (!realWeather) {
            text = text + "\n(No weather yet, so this uses normal moisture.)";
        }
        zoneText.setText(text);
    }

    // time to words
    private String timeText(long time) {
        SimpleDateFormat format = new SimpleDateFormat("MMM d, h:mm a", Locale.US);
        return format.format(new Date(time));
    }

    // check field exists first
    private boolean needField() {
        if (currentField == null) {
            Toast.makeText(this, "Add a field first", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    public void openAccount(View view) {
        startActivity(new Intent(this, AccountActivity.class));
    }

    public void addField(View view) {
        startActivity(new Intent(this, AddFieldActivity.class));
    }

    public void editField(View view) {
        if (needField()) {
            return;
        }
        Intent intent = new Intent(this, AddFieldActivity.class);
        intent.putExtra("fieldId", currentField.id);
        startActivity(intent);
    }

    public void takePhoto(View view) {
        if (needField()) {
            return;
        }
        Intent intent = new Intent(this, ScanActivity.class);
        intent.putExtra("fieldId", currentField.id);
        startActivity(intent);
    }

    public void openHistory(View view) {
        if (needField()) {
            return;
        }
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra("fieldId", currentField.id);
        startActivity(intent);
    }

    public void openGrants(View view) {
        if (needField()) {
            return;
        }
        Intent intent = new Intent(this, GrantsActivity.class);
        intent.putExtra("fieldId", currentField.id);
        startActivity(intent);
    }

    public void openGait(View view) {
        startActivity(new Intent(this, GaitActivity.class));
    }

    public void openPlanner(View view) {
        if (needField()) {
            return;
        }
        Intent intent = new Intent(this, PlannerActivity.class);
        intent.putExtra("fieldId", currentField.id);
        startActivity(intent);
    }

    public void refreshWeather(View view) {
        if (needField()) {
            return;
        }
        conditionText.setText("Loading weather...");
        showWeather(true);
    }
}
