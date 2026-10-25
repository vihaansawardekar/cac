package com.example.soilmetrics;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorInfo;
import com.google.firebase.auth.PhoneMultiFactorInfo;
import com.google.firebase.auth.UserInfo;

import java.util.List;

// account and security screen
public class AccountActivity extends AppCompatActivity {

    TextView nameText;
    TextView methodText;
    TextView twoStepText;
    TextView statusText;
    Button twoStepButton;
    LinearLayout passwordCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        nameText = findViewById(R.id.nameText);
        methodText = findViewById(R.id.methodText);
        twoStepText = findViewById(R.id.twoStepText);
        statusText = findViewById(R.id.statusText);
        twoStepButton = findViewById(R.id.twoStepButton);
        passwordCard = findViewById(R.id.passwordCard);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showUser();
    }

    // fill in user info
    private void showUser() {
        FirebaseUser user = AuthHelper.auth().getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        // name on the card
        if (user.isAnonymous()) {
            nameText.setText("Guest");
        } else if (user.getEmail() != null && !user.getEmail().equals("")) {
            nameText.setText(user.getEmail());
        } else if (user.getPhoneNumber() != null) {
            nameText.setText(user.getPhoneNumber());
        }

        // how they signed in
        String methods = "";
        boolean hasPassword = false;
        for (UserInfo info : user.getProviderData()) {
            String id = info.getProviderId();
            if (id.equals("password")) {
                methods = methods + "Email  ";
                hasPassword = true;
            } else if (id.equals("google.com")) {
                methods = methods + "Google  ";
            } else if (id.equals("phone")) {
                methods = methods + "Phone  ";
            }
        }
        if (user.isAnonymous()) {
            methods = "Guest account";
        }
        methodText.setText("Signed in with: " + methods.trim());
        if (hasPassword) {
            passwordCard.setVisibility(View.VISIBLE);
        } else {
            passwordCard.setVisibility(View.GONE);
        }

        // two step status
        List<MultiFactorInfo> factors = user.getMultiFactor().getEnrolledFactors();
        if (user.isAnonymous()) {
            twoStepText.setText("Guests can't use 2-step. Sign in with email or Google to turn it on.");
            twoStepButton.setEnabled(false);
        } else if (!user.isEmailVerified()) {
            twoStepText.setText("2-step needs a verified email account (email or Google sign in).");
            twoStepButton.setEnabled(false);
        } else if (factors.size() > 0) {
            String phone = "your phone";
            if (factors.get(0) instanceof PhoneMultiFactorInfo) {
                phone = ((PhoneMultiFactorInfo) factors.get(0)).getPhoneNumber();
            }
            twoStepText.setText("On. Codes go to " + phone + ".");
            twoStepButton.setText("Turn Off 2-Step");
            twoStepButton.setEnabled(true);
        } else {
            twoStepText.setText("Off. Turn it on to protect your farm data.");
            twoStepButton.setText("Turn On 2-Step");
            twoStepButton.setEnabled(true);
        }
    }

    public void toggleTwoStep(View view) {
        final FirebaseUser user = AuthHelper.auth().getCurrentUser();
        if (user == null) {
            return;
        }
        final List<MultiFactorInfo> factors = user.getMultiFactor().getEnrolledFactors();
        if (factors.size() == 0) {
            // add a phone factor
            Intent intent = new Intent(this, PhoneActivity.class);
            intent.putExtra("mode", "enroll");
            startActivity(intent);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Turn off 2-step?")
                .setMessage("Your account will only need your password to sign in.")
                .setPositiveButton("Turn Off", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        user.getMultiFactor().unenroll(factors.get(0)).addOnCompleteListener(AccountActivity.this, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    statusText.setText("2-step verification is off.");
                                } else {
                                    statusText.setText(AuthHelper.errorText(task.getException()));
                                }
                                showUser();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void resetPassword(View view) {
        FirebaseUser user = AuthHelper.auth().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            return;
        }
        AuthHelper.auth().sendPasswordResetEmail(user.getEmail()).addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    statusText.setText("Reset link sent to your email.");
                } else {
                    statusText.setText(AuthHelper.errorText(task.getException()));
                }
            }
        });
    }

    public void signOut(View view) {
        AuthHelper.auth().signOut();

        // forget google choice too
        CredentialManager manager = CredentialManager.create(this);
        manager.clearCredentialStateAsync(new ClearCredentialStateRequest(), null, ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                    }
                });
        goToLogin();
    }

    // back to sign in
    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // top back arrow
    public void goBack(View view) {
        finish();
    }
}
