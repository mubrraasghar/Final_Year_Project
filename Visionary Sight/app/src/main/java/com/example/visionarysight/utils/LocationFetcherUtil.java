
package com.example.visionarysight.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

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

public class LocationFetcherUtil {

    private final Context context;
    private final PlacesClient placesClient;
    private final FusedLocationProviderClient fusedLocationClient;
    private final FirebaseFirestore firestore;
    private final FirebaseUser currentUser;
    private final String translationApiKey = "AIzaSyDomaEDu0jSKF9L9_Bd0zN4-cZdSUhXcIw";

    public LocationFetcherUtil(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.firestore = FirebaseFirestore.getInstance();
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (!Places.isInitialized()) {
            Places.initialize(context, "AIzaSyBNtCCXnMEv1by1Mn49cQnesErxNteyzvM");
        }
        this.placesClient = Places.createClient(context);
    }

    public interface LocationFetchCallback {
        void onLocationFetched(String locationText);
        void onError(String errorMessage);
    }

    @SuppressLint("MissingPermission")
    public void fetchLocation(LocationFetchCallback callback) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();

        placesClient.findCurrentPlace(request)
                .addOnSuccessListener(response -> {
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

                        translateAddress(fullAddress, placeName, callback);

                    } else {
                        callback.onError("No nearby places found.");
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e("PlacesAPI", "Error fetching places: ", exception);
                    callback.onError("Failed to fetch location.");
                });
    }

    private String getFullAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
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

    private void translateAddress(String fullAddress, String placeName, LocationFetchCallback callback) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://translation.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TranslationService service = retrofit.create(TranslationService.class);
        Call<TranslationResponse> call = service.translate(fullAddress, "en", translationApiKey);

        call.enqueue(new Callback<TranslationResponse>() {
            @Override
            public void onResponse(Call<TranslationResponse> call, Response<TranslationResponse> response) {
                String translatedAddress = fullAddress;
                if (response.isSuccessful() && response.body() != null) {
                    translatedAddress = response.body().getData().getTranslations().get(0).getTranslatedText();
                }

                String locationText = placeName + ", " + translatedAddress;
                callback.onLocationFetched(locationText);

                storeLocationToFirestore(locationText);
            }

            @Override
            public void onFailure(Call<TranslationResponse> call, Throwable t) {
                String locationText = placeName + ", " + fullAddress;
                callback.onLocationFetched(locationText);

                storeLocationToFirestore(locationText);
            }
        });
    }

    private void storeLocationToFirestore(String locationText) {
        if (currentUser != null) {
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("fullAddress", locationText);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getDefault());
            String timestamp = sdf.format(new Date());

            locationData.put("timestamp", timestamp);

            firestore.collection("LocationHistory")
                    .document(currentUser.getUid())
                    .collection("userLocations")
                    .document(timestamp.replace(" ", "_").replace(":", "-"))
                    .set(locationData)
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Location stored: " + locationText))
                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to store location", e));
        } else {
            Log.e("Firestore", "User is not authenticated.");
        }
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
