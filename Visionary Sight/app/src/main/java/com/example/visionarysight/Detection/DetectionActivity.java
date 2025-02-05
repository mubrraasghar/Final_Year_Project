package com.example.visionarysight.Detection;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visionarysight.R;

import java.util.Locale;

public class DetectionActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private View lastClickedButton;
    private boolean firstClick = true;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        tts = new TextToSpeech(this, this);

        setupButton(findViewById(R.id.button_currency_detection), "Currency Detection", CurrencyDetectionActivity.class);
        setupButton(findViewById(R.id.button_activity_detection), "Activity Detection", ActivityDetectionActivity.class);
        setupButton(findViewById(R.id.button_object_detection), "Object Detection", ObjectDetectionActivity.class);
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
                startActivity(new Intent(DetectionActivity.this, activityClass));

                firstClick = true;
                lastClickedButton = null;
            }
        });

        button.setOnLongClickListener(v -> {
            speak("This is the Detection Activity panel. It contains options like Currency Detection, Activity Detection, Object Detection, and Go Back. Click a button to hear its name, and click again to open it.");
            return true;
        });
    }

    private void setupButton(Button button, String buttonText) {
        button.setOnClickListener(v -> {
            if (firstClick || button != lastClickedButton) {
                speak("You are clicking on " + buttonText + ". Click again to go to " + buttonText);
                firstClick = false;
                lastClickedButton = button;
            } else {
                speak("Opening " + buttonText);
                firstClick = true;
                lastClickedButton = null;
            }
        });

        button.setOnLongClickListener(v -> {
            speak("This is the Detection Activity panel. It contains options like Currency Detection, Activity Detection, Object Detection, and Go Back. Click a button to hear its name, and click again to open it.");
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
            speak("This is the Detection Activity panel. It contains options like Currency Detection, Activity Detection, Object Detection, and Go Back. Click a button to hear its name, and click again to perform the action.");
            return true;
        });
    }

    private void goBackAction() {
        speak("Going back.");
        finish();
    }

    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }
    @Override
    protected void onResume() {
        super.onResume();
        speak("You are in Detection Activity. Long press for instructions.");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                speak("Language not supported.");
            } else {
                speak("You are in Detection Activity. Long press for instructions.");
            }
        } else {
            speak("Text to speech initialization failed.");
        }
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
