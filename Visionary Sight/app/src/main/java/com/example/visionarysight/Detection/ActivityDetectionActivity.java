package com.example.visionarysight.Detection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.example.visionarysight.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ActivityDetectionActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "ActivityDetection";
    private static final int CAPTURE_DELAY = 5000;
  
    private static final String IMGUR_CLIENT_ID = "70bc9b8e4efc77f";

    private Preview preview;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private Button goback;
    private androidx.camera.view.PreviewView previewView;
    private ImageView capturedImageView;
    private TextView resultTextView;

    private final Handler handler = new Handler();
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                    scheduleAutoCaptureAndFinish();
                } else {
                    Log.e(TAG, "Camera permission denied");
                    finish();
                }
            });

    private TextToSpeech tts;
    private boolean firstClickGoBack = true;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection_activity);

        previewView = findViewById(R.id.camera_preview);
        capturedImageView = findViewById(R.id.captured_image_preview);
        resultTextView = findViewById(R.id.result_text_view);
        goback = findViewById(R.id.button_go_back);
        cameraExecutor = Executors.newSingleThreadExecutor();

        tts = new TextToSpeech(this, this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();  // Start camera initialization if permission granted
            scheduleAutoCaptureAndFinish();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);  // Request permission if not granted
        }

        goback.setOnClickListener(v -> {
            if (firstClickGoBack) {
                speak("You are about to go back. Click again to confirm.");
                firstClickGoBack = false;
            } else {
                speak("Going back.");
                finish();
                firstClickGoBack = true;
            }
        });
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCamera(cameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "Failed to bind camera use cases", e);
                // Optionally, you can retry the camera binding here if it fails
            }
        }, ContextCompat.getMainExecutor(this));
    }


    @SuppressLint("WrongConstant")
    private void bindCamera(@NonNull ProcessCameraProvider cameraProvider) {
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(previewView.getDisplay() != null ? previewView.getDisplay().getRotation() : 0)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }


    private void scheduleAutoCaptureAndFinish() {
        handler.postDelayed(this::captureImage, CAPTURE_DELAY);
    }

    private void captureImage() {
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture is not initialized");
            return;
        }

        File outputFile = new File(getCacheDir(), "temp_image.jpg");
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d(TAG, "Image saved successfully");
                        processCapturedImage(outputFile);
                        speak("Captured View processing");
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Error capturing image", exception);
                    }
                });
    }

    private void processCapturedImage(File imageFile) {
        try {
            Bitmap originalBitmap = android.graphics.BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            File compressedFile = compressImage(originalBitmap);

            Bitmap compressedBitmap = android.graphics.BitmapFactory.decodeFile(compressedFile.getAbsolutePath());
            capturedImageView.setImageBitmap(compressedBitmap);
            capturedImageView.setVisibility(View.VISIBLE);
            previewView.setVisibility(View.GONE);

            uploadImageToImgur(compressedFile);
        } catch (Exception e) {
            Log.e(TAG, "Error processing captured image", e);
        }
    }

    private File compressImage(Bitmap originalBitmap) throws Exception {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();
        int maxWidth = 800;
        int maxHeight = 800;

        float scalingFactor = Math.min(
                (float) maxWidth / originalWidth,
                (float) maxHeight / originalHeight
        );

        int scaledWidth = Math.round(scalingFactor * originalWidth);
        int scaledHeight = Math.round(scalingFactor * originalHeight);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true);

        File compressedFile = new File(getCacheDir(), "compressed_image.jpg");
        try (OutputStream outputStream = new java.io.FileOutputStream(compressedFile)) {
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
        }

        while (compressedFile.length() > 500 * 1024) {
            try (OutputStream outputStream = new java.io.FileOutputStream(compressedFile)) {
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            }
        }

        return compressedFile;
    }

    private void uploadImageToImgur(File imageFile) {
        if (imageFile == null || !imageFile.exists()) {
            Log.e(TAG, "Image file does not exist: " + imageFile);
            return;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/jpeg"))).build();

        Request request = new Request.Builder()
                .url("https://api.imgur.com/3/image")
                .post(requestBody)
                .addHeader("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to upload image: " + e, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Imgur upload failed: " + response.code() + " " + response.message());
                    String responseBody = response.body() != null ? response.body().string() : "No response body";
                    Log.e(TAG, "Server Response: " + responseBody);
                    return;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String imageUrl = jsonResponse.getJSONObject("data").getString("link");
                    Log.d(TAG, "Image uploaded successfully: " + imageUrl);
                    analyzeImageWithOpenAI(imageUrl);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Imgur response", e);
                }
            }
        });
    }

    private void analyzeImageWithOpenAI(String imageUrl) {
        new Thread(() -> {
            try {
                String result = analyzeImage(imageUrl);
                runOnUiThread(() -> {
                    resultTextView.setText("Detected Activity: " + result);
                    speak("Detected Results: " + result);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error analyzing image", e);
                runOnUiThread(() -> resultTextView.setText("Error analyzing image."));
            }
        }).start();
    }

    private String analyzeImage(String imageUrl) throws Exception {
        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        JSONObject payload = new JSONObject();
        payload.put("model", "gpt-4o");

        JSONArray messagesArray = new JSONArray();
        messagesArray.put(new JSONObject().put("role", "system").put("content", "You are an assistant that detects and explains activities in images."));
        messagesArray.put(new JSONObject().put("role", "user").put("content", "Describe the image; include text if present, else summarize in 12-18 words."));

        JSONObject imageMessage = new JSONObject();
        imageMessage.put("role", "user");
        JSONArray contentArray = new JSONArray();
        JSONObject imageObject = new JSONObject();
        imageObject.put("type", "image_url");
        imageObject.put("image_url", new JSONObject().put("url", imageUrl));
        contentArray.put(imageObject);
        imageMessage.put("content", contentArray);
        messagesArray.put(imageMessage);

        payload.put("messages", messagesArray);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.toString().getBytes());
            os.flush();
        }

        try (InputStream is = connection.getInputStream()) {
            StringBuilder responseBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                responseBuilder.append(new String(buffer, 0, bytesRead));
            }
            JSONObject responseJson = new JSONObject(responseBuilder.toString());
            return responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        }
    }

    private void speak(String message) {
        if (tts != null) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Reinitialize camera if activity is resumed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Unbind camera to release resources when activity is paused
        if (preview != null && imageCapture != null) {
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = ProcessCameraProvider.getInstance(this).get();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            cameraProvider.unbindAll();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int langResult = tts.setLanguage(Locale.US);
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language is not supported or missing data");
            } else {
                speak("Focusing");
            }
        } else {
            Log.e(TAG, "Text-to-speech initialization failed.");
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
