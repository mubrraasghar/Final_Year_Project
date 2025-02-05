package com.example.visionarysight;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.visionarysight.utils.ModelClasses.VisionaryUser;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.Locale;

public class SignInActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore firestore;
    private TextToSpeech tts;
    private boolean isSignInClicked = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        tts = new TextToSpeech(this, this);

        FrameLayout overlayFrame = findViewById(R.id.button_sign_in_container);
        overlayFrame.setOnClickListener(v -> {
            Log.d("SignInActivity", "Overlay frame clicked");
            if (isSignInClicked) {
                speak("Please Select your E-mail to proceed...");
                signIn();
            } else {
                isSignInClicked = true;
                speak("You have selected Sign In. Please click again to proceed.");
            }
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMainActivity();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
            } else {
                promptUserForAction();
            }
        } else {
            Toast.makeText(this, "Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void promptUserForAction() {
        speak("Welcome to Visionary Sight. Please click on the Sign In button to proceed.");
    }

    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                speak("Google sign in failed.");
                Log.w("SignInActivity", "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String displayName = acct.getDisplayName();
                                String[] nameParts = displayName != null ? displayName.split(" ") : new String[0];
                                String firstName = nameParts.length > 0 ? nameParts[0] : "Unknown";
                                String lastName = nameParts.length > 1 ? nameParts[1] : "Unknown";
                                Timestamp registrationDate = new Timestamp(new Date());

                                VisionaryUser user = new VisionaryUser(
                                        displayName,
                                        acct.getEmail(),
                                        firstName,
                                        lastName,
                                        registrationDate
                                );

                                saveUserLoginInfo(user);

                                goToMainActivity();
                            }
                        } else {
                            speak("Authentication Failed.");
                            Log.w("SignInActivity", "Authentication failed", task.getException());
                        }
                    }
                });
    }

    private void saveUserLoginInfo(VisionaryUser user) {
        firestore.collection("users").document(mAuth.getCurrentUser().getUid()).set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("SignInActivity", "User login info saved successfully.");
                })
                .addOnFailureListener(e -> {
                    Log.d("SignInActivity", "Error saving user login info", e);
                    speak("Failed to save user information.");
                });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
