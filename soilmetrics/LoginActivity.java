package com.example.soilmetrics;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

// sign in screen
public class LoginActivity extends AppCompatActivity {

    EditText emailBox;
    EditText passwordBox;
    TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // already signed in
        FirebaseUser user = AuthHelper.auth().getCurrentUser();
        if (user != null) {
            AuthHelper.goNext(this);
            return;
        }

        setContentView(R.layout.activity_login);
        emailBox = findViewById(R.id.emailBox);
        passwordBox = findViewById(R.id.passwordBox);
        statusText = findViewById(R.id.statusText);
    }

    // when sign in finishes
    private OnCompleteListener<AuthResult> doneListener() {
        return new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    AuthHelper.goNext(LoginActivity.this);
                } else {
                    AuthHelper.handleError(LoginActivity.this, task.getException(), statusText);
                }
            }
        };
    }

    // check email and password
    private boolean boxesOkay() {
        String email = emailBox.getText().toString().trim();
        String password = passwordBox.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailBox.setError("Enter a real email");
            return false;
        }
        if (password.length() < 6) {
            passwordBox.setError("Use at least 6 characters");
            return false;
        }
        return true;
    }

    public void signIn(View view) {
        if (!boxesOkay()) {
            return;
        }
        statusText.setText("Signing in...");
        String email = emailBox.getText().toString().trim();
        String password = passwordBox.getText().toString();
        AuthHelper.auth().signInWithEmailAndPassword(email, password).addOnCompleteListener(this, doneListener());
    }

    public void createAccount(View view) {
        if (!boxesOkay()) {
            return;
        }
        statusText.setText("Making your account...");
        String email = emailBox.getText().toString().trim();
        String password = passwordBox.getText().toString();
        AuthHelper.auth().createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // send the check email
                    FirebaseUser user = task.getResult().getUser();
                    if (user != null) {
                        user.sendEmailVerification();
                    }
                    AuthHelper.goNext(LoginActivity.this);
                } else {
                    statusText.setText(AuthHelper.errorText(task.getException()));
                }
            }
        });
    }

    public void forgotPassword(View view) {
        String email = emailBox.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailBox.setError("Type your email first");
            return;
        }
        AuthHelper.auth().sendPasswordResetEmail(email).addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    statusText.setText("If that email has an account, a reset link is on its way.");
                } else {
                    statusText.setText(AuthHelper.errorText(task.getException()));
                }
            }
        });
    }

    public void googleSignIn(View view) {
        statusText.setText("Opening Google...");
        GetSignInWithGoogleOption googleOption = new GetSignInWithGoogleOption.Builder(
                getString(R.string.default_web_client_id)).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();

        CredentialManager manager = CredentialManager.create(this);
        manager.getCredentialAsync(this, request, null, ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        useGoogleCredential(response.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException) {
                            statusText.setText("Google sign in was canceled.");
                        } else {
                            statusText.setText("Google sign in stopped. " + e.getMessage());
                        }
                    }
                });
    }

    // google token to firebase
    private void useGoogleCredential(Credential credential) {
        if (credential instanceof CustomCredential
                && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(credential.getData());
            AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(google.getIdToken(), null);
            statusText.setText("Signing in with Google...");
            AuthHelper.auth().signInWithCredential(firebaseCredential).addOnCompleteListener(this, doneListener());
        } else {
            statusText.setText("That was not a Google account.");
        }
    }

    public void phoneSignIn(View view) {
        Intent intent = new Intent(this, PhoneActivity.class);
        intent.putExtra("mode", "signin");
        startActivity(intent);
    }

    public void guestSignIn(View view) {
        statusText.setText("Starting guest mode...");
        AuthHelper.auth().signInAnonymously().addOnCompleteListener(this, doneListener());
    }
}
