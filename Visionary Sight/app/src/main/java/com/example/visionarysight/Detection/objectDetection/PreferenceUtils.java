package com.example.visionarysight.Detection.objectDetection;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build.VERSION_CODES;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.camera.core.CameraSelector;

import com.example.visionarysight.R;
import com.google.android.gms.common.images.Size;
import com.google.common.base.Preconditions;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

public class PreferenceUtils {

  private static final int POSE_DETECTOR_PERFORMANCE_MODE_FAST = 1;

  static void saveString(Context context, @StringRes int prefKeyId, @Nullable String value) {
    PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(context.getString(prefKeyId), value)
            .apply();
  }

  @Nullable
  public static CameraSource.SizePair getCameraPreviewSizePair(Context context, int cameraId) {
    Preconditions.checkArgument(
            cameraId == CameraSource.CAMERA_FACING_BACK
                    || cameraId == CameraSource.CAMERA_FACING_FRONT);
    String previewSizePrefKey;
    String pictureSizePrefKey;
    if (cameraId == CameraSource.CAMERA_FACING_BACK) {
      previewSizePrefKey = context.getString(R.string.pref_key_rear_camera_preview_size);
      pictureSizePrefKey = context.getString(R.string.pref_key_rear_camera_picture_size);
    } else {
      previewSizePrefKey = context.getString(R.string.pref_key_front_camera_preview_size);
      pictureSizePrefKey = context.getString(R.string.pref_key_front_camera_picture_size);
    }

    try {
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
      return new CameraSource.SizePair(
              Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)),
              Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null)));
    } catch (Exception e) {
      return null;
    }
  }

  @RequiresApi(VERSION_CODES.LOLLIPOP)
  @Nullable
  public static android.util.Size getCameraXTargetResolution(Context context, int lensfacing) {
    Preconditions.checkArgument(
            lensfacing == CameraSelector.LENS_FACING_BACK
                    || lensfacing == CameraSelector.LENS_FACING_FRONT);
    String prefKey =
            lensfacing == CameraSelector.LENS_FACING_BACK
                    ? context.getString(R.string.pref_key_camerax_rear_camera_target_resolution)
                    : context.getString(R.string.pref_key_camerax_front_camera_target_resolution);
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    try {
      return android.util.Size.parseSize(sharedPreferences.getString(prefKey, null));
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean shouldHideDetectionInfo(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(R.string.pref_key_info_hide);
    return sharedPreferences.getBoolean(prefKey, false);
  }

  public static ObjectDetectorOptions getObjectDetectorOptionsForStillImage(Context context) {
    return getObjectDetectorOptions(
            context,
            R.string.pref_key_still_image_object_detector_enable_multiple_objects,
            R.string.pref_key_still_image_object_detector_enable_classification,
            ObjectDetectorOptions.SINGLE_IMAGE_MODE);
  }

  public static ObjectDetectorOptions getObjectDetectorOptionsForLivePreview(Context context) {
    return getObjectDetectorOptions(
            context,
            R.string.pref_key_live_preview_object_detector_enable_multiple_objects,
            R.string.pref_key_live_preview_object_detector_enable_classification,
            ObjectDetectorOptions.STREAM_MODE);
  }

  private static ObjectDetectorOptions getObjectDetectorOptions(
          Context context,
          @StringRes int prefKeyForMultipleObjects,
          @StringRes int prefKeyForClassification,
          @ObjectDetectorOptionsBase.DetectorMode int mode) {

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    boolean enableMultipleObjects =
            sharedPreferences.getBoolean(context.getString(prefKeyForMultipleObjects), false);
    boolean enableClassification =
            sharedPreferences.getBoolean(context.getString(prefKeyForClassification), true);

    ObjectDetectorOptions.Builder builder =
            new ObjectDetectorOptions.Builder().setDetectorMode(mode);
    if (enableMultipleObjects) {
      builder.enableMultipleObjects();
    }
    if (enableClassification) {
      builder.enableClassification();
    }
    return builder.build();
  }

  public static CustomObjectDetectorOptions getCustomObjectDetectorOptionsForStillImage(
          Context context, LocalModel localModel) {
    return getCustomObjectDetectorOptions(
            context,
            localModel,
            R.string.pref_key_still_image_object_detector_enable_multiple_objects,
            R.string.pref_key_still_image_object_detector_enable_classification,
            CustomObjectDetectorOptions.SINGLE_IMAGE_MODE);
  }

  public static CustomObjectDetectorOptions getCustomObjectDetectorOptionsForLivePreview(
          Context context, LocalModel localModel) {
    return getCustomObjectDetectorOptions(
            context,
            localModel,
            R.string.pref_key_live_preview_object_detector_enable_multiple_objects,
            R.string.pref_key_live_preview_object_detector_enable_classification,
            CustomObjectDetectorOptions.STREAM_MODE);
  }

  private static CustomObjectDetectorOptions getCustomObjectDetectorOptions(
          Context context,
          LocalModel localModel,
          @StringRes int prefKeyForMultipleObjects,
          @StringRes int prefKeyForClassification,
          @ObjectDetectorOptionsBase.DetectorMode int mode) {

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    boolean enableMultipleObjects =
            sharedPreferences.getBoolean(context.getString(prefKeyForMultipleObjects), false);
    boolean enableClassification =
            sharedPreferences.getBoolean(context.getString(prefKeyForClassification), true);

    CustomObjectDetectorOptions.Builder builder =
            new CustomObjectDetectorOptions.Builder(localModel).setDetectorMode(mode);
    if (enableMultipleObjects) {
      builder.enableMultipleObjects();
    }
    if (enableClassification) {
      builder.enableClassification().setMaxPerObjectLabelCount(1);
    }
    return builder.build();
  }

  public static boolean shouldEnableAutoZoom(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(R.string.pref_key_enable_auto_zoom);
    return sharedPreferences.getBoolean(prefKey, true);
  }

  public static boolean shouldGroupRecognizedTextInBlocks(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(R.string.pref_key_group_recognized_text_in_blocks);
    return sharedPreferences.getBoolean(prefKey, false);
  }

  public static boolean showLanguageTag(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(R.string.pref_key_show_language_tag);
    return sharedPreferences.getBoolean(prefKey, false);
  }

  public static boolean shouldShowTextConfidence(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(R.string.pref_key_show_text_confidence);
    return sharedPreferences.getBoolean(prefKey, false);
  }

  private static int getModeTypePreferenceValue(
          Context context, @StringRes int prefKeyResId, int defaultValue) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(prefKeyResId);
    return Integer.parseInt(sharedPreferences.getString(prefKey, String.valueOf(defaultValue)));
  }

  public static boolean isCameraLiveViewportEnabled(Context context) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    String prefKey = context.getString(R.string.pref_key_camera_live_viewport);
    return sharedPreferences.getBoolean(prefKey, false);
  }

  private PreferenceUtils() {}
}
