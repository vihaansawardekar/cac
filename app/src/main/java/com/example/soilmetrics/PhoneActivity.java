package com.example.soilmetrics;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorAssertion;
import com.google.firebase.auth.MultiFactorInfo;
import com.google.firebase.auth.MultiFactorSession;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneMultiFactorGenerator;
import com.google.firebase.auth.PhoneMultiFactorInfo;

import java.util.concurrent.TimeUnit;

// text message code screen
public class PhoneActivity extends AppCompatActivity {

    // signin, enroll or second
    String mode;
    String verificationId = "";

    EditText phoneBox;
    EditText codeBox;
    TextView statusText;
    TextView infoText;
    PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        phoneBox = findViewById(R.id.phoneBox);
        codeBox = findViewById(R.id.codeBox);
        statusText = findViewById(R.id.statusText);
        infoText = findViewById(R.id.infoText);
        TextView titleBar = findViewById(R.id.titleBar);
        ImageView heroIcon = findViewById(R.id.heroIcon);

        mode = getIntent().getStringExtra("mode");
        if (mode == null) {
            mode = "signin";
        }

        // what happens with codes
        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                // phone checked by itself
                statusText.setText("Code checked automatically.");
                useCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                statusText.setText(AuthHelper.errorText(e));
            }

            @Override
            public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                verificationId = id;
                statusText.setText("Code sent! Check your text messages.");
            }
        };

        if (mode.equals("enroll")) {
            titleBar.setText("Turn On 2-Step");
            heroIcon.setImageResource(R.drawable.ic_shield);
            infoText.setText("Add your phone. After this, signing in needs your password AND a code we text you.");
        } else if (mode.equals("second")) {
            titleBar.setText("2-Step Verification");
            heroIcon.setImageResource(R.drawable.ic_shield_check);
            PhoneMultiFactorInfo hint = phoneHint();
            if (hint == null) {
                finish();
                return;
            }
            infoText.setText("Your account has 2-step verification. We are texting a code to " + hint.getPhoneNumber() + ".");
            phoneBox.setText(hint.getPhoneNumber());
            phoneBox.setEnabled(false);
            Button sendButton = findViewById(R.id.sendButton);
            sendButton.setText("Send Code Again");
            sendCode(null);
        } else {
            titleBar.setText("Phone Sign In");
            infoText.setText("We will text you a 6-digit code to sign in. No password needed.");
        }
    }

    // the saved phone hint
    private PhoneMultiFactorInfo phoneHint() {
        if (AuthHelper.resolver == null) {
            return null;
        }
        for (MultiFactorInfo info : AuthHelper.resolver.getHints()) {
            if (info instanceof PhoneMultiFactorInfo) {
                return (PhoneMultiFactorInfo) info;
            }
        }
        return null;
    }

    public void sendCode(View view) {
        statusText.setText("Sending code...");

        // second step at sign
        if (mode.equals("second")) {
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(AuthHelper.auth())
                    .setMultiFactorHint(phoneHint())
                    .setMultiFactorSession(AuthHelper.resolver.getSession())
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(callbacks)
                    .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
            return;
        }

        final String phone = phoneBox.getText().toString().replace(" ", "").replace("-", "");
        if (!phone.startsWith("+") || phone.length() < 8) {
            phoneBox.setError("Start with + and your country code, like +1");
            statusText.setText("");
            return;
        }

        if (mode.equals("enroll")) {
            FirebaseUser user = AuthHelper.auth().getCurrentUser();
            if (user == null) {
                finish();
                return;
            }
            // get two step session
            user.getMultiFactor().getSession().addOnCompleteListener(this, new OnCompleteListener<MultiFactorSession>() {
                @Override
                public void onComplete(@NonNull Task<MultiFactorSession> task) {
                    if (!task.isSuccessful()) {
                        statusText.setText(AuthHelper.errorText(task.getException()));
                        return;
                    }
                    PhoneAuthOptions options = PhoneAuthOptions.newBuilder(AuthHelper.auth())
                            .setPhoneNumber(phone)
                            .setMultiFactorSession(task.getResult())
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(PhoneActivity.this)
                            .setCallbacks(callbacks)
                            .build();
                    PhoneAuthProvider.verifyPhoneNumber(options);
                }
            });
        } else {
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(AuthHelper.auth())
                    .setPhoneNumber(phone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(callbacks)
                    .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
        }
    }

    public void checkCode(View view) {
        String code = codeBox.getText().toString().trim();
        if (verificationId.equals("")) {
            statusText.setText("Tap Text Me a Code first.");
            return;
        }
        if (code.length() != 6) {
            codeBox.setError("Enter all 6 digits");
            return;
        }
        useCredential(PhoneAuthProvider.getCredential(verificationId, code));
    }

    // finish with phone code
    private void useCredential(PhoneAuthCredential credential) {
        statusText.setText("Checking code...");

        if (mode.equals("enroll")) {
            FirebaseUser user = AuthHelper.auth().getCurrentUser();
            if (user == null) {
                finish();
                return;
            }
            MultiFactorAssertion assertion = PhoneMultiFactorGenerator.getAssertion(credential);
            user.getMultiFactor().enroll(assertion, "My phone").addOnCompleteListener(this, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(PhoneActivity.this, "2-step verification is on!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        statusText.setText(AuthHelper.errorText(task.getException()));
                    }
                }
            });
        } else if (mode.equals("second")) {
            MultiFactorAssertion assertion = PhoneMultiFactorGenerator.getAssertion(credential);
            AuthHelper.resolver.resolveSignIn(assertion).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        AuthHelper.resolver = null;
                        AuthHelper.goNext(PhoneActivity.this);
                    } else {
                        statusText.setText(AuthHelper.errorText(task.getException()));
                    }
                }
            });
        } else {
            AuthHelper.auth().signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        AuthHelper.goNext(PhoneActivity.this);
                    } else {
                        AuthHelper.handleError(PhoneActivity.this, task.getException(), statusText);
                    }
                }
            });
        }
    }

    // top back arrow
    public void goBack(View view) {
        finish();
    }
}
