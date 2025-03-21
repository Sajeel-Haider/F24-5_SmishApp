package com.example.f24_5;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class VishingDetectionActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 200;
    private MediaRecorder mediaRecorder;
    private String outputFilePath;
    private TextView tvStatus;
    private Button btnRecord;
    private Button btnStop;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vishing_detection);

        tvStatus = findViewById(R.id.tvStatus);
        btnRecord = findViewById(R.id.btnRecord);
        btnStop = findViewById(R.id.btnStop);

        // Initially disable the Stop button
        btnStop.setEnabled(false);

        // Record button click
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });

        // Stop button click
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
            }
        });

        // Check RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE
            );
        } else {
            tvStatus.setText("Permission granted. Ready to record.");
        }
    }

    private void startRecording() {
        // File path for the recorded audio
        outputFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath()
                + "/vishing_record.3gp";

        // Configure MediaRecorder
        mediaRecorder = new MediaRecorder();
        // This uses MIC for demonstration; direct call audio is restricted on modern Android.
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(outputFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            tvStatus.setText("Recording started...");
            btnRecord.setEnabled(false);
            btnStop.setEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
            tvStatus.setText("Recording failed: " + e.getMessage());
        }
    }

    private void stopRecording() {
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;

            tvStatus.setText("Recorded");
            btnRecord.setEnabled(true);
            btnStop.setEnabled(false);

            // Now convert the audio file to text by sending it to your backend
            sendRecordingForTranscription(outputFilePath);
        }
    }

    /**
     * Reads the 3GP audio file, encodes it in Base64,
     * and sends it to your backend API for transcription.
     */
    private void sendRecordingForTranscription(String filePath) {
        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            tvStatus.append("\nAudio file not found!");
            return;
        }

        // Convert file to Base64
        byte[] audioBytes = fileToBytes(audioFile);
        if (audioBytes == null) {
            tvStatus.append("\nError reading audio file.");
            return;
        }
        String audioBase64 = Base64.encodeToString(audioBytes, Base64.DEFAULT);

        // Build JSON payload
        JSONObject jsonBody = new JSONObject();
        try {
            // "audio_data" is a field name you decide; your server must parse it accordingly
            jsonBody.put("audio_data", audioBase64);

            // Show the request JSON on the screen (for testing):
//            tvStatus.append("\n\nRequest JSON (for testing):\n" + jsonBody.toString(2));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Example server endpoint
        String url = "http://192.168.10.9:8000/vish";

        // Create a Volley request
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Show the server response in a custom dialog
                        showResponseDialog(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tvStatus.append("\nError sending file: " + error.toString());

                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            String body = new String(error.networkResponse.data);
                            tvStatus.append("\nStatus Code: " + statusCode);
                            tvStatus.append("\nServer Response Body: " + body);
                        }
                    }

                }
        );

        // Send the request
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);

        tvStatus.append("\n\nSending audio to server for transcription...");
    }

    /**
     * Helper method to read a file into a byte array.
     */
    private byte[] fileToBytes(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            int read = fis.read(data);
            fis.close();
            if (read == data.length) {
                return data;
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Displays a custom dialog showing the server's JSON response,
     * similar to how you did for smishing detection.
     */
    private void showResponseDialog(JSONObject response) {
        // Inflate the custom layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_api_response_vish, null);
        builder.setView(dialogView);

        // References to views in dialog layout
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvResponseDetails = dialogView.findViewById(R.id.tvResponseDetails);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        // NEW references for donut chart
        ProgressBar circleProgressBar = dialogView.findViewById(R.id.circleProgressBar);
        TextView tvProbability = dialogView.findViewById(R.id.tvProbability);

        // Set a custom title
        tvDialogTitle.setText("Vishing Analysis Result");

        try {
            // 1) Parse the fields you expect
            String prediction = response.optString("prediction", "N/A");
            String transcribedText = response.optString("text", "No text available");
            String vishingStatus = response.optString("status", "Unknown");
            // NEW: Probability field from backend
            // 2) Decide donut progress based on "Phishing" or "Not Phishing"
            int progressValue;
            if (prediction.equalsIgnoreCase("Phishing")) {
                progressValue = 100; // 100% if it's phishing
            } else {
                progressValue = 0;   // 0% if it's not phishing
            }

            circleProgressBar.setProgress(progressValue);
            tvProbability.setText(progressValue + "%");

            // 3) Build a readable report text
            String detailsText =
                    "Prediction: " + prediction + "\n\n" +
                            "Transcribed Text:\n" + transcribedText + "\n\n" +
                            "Status: " + vishingStatus;

            tvResponseDetails.setText(detailsText);

        } catch (Exception e) {
            e.printStackTrace();
            tvResponseDetails.setText("Error parsing response.");
        }

        // 4) OK button to dismiss the dialog
        AlertDialog dialog = builder.create();
        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }



    // Handle runtime permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tvStatus.setText("Permission granted. Ready to record.");
            } else {
                tvStatus.setText("Audio recording permission denied.");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
