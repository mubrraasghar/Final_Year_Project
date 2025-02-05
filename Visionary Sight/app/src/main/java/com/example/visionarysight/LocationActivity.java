package com.example.visionarysight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.visionarysight.moreOptions.LocationHistoryActivity;
import com.example.visionarysight.moreOptions.NearbyPlacesHistoryActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class LocationActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TextToSpeech tts;
    private Button buttonFetchLocation;
    private Button buttonNearbyPlaces;
    private Button buttonMainMenu;
    private Button buttonLocationHistory;
    private Button buttonNearbyPlacesHistory;
    private boolean firstClick = true;
    private View lastClickedButton;
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        tts = new TextToSpeech(this, this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        buttonFetchLocation = findViewById(R.id.button_fetch_location);
        buttonNearbyPlaces = findViewById(R.id.button_nearby_places);
        buttonMainMenu = findViewById(R.id.button_main_menu);
        buttonLocationHistory = findViewById(R.id.button_location_history);
        buttonNearbyPlacesHistory = findViewById(R.id.button_nearby_places_history);

        setupButton(buttonFetchLocation, "Fetch Location", this::fetchNearestPlace);
        setupButton(buttonNearbyPlaces, "Nearby Places", this::findNearbyPlaces);
        setupButton(buttonMainMenu, "Go Back ", this::goToMainMenu);
        setupButton(buttonLocationHistory, "Fetch Location History", this::checkLocationHistory);
        setupButton(buttonNearbyPlacesHistory, "Nearby Places History", this::checkNearbyPlacesHistory);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), " ");
        }
        placesClient = Places.createClient(this);

        requestLocationPermission();
    }

    private void checkNearbyPlacesHistory() {
        Intent intent = new Intent(LocationActivity.this, NearbyPlacesHistoryActivity.class);
        startActivity(intent);
    }

    private void setupButton(Button button, String buttonText, Runnable action) {
        button.setOnClickListener(v -> {
            if (firstClick || button != lastClickedButton) {
                speak("You are clicking on " + buttonText + ". Click again to perform " + buttonText);
                firstClick = false;
                lastClickedButton = button;
            } else {
                speak("Performing " + buttonText);
                action.run();
                firstClick = true;
                lastClickedButton = null;
            }
        });

        button.setOnLongClickListener(v -> {
            speak("There are four options: Fetch Location History, Nearby Places History and Go Back . Click on any button to hear its name, and click again to perform the action.");
            return true;
        });
    }

    @SuppressLint("MissingPermission")
    private void fetchNearestPlace() {
        if (checkLocationPermission()) {
            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
            FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();

            placesClient.findCurrentPlace(request)
                    .addOnSuccessListener((response) -> {
                        List<PlaceLikelihood> placeLikelihoods = response.getPlaceLikelihoods();
                        if (placeLikelihoods != null && !placeLikelihoods.isEmpty()) {

                            Place nearestPlace = placeLikelihoods.get(0).getPlace();
                            String placeName = nearestPlace.getName();
                            String fullAddress = nearestPlace.getAddress();


                            if (fullAddress == null || fullAddress.isEmpty()) {
                                double latitude = nearestPlace.getLatLng().latitude;
                                double longitude = nearestPlace.getLatLng().longitude;
                                fullAddress = getFullAddress(latitude, longitude);
                            }


                            translateAddress(fullAddress, placeName);

                        } else {
                            speak("Unable to find nearby places. Try again.");
                        }
                    })
                    .addOnFailureListener((exception) -> {
                        speak("Failed to fetch nearby places. Please try again.");
                        Log.e("PlacesAPI", "Error fetching places: ", exception);
                    });
        } else {
            requestLocationPermission();
        }
    }

    private String getFullAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String subLocality = address.getSubLocality();
                String city = address.getLocality();
                String state = address.getAdminArea();
                String country = address.getCountryName();


                return (subLocality != null ? subLocality + ", " : "") +
                        (city != null ? city + ", " : "") +
                        (state != null ? state + ", " : "") +
                        (country != null ? country : "");
            }
        } catch (IOException e) {
            Log.e("Geocoder", "Failed to get address: ", e);
        }
        return "Unknown location";
    }

    private void storePlaceInFirestore(String fullAddress) {
        if (currentUser != null) {
            Map<String, Object> placeData = new HashMap<>();
            placeData.put("fullAddress", fullAddress);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());
            String formattedDate = sdf.format(new Date());
            String formattedDocumentId = formattedDate.replace(" ", "_").replace(":", "-");

            placeData.put("timestamp", formattedDate);

            firestore.collection("LocationHistory")
                    .document(currentUser.getUid())
                    .collection("userLocations")
                    .document(formattedDocumentId)
                    .set(placeData)
                    .addOnSuccessListener(documentReference -> Log.d("Firestore", "Place stored: " + fullAddress))
                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to store place: " + fullAddress));
        } else {
            Log.e("Firestore", "User is not authenticated.");
        }
    }

    private void translateAddress(String fullAddress, String placeName) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://translation.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TranslationService service = retrofit.create(TranslationService.class);
        String apiKey = " ";
        Call<TranslationResponse> call = service.translate(fullAddress, "en", apiKey);

        call.enqueue(new Callback<TranslationResponse>() {
            @Override
            public void onResponse(Call<TranslationResponse> call, Response<TranslationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String translatedAddress = response.body().getData().getTranslations().get(0).getTranslatedText();
                    storePlaceInFirestore(placeName + ", " + translatedAddress);
                    String message = "The nearest place is: " + placeName + ", " + translatedAddress;
                    speak(message);
                    showPopupWithTimeout(message, 12000);
                } else {
                    speak("Translation failed. Displaying original address.");
                    storePlaceInFirestore(placeName + ", " + fullAddress);
                    String message = "The nearest place is: " + placeName + ", " + fullAddress;
                    speak(message);
                    showPopupWithTimeout(message, 12000);
                }
            }

            @Override
            public void onFailure(Call<TranslationResponse> call, Throwable t) {
                speak("Translation service is unavailable. Displaying original address.");
                storePlaceInFirestore(placeName + ", " + fullAddress);
                String message = "The nearest place is: " + placeName + ", " + fullAddress;
                speak(message);
                showPopupWithTimeout(message, 12000);
            }
        });
    }

    private void findNearbyPlaces() {
        Intent intent = new Intent(LocationActivity.this, NearbyPlacesActivity.class);
        startActivity(intent);
    }

    private void goToMainMenu() {
                    speak("Going back.");
                    finish();
    }

    private void checkLocationHistory() {
        Intent intent = new Intent(LocationActivity.this, LocationHistoryActivity.class);
        startActivity(intent);
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                speak("Language not supported.");
            } else {
                speak("You are in Location History Activity. Long press for instructions.");
            }
        } else {
            speak("Text to speech initialization failed.");
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        speak("You are in Location History Activity. Long press for instructions.");
    }


    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showPopupWithTimeout(String message, int timeoutMillis) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setCancelable(true);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        new android.os.Handler().postDelayed(() -> {
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
        }, timeoutMillis);

        alertDialog.setOnDismissListener(dialog -> alertDialog.dismiss());
        alertDialog.getWindow().getDecorView().setOnTouchListener((v, event) -> {
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }


    interface TranslationService {
        @GET("language/translate/v2")
        Call<TranslationResponse> translate(
                @Query("q") String text,
                @Query("target") String targetLanguage,
                @Query("key") String apiKey);
    }


    class TranslationResponse {
        private Data data;

        public Data getData() {
            return data;
        }

        class Data {
            private List<Translation> translations;

            public List<Translation> getTranslations() {
                return translations;
            }

            class Translation {
                private String translatedText;

                public String getTranslatedText() {
                    return translatedText;
                }
            }
        }
    }
}
