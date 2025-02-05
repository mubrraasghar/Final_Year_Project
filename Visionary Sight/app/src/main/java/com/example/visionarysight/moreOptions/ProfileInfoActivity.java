package com.example.visionarysight.moreOptions;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.visionarysight.R;
import com.example.visionarysight.SignInActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileInfoActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private TextView textViewProfileInfo;
    private Button buttonGoBack, buttonReadAgain;
    private String userInfo = "Loading user information...";
    private boolean isTtsInitialized = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_info);

        tts = new TextToSpeech(this, this);

        textViewProfileInfo = findViewById(R.id.textViewProfileInfo);
        buttonGoBack = findViewById(R.id.buttonGoBack);
        buttonReadAgain = findViewById(R.id.read_again);

        fetchUserInfo();

        buttonReadAgain.setOnClickListener(v -> {
            if (isTtsInitialized) {
                speak(userInfo);
            }
        });

        buttonGoBack.setOnClickListener(new View.OnClickListener() {
            private boolean firstClick = true;

            @Override
            public void onClick(View v) {
                if (firstClick) {
                    speak("Click again to go back.");
                    firstClick = false;
                } else {
                    speak("Going back.");
                    finish();
                }
            }
        });

        textViewProfileInfo.setOnClickListener(v -> {
            if (isTtsInitialized) {
                speak(userInfo);
            }
        });
    }

    private void fetchUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                String fullName = account.getDisplayName() != null ? account.getDisplayName() : "N/A";
                String email = account.getEmail() != null ? account.getEmail() : "N/A";
                String firstName = account.getGivenName() != null ? account.getGivenName() : "N/A";
                String familyName = account.getFamilyName() != null ? account.getFamilyName() : "N/A";

                FirebaseUserMetadata metadata = user.getMetadata();
                String registrationDate = "N/A";
                if (metadata != null) {
                    long creationTimestamp = metadata.getCreationTimestamp();
                    Date creationDate = new Date(creationTimestamp);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    registrationDate = sdf.format(creationDate);
                }

                userInfo = String.format("Full Name: %s\nEmail: %s\nFirst Name: %s\nFamily Name: %s\nRegistration Date: %s",
                        fullName,
                        email,
                        firstName,
                        familyName,
                        registrationDate);

                textViewProfileInfo.setText(userInfo);
                if (isTtsInitialized) {
                    provideVoiceAssistance();
                }
            } else {
                userInfo = "No Google account information available.";
                speak(userInfo);
            }
        } else {
            userInfo = "No user signed in.";
            speak(userInfo);
            Intent signInIntent = new Intent(ProfileInfoActivity.this, SignInActivity.class);
            startActivity(signInIntent);
            finish();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
            } else {
                isTtsInitialized = true;
                provideVoiceAssistance();
            }
        } else {
            Toast.makeText(this, "TTS Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void speak(String text) {
        if (isTtsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ProfileInfo");
        }
    }

    private void provideVoiceAssistance() {
        speak("You are in the Profile Information screen. Here are the details of your profile: " + userInfo +
                ". Tap the 'Read Again' button to hear this information again, or the 'Go Back' button to return to the main menu.");
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
