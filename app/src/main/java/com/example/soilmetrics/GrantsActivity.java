package com.example.soilmetrics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

// grants and carbon screen
public class GrantsActivity extends AppCompatActivity {

    EditText omBox;
    EditText acresBox;
    Spinner stateSpinner;
    Spinner tillageSpinner;
    CheckBox coverCheck;
    CheckBox nutrientCheck;
    CheckBox willingCheck;
    CheckBox beginnerCheck;
    TextView totalText;
    TextView countText;
    LinearLayout programList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grants);

        omBox = findViewById(R.id.omBox);
        acresBox = findViewById(R.id.acresBox);
        stateSpinner = findViewById(R.id.stateSpinner);
        tillageSpinner = findViewById(R.id.tillageSpinner);
        coverCheck = findViewById(R.id.coverCheck);
        nutrientCheck = findViewById(R.id.nutrientCheck);
        willingCheck = findViewById(R.id.willingCheck);
        beginnerCheck = findViewById(R.id.beginnerCheck);
        totalText = findViewById(R.id.totalText);
        countText = findViewById(R.id.countText);
        programList = findViewById(R.id.programList);

        // fill from the field
        DatabaseHelper db = new DatabaseHelper(this);
        Field field = db.getField(getIntent().getIntExtra("fieldId", -1));
        if (field != null) {
            TextView title = findViewById(R.id.grantTitle);
            title.setText(field.name);
            acresBox.setText("" + Math.round(field.acres));
            AppTools.pickInSpinner(stateSpinner, field.state);
            AppTools.pickInSpinner(tillageSpinner, field.tillage);
            coverCheck.setChecked(field.coverCrops);

            ArrayList<Sample> samples = db.getSamples(field.id);
            if (samples.size() > 0) {
                double om = samples.get(samples.size() - 1).om;
                omBox.setText(String.format("%.1f", om));
            } else {
                omBox.setText("3.0");
            }
        }

        // check right away
        checkPrograms(null);
    }

    public void checkPrograms(View view) {
        double om = AppTools.readNumber(omBox, 3.0);
        double acres = AppTools.readNumber(acresBox, 100);
        String state = stateSpinner.getSelectedItem().toString();
        String tillage = tillageSpinner.getSelectedItem().toString();

        ArrayList<Program> programs = GrantChecker.check(om, acres, state, tillage,
                coverCheck.isChecked(), nutrientCheck.isChecked(),
                willingCheck.isChecked(), beginnerCheck.isChecked());

        // clear old cards
        programList.removeAllViews();
        double total = 0;
        int count = 0;

        // yes ones go first
        for (int i = 0; i < programs.size(); i++) {
            Program p = programs.get(i);
            if (p.eligible) {
                count++;
                if (p.countInTotal) {
                    total = total + p.payout;
                }
                addProgramCard(p);
            }
        }
        for (int i = 0; i < programs.size(); i++) {
            if (!programs.get(i).eligible) {
                addProgramCard(programs.get(i));
            }
        }

        totalText.setText(SoilSimulator.money(total));
        countText.setText("per year from " + count + " programs you fit");
    }

    // make one program card
    private void addProgramCard(Program p) {
        View card = LayoutInflater.from(this).inflate(R.layout.row_program, programList, false);
        ImageView icon = card.findViewById(R.id.programIcon);
        TextView name = card.findViewById(R.id.programName);
        TextView kind = card.findViewById(R.id.programKind);
        TextView money = card.findViewById(R.id.programMoney);
        TextView reason = card.findViewById(R.id.programReason);

        name.setText(p.name);
        kind.setText(p.kind);
        reason.setText(p.reason);

        if (p.eligible) {
            icon.setImageResource(R.drawable.ic_check_circle);
            icon.setColorFilter(Color.parseColor("#415B57"));
            if (p.payout > 0) {
                money.setText("About " + SoilSimulator.money(p.payout) + " / year");
            } else {
                money.setText("Free help");
            }
        } else {
            // dim the no ones
            icon.setImageResource(R.drawable.ic_x_circle);
            icon.setColorFilter(Color.parseColor("#C9604D"));
            money.setVisibility(View.GONE);
            card.setAlpha(0.6f);
        }
        programList.addView(card);
    }

    // top back arrow
    public void goBack(View view) {
        finish();
    }
}
