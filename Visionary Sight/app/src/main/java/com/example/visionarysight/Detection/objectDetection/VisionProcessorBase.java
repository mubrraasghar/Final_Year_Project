package com.example.visionarysight.Detection.objectDetection;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.gms.tasks.Tasks;
import com.google.android.odml.image.BitmapMlImageBuilder;
import com.google.android.odml.image.ByteBufferMlImageBuilder;
import com.google.android.odml.image.MediaMlImageBuilder;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public abstract class VisionProcessorBase<T> implements VisionImageProcessor {

  protected static final String MANUAL_TESTING_LOG = "LogTagForTest";
  private static final String TAG = "VisionProcessorBase";

  private final ActivityManager activityManager;
  private final Timer fpsTimer = new Timer();
  private final ScopedExecutor executor;
  private final TemperatureMonitor temperatureMonitor;

  private boolean isShutdown;
  private int numRuns = 0;
  private long totalFrameMs = 0;
  private long maxFrameMs = 0;
  private long minFrameMs = Long.MAX_VALUE;
  private long totalDetectorMs = 0;
  private long maxDetectorMs = 0;
  private long minDetectorMs = Long.MAX_VALUE;
  private int frameProcessedInOneSecondInterval = 0;
  private int framesPerSecond = 0;

  @GuardedBy("this")
  private ByteBuffer latestImage;

  @GuardedBy("this")
  private FrameMetadata latestImageMetaData;

  @GuardedBy("this")
  private ByteBuffer processingImage;

  @GuardedBy("this")
  private FrameMetadata processingMetaData;

  protected VisionProcessorBase(Context context) {
    activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
    fpsTimer.scheduleAtFixedRate(
            new TimerTask() {
              @Override
              public void run() {
                framesPerSecond = frameProcessedInOneSecondInterval;
                frameProcessedInOneSecondInterval = 0;
              }
            },
            0,
            1000);
    temperatureMonitor = new TemperatureMonitor(context);
  }

  @Override
  public void processBitmap(Bitmap bitmap, final GraphicOverlay graphicOverlay) {
    long frameStartMs = SystemClock.elapsedRealtime();

    if (isMlImageEnabled(graphicOverlay.getContext())) {
      MlImage mlImage = new BitmapMlImageBuilder(bitmap).build();
      requestDetectInImage(
              mlImage,
              graphicOverlay,
              null,
              frameStartMs);
      mlImage.close();
      return;
    }

    requestDetectInImage(
            InputImage.fromBitmap(bitmap, 0),
            graphicOverlay,
            null,
            frameStartMs);
  }

  @Override
  public synchronized void processByteBuffer(
          ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) {
    latestImage = data;
    latestImageMetaData = frameMetadata;
    if (processingImage == null && processingMetaData == null) {
      processLatestImage(graphicOverlay);
    }
  }

  private synchronized void processLatestImage(final GraphicOverlay graphicOverlay) {
    processingImage = latestImage;
    processingMetaData = latestImageMetaData;
    latestImage = null;
    latestImageMetaData = null;
    if (processingImage != null && processingMetaData != null && !isShutdown) {
      processImage(processingImage, processingMetaData, graphicOverlay);
    }
  }

  private void processImage(
          ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) {
    long frameStartMs = SystemClock.elapsedRealtime();

    Bitmap bitmap =
            PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.getContext())
                    ? null
                    : BitmapUtils.getBitmap(data, frameMetadata);

    if (isMlImageEnabled(graphicOverlay.getContext())) {
      MlImage mlImage =
              new ByteBufferMlImageBuilder(
                      data,
                      frameMetadata.getWidth(),
                      frameMetadata.getHeight(),
                      MlImage.IMAGE_FORMAT_NV21)
                      .setRotation(frameMetadata.getRotation())
                      .build();

      requestDetectInImage(mlImage, graphicOverlay, bitmap, frameStartMs)
              .addOnSuccessListener(executor, results -> processLatestImage(graphicOverlay));

      mlImage.close();
      return;
    }

    requestDetectInImage(
            InputImage.fromByteBuffer(
                    data,
                    frameMetadata.getWidth(),
                    frameMetadata.getHeight(),
                    frameMetadata.getRotation(),
                    InputImage.IMAGE_FORMAT_NV21),
            graphicOverlay,
            bitmap,
            frameStartMs)
            .addOnSuccessListener(executor, results -> processLatestImage(graphicOverlay));
  }

  @Override
  @RequiresApi(VERSION_CODES.LOLLIPOP)
  @ExperimentalGetImage
  public void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) {
    long frameStartMs = SystemClock.elapsedRealtime();
    if (isShutdown) {
      image.close();
      return;
    }

    Bitmap bitmap = null;
    if (!PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.getContext())) {
      bitmap = BitmapUtils.getBitmap(image);
    }

    if (isMlImageEnabled(graphicOverlay.getContext())) {
      MlImage mlImage =
              new MediaMlImageBuilder(image.getImage())
                      .setRotation(image.getImageInfo().getRotationDegrees())
                      .build();

      requestDetectInImage(mlImage, graphicOverlay, bitmap, frameStartMs)
              .addOnCompleteListener(results -> image.close());
      return;
    }

    requestDetectInImage(
            InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees()),
            graphicOverlay,
            bitmap,
            frameStartMs)
            .addOnCompleteListener(results -> image.close());
  }

  private Task<T> requestDetectInImage(
          final InputImage image,
          final GraphicOverlay graphicOverlay,
          @Nullable final Bitmap originalCameraImage,
          long frameStartMs) {
    return setUpListener(
            detectInImage(image), graphicOverlay, originalCameraImage, frameStartMs);
  }

  private Task<T> requestDetectInImage(
          final MlImage image,
          final GraphicOverlay graphicOverlay,
          @Nullable final Bitmap originalCameraImage,
          long frameStartMs) {
    return setUpListener(
            detectInImage(image), graphicOverlay, originalCameraImage, frameStartMs);
  }

  private Task<T> setUpListener(
          Task<T> task,
          final GraphicOverlay graphicOverlay,
          @Nullable final Bitmap originalCameraImage,
          long frameStartMs) {
    final long detectorStartMs = SystemClock.elapsedRealtime();
    return task.addOnSuccessListener(
                    executor,
                    results -> {
                      graphicOverlay.clear();
                      if (originalCameraImage != null) {
                        graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage));
                      }
                      VisionProcessorBase.this.onSuccess(results, graphicOverlay);
                      graphicOverlay.postInvalidate();
                    })
            .addOnFailureListener(
                    executor,
                    e -> {
                      graphicOverlay.clear();
                      graphicOverlay.postInvalidate();
                      String error = "Failed to process. Error: " + e.getLocalizedMessage();
                      Toast.makeText(
                                      graphicOverlay.getContext(),
                                      error + "\nCause: " + e.getCause(),
                                      Toast.LENGTH_SHORT)
                              .show();
                      Log.d(TAG, error);
                      e.printStackTrace();
                      VisionProcessorBase.this.onFailure(e);
                    });
  }

  @Override
  public void stop() {
    executor.shutdown();
    isShutdown = true;
    fpsTimer.cancel();
    temperatureMonitor.stop();
  }

  protected abstract Task<T> detectInImage(InputImage image);

  protected Task<T> detectInImage(MlImage image) {
    return Tasks.forException(
            new MlKitException(
                    "MlImage is currently not demonstrated for this feature",
                    MlKitException.INVALID_ARGUMENT));
  }

  protected abstract void onSuccess(@NonNull T results, @NonNull GraphicOverlay graphicOverlay);

  protected abstract void onFailure(@NonNull Exception e);

  protected boolean isMlImageEnabled(Context context) {
    return false;
  }
}
