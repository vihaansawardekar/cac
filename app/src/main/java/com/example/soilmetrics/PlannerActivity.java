package com.example.soilmetrics;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

// best crop plan screen
public class PlannerActivity extends AppCompatActivity {

    Field field;
    SharedPreferences prefs;
    ArrayList<Sample> samples = new ArrayList<>();

    EditText omBox;
    Spinner currentCropSpinner;
    Spinner climateSpinner;
    Spinner pestSpinner;
    EditText cornPrice;
    EditText soyPrice;
    EditText wheatPrice;
    EditText oatsPrice;
    EditText hayPrice;
    Spinner tillageSpinner;
    EditText nitrogenBox;
    CheckBox manureCheck;
    CheckBox[] useBoxes = new CheckBox[6];
    CheckBox hasPlanter;
    CheckBox hasDrill;
    CheckBox hasCombine;
    CheckBox hasHay;
    CheckBox oatsBuyer;
    CheckBox hayBuyer;
    Spinner rotationSpinner;
    SeekBar soilSeek;
    TextView weightLabel;
    TextView rotationText;
    LinearLayout rotationList;
    TextView bestTitle;
    GraphView bestGraph;
    LinearLayout bestYears;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planner);
        prefs = getSharedPreferences("soil", MODE_PRIVATE);

        omBox = findViewById(R.id.omBox);
        currentCropSpinner = findViewById(R.id.currentCropSpinner);
        climateSpinner = findViewById(R.id.climateSpinner);
        pestSpinner = findViewById(R.id.pestSpinner);
        cornPrice = findViewById(R.id.cornPrice);
        soyPrice = findViewById(R.id.soyPrice);
        wheatPrice = findViewById(R.id.wheatPrice);
        oatsPrice = findViewById(R.id.oatsPrice);
        hayPrice = findViewById(R.id.hayPrice);
        tillageSpinner = findViewById(R.id.tillageSpinner);
        nitrogenBox = findViewById(R.id.nitrogenBox);
        manureCheck = findViewById(R.id.manureCheck);
        useBoxes[0] = findViewById(R.id.useCorn);
        useBoxes[1] = findViewById(R.id.useSoy);
        useBoxes[2] = findViewById(R.id.useWheat);
        useBoxes[3] = findViewById(R.id.useOats);
        useBoxes[4] = findViewById(R.id.useAlfalfa);
        useBoxes[5] = findViewById(R.id.useCover);
        hasPlanter = findViewById(R.id.hasPlanter);
        hasDrill = findViewById(R.id.hasDrill);
        hasCombine = findViewById(R.id.hasCombine);
        hasHay = findViewById(R.id.hasHay);
        oatsBuyer = findViewById(R.id.oatsBuyer);
        hayBuyer = findViewById(R.id.hayBuyer);
        rotationSpinner = findViewById(R.id.rotationSpinner);
        soilSeek = findViewById(R.id.soilSeek);
        weightLabel = findViewById(R.id.weightLabel);
        rotationText = findViewById(R.id.rotationText);
        rotationList = findViewById(R.id.rotationList);
        bestTitle = findViewById(R.id.bestTitle);
        bestGraph = findViewById(R.id.bestGraph);
        bestYears = findViewById(R.id.bestYears);

        // fill from the field
        DatabaseHelper db = new DatabaseHelper(this);
        field = db.getField(getIntent().getIntExtra("fieldId", -1));
        if (field == null) {
            finish();
            return;
        }
        TextView title = findViewById(R.id.titleBar);
        title.setText("Best Plan for " + field.name);
        AppTools.pickInSpinner(tillageSpinner, field.tillage);
        samples = db.getSamples(field.id);
        if (samples.size() > 0) {
            omBox.setText(String.format("%.2f", samples.get(samples.size() - 1).om));
        } else {
            omBox.setText("3.00");
        }

        // load saved prices
        cornPrice.setText(prefs.getString("cornPrice", "4.20"));
        soyPrice.setText(prefs.getString("soyPrice", "10.50"));
        wheatPrice.setText(prefs.getString("wheatPrice", "5.60"));
        oatsPrice.setText(prefs.getString("oatsPrice", "3.50"));
        hayPrice.setText(prefs.getString("hayPrice", "180"));

        // slider label changes
        soilSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                weightLabel.setText("What matters more? Money " + (100 - progress) + " / Soil " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    // read all the boxes
    private FarmPlan readPlan() {
        FarmPlan plan = new FarmPlan();
        plan.fieldName = field.name;
        plan.acres = field.acres;
        plan.om = AppTools.readNumber(omBox, 3.0);
        if (plan.om < 0.1) {
            plan.om = 0.1;
        }
        plan.currentCrop = currentCropSpinner.getSelectedItem().toString();
        plan.climate = climateSpinner.getSelectedItem().toString();
        plan.pest = pestSpinner.getSelectedItem().toString();
        plan.tillage = tillageSpinner.getSelectedItem().toString();
        plan.nRate = Math.max(0, AppTools.readNumber(nitrogenBox, 150));
        plan.manure = manureCheck.isChecked();

        plan.prices[0] = AppTools.readNumber(cornPrice, 4.20);
        plan.prices[1] = AppTools.readNumber(soyPrice, 10.50);
        plan.prices[2] = AppTools.readNumber(wheatPrice, 5.60);
        plan.prices[3] = AppTools.readNumber(oatsPrice, 3.50);
        plan.prices[4] = AppTools.readNumber(hayPrice, 180);
        plan.prices[5] = 0;

        for (int i = 0; i < 6; i++) {
            plan.useCrop[i] = useBoxes[i].isChecked();
        }
        plan.planter = hasPlanter.isChecked();
        plan.drill = hasDrill.isChecked();
        plan.combine = hasCombine.isChecked();
        plan.hayTools = hasHay.isChecked();
        plan.oatsBuyer = oatsBuyer.isChecked();
        plan.hayBuyer = hayBuyer.isChecked();
        plan.rotationYears = rotationSpinner.getSelectedItemPosition() + 3;
        plan.soilWeight = soilSeek.getProgress();

        // use field past trend
        if (samples.size() >= 3 && SoilSimulator.daysCovered(samples) >= 180) {
            plan.useTrend = true;
            plan.trend = SoilSimulator.limit(SoilSimulator.trendPerYear(samples), -0.3, 0.3);
        }

        // remember the prices
        prefs.edit()
                .putString("cornPrice", cornPrice.getText().toString())
                .putString("soyPrice", soyPrice.getText().toString())
                .putString("wheatPrice", wheatPrice.getText().toString())
                .putString("oatsPrice", oatsPrice.getText().toString())
                .putString("hayPrice", hayPrice.getText().toString())
                .apply();
        return plan;
    }

    public void findRotations(View view) {
        FarmPlan plan = readPlan();
        long start = System.currentTimeMillis();
        ArrayList<String> lines = SoilSimulator.findBestRotations(plan);
        long time = System.currentTimeMillis() - start;
        showRotations(lines, "Made on your phone in " + time + " ms, no internet needed.");
        showBestPlan(plan, SoilSimulator.bestRotation);
    }

    // make rotation cards
    private void showRotations(ArrayList<String> lines, String madeBy) {
        rotationList.removeAllViews();
        String notes = "";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("#")) {
                // split name and words
                int lineEnd = line.indexOf("\n");
                String top = line.substring(0, lineEnd);
                String body = line.substring(lineEnd + 1);
                int colon = top.indexOf(":");

                View card = LayoutInflater.from(this).inflate(R.layout.row_rotation, rotationList, false);
                TextView rank = card.findViewById(R.id.rankNumber);
                TextView name = card.findViewById(R.id.rotationName);
                TextView words = card.findViewById(R.id.rotationBody);
                rank.setText(top.substring(1, colon));
                name.setText(top.substring(colon + 2));
                words.setText(body);
                rotationList.addView(card);
            } else {
                notes = notes + line + "\n\n";
            }
        }
        rotationText.setText(notes + madeBy);
    }

    // year cards for winner
    private void showBestPlan(FarmPlan plan, int[] best) {
        bestYears.removeAllViews();
        if (best == null) {
            bestTitle.setVisibility(View.GONE);
            bestGraph.setVisibility(View.GONE);
            return;
        }
        ArrayList<YearResult> results = SoilSimulator.simulate(plan, best);

        for (int i = 0; i < results.size(); i++) {
            YearResult r = results.get(i);
            View card = LayoutInflater.from(this).inflate(R.layout.row_year, bestYears, false);
            TextView number = card.findViewById(R.id.yearNumber);
            TextView crop = card.findViewById(R.id.yearCrop);
            TextView profit = card.findViewById(R.id.yearProfit);
            TextView om = card.findViewById(R.id.yearOm);
            TextView nitrogen = card.findViewById(R.id.yearNitrogen);
            TextView compaction = card.findViewById(R.id.yearCompaction);

            number.setText("" + (i + 1));
            crop.setText(SoilSimulator.crops[r.crop]);
            profit.setText(SoilSimulator.money(r.profit) + "/ac");
            if (r.profit < 0) {
                profit.setTextColor(Color.parseColor("#C9604D"));
            }
            om.setText(String.format("%.2f%%", r.om) + "\norganic");
            nitrogen.setText(Math.round(r.nitrogen) + " lb\nnitrogen");
            compaction.setText(Math.round(r.compaction) + " " + SoilSimulator.compactionWords(r.compaction) + "\ncompaction");
            bestYears.addView(card);
        }

        // graph organic matter
        ArrayList<Double> values = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        values.add(plan.om);
        labels.add("Now");
        for (int i = 0; i < results.size(); i++) {
            values.add(results.get(i).om);
            labels.add("Year " + (i + 1));
        }
        bestTitle.setText("Plan #1, year by year (organic matter %)");
        bestTitle.setVisibility(View.VISIBLE);
        bestGraph.setVisibility(View.VISIBLE);
        bestGraph.setData(values, labels);
    }

    // top back arrow
    public void goBack(View view) {
        finish();
    }
}
