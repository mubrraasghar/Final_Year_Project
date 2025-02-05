package com.example.visionarysight.moreOptions;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.visionarysight.R;

import java.util.Locale;

public class AboutUsActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private boolean isReading = false;
    private Button readButton;
    private Button goBackButton;
    private boolean firstClickReadButton = true;
    private boolean firstClickGoBackButton = true;
    private Handler handler = new Handler();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        textToSpeech = new TextToSpeech(this, this);

        readButton = findViewById(R.id.buttonReadText);
        goBackButton = findViewById(R.id.buttonGoBack);

        TextView aboutUsText = findViewById(R.id.textViewAboutUsInfo);
        aboutUsText.setMovementMethod(new ScrollingMovementMethod());

        readButton.setOnClickListener(v -> {
            if (!isReading) {
                if (firstClickReadButton) {
                    speak("Click again to start reading about us.");
                    firstClickReadButton = false;
                    handler.postDelayed(() -> firstClickReadButton = true, 5000);
                } else {
                    startReading();
                    firstClickReadButton = true;
                }
            } else {
                stopReading();
            }
        });


        goBackButton.setOnClickListener(v -> {
            if (firstClickGoBackButton) {
                speak("Click again to go back.");
                firstClickGoBackButton = false;
                handler.postDelayed(() -> firstClickGoBackButton = true, 5000);
            } else {
                finish();
                firstClickGoBackButton = true;
            }
        });
    }

    private void startReading() {
        isReading = true;
        readButton.setText("Stop Reading");
        TextView aboutUsText = findViewById(R.id.textViewAboutUsInfo);
        String aboutText = aboutUsText.getText().toString();

        textToSpeech.speak("Reading about us. Click again to stop.", TextToSpeech.QUEUE_ADD, null, null);
        textToSpeech.speak(aboutText, TextToSpeech.QUEUE_ADD, null, null);
    }

    private void stopReading() {
        isReading = false;
        readButton.setText("Read About Us");
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        speak("Stopped reading.");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US);
        }
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
