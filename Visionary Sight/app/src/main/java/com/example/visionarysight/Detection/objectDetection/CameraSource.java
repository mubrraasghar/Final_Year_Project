package com.example.visionarysight.Detection.objectDetection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.common.images.Size;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;


public class CameraSource {
  @SuppressLint("InlinedApi")
  public static final int CAMERA_FACING_BACK = CameraInfo.CAMERA_FACING_BACK;

  @SuppressLint("InlinedApi")
  public static final int CAMERA_FACING_FRONT = CameraInfo.CAMERA_FACING_FRONT;

  public static final int IMAGE_FORMAT = ImageFormat.NV21;
  public static final int DEFAULT_REQUESTED_CAMERA_PREVIEW_WIDTH = 480;
  public static final int DEFAULT_REQUESTED_CAMERA_PREVIEW_HEIGHT = 360;

  private static final String TAG = "MIDemoApp:CameraSource";

  private static final int DUMMY_TEXTURE_NAME = 100;


  private static final float ASPECT_RATIO_TOLERANCE = 0.01f;

  protected Activity activity;

  private Camera camera;

  private int facing = CAMERA_FACING_BACK;


  private int rotationDegrees;

  private Size previewSize;

  private static final float REQUESTED_FPS = 30.0f;
  private static final boolean REQUESTED_AUTO_FOCUS = true;

  private SurfaceTexture dummySurfaceTexture;

  private final GraphicOverlay graphicOverlay;

  private Thread processingThread;

  private final FrameProcessingRunnable processingRunnable;
  private final Object processorLock = new Object();

  private VisionImageProcessor frameProcessor;

  private final IdentityHashMap<byte[], ByteBuffer> bytesToByteBuffer = new IdentityHashMap<>();

  public CameraSource(Activity activity, GraphicOverlay overlay) {
    this.activity = activity;
    graphicOverlay = overlay;
    graphicOverlay.clear();
    processingRunnable = new FrameProcessingRunnable();
  }



  public void release() {
    synchronized (processorLock) {
      stop();
      cleanScreen();

      if (frameProcessor != null) {
        frameProcessor.stop();
      }
    }
  }

  @RequiresPermission(Manifest.permission.CAMERA)
  public synchronized CameraSource start() throws IOException {
    if (camera != null) {
      return this;
    }

    camera = createCamera();
    dummySurfaceTexture = new SurfaceTexture(DUMMY_TEXTURE_NAME);
    camera.setPreviewTexture(dummySurfaceTexture);
    camera.startPreview();

    processingThread = new Thread(processingRunnable);
    processingRunnable.setActive(true);
    processingThread.start();
    return this;
  }

  @RequiresPermission(Manifest.permission.CAMERA)
  public synchronized CameraSource start(SurfaceHolder surfaceHolder) throws IOException {
    if (camera != null) {
      return this;
    }

    camera = createCamera();
    camera.setPreviewDisplay(surfaceHolder);
    camera.startPreview();

    processingThread = new Thread(processingRunnable);
    processingRunnable.setActive(true);
    processingThread.start();
    return this;
  }

  public synchronized void stop() {
    processingRunnable.setActive(false);
    if (processingThread != null) {
      try {
        processingThread.join();
      } catch (InterruptedException e) {
        Log.d(TAG, "Frame processing thread interrupted on release.");
      }
      processingThread = null;
    }

    if (camera != null) {
      camera.stopPreview();
      camera.setPreviewCallbackWithBuffer(null);
      try {
        camera.setPreviewTexture(null);
        dummySurfaceTexture = null;
        camera.setPreviewDisplay(null);
      } catch (Exception e) {
        Log.e(TAG, "Failed to clear camera preview: " + e);
      }
      camera.release();
      camera = null;
    }

    bytesToByteBuffer.clear();
  }


  public synchronized void setFacing(int facing) {
    if ((facing != CAMERA_FACING_BACK) && (facing != CAMERA_FACING_FRONT)) {
      throw new IllegalArgumentException("Invalid camera: " + facing);
    }
    this.facing = facing;
  }


  public Size getPreviewSize() {
    return previewSize;
  }

  public int getCameraFacing() {
    return facing;
  }

  public boolean setZoom(float zoomRatio) {
    Log.d(TAG, "setZoom: " + zoomRatio);
    if (camera == null) {
      return false;
    }

    Parameters parameters = camera.getParameters();
    parameters.setZoom(getZoomValue(parameters, zoomRatio));
    camera.setParameters(parameters);
    return true;
  }


  private static int getZoomValue(Parameters params, float zoomRatio) {
    int zoom = (int) (Math.max(zoomRatio, 1) * 100);
    List<Integer> zoomRatios = params.getZoomRatios();
    int maxZoom = params.getMaxZoom();
    for (int i = 0; i < maxZoom; ++i) {
      if (zoomRatios.get(i + 1) > zoom) {
        return i;
      }
    }
    return maxZoom;
  }

