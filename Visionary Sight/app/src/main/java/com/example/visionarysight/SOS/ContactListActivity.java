package com.example.visionarysight.SOS;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.visionarysight.R;
import com.example.visionarysight.SOS.SOSModel.Contact;
import com.example.visionarysight.SOS.SOSModel.SOSViewModel;

import java.util.List;
import java.util.Locale;

public class ContactListActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private ListView contactListView;
    private SOSViewModel SOSViewModel;
    private TextToSpeech tts;
    private boolean isGoBackClickedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        tts = new TextToSpeech(this, this);

        contactListView = findViewById(R.id.contact_list_view);

        SOSViewModel = new ViewModelProvider(this).get(SOSViewModel.class);

        SOSViewModel.getAllContacts().observe(this, new Observer<List<Contact>>() {
            @Override
            public void onChanged(List<Contact> contacts) {
                if (contacts != null && !contacts.isEmpty()) {
                    ArrayAdapter<Contact> adapter = new ArrayAdapter<Contact>(ContactListActivity.this, R.layout.contact_list_item, contacts) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            if (convertView == null) {
                                LayoutInflater inflater = LayoutInflater.from(getContext());
                                convertView = inflater.inflate(R.layout.contact_list_item, parent, false);
                            }

                            Contact contact = getItem(position);

                            TextView nameTextView = convertView.findViewById(R.id.contact_name);
                            TextView phoneTextView = convertView.findViewById(R.id.contact_phone);

                            if (contact != null) {
                                nameTextView.setText(contact.getName());
                                phoneTextView.setText(contact.getPhoneNumber());
                            }

                            convertView.setOnClickListener(v -> {
                                if (contact != null) {
                                    isGoBackClickedOnce = false;
                                    speak(contact.getName() + ". ,, " + contact.getPhoneNumber());
                                }
                            });

                            convertView.setOnLongClickListener(v -> {
                                showDeleteDialog(contact);
                                return true;
                            });

                            return convertView;
                        }
                    };

                    contactListView.setAdapter(adapter);

                    for (Contact contact : contacts) {
                        speak(contact.getName());
                    }
                } else {
                    Toast.makeText(ContactListActivity.this, "No contacts found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.button_go_back).setOnClickListener(v -> handleGoBackClick());
    }

    private void showDeleteDialog(Contact contact) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this contact?")
                .setPositiveButton("Yes", (dialog, id) -> SOSViewModel.deleteContact(contact.getDocumentId()))
                .setNegativeButton("No", (dialog, id) -> dialog.dismiss())
                .create()
                .show();
    }

    private void handleGoBackClick() {
        if (!isGoBackClickedOnce) {
            isGoBackClickedOnce = true;
            speak("Press again to go back.");
        } else {
            finish();
        }
    }

    private void speak(String text) {
        if (tts != null && !tts.isSpeaking()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported.", Toast.LENGTH_SHORT).show();
            } else {
                speak("You are in the Contact List Activity.");
            }
        } else {
            Toast.makeText(this, "Text to speech initialization failed.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        speak("You are in the Contact List Activity.");
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
