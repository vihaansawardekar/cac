package com.example.soilmetrics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

// confirm email link screen
public class VerifyEmailActivity extends AppCompatActivity {

    TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);
        statusText = findViewById(R.id.statusText);

        FirebaseUser user = AuthHelper.auth().getCurrentUser();
        if (user == null) {
            useOtherAccount(null);
            return;
        }
        TextView infoText = findViewById(R.id.infoText);
        infoText.setText("We sent a link to " + user.getEmail()
                + ". Open the email, tap the link, then come back and press the button below.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // maybe they verified already
        checkAgain(null);
    }

    public void checkAgain(View view) {
        final FirebaseUser user = AuthHelper.auth().getCurrentUser();
        if (user == null) {
            return;
        }
        // get fresh user info
        user.reload().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                FirebaseUser fresh = AuthHelper.auth().getCurrentUser();
                if (fresh != null && fresh.isEmailVerified()) {
                    AuthHelper.goNext(VerifyEmailActivity.this);
                } else if (view != null) {
                    statusText.setText("Not verified yet. Tap the link in the email first.");
                }
            }
        });
    }

    public void sendAgain(View view) {
        FirebaseUser user = AuthHelper.auth().getCurrentUser();
        if (user == null) {
            return;
        }
        user.sendEmailVerification().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    statusText.setText("Sent! Check your inbox and spam folder.");
                } else {
                    statusText.setText(AuthHelper.errorText(task.getException()));
                }
            }
        });
    }

    public void useOtherAccount(View view) {
        AuthHelper.auth().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
