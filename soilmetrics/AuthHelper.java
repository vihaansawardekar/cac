package com.example.soilmetrics;

import android.app.Activity;
import android.content.Intent;
import android.widget.TextView;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorResolver;
import com.google.firebase.auth.UserInfo;

// shared sign in helpers
public class AuthHelper {

    // true for local testing
    public static final boolean USE_EMULATOR = false;
    private static boolean emulatorReady = false;

    // waiting two step sign
    public static MultiFactorResolver resolver;

    // get firebase sign in
    public static synchronized FirebaseAuth auth() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (USE_EMULATOR && !emulatorReady) {
            auth.useEmulator("10.0.2.2", 9099);
            emulatorReady = true;
        }
        return auth;
    }

    // email accounts need verify
    public static boolean needsEmailCheck(FirebaseUser user) {
        for (UserInfo info : user.getProviderData()) {
            if (info.getProviderId().equals("password")) {
                return !user.isEmailVerified();
            }
        }
        return false;
    }

    // is user fully in
    public static boolean isSignedIn() {
        FirebaseUser user = auth().getCurrentUser();
        if (user == null) {
            return false;
        }
        return !needsEmailCheck(user);
    }

    // go to right screen
    public static void goNext(Activity activity) {
        FirebaseUser user = auth().getCurrentUser();
        Intent intent;
        if (user != null && needsEmailCheck(user)) {
            intent = new Intent(activity, VerifyEmailActivity.class);
        } else {
            intent = new Intent(activity, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    // sign in went wrong
    public static void handleError(Activity activity, Exception e, TextView status) {
        if (e instanceof FirebaseAuthMultiFactorException) {
            // needs the second step
            resolver = ((FirebaseAuthMultiFactorException) e).getResolver();
            Intent intent = new Intent(activity, PhoneActivity.class);
            intent.putExtra("mode", "second");
            activity.startActivity(intent);
            status.setText("");
        } else {
            status.setText(errorText(e));
        }
    }

    // friendly error words
    public static String errorText(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "That password is too weak. Use at least 6 characters.";
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            return "An account already uses that email. Try signing in instead.";
        } else if (e instanceof FirebaseAuthRecentLoginRequiredException) {
            return "For safety, please sign out and sign in again first.";
        } else if (e instanceof FirebaseAuthInvalidUserException) {
            return "We could not find that account.";
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Wrong email, password or code. Please try again.";
        } else if (e instanceof FirebaseNetworkException) {
            return "No internet. Check your connection and try again.";
        } else if (e instanceof FirebaseTooManyRequestsException) {
            return "Too many tries. Please wait a bit and try again.";
        } else if (e != null && e.getMessage() != null) {
            return e.getMessage();
        }
        return "Something went wrong. Please try again.";
    }
}