  @SuppressLint("InlinedApi")
  private Camera createCamera() throws IOException {
    int requestedCameraId = getIdForRequestedCamera(facing);
    if (requestedCameraId == -1) {
      throw new IOException("Could not find requested camera.");
    }
    Camera camera = Camera.open(requestedCameraId);

    SizePair sizePair = PreferenceUtils.getCameraPreviewSizePair(activity, requestedCameraId);
    if (sizePair == null) {
      sizePair =
          selectSizePair(
              camera,
              DEFAULT_REQUESTED_CAMERA_PREVIEW_WIDTH,
              DEFAULT_REQUESTED_CAMERA_PREVIEW_HEIGHT);
    }

    if (sizePair == null) {
      throw new IOException("Could not find suitable preview size.");
    }

    previewSize = sizePair.preview;
    Log.v(TAG, "Camera preview size: " + previewSize);

    int[] previewFpsRange = selectPreviewFpsRange(camera, REQUESTED_FPS);
    if (previewFpsRange == null) {
      throw new IOException("Could not find suitable preview frames per second range.");
    }

    Parameters parameters = camera.getParameters();

    Size pictureSize = sizePair.picture;
    if (pictureSize != null) {
      Log.v(TAG, "Camera picture size: " + pictureSize);
      parameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
    }
    parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
    parameters.setPreviewFpsRange(
        previewFpsRange[Parameters.PREVIEW_FPS_MIN_INDEX],
        previewFpsRange[Parameters.PREVIEW_FPS_MAX_INDEX]);
    parameters.setPreviewFormat(IMAGE_FORMAT);

    setRotation(camera, parameters, requestedCameraId);

    if (REQUESTED_AUTO_FOCUS) {
      if (parameters
          .getSupportedFocusModes()
          .contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
        parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
      } else {
        Log.i(TAG, "Camera auto focus is not supported on this device.");
      }
    }

    camera.setParameters(parameters);

    camera.setPreviewCallbackWithBuffer(new CameraPreviewCallback());
    camera.addCallbackBuffer(createPreviewBuffer(previewSize));
    camera.addCallbackBuffer(createPreviewBuffer(previewSize));
    camera.addCallbackBuffer(createPreviewBuffer(previewSize));
    camera.addCallbackBuffer(createPreviewBuffer(previewSize));

    return camera;
  }

