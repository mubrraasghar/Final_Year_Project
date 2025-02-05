package com.example.visionarysight.SOS;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visionarysight.R;

import java.util.Locale;

public class SOSSetting extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private View lastClickedButton;
    private boolean firstClick = true;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sossetting);

        tts = new TextToSpeech(this, this);

        setupButton(findViewById(R.id.button_add_contacts), "Add Contacts", AddContactsActivity.class);
        setupButton(findViewById(R.id.button_contact_list), "Contact List", ContactListActivity.class);
        setupButton(findViewById(R.id.button_edit_message), "Edit Message", EditMessageActivity.class);
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
                startActivity(new Intent(SOSSetting.this, activityClass));
                firstClick = true;
                lastClickedButton = null;
            }
        });

        button.setOnLongClickListener(v -> {
            speak("In SOS Settings, you can add contacts, view the contact list, edit the SOS message, or go back. Click on any button to hear its name, and click again to open it.");
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
            speak("This is the SOS Settings panel. It contains four options: Add Contacts, Contact List, Edit Message, and Go Back. Click a button to hear its name, and click again to perform the action.");
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
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                speak("Language not supported.");
            } else {
                speak("You are in SOS Settings. Long press for instructions.");
            }
        } else {
            speak("Text to speech initialization failed.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        speak("You are in SOS Settings. Long press for instructions.");
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
