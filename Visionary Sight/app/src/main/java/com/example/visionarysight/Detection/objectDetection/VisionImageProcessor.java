package com.example.visionarysight.Detection.objectDetection;

import android.graphics.Bitmap;

import androidx.camera.core.ImageProxy;

import com.google.mlkit.common.MlKitException;

import java.nio.ByteBuffer;

public interface VisionImageProcessor {

  void processBitmap(Bitmap bitmap, GraphicOverlay graphicOverlay);

  void processByteBuffer(
      ByteBuffer data, FrameMetadata frameMetadata, GraphicOverlay graphicOverlay)
      throws MlKitException;

  void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) throws MlKitException;

  void stop();
}
