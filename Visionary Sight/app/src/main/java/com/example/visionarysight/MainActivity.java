package com.example.visionarysight;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.visionarysight.Detection.DetectionActivity;
import com.example.visionarysight.SOS.MessageSenderUtil;
import com.example.visionarysight.SOS.SOSModel.SOSViewModel;
import com.example.visionarysight.utils.ModelClasses.VisionaryUser;
import com.example.visionarysight.utils.LocationFetcherUtil;
import com.example.visionarysight.utils.PermissionsUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnInitListener {

    private TextToSpeech tts;
    private Button buttonDetection, buttonFetchLocation, buttonNearByPlaces, buttonMore, buttonLogOut;
    private VisionaryUser user;
    private boolean isTtsInitialized = false;
    private static String userDisplayName;

    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private boolean firstClick = true;
    private View lastClickedButton;
    private PopupWindow popupWindow;
    private LocationFetcherUtil locationFetcherUtil;
    private MessageSenderUtil messageSender;
    private SOSViewModel sosViewModel;
    static {
        userDisplayName = "Dear User";
    }

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        locationFetcherUtil = new LocationFetcherUtil(this);
        tts = new TextToSpeech(this, this);
        sosViewModel = new ViewModelProvider(this).get(SOSViewModel.class);

        buttonFetchLocation = findViewById(R.id.button_fetch_location);
        buttonNearByPlaces = findViewById(R.id.button_nearby_places);
        buttonDetection = findViewById(R.id.button_detection);
        buttonMore = findViewById(R.id.button_more);
        buttonLogOut = findViewById(R.id.button_logout);

        SetupfetchLocationText(buttonFetchLocation, "Fetch Location");
        setupButton(buttonNearByPlaces, "Nearby Places", NearbyPlacesActivity.class);
        setupButton(findViewById(R.id.button_weather_updates), "Weather Updates", WeatherUpdatesActivity.class);
        setupButton(buttonDetection, "Detection", DetectionActivity.class);
        setupMoreOptionButton(buttonMore, "More Options");
        setupLogOutButton(buttonLogOut, "Log Out");

        if (PermissionsUtil.hasAllPermissions(this)) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
        } else {
            PermissionsUtil.requestAllPermissions(this);
        }

        mainView.setOnLongClickListener(v -> {
            speak("There are seven options: Detection, Location, More Options, Profile Info, About Us, Share App, and Log Out. Click on any button to hear its name, and click again to open it.");
            return true;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        } else {
            retrieveUserDataAndSpeak();
        }
    }

    private void retrieveUserDataAndSpeak() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            if (userDisplayName.equals("Dear User")) {
                userDisplayName = account.getDisplayName();
            }

            if (isTtsInitialized) {
                String welcomeMessage = "Welcome to Visionary Sight, " + userDisplayName + ". Long press to Send SOS message. Click a button to begin.";
                speak(welcomeMessage);
            }
        } else {
            speak(userDisplayName + ", no account information available.");
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
            } else {
                isTtsInitialized = true;
                retrieveUserDataAndSpeak();
            }
        } else {
            Toast.makeText(this, "TTS Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void SetupfetchLocationText(Button button, String buttonText) {
        button.setOnClickListener(v -> {
            if (firstClick || button != lastClickedButton) {
                speak("You are clicking on " + buttonText + ". Click again to Fetch Location.");
                firstClick = false;
                lastClickedButton = button;
            } else {
                speak("Fetching Location");
                locationFetcherUtil.fetchLocation(new LocationFetcherUtil.LocationFetchCallback() {
                    @Override
                    public void onLocationFetched(String locationText) {
                        Log.d("LocationActivity", "Fetched location: " + locationText);
                        speak(locationText);
                        showLongToast(locationText, 5000);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e("LocationActivity", "Error: " + errorMessage);
                        speak(errorMessage);
                        showLongToast(errorMessage, 5000);
                    }
                });
                firstClick = true;
                lastClickedButton = null;
            }
        });

        button.setOnLongClickListener(v -> {
            speak("There are Seven options: Detection, Location, More Options, Profile Info, About Us, Share App, and Log Out. Click on any button to hear its name, and click again to open it.");
            return true;
        });
    }

    private void showLongToast(String message, int durationInMillis) {
        final Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);

        new Handler().postDelayed(toast::cancel, durationInMillis);
        toast.show();
    }

    private void setupButton(Button button, String buttonText, Class<?> activityClass) {
        button.setOnClickListener(v -> {
            if (firstClick || button != lastClickedButton) {
                speak("You are clicking on " + buttonText + ". Click again to go to " + buttonText);
                firstClick = false;
                lastClickedButton = button;
            } else {
                speak("Opening " + buttonText);
                startActivity(new Intent(MainActivity.this, activityClass));
                firstClick = true;
                lastClickedButton = null;
            }
        });

        button.setOnLongClickListener(v -> {
            speak("SOS Message Sent. , ,, There are Seven options: Detection, Location, More Options, Profile Info, About Us, Share App, and Log Out. Click on any button to hear its name, and click again to open it.");
            SOSSenderm();
            return true;
        });
    }

    private void SOSSenderm() {
        messageSender = new MessageSenderUtil(this);
        messageSender.sendSOSMessages(sosViewModel, new MessageSenderUtil.SOSCallback() {
            @Override
            public void onComplete(boolean success) {
                if (success) {
                    speak("SOS messages sent successfully.");
                } else {
                    speak("Failed to send SOS messages.");
                }
            }
        });
    }

    private void setupMoreOptionButton(Button button, String buttonText) {
        button.setOnClickListener(v -> {
            if (firstClick || button != lastClickedButton) {
                speak("You are clicking on " + buttonText + ". Click again to see " + buttonText);
                firstClick = false;
                lastClickedButton = button;
            } else {
                speak("Opening " + buttonText);
                Intent intent = new Intent(MainActivity.this, MoreOptionsActivity.class);
                intent.putExtra("User", user);
                startActivity(intent);
                firstClick = true;
                lastClickedButton = null;
            }
        });

        button.setOnLongClickListener(v -> {
            speak("SOS Message Sent. , ,, There are Seven options: Detection, Location, More Options, Profile Info, About Us, Share App, and Log Out. Click on any button to hear its name, and click again to open it.");
            SOSSenderm();
            return true;
        });
    }

    private void setupLogOutButton(Button button, String buttonText) {
        button.setOnClickListener(v -> {
            if (firstClick || button != lastClickedButton) {
                speak("You are clicking on " + buttonText + ". Click again to " + buttonText);
                firstClick = false;
                lastClickedButton = button;
            } else {
                speak("Logging out");
                logOut();
                firstClick = true;
                lastClickedButton = null;
            }
        });

        button.setOnLongClickListener(v -> {
            speak("SOS Message Sent. , ,, There are Seven options: Detection, Location, More Options, Profile Info, About Us, Share App, and Log Out. Click on any button to hear its name, and click again to open it.");
            SOSSenderm();
            return true;
        });
    }

    private void logOut() {
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            FirebaseAuth.getInstance().signOut();
            userDisplayName = "Dear User";
            speak("Logged out successfully");
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void speak(String text) {
        if (isTtsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MainActivity");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isTtsInitialized) {
            retrieveUserDataAndSpeak();
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