  private static int getIdForRequestedCamera(int facing) {
    CameraInfo cameraInfo = new CameraInfo();
    for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
      Camera.getCameraInfo(i, cameraInfo);
      if (cameraInfo.facing == facing) {
        return i;
      }
    }
    return -1;
  }


  public static SizePair selectSizePair(Camera camera, int desiredWidth, int desiredHeight) {
    List<SizePair> validPreviewSizes = generateValidPreviewSizeList(camera);

    SizePair selectedPair = null;
    int minDiff = Integer.MAX_VALUE;
    for (SizePair sizePair : validPreviewSizes) {
      Size size = sizePair.preview;
      int diff =
          Math.abs(size.getWidth() - desiredWidth) + Math.abs(size.getHeight() - desiredHeight);
      if (diff < minDiff) {
        selectedPair = sizePair;
        minDiff = diff;
      }
    }

    return selectedPair;
  }

  public static class SizePair {
    public final Size preview;
    @Nullable public final Size picture;

    SizePair(Camera.Size previewSize, @Nullable Camera.Size pictureSize) {
      preview = new Size(previewSize.width, previewSize.height);
      picture = pictureSize != null ? new Size(pictureSize.width, pictureSize.height) : null;
    }

    public SizePair(Size previewSize, @Nullable Size pictureSize) {
      preview = previewSize;
      picture = pictureSize;
    }
  }


  public static List<SizePair> generateValidPreviewSizeList(Camera camera) {
    Parameters parameters = camera.getParameters();
    List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
    List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
    List<SizePair> validPreviewSizes = new ArrayList<>();
    for (Camera.Size previewSize : supportedPreviewSizes) {
      float previewAspectRatio = (float) previewSize.width / (float) previewSize.height;

      for (Camera.Size pictureSize : supportedPictureSizes) {
        float pictureAspectRatio = (float) pictureSize.width / (float) pictureSize.height;
        if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
          validPreviewSizes.add(new SizePair(previewSize, pictureSize));
          break;
        }
      }
    }

    if (validPreviewSizes.size() == 0) {
      Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size");
      for (Camera.Size previewSize : supportedPreviewSizes) {
        validPreviewSizes.add(new SizePair(previewSize, null));
      }
    }

    return validPreviewSizes;
  }


  @SuppressLint("InlinedApi")
  private static int[] selectPreviewFpsRange(Camera camera, float desiredPreviewFps) {
    int desiredPreviewFpsScaled = (int) (desiredPreviewFps * 1000.0f);

    int[] selectedFpsRange = null;
    int minUpperBoundDiff = Integer.MAX_VALUE;
    int minLowerBound = Integer.MAX_VALUE;
    List<int[]> previewFpsRangeList = camera.getParameters().getSupportedPreviewFpsRange();
    for (int[] range : previewFpsRangeList) {
      int upperBoundDiff =
          Math.abs(desiredPreviewFpsScaled - range[Parameters.PREVIEW_FPS_MAX_INDEX]);
      int lowerBound = range[Parameters.PREVIEW_FPS_MIN_INDEX];
      if (upperBoundDiff <= minUpperBoundDiff && lowerBound <= minLowerBound) {
        selectedFpsRange = range;
        minUpperBoundDiff = upperBoundDiff;
        minLowerBound = lowerBound;
      }
    }
    return selectedFpsRange;
  }


  private void setRotation(Camera camera, Parameters parameters, int cameraId) {
    WindowManager windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
    int degrees = 0;
    int rotation = windowManager.getDefaultDisplay().getRotation();
    switch (rotation) {
      case Surface.ROTATION_0:
        degrees = 0;
        break;
      case Surface.ROTATION_90:
        degrees = 90;
        break;
      case Surface.ROTATION_180:
        degrees = 180;
        break;
      case Surface.ROTATION_270:
        degrees = 270;
        break;
      default:
        Log.e(TAG, "Bad rotation value: " + rotation);
    }

    CameraInfo cameraInfo = new CameraInfo();
    Camera.getCameraInfo(cameraId, cameraInfo);

    int displayAngle;
    if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
      this.rotationDegrees = (cameraInfo.orientation + degrees) % 360;
      displayAngle = (360 - this.rotationDegrees) % 360;
    } else {
      this.rotationDegrees = (cameraInfo.orientation - degrees + 360) % 360;
      displayAngle = this.rotationDegrees;
    }
    Log.d(TAG, "Display rotation is: " + rotation);
    Log.d(TAG, "Camera face is: " + cameraInfo.facing);
    Log.d(TAG, "Camera rotation is: " + cameraInfo.orientation);
    Log.d(TAG, "RotationDegrees is: " + this.rotationDegrees);

    camera.setDisplayOrientation(displayAngle);
    parameters.setRotation(this.rotationDegrees);
  }

  @SuppressLint("InlinedApi")
  private byte[] createPreviewBuffer(Size previewSize) {
    int bitsPerPixel = ImageFormat.getBitsPerPixel(IMAGE_FORMAT);
    long sizeInBits = (long) previewSize.getHeight() * previewSize.getWidth() * bitsPerPixel;
    int bufferSize = (int) Math.ceil(sizeInBits / 8.0d) + 1;

    byte[] byteArray = new byte[bufferSize];
    ByteBuffer buffer = ByteBuffer.wrap(byteArray);
    if (!buffer.hasArray() || (buffer.array() != byteArray)) {
      throw new IllegalStateException("Failed to create valid buffer for camera source.");
    }

    bytesToByteBuffer.put(byteArray, buffer);
    return byteArray;
  }



  private class CameraPreviewCallback implements Camera.PreviewCallback {
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
      processingRunnable.setNextFrame(data, camera);
    }
  }

  public void setMachineLearningFrameProcessor(VisionImageProcessor processor) {
    synchronized (processorLock) {
      cleanScreen();
      if (frameProcessor != null) {
        frameProcessor.stop();
      }
      frameProcessor = processor;
    }
  }

  private class FrameProcessingRunnable implements Runnable {

    private final Object lock = new Object();
    private boolean active = true;

    private ByteBuffer pendingFrameData;

    FrameProcessingRunnable() {}


    void setActive(boolean active) {
      synchronized (lock) {
        this.active = active;
        lock.notifyAll();
      }
    }


    @SuppressWarnings("ByteBufferBackingArray")
    void setNextFrame(byte[] data, Camera camera) {
      synchronized (lock) {
        if (pendingFrameData != null) {
          camera.addCallbackBuffer(pendingFrameData.array());
          pendingFrameData = null;
        }

        if (!bytesToByteBuffer.containsKey(data)) {
          Log.d(
              TAG,
              "Skipping frame. Could not find ByteBuffer associated with the image "
                  + "data from the camera.");
          return;
        }

        pendingFrameData = bytesToByteBuffer.get(data);

        lock.notifyAll();
      }
    }

    @SuppressLint("InlinedApi")
    @SuppressWarnings({"GuardedBy", "ByteBufferBackingArray"})
    @Override
    public void run() {
      ByteBuffer data;

      while (true) {
        synchronized (lock) {
          while (active && (pendingFrameData == null)) {
            try {
              lock.wait();
            } catch (InterruptedException e) {
              Log.d(TAG, "Frame processing loop terminated.", e);
              return;
            }
          }

          if (!active) {
            return;
          }

          data = pendingFrameData;
          pendingFrameData = null;
        }


        try {
          synchronized (processorLock) {
            frameProcessor.processByteBuffer(
                data,
                new FrameMetadata.Builder()
                    .setWidth(previewSize.getWidth())
                    .setHeight(previewSize.getHeight())
                    .setRotation(rotationDegrees)
                    .build(),
                graphicOverlay);
          }
        } catch (Exception t) {
          Log.e(TAG, "Exception thrown from receiver.", t);
        } finally {
          camera.addCallbackBuffer(data.array());
        }
      }
    }
  }


  private void cleanScreen() {
    graphicOverlay.clear();
  }
}
