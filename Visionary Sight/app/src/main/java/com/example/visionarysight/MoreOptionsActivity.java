package com.example.visionarysight;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visionarysight.SOS.SOSSetting;
import com.example.visionarysight.moreOptions.AboutUsActivity;
import com.example.visionarysight.moreOptions.ProfileInfoActivity;

import java.util.Locale;

public class MoreOptionsActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private View lastClickedButton;
    private boolean firstClick = true;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_options);

        tts = new TextToSpeech(this, this);

        setupButton(findViewById(R.id.button_profile_info), "Profile Info", ProfileInfoActivity.class);
        setupButton(findViewById(R.id.button_about_us), "About Us", AboutUsActivity.class);
        setupButton(findViewById(R.id.button_share_app), "Share App", this::shareAppAction);
        setupButton(findViewById(R.id.button_sos_messaging), "SOS Message Setting", SOSSetting.class);
        setupButton(findViewById(R.id.button_location), "location History", LocationActivity.class);
        setupButton(findViewById(R.id.button_go_back), "Go Back", this::goBackAction);
    }

    private void setupButton(Button button, String buttonText, Class<?> activityClass) {
        button.setOnClickListener(v -> {
            if (firstClick || button != lastClickedButton) {
                speak("You are clicking on " + buttonText + ". Click again to go to " + buttonText);
                firstClick = false;
                lastClickedButton = button;
            } else {
                speak("Opening " + buttonText);
                startActivity(new Intent(MoreOptionsActivity.this, activityClass));
                firstClick = true;
                lastClickedButton = null;
            }
        });

        button.setOnLongClickListener(v -> {
            speak("There are Seven options: Detection, Location, More Options, Profile Info, About Us, Share App, and Log Out. Click on any button to hear its name, and click again to open it.");
            return true;
        });
    }
    private void setupButton(Button button, String buttonText, Runnable action) {
        button.setOnClickListener(v -> {
            if (button != lastClickedButton) {
                speak("You are clicking on " + buttonText + ". Click again to perform " + buttonText);
                lastClickedButton = button;
            } else {
                speak("Performing " + buttonText);
                action.run();
                lastClickedButton = null;
            }
        });

        button.setOnLongClickListener(v -> {
            speak("This is the More Options panel. It contains four buttons: Book Travelling, SOS Messaging, Weather Updates, and Go Back. Click a button to hear its name, and click again to perform the action.");
            return true;
        });
    }

    private void shareAppAction() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareMessage = "Check out the Visionary Sight app for visually impaired users. Download it now from [App Store Link or Website Link].";
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        startActivity(Intent.createChooser(shareIntent, "Share Visionary Sight using"));
    }

    private void sosMessagingAction() {
        speak("Opeining SOS message setting.");
    }
    private void goBackAction() {
        speak("Going back.");
        finish();
    }


    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                speak("Language not supported.");
            } else {
                speak("You are in More Options Activity. Long press for instructions.");
            }
        } else {
            speak("Text to speech initialization failed.");
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        speak("You are in More Options Activity. Long press for instructions.");
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
