package com.example.soilmetrics;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

// list of soil tests
public class HistoryActivity extends AppCompatActivity {

    DatabaseHelper db;
    Field field;
    ArrayList<Sample> shownSamples = new ArrayList<>();

    ListView sampleList;
    EditText labOmBox;
    EditText labDateBox;
    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = new DatabaseHelper(this);
        field = db.getField(getIntent().getIntExtra("fieldId", -1));
        if (field == null) {
            finish();
            return;
        }

        sampleList = findViewById(R.id.sampleList);
        labOmBox = findViewById(R.id.labOmBox);
        labDateBox = findViewById(R.id.labDateBox);
        TextView title = findViewById(R.id.historyTitle);
        title.setText("Soil Tests for " + field.name);
        labDateBox.setText(dateFormat.format(new Date()));

        // hold to delete test
        sampleList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < shownSamples.size()) {
                    askDelete(shownSamples.get(position));
                }
                return true;
            }
        });

        showList();
    }

    // fill the list
    private void showList() {
        ArrayList<Sample> samples = db.getSamples(field.id);
        shownSamples = new ArrayList<>();
        ArrayList<String> lines = new ArrayList<>();
        SimpleDateFormat niceDate = new SimpleDateFormat("MMM d, yyyy", Locale.US);

        // newest at the top
        for (int i = samples.size() - 1; i >= 0; i--) {
            Sample s = samples.get(i);
            shownSamples.add(s);
            lines.add(String.format("%.2f%%", s.om) + "   •   " + niceDate.format(new Date(s.date)) + "\n" + s.note);
        }
        if (lines.size() == 0) {
            lines.add("No soil tests yet. Scan your soil or add a lab test.");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.row_sample, lines);
        sampleList.setAdapter(adapter);
    }

    // make sure to delete
    private void askDelete(final Sample s) {
        if (s == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete this test?")
                .setMessage(String.format("%.2f%%", s.om) + " - " + s.note)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteSample(s.id);
                        showList();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void addLabTest(View view) {
        double om = AppTools.readNumber(labOmBox, -1);
        if (om <= 0 || om > 30) {
            labOmBox.setError("Enter 0 to 30");
            return;
        }

        Date date;
        try {
            dateFormat.setLenient(false);
            date = dateFormat.parse(labDateBox.getText().toString().trim());
        } catch (Exception e) {
            labDateBox.setError("Use MM/DD/YYYY");
            return;
        }
        if (date == null || date.getTime() > System.currentTimeMillis()) {
            labDateBox.setError("Date can't be in the future");
            return;
        }

        db.addSample(field.id, date.getTime(), om, "Lab test");
        labOmBox.setText("");
        Toast.makeText(this, "Lab test added", Toast.LENGTH_SHORT).show();
        showList();
    }

    // top back arrow
    public void goBack(View view) {
        finish();
    }
}
