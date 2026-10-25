package com.example.soilmetrics;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.util.ArrayList;

// take photo and predict
public class ScanActivity extends AppCompatActivity {

    static final int CAMERA_CODE = 1;
    static final int GALLERY_CODE = 2;

    SoilModel model;
    DatabaseHelper db;
    ArrayList<Field> fields;
    double lastOm = -1;

    ImageView photoView;
    LinearLayout photoHint;
    GaugeView resultGauge;
    TextView detailText;
    Spinner fieldSpinner;
    EditText noteBox;
    Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        db = new DatabaseHelper(this);
        photoView = findViewById(R.id.photoView);
        photoHint = findViewById(R.id.photoHint);
        resultGauge = findViewById(R.id.resultGauge);
        resultGauge.clear("No photo yet");
        detailText = findViewById(R.id.detailText);
        fieldSpinner = findViewById(R.id.fieldSpinner);
        noteBox = findViewById(R.id.noteBox);
        saveButton = findViewById(R.id.saveButton);

        // load the AI model
        try {
            model = new SoilModel(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not load the AI model", Toast.LENGTH_LONG).show();
        }

        // fill field list
        fields = db.getAllFields();
        ArrayList<String> names = new ArrayList<>();
        int fieldId = getIntent().getIntExtra("fieldId", -1);
        int spot = 0;
        for (int i = 0; i < fields.size(); i++) {
            names.add(fields.get(i).name);
            if (fields.get(i).id == fieldId) {
                spot = i;
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fieldSpinner.setAdapter(adapter);
        fieldSpinner.setSelection(spot);
    }

    public void openCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(intent, CAMERA_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    public void openGallery(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        Bitmap photo = null;
        if (requestCode == CAMERA_CODE && data.getExtras() != null) {
            photo = (Bitmap) data.getExtras().get("data");
        } else if (requestCode == GALLERY_CODE && data.getData() != null) {
            photo = loadPhoto(data.getData());
        }

        if (photo == null) {
            Toast.makeText(this, "Could not open that photo", Toast.LENGTH_SHORT).show();
            return;
        }
        checkPhoto(photo);
    }

    // open gallery photo smaller
    private Bitmap loadPhoto(Uri uri) {
        try {
            // first just get size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream input = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(input, null, options);
            input.close();

            // shrink big photos
            int smallSide = Math.min(options.outWidth, options.outHeight);
            options.inSampleSize = Math.max(1, smallSide / 512);
            options.inJustDecodeBounds = false;
            input = getContentResolver().openInputStream(uri);
            Bitmap photo = BitmapFactory.decodeStream(input, null, options);
            input.close();
            return photo;
        } catch (Exception e) {
            return null;
        }
    }

    // run AI on photo
    private void checkPhoto(Bitmap photo) {
        photoView.setImageBitmap(photo);
        photoView.setVisibility(View.VISIBLE);
        photoHint.setVisibility(View.GONE);
        if (model == null) {
            detailText.setText("The AI model did not load.");
            return;
        }

        long start = System.currentTimeMillis();
        lastOm = model.predict(photo);
        long time = System.currentTimeMillis() - start;

        resultGauge.setValue(lastOm, SoilModel.omRating(lastOm));

        // carbon in topsoil
        double carbonTons = lastOm * 5.8;

        String details = meaning(lastOm) + "\n\n";
        details = details + "About " + Math.round(carbonTons) + " tons of carbon per acre in the top 6 inches\n";
        details = details + "Checked on your phone in " + time + " ms, no internet needed";
        if (!model.looksLikeSoil(photo)) {
            details = "Warning: this does not look like bare soil, so the result may be wrong.\n\n" + details;
        }
        detailText.setText(details);
        saveButton.setEnabled(true);
    }

    // what the number means
    private String meaning(double om) {
        if (om < 2) {
            return "Low fertility. This soil can store a lot more carbon. Cover crops and less tillage help.";
        } else if (om < 3.5) {
            return "Average. There is room to build more organic matter.";
        } else if (om < 5) {
            return "Healthy soil with good fertility.";
        }
        return "Very rich soil. Protect it from tillage and erosion.";
    }

    public void saveSample(View view) {
        if (lastOm < 0 || fields.size() == 0) {
            return;
        }
        Field field = fields.get(fieldSpinner.getSelectedItemPosition());
        String note = "Photo";
        String typed = noteBox.getText().toString().trim();
        if (!typed.equals("")) {
            note = note + ": " + typed;
        }
        db.addSample(field.id, System.currentTimeMillis(), lastOm, note);
        getSharedPreferences("soil", MODE_PRIVATE).edit().putInt("fieldId", field.id).apply();
        Toast.makeText(this, "Saved to " + field.name, Toast.LENGTH_SHORT).show();
        finish();
    }

    // top back arrow
    public void goBack(View view) {
        finish();
    }
}
