package com.example.soilmetrics;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

// add or edit field
public class AddFieldActivity extends AppCompatActivity {

    DatabaseHelper db;
    int fieldId = -1;

    EditText nameBox;
    EditText acresBox;
    EditText latBox;
    EditText lonBox;
    Spinner stateSpinner;
    Spinner tillageSpinner;
    CheckBox coverCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_field);

        db = new DatabaseHelper(this);
        nameBox = findViewById(R.id.nameBox);
        acresBox = findViewById(R.id.acresBox);
        latBox = findViewById(R.id.latBox);
        lonBox = findViewById(R.id.lonBox);
        stateSpinner = findViewById(R.id.stateSpinner);
        tillageSpinner = findViewById(R.id.tillageSpinner);
        coverCheck = findViewById(R.id.coverCheck);
        TextView titleBar = findViewById(R.id.titleBar);
        AppTools.pickInSpinner(stateSpinner, "Iowa");

        // editing an old field
        fieldId = getIntent().getIntExtra("fieldId", -1);
        if (fieldId > 0) {
            Field f = db.getField(fieldId);
            if (f != null) {
                titleBar.setText("Edit Field");
                nameBox.setText(f.name);
                acresBox.setText("" + f.acres);
                latBox.setText("" + f.lat);
                lonBox.setText("" + f.lon);
                AppTools.pickInSpinner(stateSpinner, f.state);
                AppTools.pickInSpinner(tillageSpinner, f.tillage);
                coverCheck.setChecked(f.coverCrops);
                Button deleteButton = findViewById(R.id.deleteButton);
                deleteButton.setVisibility(View.VISIBLE);
            }
        } else {
            titleBar.setText("New Field");
        }
    }

    // fill in GPS spot
    public void useMyLocation(View view) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location spot = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (spot == null) {
            spot = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (spot != null) {
            showLocation(spot);
        } else {
            // wait for new GPS
            Toast.makeText(this, "Finding your location...", Toast.LENGTH_SHORT).show();
            manager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    showLocation(location);
                }
            }, null);
        }
    }

    // put spot in boxes
    private void showLocation(Location spot) {
        latBox.setText("" + Math.round(spot.getLatitude() * 100000) / 100000.0);
        lonBox.setText("" + Math.round(spot.getLongitude() * 100000) / 100000.0);
        Toast.makeText(this, "Location found", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            useMyLocation(null);
        } else {
            Toast.makeText(this, "Type the location in instead", Toast.LENGTH_LONG).show();
        }
    }

    public void saveField(View view) {
        String name = nameBox.getText().toString().trim();
        double acres = AppTools.readNumber(acresBox, -1);
        double lat = AppTools.readNumber(latBox, 999);
        double lon = AppTools.readNumber(lonBox, 999);

        // check the inputs
        if (name.equals("")) {
            nameBox.setError("Enter a name");
            return;
        }
        if (acres <= 0) {
            acresBox.setError("Enter acres");
            return;
        }
        if (lat < -90 || lat > 90) {
            latBox.setError("Latitude is -90 to 90");
            return;
        }
        if (lon < -180 || lon > 180) {
            lonBox.setError("Longitude is -180 to 180");
            return;
        }

        Field f = new Field();
        f.id = fieldId;
        f.name = name;
        f.acres = acres;
        f.lat = lat;
        f.lon = lon;
        f.state = stateSpinner.getSelectedItem().toString();
        f.tillage = tillageSpinner.getSelectedItem().toString();
        f.coverCrops = coverCheck.isChecked();
        int savedId = db.saveField(f);

        // forget old weather
        SharedPreferences prefs = getSharedPreferences("soil", MODE_PRIVATE);
        prefs.edit().remove("weather_" + savedId).remove("ndvi_" + savedId).putInt("fieldId", savedId).apply();

        Toast.makeText(this, "Field saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void deleteField(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Delete field?")
                .setMessage("This also deletes all soil tests for this field.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteField(fieldId);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // top back arrow
    public void goBack(View view) {
        finish();
    }
}
