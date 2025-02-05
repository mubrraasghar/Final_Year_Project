package com.example.visionarysight.Detection.objectDetection.objectdetector;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;


import com.example.visionarysight.Detection.objectDetection.GraphicOverlay;
import com.example.visionarysight.Detection.objectDetection.VisionProcessorBase;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectDetectorProcessor extends VisionProcessorBase<List<DetectedObject>> {

  private static final String TAG = "ObjectDetectorProcessor";
  private static final long MIN_SPEAK_INTERVAL = 3000;

  private final ObjectDetector detector;
  private final TextToSpeech textToSpeech;

  private final Map<Integer, Long> lastSpokenTime;

  public ObjectDetectorProcessor(Context context, ObjectDetectorOptionsBase options, TextToSpeech tts) {
    super(context);
    detector = ObjectDetection.getClient(options);
    textToSpeech = tts;
    lastSpokenTime = new HashMap<>();
  }

  @Override
  public void stop() {
    super.stop();
    detector.close();
  }

  @Override
  protected Task<List<DetectedObject>> detectInImage(InputImage image) {
    return detector.process(image);
  }

  @Override
  protected void onSuccess(@NonNull List<DetectedObject> results, @NonNull GraphicOverlay graphicOverlay) {
    long currentTime = System.currentTimeMillis();

    for (DetectedObject object : results) {
      graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));

      int trackingId = object.getTrackingId();

      if (shouldSpeakObject(trackingId, currentTime)) {
        String label = object.getLabels().isEmpty() ? "Object detected" : object.getLabels().get(0).getText();
        textToSpeech.speak(label, TextToSpeech.QUEUE_FLUSH, null, null);

        lastSpokenTime.put(trackingId, currentTime);
      }
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Object detection failed!", e);
  }

  private boolean shouldSpeakObject(int trackingId, long currentTime) {
    if (lastSpokenTime.containsKey(trackingId)) {
      long lastSpoken = lastSpokenTime.get(trackingId);
      return currentTime - lastSpoken > MIN_SPEAK_INTERVAL;
    } else {
      return true;
    }
  }
}
