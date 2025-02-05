package com.example.visionarysight.SOS;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.visionarysight.R;
import com.example.visionarysight.SOS.SOSModel.SOSMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditMessageActivity extends AppCompatActivity {

    private EditText messageInput;
    private Button buttonSaveMessage;
    private Button buttonGoBack;
    private TextView lastUpdate;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private DocumentReference sosMessageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_message);

        messageInput = findViewById(R.id.edit_message);
        buttonSaveMessage = findViewById(R.id.button_save_message);
        buttonGoBack = findViewById(R.id.button_go_back);
        lastUpdate = findViewById(R.id.updated_time);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        if (currentUser != null) {
            sosMessageRef = firestore.collection("sosMessages").document(currentUser.getUid());
            loadSOSMessage();
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            finish();
        }

        buttonSaveMessage.setOnClickListener(v -> saveSOSMessage());
        buttonGoBack.setOnClickListener(v -> finish());
    }

    private void loadSOSMessage() {
        sosMessageRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        SOSMessage sosMessage = documentSnapshot.toObject(SOSMessage.class);
                        if (sosMessage != null) {
                            messageInput.setText(sosMessage.getContent());
                            lastUpdate.setText("Last Updated: " + sosMessage.getTimestamp());
                        }
                    } else {
                        createDefaultSOSMessage();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("EditMessageActivity", "Error fetching SOS message", e);
                    Toast.makeText(this, "Error loading SOS message.", Toast.LENGTH_SHORT).show();
                });
    }

    private void createDefaultSOSMessage() {
        String defaultMessage = "This is the default SOS template message.";
        String timestamp = getCurrentTimestamp();
        SOSMessage sosMessage = new SOSMessage(defaultMessage, timestamp);
        sosMessageRef.set(sosMessage)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText(defaultMessage);
                    lastUpdate.setText("Last Updated: " + timestamp);
                    Toast.makeText(this, "Default SOS message created.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("EditMessageActivity", "Error creating default SOS message", e);
                    Toast.makeText(this, "Error creating default SOS message.", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveSOSMessage() {
        String newMessage = messageInput.getText().toString().trim();
        if (!newMessage.isEmpty()) {
            String timestamp = getCurrentTimestamp();
            SOSMessage updatedMessage = new SOSMessage(newMessage, timestamp);
            sosMessageRef.set(updatedMessage)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "SOS message updated successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EditMessageActivity", "Error updating SOS message", e);
                        Toast.makeText(this, "Error updating SOS message.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "SOS message cannot be empty.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
