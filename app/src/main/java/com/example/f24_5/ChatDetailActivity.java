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

        // On clicking "Report", we make an API call (or perform our custom check)
        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoaderAndSendReport(name, message);
            }
        });
    }

    private void showLoaderAndSendReport(String name, String message) {
        // Show a loader until the API returns or our check completes
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

        // Check: if the phone number includes any non-digit characters,
        // consider it invalid and mark as 0% smishing.
        if (!phoneNumber.matches("^\\+?\\d+$")) {
            progressDialog.dismiss();
            // Create a dummy JSON response for the invalid phone number
            JSONObject dummyResponse = new JSONObject();
            try {
                // 0% probability => "safe"
                dummyResponse.put("phishing_probability", 0.0);

                // Mark phone_type as "organization"
                // so in showCustomDialog() it becomes "Organization number was used"
                dummyResponse.put("phone_type", "organization");


            } catch (JSONException e) {
                e.printStackTrace();
            }
            showCustomDialogNonSmishing(dummyResponse);

            return;
        }


        // Build the JSON payload for the API
        JSONObject payload = new JSONObject();
        try {
            payload.put("phone_number", phoneNumber);
            payload.put("sms_text", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://192.168.10.9:8000/analyze"; // or your LAN IP if on real device

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
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                240 * 1000,  // Timeout in ms (60 seconds)
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        // Add the request to the Volley request queue
        RequestQueue queue = Volley.newRequestQueue(ChatDetailActivity.this);
        queue.add(request);
    }
    private void showCustomDialogNonSmishing(JSONObject response) {
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
        TextView tvSpoofedNote = dialogView.findViewById(R.id.tvSpoofedNote); // if you have this in layout

        // Custom title
        tvDialogTitle.setText("Smishing Analysis Result");

        try {
            // We expect phishing_probability = 0.0, phone_type = "organization"
            double phishingProbability = response.optDouble("phishing_probability", 0.0);
            String phoneType = response.optString("phone_type", "organization");

            // Convert 0.0 to a 0% donut
            int progressValue = (int) Math.round(phishingProbability * 100);
            circleProgressBar.setProgress(progressValue);
            tvProbability.setText(progressValue + "%");  // Should be "0%"

            // Show a simple statement
            String phoneTypeText = "Organization number was used";
            // If you want a condition, for example if phoneType is "invalid" or something, handle that
            // but we'll assume "organization" here.

            // Our simple details. “Not smishing”:
            String detailsText =
                    "Number Type: " + phoneTypeText + "\n\n" +
                            "This message is not smishing.";

            tvResponseDetails.setText(detailsText);

            // If you have a small "spoofed" note, you can hide or change it
            if (tvSpoofedNote != null) {
                tvSpoofedNote.setText(""); // or set visibility GONE if you want
            }

        } catch (Exception e) {
            e.printStackTrace();
            tvResponseDetails.setText("Error parsing response.");
        }

        AlertDialog dialog = builder.create();
        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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
        TextView tvSpoofedNote = dialogView.findViewById(R.id.tvSpoofedNote);  // <-- new note

        // Optionally set a custom title
        tvDialogTitle.setText("Smishing Analysis Result");

        try {
            // -------------------------------------------------------------
            // 1) Extract fields from the JSON
            // -------------------------------------------------------------
            double phishingProbability = response.optDouble("phishing_probability", 0.0);
            String phoneType = response.optString("phone_type", "unknown");
            boolean linkPresent = response.optBoolean("linkPresent", false);
            boolean linkAuthentic = response.optBoolean("link_authentic", false);

            // "reasons" is an array, e.g. ["New domain", "Flagged by 5 engines"]
            // We'll handle it gracefully
            org.json.JSONArray reasonsArray = response.optJSONArray("reasons");
            String recommendation = response.optString("recommendation", "No recommendation provided");

            // model_used and status are omitted intentionally

            // -------------------------------------------------------------
            // 2) Configure and set the donut progress bar (phishing prob)
            // -------------------------------------------------------------
            // Convert probability to 0-100 scale
            int progressValue = (int) Math.round(phishingProbability * 100);
            circleProgressBar.setProgress(progressValue);
            tvProbability.setText(progressValue + "%");

            // -------------------------------------------------------------
            // 3) Build the "report style" text
            // -------------------------------------------------------------
            // phone_type mapping
            String phoneTypeText = phoneType.equals("not mobile")
                    ? "Private number was used"
                    : "Organization number was used";

            // linkPresent mapping
            String linkPresentText = linkPresent
                    ? "Link is present in the message"
                    : "No link found in the message";

            // linkAuthentic mapping
            String linkAuthenticText = linkAuthentic
                    ? "Link appears authentic"
                    : "Link does NOT appear authentic";

            // reasons
            StringBuilder reasonsBuilder = new StringBuilder();
            if (reasonsArray != null && reasonsArray.length() > 0) {
                for (int i = 0; i < reasonsArray.length(); i++) {
                    String reason = reasonsArray.optString(i, "-");
                    reasonsBuilder.append("• ").append(reason).append("\n");
                }
            } else {
                reasonsBuilder.append("No specific reasons given.\n");
            }

            // Compose a final details string
            // You can customize the formatting/headings as you prefer
            String detailsText =
                    "Number Type: " + phoneTypeText + "\n\n" +
                            linkPresentText + "\n" +
                            linkAuthenticText + "\n\n" +
                            "Reasons:\n" + reasonsBuilder.toString() + "\n" +
                            "Recommendation:\n" + recommendation;

            tvResponseDetails.setText(detailsText);

            tvSpoofedNote.setText("This number might be spoofed");


        } catch (Exception e) {
            e.printStackTrace();
            tvResponseDetails.setText("Error parsing response.");
        }

        // -------------------------------------------------------------
        // 4) OK button to dismiss the dialog
        // -------------------------------------------------------------
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
