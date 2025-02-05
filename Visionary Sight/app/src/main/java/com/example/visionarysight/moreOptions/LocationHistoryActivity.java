package com.example.visionarysight.moreOptions;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visionarysight.LocationActivity;
import com.example.visionarysight.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationHistoryActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private ListView listView;
    private List<Map<String, String>> locationList;
    private TextToSpeech tts;
    private Button goBackButton;
    private TextView loadingTextView;
    private boolean goBackButtonClickedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_history);

        listView = findViewById(R.id.listView);
        loadingTextView = findViewById(R.id.loadingTextView);
        goBackButton = findViewById(R.id.goBackButton);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();
        locationList = new ArrayList<>();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });

        setGoBackButtonBehaviour();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            fetchLocationHistory();
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, String> selectedLocation = locationList.get(position);
            String details = selectedLocation.get("timestamp") + " - " + selectedLocation.get("placeName") +
                    " located at Latitude: " + selectedLocation.get("latitude") + ", Longitude: " + selectedLocation.get("longitude");
            speakText(details);
        });
    }

    private void setGoBackButtonBehaviour() {
        goBackButton.setOnClickListener(v -> {
            if (!goBackButtonClickedOnce) {
                speakText("Go Back button Clicked");
                goBackButtonClickedOnce = true;
            } else {
                speakText("Going back");
                finish();
                goBackButtonClickedOnce = false;
            }
        });

        goBackButton.setOnLongClickListener(v -> {
            speakText("Going back");
            finish();
            return true;
        });
    }

    private void goToLocationActivity() {
        Intent intent = new Intent(LocationHistoryActivity.this, LocationActivity.class);
        startActivity(intent);
        finish();
    }

    private void fetchLocationHistory() {
        showLoading(true);

        firestore.collection("LocationHistory")
                .document(currentUser.getUid())
                .collection("userLocations")
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot snapshot = task.getResult();
                        locationList.clear();

                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            String placeName = document.getString("fullAddress");
                            String timestamp = document.getString("timestamp");

                            if (placeName != null  && timestamp != null) {
                                Map<String, String> locationMap = new HashMap<>();
                                locationMap.put("placeName", placeName);
                                locationMap.put("timestamp", "Visited at: " + formatTimestamp(timestamp));
                                locationList.add(locationMap);
                            }
                        }

                        if (!locationList.isEmpty()) {
                            listView.setAdapter(new SimpleAdapter(
                                    LocationHistoryActivity.this,
                                    locationList,
                                    R.layout.list_item_nearby,
                                    new String[]{"timestamp", "placeName"},
                                    new int[]{R.id.timestampTextView, R.id.placeTextView}
                            ));

                            readAllAddresses();
                        } else {
                            Toast.makeText(LocationHistoryActivity.this, "No location history available", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LocationHistoryActivity.this, "Failed to fetch location history", Toast.LENGTH_SHORT).show();
                        Log.e("LocationHistoryActivity", "Error getting documents: ", task.getException());
                    }
                });
    }

    private void readAllAddresses() {
        for (Map<String, String> location : locationList) {
            String message = location.get("timestamp") + " - " + location.get("placeName");
            speakText(message);
        }
    }

    private String formatTimestamp(String timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(sdf.parse(timestamp));
        } catch (ParseException e) {
            Log.e("LocationHistoryActivity", "Error formatting timestamp: ", e);
            return timestamp;
        }
    }

    private void showLoading(boolean isLoading) {
        loadingTextView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            loadingTextView.setText("Loading...");
        }
    }

    private void speakText(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
