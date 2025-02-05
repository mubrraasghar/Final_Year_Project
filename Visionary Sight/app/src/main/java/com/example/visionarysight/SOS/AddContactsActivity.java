package com.example.visionarysight.SOS;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.visionarysight.R;
import com.example.visionarysight.SOS.SOSModel.Contact;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.Locale;

public class AddContactsActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private EditText contactNameEditText;
    private EditText contactNumberEditText;
    private TextToSpeech tts;
    private static final int REQUEST_CONTACT_PICKER = 1;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contacts);

        tts = new TextToSpeech(this, this);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        contactNameEditText = findViewById(R.id.contact_name);
        contactNumberEditText = findViewById(R.id.contact_number);
        Button saveContactButton = findViewById(R.id.button_save_contact);
        Button chooseFromContactsButton = findViewById(R.id.button_choose_from_contacts);
        Button goBackButton = findViewById(R.id.button_go_back);

        saveContactButton.setOnClickListener(v -> saveContact());
        chooseFromContactsButton.setOnClickListener(v -> openContactsPicker());
        goBackButton.setOnClickListener(v -> goBack());
    }

    private void saveContact() {
        String contactName = contactNameEditText.getText().toString().trim();
        String contactNumber = contactNumberEditText.getText().toString().trim();

        if (contactName.isEmpty() || contactNumber.isEmpty()) {
            Toast.makeText(this, "Please enter both name and number.", Toast.LENGTH_SHORT).show();
            speak("Please enter both name and number.");
        } else if (contactNumber.length() != 11) {
            Toast.makeText(this, "Contact number must be of 11 digits.", Toast.LENGTH_SHORT).show();
            speak("Contact number must be of 11 digits.");
        } else {
            contactNumber = formatPhoneNumber(contactNumber);
            saveContactToFirestore(contactName, contactNumber);
            speak("Contact saved: " + contactName);
            Toast.makeText(this, "Contact saved: " + contactName + " " + contactNumber, Toast.LENGTH_SHORT).show();
            contactNameEditText.setText("");
            contactNumberEditText.setText("");
        }
    }

    private String formatPhoneNumber(String number) {
        number = number.replace(" ", "").replace("-", "");
        if (number.startsWith("+92")) {
            number = "0" + number.substring(3);
        } else if (number.startsWith("92")) {
            number = "0" + number.substring(2);
        } else if (!number.startsWith("0")) {
            number = "" + number;
        }
        return number;
    }

    private void openContactsPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CONTACT_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTACT_PICKER && resultCode == RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            retrieveContactDetails(contactUri);
        }
    }

    private void retrieveContactDetails(Uri contactUri) {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(contactUri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            String contactName = cursor.getString(nameIndex);
            String contactNumber = cursor.getString(numberIndex);
            contactNameEditText.setText(contactName);
            contactNumberEditText.setText(contactNumber);
            speak("Contact chosen: " + contactName);
            cursor.close();
        }
    }

    private void goBack() {
        speak("Going back.");
        finish();
    }

    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void saveContactToFirestore(String contactName, String contactNumber) {
        if (currentUser != null) {
            Contact contact = new Contact(contactName, contactNumber);

            String documentId = String.valueOf(System.currentTimeMillis());
            contact.setDocumentId(documentId);

            DocumentReference userDocRef = firestore.collection("contacts")
                    .document(currentUser.getUid())
                    .collection("userContacts")
                    .document(documentId);

            userDocRef.set(contact)
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Contact saved successfully"))
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error saving contact", e);
                        Toast.makeText(this, "Error saving contact. Try again.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported.", Toast.LENGTH_SHORT).show();
            } else {
                speak("You are in the Add Contacts Activity.");
            }
        } else {
            Toast.makeText(this, "Text-to-speech initialization failed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        speak("You are in the Add Contacts Activity.");
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
