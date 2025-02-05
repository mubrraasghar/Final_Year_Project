package com.example.visionarysight.SOS;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.visionarysight.SOS.SOSModel.Contact;
import com.example.visionarysight.SOS.SOSModel.SOSMessage;
import com.example.visionarysight.SOS.SOSModel.SOSViewModel;

import java.util.List;

public class MessageSenderUtil {

    private final Context context;
    private static final String TAG = "MessageSenderUtil";

    public interface SOSCallback {
        void onComplete(boolean success);
    }

    public MessageSenderUtil(Context context) {
        this.context = context;
    }

    public void sendSOSMessages(SOSViewModel sosViewModel, SOSCallback callback) {
        LiveData<SOSMessage> sosMessageLiveData = sosViewModel.getSOSMessage();
        LiveData<List<Contact>> contactsLiveData = sosViewModel.getAllContacts();

        sosMessageLiveData.observeForever(new Observer<SOSMessage>() {
            @Override
            public void onChanged(SOSMessage sosMessage) {
                String messageToSend = sosMessage != null ? sosMessage.getContent() : "This is SOS message!";

                contactsLiveData.observeForever(new Observer<List<Contact>>() {
                    @Override
                    public void onChanged(List<Contact> contacts) {
                        if (contacts != null && !contacts.isEmpty()) {
                            boolean allSuccess = true;
                            for (Contact contact : contacts) {
                                if (!sendSMS(contact.getPhoneNumber(), messageToSend)) {
                                    allSuccess = false;
                                }
                            }
                            callback.onComplete(allSuccess);
                        } else {
                            Toast.makeText(context, "No contacts found to send SOS.", Toast.LENGTH_SHORT).show();
                            callback.onComplete(false);
                        }
                        contactsLiveData.removeObserver(this);
                    }
                });
                sosMessageLiveData.removeObserver(this);
            }
        });
    }

    private boolean sendSMS(String phoneNumber, String message) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "SMS permission not granted. Please enable it in settings.", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d(TAG, "SMS sent to: " + phoneNumber);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS to " + phoneNumber, e);
            return false;
        }
    }
}
