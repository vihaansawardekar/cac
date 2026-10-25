package com.example.soilmetrics;

import android.widget.EditText;
import android.widget.Spinner;

// small helper methods
public class AppTools {

    // select text in spinner
    public static void pickInSpinner(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    // number from text box
    public static double readNumber(EditText box, double backup) {
        try {
            return Double.parseDouble(box.getText().toString().trim());
        } catch (Exception e) {
            return backup;
        }
    }
}
