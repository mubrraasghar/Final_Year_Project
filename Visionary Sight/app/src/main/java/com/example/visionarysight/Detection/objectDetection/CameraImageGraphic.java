package com.example.visionarysight.Detection.objectDetection;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class CameraImageGraphic extends GraphicOverlay.Graphic {

  private final Bitmap bitmap;

  public CameraImageGraphic(GraphicOverlay overlay, Bitmap bitmap) {
    super(overlay);
    this.bitmap = bitmap;
  }

  @Override
  public void draw(Canvas canvas) {
    canvas.drawBitmap(bitmap, getTransformationMatrix(), null);
  }
}
