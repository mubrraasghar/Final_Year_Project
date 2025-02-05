package com.example.visionarysight.SOS.SOSModel;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class SOSViewModel extends AndroidViewModel {
    private String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private FirebaseFirestore firestore;
    private MutableLiveData<List<Contact>> allContacts;
    private MutableLiveData<SOSMessage> sosMessage;

    public SOSViewModel(Application application) {
        super(application);

        firestore = FirebaseFirestore.getInstance();
        allContacts = new MutableLiveData<>();
        sosMessage = new MutableLiveData<>();
    }
    public LiveData<List<Contact>> getAllContacts() {

        firestore.collection("contacts")
                .document(userId)
                .collection("userContacts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Contact> contacts = task.getResult().toObjects(Contact.class);
                        allContacts.setValue(contacts);
                    } else {
                        allContacts.setValue(null);
                    }
                });
        return allContacts;
    }

    public LiveData<SOSMessage> getSOSMessage() {
        firestore.collection("sosMessages")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        SOSMessage message = task.getResult().toObject(SOSMessage.class);
                        sosMessage.setValue(message);
                    } else {
                        Log.d("SOS MESSAGE","message didn't retrived");
                    }
                });
        return sosMessage;
    }
    public void deleteContact(String documentId) {
        if (userId != null) {
            DocumentReference contactDocRef = firestore.collection("contacts")
                    .document(userId)
                    .collection("userContacts")
                    .document(documentId);
            contactDocRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firestore", "Contact deleted successfully");
                        Toast.makeText(this.getApplication(), "Contact deleted successfully", Toast.LENGTH_SHORT).show();
                        getAllContacts();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error deleting contact", e);
                        Toast.makeText(this.getApplication(), "Error deleting contact. Try again.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this.getApplication(), "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }
}
