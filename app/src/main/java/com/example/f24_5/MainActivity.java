package com.example.f24_5;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.annotation.SuppressLint;
import android.util.Log;
import android.text.format.DateFormat;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int CONTACT_PERMISSION_REQUEST_CODE = 101;

    private RecyclerView rvChats;
    private ChatAdapter chatAdapter;
    private List<Chat> chatList;
    private List<Chat> allChats;
    private SmsContentObserver smsObserver;

    // NEW: SwipeRefreshLayout reference
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Updated layout with SwipeRefreshLayout and ToggleButton

        // Initialize views
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvChats = findViewById(R.id.rvChats);
        EditText etSearch = findViewById(R.id.etSearch);
        // NEW: Initialize the ToggleButton for Vishing Detection
        ToggleButton toggleVishing = findViewById(R.id.toggleVishing);

        // Setup RecyclerView
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        chatList = new ArrayList<>();
        allChats = new ArrayList<>();

        // Adapter
        chatAdapter = new ChatAdapter(chatList);
        rvChats.setAdapter(chatAdapter);

        // TextWatcher for searching
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                if (query.isEmpty()) {
                    chatList.clear();
                    chatList.addAll(allChats);
                } else {
                    List<Chat> matchingChats = new ArrayList<>();
                    List<Chat> nonMatchingChats = new ArrayList<>();
                    for (Chat chat : allChats) {
                        if (chat.getName().toLowerCase().contains(query) ||
                                chat.getMessage().toLowerCase().contains(query)) {
                            matchingChats.add(chat);
                        } else {
                            nonMatchingChats.add(chat);
                        }
                    }
                    chatList.clear();
                    chatList.addAll(matchingChats);
                    chatList.addAll(nonMatchingChats);
                }
                chatAdapter.notifyDataSetChanged();
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });

        // NEW: Set listener on ToggleButton for Vishing Detection
        toggleVishing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Redirect to VishingDetectionActivity when toggled on
                    Intent intent = new Intent(MainActivity.this, VishingDetectionActivity.class);
                    startActivity(intent);
                    // Reset the toggle button state
                    buttonView.setChecked(false);
                }
            }
        });

        // Check Permissions
        if (!hasContactPermission()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    CONTACT_PERMISSION_REQUEST_CODE
            );
        }
        if (!hasSmsPermission()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_SMS},
                    PERMISSION_REQUEST_CODE
            );
        } else {
            fetchSmsMessages();
            registerSmsObserver();
        }

        // Set up the pull-to-refresh listener
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // User swiped down. Re-fetch or update your data here.
                fetchSmsMessages();
                // Stop the refresh indicator automatically after 5 seconds
                swipeRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 5000);
            }
        });
    }

    // Check if READ_SMS permission is granted
    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    // Check if READ_CONTACTS permission is granted
    private boolean hasContactPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("Range")
    private void fetchSmsMessages() {
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");
        if (cursor != null) {
            chatList.clear();
            allChats.clear();
            int count = 0;
            int indexAddress = cursor.getColumnIndex("address");
            int indexBody = cursor.getColumnIndex("body");
            int indexDate = cursor.getColumnIndex("date");

            if (indexAddress == -1 || indexBody == -1 || indexDate == -1) {
                Log.e("SMS", "One or more required columns are missing.");
                cursor.close();
                return;
            }

            while (cursor.moveToNext() && count < 100) {
                String address = cursor.getString(indexAddress);
                String contactName = getContactName(address);
                if (contactName.equals("Unknown")) {
                    contactName = "Unknown (" + address + ")";
                } else {
                    contactName = contactName + " (" + address + ")";
                }
                String body = cursor.getString(indexBody);
                long dateMillis = cursor.getLong(indexDate);
                String formattedDate = DateFormat.format("dd MMM yyyy hh:mm a", new Date(dateMillis)).toString();
                Chat chat = new Chat(contactName, body, formattedDate, "SMS");
                chatList.add(chat);
                count++;
            }
            cursor.close();
            allChats.addAll(chatList);
            chatAdapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("Range")
    private void fetchLatestSmsMessage() {
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");
        if (cursor != null && cursor.moveToFirst()) {
            int indexAddress = cursor.getColumnIndex("address");
            int indexBody = cursor.getColumnIndex("body");
            int indexDate = cursor.getColumnIndex("date");

            if (indexAddress == -1 || indexBody == -1 || indexDate == -1) {
                Log.e("SMS", "One or more required columns are missing in latest SMS fetch.");
                cursor.close();
                return;
            }

            String address = cursor.getString(indexAddress);
            String contactName = getContactName(address);
            if (contactName.equals("Unknown")) {
                contactName = "Unknown (" + address + ")";
            } else {
                contactName = contactName + " (" + address + ")";
            }
            String body = cursor.getString(indexBody);
            long dateMillis = cursor.getLong(indexDate);
            String formattedDate = DateFormat.format("dd MMM yyyy hh:mm a", new Date(dateMillis)).toString();
            Chat newChat = new Chat(contactName, body, formattedDate, "SMS");

            if (chatList.isEmpty() || !chatList.get(0).getTime().equals(formattedDate)) {
                chatList.add(0, newChat);
                allChats.add(0, newChat);
                chatAdapter.notifyItemInserted(0);
                rvChats.scrollToPosition(0);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private String getContactName(String phoneNumber) {
        if (!hasContactPermission()) {
            return "Unknown";
        }
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                String contactName = cursor.getString(index);
                cursor.close();
                return contactName;
            }
            cursor.close();
        }
        return "Unknown";
    }

    private class SmsContentObserver extends ContentObserver {
        public SmsContentObserver(Handler handler) {
            super(handler);
        }
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fetchLatestSmsMessage();
                }
            });
        }
    }

    private void registerSmsObserver() {
        smsObserver = new SmsContentObserver(new Handler());
        getContentResolver().registerContentObserver(Uri.parse("content://sms/inbox"), true, smsObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (smsObserver != null) {
            getContentResolver().unregisterContentObserver(smsObserver);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CONTACT_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchSmsMessages();
            }
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchSmsMessages();
                registerSmsObserver();
            }
        }
    }
}
