package com.example.visionarysight.Detection.objectDetection;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TemperatureMonitor implements SensorEventListener {

  private static final String TAG = "TemperatureMonitor";

  public Map<String, Float> sensorReadingsCelsius = new HashMap<>();

  private final SensorManager sensorManager;

  public TemperatureMonitor(Context context) {
    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    List<Sensor> allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
    for (Sensor sensor : allSensors) {
      if (sensor.getName().toLowerCase().contains("temperature")) {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

      }
    }
  }

  public void stop() {
    sensorManager.unregisterListener(this);
  }

  public void logTemperature() {
    for (Map.Entry<String, Float> entry : sensorReadingsCelsius.entrySet()) {
      float tempC = entry.getValue();
      if (tempC < 0) {
        continue;
      }
      float tempF = tempC * 1.8f + 32f;
      Log.i(TAG, String.format(Locale.US, "%s:\t%.1fC\t%.1fF", entry.getKey(), tempC, tempF));
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    sensorReadingsCelsius.put(sensorEvent.sensor.getName(), sensorEvent.values[0]);
  }
}
