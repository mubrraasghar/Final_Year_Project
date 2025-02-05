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

import com.example.visionarysight.NearbyPlacesActivity;
import com.example.visionarysight.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NearbyPlacesHistoryActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private ListView listView;
    private List<Map<String, String>> placesList;
    private TextToSpeech tts;
    private Button goBackButton;
    private TextView loadingTextView;
    private boolean goBackButtonClickedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_places_history);

        listView = findViewById(R.id.listView);
        loadingTextView = findViewById(R.id.loadingTextView);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });

        goBackButton = findViewById(R.id.goBackButton);
        setGoBackButtonBehaviour();

        if (currentUser != null) {
            fetchPlacesHistory();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, String> selectedPlace = placesList.get(position);
            String place = selectedPlace.get("place");
            String timestamp = selectedPlace.get("timestamp");

            String fullText = timestamp + "Place: " + place + ".";
            speakText(fullText);
        });
    }

    private void setGoBackButtonBehaviour() {
        goBackButton.setOnClickListener(v -> {
            if (!goBackButtonClickedOnce) {
                speakText("Go Back button clicked");
                goBackButtonClickedOnce = true;
            } else {
                speakText("Going back");
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

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

    private void goToNearbyPlacesActivity() {
        Intent intent = new Intent(NearbyPlacesHistoryActivity.this, NearbyPlacesActivity.class);
        startActivity(intent);
        finish();
    }

    private void fetchPlacesHistory() {
        placesList = new ArrayList<>();
        showLoading(true);

        firestore.collection("LocationHistory")
                .document(currentUser.getUid())
                .collection("UserNearbyPlaces")
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();

                        if (!querySnapshot.isEmpty()) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                String timestamp = document.getId();
                                List<String> places = (List<String>) document.get("allPlacesInfo");

                                if (places != null && !places.isEmpty()) {
                                    StringBuilder placesBuilder = new StringBuilder();

                                    for (String place : places) {
                                        placesBuilder.append(place).append("\n");
                                    }

                                    Map<String, String> placeMap = new HashMap<>();
                                    placeMap.put("timestamp", "Visited at: " + timestamp);
                                    placeMap.put("place", placesBuilder.toString().trim());
                                    placesList.add(placeMap);
                                }
                            }

                            if (!placesList.isEmpty()) {
                                SimpleAdapter adapter = new SimpleAdapter(
                                        NearbyPlacesHistoryActivity.this,
                                        placesList,
                                        R.layout.list_item_nearby,
                                        new String[]{"timestamp", "place"},
                                        new int[]{R.id.timestampTextView, R.id.placeTextView}
                                );
                                listView.setAdapter(adapter);
                            } else {
                                Toast.makeText(NearbyPlacesHistoryActivity.this, "No places history available", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(NearbyPlacesHistoryActivity.this, "No documents found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(NearbyPlacesHistoryActivity.this, "Failed to fetch places history", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void showLoading(boolean isLoading) {
        loadingTextView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        listView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
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
