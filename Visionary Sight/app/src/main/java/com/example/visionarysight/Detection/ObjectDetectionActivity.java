package com.example.visionarysight.Detection;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visionarysight.Detection.objectDetection.CameraSource;
import com.example.visionarysight.Detection.objectDetection.CameraSourcePreview;
import com.example.visionarysight.Detection.objectDetection.GraphicOverlay;
import com.example.visionarysight.Detection.objectDetection.PreferenceUtils;
import com.example.visionarysight.Detection.objectDetection.objectdetector.ObjectDetectorProcessor;
import com.example.visionarysight.R;
import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.common.model.LocalModel;

import java.io.IOException;
import java.util.Locale;

@KeepName
public final class ObjectDetectionActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, OnUtteranceCompletedListener {

  private static final String OBJECT_DETECTION_CUSTOM = "Custom Object Detection";
  private static final String TAG = "LivePreviewActivity";

  private CameraSource cameraSource = null;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;
  private String selectedModel = OBJECT_DETECTION_CUSTOM;

  private TextToSpeech textToSpeech;
  private Button goBackButton;

  private boolean firstClick = true;
  private boolean isSpeaking = false;
  private Handler handler = new Handler();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");

    setContentView(R.layout.activity_object_detection);

    textToSpeech = new TextToSpeech(this, this);

    preview = findViewById(R.id.preview_view);
    graphicOverlay = findViewById(R.id.graphic_overlay);
    goBackButton = findViewById(R.id.buttonGoBack);

    textToSpeech.setOnUtteranceCompletedListener(this);

    goBackButton.setOnClickListener(v -> {
      if (firstClick) {
        if (textToSpeech != null && !isSpeaking) {
          isSpeaking = true;
          textToSpeech.speak("Click again to go back.", TextToSpeech.QUEUE_FLUSH, null, null);

          handler.postDelayed(() -> {
            if (!firstClick) {
              firstClick = true;
            }
          }, 4000);
        }
        firstClick = false;
      } else {
        if (textToSpeech != null && !isSpeaking) {
          isSpeaking = true;
          textToSpeech.speak("Going back.", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        finish();
      }
    });

    createCameraSource();
  }

  private void createCameraSource() {
    if (cameraSource == null) {
      cameraSource = new CameraSource(this, graphicOverlay);
      cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
    }

    try {
      Log.i(TAG, "Using Custom Object Detector Processor");
      LocalModel localModel =
              new LocalModel.Builder()
                      .setAssetFilePath("custom_models/object_labeler.tflite")
                      .build();
      cameraSource.setMachineLearningFrameProcessor(
              new ObjectDetectorProcessor(
                      this,
                      PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                              this, localModel),
                      textToSpeech));
    } catch (RuntimeException e) {
      Log.e(TAG, "Cannot create image processor: " + e.getMessage(), e);
      Toast.makeText(getApplicationContext(), "Cannot create image processor: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  private void startCameraSource() {
    if (cameraSource != null) {
      try {
        preview.start(cameraSource, graphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        cameraSource.release();
        cameraSource = null;
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    createCameraSource();
    startCameraSource();
  }

  @Override
  protected void onPause() {
    super.onPause();
    preview.stop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (cameraSource != null) {
      cameraSource.release();
    }
    if (textToSpeech != null) {
      textToSpeech.stop();
      textToSpeech.shutdown();
    }
  }

  @Override
  public void onInit(int status) {
    if (status == TextToSpeech.SUCCESS) {
      int langResult = textToSpeech.setLanguage(Locale.ENGLISH);
      if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
        Log.e(TAG, "Language not supported or missing data");
      }
    } else {
      Log.e(TAG, "TTS initialization failed");
    }
  }

  @Override
  public void onUtteranceCompleted(String utteranceId) {
    isSpeaking = false;
  }
}