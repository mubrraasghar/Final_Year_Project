package com.example.visionarysight.Detection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.example.visionarysight.Detection.currencies.ColorModel;
import com.example.visionarysight.Detection.currencies.Constants;
import com.example.visionarysight.Detection.currencies.PKR;
import com.example.visionarysight.Detection.currencies.Vision;
import com.example.visionarysight.R;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrencyDetectionActivity extends AppCompatActivity implements SurfaceHolder.Callback, Detector.Processor<TextBlock> {

    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    private SurfaceView mCameraView;
    private CameraSource mCameraSource;
    private TextToSpeech textToSpeech;
    private Handler handler;
    private ExecutorService executorService;
    private ArrayList<Rect> detectTextBlocks;
    private Map<String, Object> noteLandMarks;
    private boolean firstClick = true;
    private Button lastClickedButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency_detection);

        mCameraView = findViewById(R.id.surfaceView);
        textToSpeech = new TextToSpeech(this, status -> speak("Camera is open, show note"));
        Button goBackButton = findViewById(R.id.button_go_back);
        setupGoBackButton(goBackButton, "Go Back");
        executorService = Executors.newFixedThreadPool(2);
        noteLandMarks = PKR.getInstance().noteLandmarks();
        detectTextBlocks = new ArrayList<>();
        handler = new Handler();

        if (allPermissionsGranted()) {
            startCameraSource();
            executorService.submit(() -> speak("Camera is open, place a note"));
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }



    private void setupGoBackButton(Button button, String buttonText) {
        button.setOnClickListener(v -> {
            if (firstClick || button != lastClickedButton) {
                speak("You are clicking on " + buttonText + ". Click again to go back.");
                firstClick = false;
                lastClickedButton = button;
            } else {
                firstClick = true;
                lastClickedButton = null;
                finish();
            }
        });

        button.setOnLongClickListener(v -> {
            speak("This button allows you to go back to the previous screen. Tap once to hear its function and tap again to go back.");
            return true;
        });
    }

    private void startCameraSource() {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.d("CurrencyDetection", "Detector dependencies not loaded yet");
            return;
        }

        mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(420, 360)
                .setAutoFocusEnabled(true)
                .setRequestedFps(2.0f)
                .build();

        mCameraView.getHolder().addCallback(this);
        textRecognizer.setProcessor(this);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCameraSource();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        final SparseArray<TextBlock> items = detections.getDetectedItems();
        if (items.size() != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                TextBlock item = items.valueAt(i);
                stringBuilder.append(item.getValue()).append(" ");
                detectTextBlocks.add(item.getBoundingBox());
            }

            String data = stringBuilder.toString();
            if (!data.isEmpty() && !PKR.getInstance().whichNoteIsIt(data).isEmpty()) {
                executorService.submit(() -> {
                    if (!textToSpeech.isSpeaking()) {
                        speak("Move a little up");
                        SystemClock.sleep(1500);
                        capture(data);
                    }
                });
            }
        }
    }

    private synchronized void capture(final String data) {
        mCameraSource.takePicture(null, bytes -> {
            speak("Captured. Processing");
            mCameraSource.stop();
            mCameraView.setVisibility(View.GONE);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            int pixel = getDominantColor(bitmap);
            int redValue = Color.red(pixel);
            int blueValue = Color.blue(pixel);
            int greenValue = Color.green(pixel);

            ColorModel model = new ColorModel();
            model.setGreenMax(greenValue);
            model.setRedMax(redValue);
            model.setBlueMax(blueValue);

            Map<String, String> results = Vision.getInstance().getResults(data, model);
            if (results != null) {
                String note = results.get(Constants.NOTE);
                String confidence = "it is "+ note + " note";
                speak(confidence);

                handler.post(() -> {
                    TextView textView = findViewById(R.id.currencyDetectionResults);
                    textView.setText(confidence);
                    ImageView imageView = findViewById(R.id.currencyDetectionResultImage);
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(bitmap);
                });

                handler.postDelayed(this::finish, 10000);
            }
        });
    }

    private static int getDominantColor(Bitmap bitmap) {
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
    }

    private synchronized void speak(final String msg) {
        if (textToSpeech.getEngines().size() > 0) {
            textToSpeech.setLanguage(Locale.getDefault());
            textToSpeech.setSpeechRate(-10);
            textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSIONS);
                return;
            }
            mCameraSource.start(mCameraView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCameraSource.stop();
    }

    @Override
    public void release() { }

    @Override
    protected void onDestroy() {
        textToSpeech.shutdown();
        mCameraSource.release();
        super.onDestroy();
    }
}
