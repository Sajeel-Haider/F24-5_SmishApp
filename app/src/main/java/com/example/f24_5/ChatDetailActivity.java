package com.example.f24_5;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

// Volley and JSON imports
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.json.JSONObject;
import org.json.JSONException;
import android.widget.ProgressBar;

public class ChatDetailActivity extends AppCompatActivity {
    private TextView tvDetailName, tvDetailSource, tvDetailTime, tvDetailMessage;
    private ImageView ivDetailAvatar;
    private Button btnReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        // Initialize views
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailSource = findViewById(R.id.tvDetailSource);
        tvDetailTime = findViewById(R.id.tvDetailTime);
        tvDetailMessage = findViewById(R.id.tvDetailMessage);
        ivDetailAvatar = findViewById(R.id.ivDetailAvatar);
        btnReport = findViewById(R.id.btnReport);

        // Retrieve data from the intent
        Intent intent = getIntent();
        final String name = intent.getStringExtra("name");
        final String message = intent.getStringExtra("message");
        String time = intent.getStringExtra("time");
        String source = intent.getStringExtra("source");

        // Set the details
        tvDetailName.setText(name);
        tvDetailMessage.setText(message);
        tvDetailTime.setText(time);
        tvDetailSource.setText(source);

        // Set a dummy profile picture
        ivDetailAvatar.setImageResource(R.drawable.ic_avatar);

        // On clicking "Report", we make an API call
        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoaderAndSendReport(name, message);
            }
        });
    }

    private void showLoaderAndSendReport(String name, String message) {
        // Show a loader until the API returns
        final ProgressDialog progressDialog = new ProgressDialog(ChatDetailActivity.this);
        progressDialog.setMessage("Reporting...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Extract phone number from name assuming it's within parentheses
        String phoneNumber;
        int start = name.indexOf("(");
        int end = name.indexOf(")");
        if (start != -1 && end != -1 && end > start) {
            phoneNumber = name.substring(start + 1, end);
        } else {
            phoneNumber = name; // Fallback if no parentheses found
        }

        // Build the JSON payload for the API
        JSONObject payload = new JSONObject();
        try {
            payload.put("phone_number", phoneNumber);
            payload.put("sms_text", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://172.20.10.5:8000/analyze"; // or your LAN IP if on real device

        // Create a POST request using Volley
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                payload,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressDialog.dismiss();
                        showCustomDialog(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        showErrorDialog(error);
                    }
                }
        );

        // Add the request to the Volley request queue
        RequestQueue queue = Volley.newRequestQueue(ChatDetailActivity.this);
        queue.add(request);
    }

    /**
     * Displays a custom dialog with a circular progress bar for the phishing probability
     * and other fields from the API response.
     */
    private void showCustomDialog(JSONObject response) {
        // Inflate the custom layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_api_response, null);
        builder.setView(dialogView);

        // References to views in dialog layout
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        ProgressBar circleProgressBar = dialogView.findViewById(R.id.circleProgressBar);
        TextView tvProbability = dialogView.findViewById(R.id.tvProbability);
        TextView tvResponseDetails = dialogView.findViewById(R.id.tvResponseDetails);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        // Optionally set a custom title
        tvDialogTitle.setText("Phishing Analysis Result");

        try {
            // Example fields from your JSON response:
            // {"phone_type":"not mobile","linkPresent":"false","phishing_probability":0.178,"status":"success", ...}

            double phishingProbability = response.optDouble("phishing_probability", 0.0);
            String phoneType = response.optString("phone_type", "unknown");
            String linkPresent = response.optString("linkPresent", "unknown");
            String status = response.optString("status", "unknown");
            String modelUsed = response.optString("model_used", "N/A");

            // Set progress bar
            // Convert probability to 0-100 scale
            int progressValue = (int) Math.round(phishingProbability * 100);
            circleProgressBar.setProgress(progressValue);
            tvProbability.setText(progressValue + "%");

            // Format additional info
            // You can show more fields as you like
            String detailsText =
                    "Phone Type: " + phoneType + "\n" +
                            "Link Present: " + linkPresent + "\n" +
                            "Model Used: " + modelUsed + "\n" +
                            "Status: " + status;
            tvResponseDetails.setText(detailsText);

        } catch (Exception e) {
            e.printStackTrace();
            tvResponseDetails.setText("Error parsing response.");
        }

        // OK button to dismiss the dialog
        AlertDialog dialog = builder.create();
        btnOk.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Show an error dialog if Volley fails.
     */
    private void showErrorDialog(VolleyError error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report Error");
        if (error.getMessage() != null) {
            builder.setMessage("Error: " + error.getMessage());
        } else {
            builder.setMessage("An unknown error occurred.");
        }
        builder.setPositiveButton("OK", null);
        builder.show();
    }
}
