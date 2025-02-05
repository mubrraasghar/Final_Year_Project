package com.example.visionarysight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class WeatherUpdatesActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Handler handler = new Handler();

    private static final String API_KEY = "b782c71a609b40856f6b037b10204dd9";

    private TextView cityTextView;
    private TextView temperatureTextView;
    private TextView humidityTextView;
    private TextView descriptionTextView;
    private Button readAgainButton;
    private Button goBackButton;

    private boolean firstClickReadAgain = true;
    private boolean firstClickGoBack = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_updates);

        cityTextView = findViewById(R.id.cityTextView);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        readAgainButton = findViewById(R.id.read_again);
        goBackButton = findViewById(R.id.buttonGoBack);

        tts = new TextToSpeech(this, this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        getCurrentLocationWeather();

        readAgainButton.setOnClickListener(v -> {
            if (firstClickReadAgain) {
                speak("Click again to read the weather update.");
                firstClickReadAgain = false;
                handler.postDelayed(() -> firstClickReadAgain = true, 5000);
            } else {
                speakWeatherInfo();
                firstClickReadAgain = true;
            }
        });

        goBackButton.setOnClickListener(v -> {
            if (firstClickGoBack) {
                speak("Click again to go back.");
                firstClickGoBack = false;
                handler.postDelayed(() -> firstClickGoBack = true, 5000);
            } else {
                speak("going back.");
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finish();
                firstClickGoBack = true;
            }
        });
    }

    private void getCurrentLocationWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    new FetchWeatherTask(latitude, longitude).execute();
                } else {
                    Toast.makeText(WeatherUpdatesActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchWeatherTask extends AsyncTask<Void, Void, String> {
        private final double latitude;
        private final double longitude;

        public FetchWeatherTask(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String urlString = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + API_KEY + "&units=metric";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);

                    String cityName = jsonObject.getString("name");
                    JSONObject main = jsonObject.getJSONObject("main");
                    String temperature = main.getString("temp");
                    String humidity = main.getString("humidity");
                    JSONObject weather = jsonObject.getJSONArray("weather").getJSONObject(0);
                    String description = weather.getString("description");

                    cityTextView.setText("City: " + cityName);
                    temperatureTextView.setText("Temperature: " + temperature + "°C");
                    humidityTextView.setText("Humidity: " + humidity + "%");
                    descriptionTextView.setText("Condition: " + description);

                    speakWeatherInfo();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(WeatherUpdatesActivity.this, "Error parsing weather data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(WeatherUpdatesActivity.this, "Failed to fetch weather data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void speakWeatherInfo() {
        String city = cityTextView.getText().toString();
        String temperature = temperatureTextView.getText().toString();
        String humidity = humidityTextView.getText().toString();
        String description = descriptionTextView.getText().toString();

        String weatherInfo = city + ". " + temperature + ". " + humidity + ". " + description + ".";
        speak(weatherInfo);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.getDefault());
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationWeather();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
