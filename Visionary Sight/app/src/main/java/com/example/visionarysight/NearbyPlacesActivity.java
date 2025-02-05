package com.example.visionarysight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NearbyPlacesActivity extends AppCompatActivity {

    private static final String TAG = "NearbyPlacesActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private TextToSpeech textToSpeech;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private ListView listViewPlaces;
    private Button buttonGoBack, buttonReadAllPlaces;
    private boolean isGoBackClickedOnce = false;
    private FirebaseFirestore firestore;
    private List<String> placeDetailsList;
    private boolean isDataLoaded = false;

    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private StringBuilder allPlacesDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_places);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        listViewPlaces = findViewById(R.id.listViewPlaces);
        buttonGoBack = findViewById(R.id.button_go_back);
        buttonReadAllPlaces = findViewById(R.id.button_read_all_places);

        firestore = FirebaseFirestore.getInstance();

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        placeDetailsList = new ArrayList<>();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLastKnownLocation();
        }

        buttonGoBack.setOnClickListener(v -> {
            if (!isGoBackClickedOnce) {
                textToSpeech.speak("Button clicked", TextToSpeech.QUEUE_FLUSH, null, null);
                isGoBackClickedOnce = true;
            } else {
                textToSpeech.speak("Going back", TextToSpeech.QUEUE_FLUSH, null, null);
                onBackPressed();
            }
        });

        buttonReadAllPlaces.setOnClickListener(v -> {
            readAllPlaces();
        });

        listViewPlaces.setOnItemClickListener((parent, view, position, id) -> {
            String placeDetail = placeDetailsList.get(position);
            textToSpeech.speak(placeDetail, TextToSpeech.QUEUE_FLUSH, null, null);
        });
    }

    @SuppressLint("MissingPermission")
    private void getLastKnownLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = location;
                new FetchPlacesTask().execute(currentLocation.getLatitude(), currentLocation.getLongitude());
            } else {
                textToSpeech.speak("Unable to find location. Try again.", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    private void readAllPlaces() {
        if (placeDetailsList.isEmpty()) {
            textToSpeech.speak("No places available to read.", TextToSpeech.QUEUE_FLUSH, null, null);
            return;
        }

        for (String place : placeDetailsList) {
            textToSpeech.speak(place, TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    private class FetchPlacesTask extends AsyncTask<Double, Void, List<String>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            startLoadingMessage();
            allPlacesDetails = new StringBuilder();
        }

        @Override
        protected List<String> doInBackground(Double... params) {
            List<String> places = new ArrayList<>();
            double latitude = params[0];
            double longitude = params[1];
            String apiKey = " ";
            String urlString = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                    + latitude + "," + longitude
                    + "&radius=100&key=" + apiKey;

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray results = jsonObject.getJSONArray("results");

                for (int i = 0; i < results.length(); i++) {
                    JSONObject place = results.getJSONObject(i);
                    String placeName = place.getString("name");

                    Location placeLocation = new Location("");
                    placeLocation.setLatitude(place.getJSONObject("geometry").getJSONObject("location").getDouble("lat"));
                    placeLocation.setLongitude(place.getJSONObject("geometry").getJSONObject("location").getDouble("lng"));

                    String direction = getDirection(currentLocation, placeLocation);
                    Integer distanceToPlace = (int) currentLocation.distanceTo(placeLocation);

                    String numberedPlace = "\n" + (i + 1) + ". " + placeName + " is " + distanceToPlace + " meters away to the " + direction;

                    if (distanceToPlace <= 17) {
                        numberedPlace += ". You are at this place.\n";
                    } else {
                        numberedPlace += ".\n";
                    }

                    placeDetailsList.add(numberedPlace);
                    places.add(numberedPlace);
                    allPlacesDetails.append(numberedPlace+"\n");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching places: " + e.getMessage());
            }
            return places;
        }

        @Override
        protected void onPostExecute(List<String> places) {
            stopLoadingMessage();
            if (!places.isEmpty()) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(NearbyPlacesActivity.this, R.layout.list_item_place, places);
                listViewPlaces.setAdapter(adapter);
                textToSpeech.speak("Here are some nearby places.", TextToSpeech.QUEUE_FLUSH, null, null);
                readAllPlaces();
                storePlacesInFirestore(placeDetailsList);
            } else {
                Toast.makeText(NearbyPlacesActivity.this, "No places found nearby.", Toast.LENGTH_SHORT).show();
                textToSpeech.speak("No places found nearby.", TextToSpeech.QUEUE_FLUSH, null, null);
            }
            isDataLoaded = true;
        }


        private String getDirection(Location current, Location place) {
            double bearing = current.bearingTo(place);

            if (bearing < 0) {
                bearing += 360;
            }

            if (bearing >= 337.5 || bearing < 22.5) {
                return "north";
            } else if (bearing >= 22.5 && bearing < 67.5) {
                return "northeast";
            } else if (bearing >= 67.5 && bearing < 112.5) {
                return "east";
            } else if (bearing >= 112.5 && bearing < 157.5) {
                return "southeast";
            } else if (bearing >= 157.5 && bearing < 202.5) {
                return "south";
            } else if (bearing >= 202.5 && bearing < 247.5) {
                return "southwest";
            } else if (bearing >= 247.5 && bearing < 292.5) {
                return "west";
            } else if (bearing >= 292.5 && bearing < 337.5) {
                return "northwest";
            }

            return "unknown";
        }


        private void storePlacesInFirestore(List<String> placesList) {
            Map<String, Object> placeData = new HashMap<>();
            placeData.put("allPlacesInfo", placesList);

            firestore.collection("LocationHistory")
                    .document(currentUser.getUid())
                    .collection("UserNearbyPlaces")
                    .document(new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(new Date()))
                    .set(placeData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Nearby places successfully written as a list!"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error writing nearby places", e));
        }


        private void startLoadingMessage() {
            isDataLoaded = false;
            textToSpeech.speak("Fetching nearby places. Please wait.", TextToSpeech.QUEUE_FLUSH, null, null);
        }

        private void stopLoadingMessage() {
            textToSpeech.speak("Loading complete.", TextToSpeech.QUEUE_FLUSH, null, null);
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
